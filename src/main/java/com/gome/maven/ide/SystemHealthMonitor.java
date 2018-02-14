/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.ide;

import com.gome.maven.concurrency.JobScheduler;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.notification.*;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.popup.Balloon;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.ui.HyperlinkAdapter;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.SystemProperties;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystemHealthMonitor extends ApplicationComponent.Adapter {
    private static final Logger LOG = Logger.getInstance("#com.intellij.ide.SystemHealthMonitor");

    private static final NotificationGroup GROUP = new NotificationGroup("System Health", NotificationDisplayType.STICKY_BALLOON, false);
    private static final NotificationGroup LOG_GROUP = NotificationGroup.logOnlyGroup("System Health (minor)");

     private final PropertiesComponent myProperties;

    public SystemHealthMonitor( PropertiesComponent properties) {
        myProperties = properties;
    }

    @Override
    public void initComponent() {
        checkJvm();
        startDiskSpaceMonitoring();
    }

    private void checkJvm() {
        if (StringUtil.containsIgnoreCase(System.getProperty("java.vm.name", ""), "OpenJDK") && !SystemInfo.isJavaVersionAtLeast("1.7")) {
            notifyUnsupportedJvm("unsupported.jvm.openjdk.message");
        }
        else if (StringUtil.endsWithIgnoreCase(System.getProperty("java.version", ""), "-ea")) {
            notifyUnsupportedJvm("unsupported.jvm.ea.message");
        }
    }

    private void notifyUnsupportedJvm( final String key) {
        final String ignoreKey = "ignore." + key;
        final String message = IdeBundle.message(key) + IdeBundle.message("unsupported.jvm.link");
        showNotification(ignoreKey, message, new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                myProperties.setValue(ignoreKey, "true");
            }
        });
    }

    private void showNotification(final String ignoreKey, final String message, final HyperlinkAdapter hyperlinkAdapter) {
        if (myProperties.isValueSet(ignoreKey)) {
            return;
        }

        final Application app = ApplicationManager.getApplication();
        app.getMessageBus().connect(app).subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener.Adapter() {
            @Override
            public void appFrameCreated(String[] commandLineArgs,  Ref<Boolean> willOpenProject) {
                app.invokeLater(new Runnable() {
                    public void run() {
                        JComponent component = WindowManager.getInstance().findVisibleFrame().getRootPane();
                        if (component != null) {
                            Rectangle rect = component.getVisibleRect();
                            JBPopupFactory.getInstance()
                                    .createHtmlTextBalloonBuilder(message, MessageType.WARNING, hyperlinkAdapter)
                                    .setFadeoutTime(-1)
                                    .setHideOnFrameResize(false)
                                    .setHideOnLinkClick(true)
                                    .setDisposable(app)
                                    .createBalloon()
                                    .show(new RelativePoint(component, new Point(rect.x + 30, rect.y + rect.height - 10)), Balloon.Position.above);
                        }

                        Notification notification = LOG_GROUP.createNotification(message, NotificationType.WARNING);
                        notification.setImportant(true);
                        Notifications.Bus.notify(notification);
                    }
                });
            }
        });
    }

    private static void startDiskSpaceMonitoring() {
        if (SystemProperties.getBooleanProperty("idea.no.system.path.space.monitoring", false)) {
            return;
        }

        final File file = new File(PathManager.getSystemPath());
        final AtomicBoolean reported = new AtomicBoolean();
        final ThreadLocal<Future<Long>> ourFreeSpaceCalculation = new ThreadLocal<Future<Long>>();

        JobScheduler.getScheduler().schedule(new Runnable() {
            private static final long LOW_DISK_SPACE_THRESHOLD = 50 * 1024 * 1024;
            private static final long MAX_WRITE_SPEED_IN_BPS = 500 * 1024 * 1024;  // 500 MB/sec is near max SSD sequential write speed

            @Override
            public void run() {
                if (!reported.get()) {
                    Future<Long> future = ourFreeSpaceCalculation.get();
                    if (future == null) {
                        ourFreeSpaceCalculation.set(future = ApplicationManager.getApplication().executeOnPooledThread(new Callable<Long>() {
                            @Override
                            public Long call() throws Exception {
                                // file.getUsableSpace() can fail and return 0 e.g. after MacOSX restart or awakening from sleep
                                // so several times try to recalculate usable space on receiving 0 to be sure
                                long fileUsableSpace = file.getUsableSpace();
                                while(fileUsableSpace == 0) {
                                    Thread.sleep(5000); // hopefully we will not hummer disk too much
                                    fileUsableSpace = file.getUsableSpace();
                                }

                                return fileUsableSpace;
                            }
                        }));
                    }
                    if (!future.isDone() || future.isCancelled()) {
                        JobScheduler.getScheduler().schedule(this, 1, TimeUnit.SECONDS);
                        return;
                    }

                    try {
                        final long fileUsableSpace = future.get();
                        final long timeout = Math.max(5, (fileUsableSpace - LOW_DISK_SPACE_THRESHOLD) / MAX_WRITE_SPEED_IN_BPS);
                        ourFreeSpaceCalculation.set(null);

                        if (fileUsableSpace < LOW_DISK_SPACE_THRESHOLD) {
                            if (!notificationsComponentIsLoaded()) {
                                ourFreeSpaceCalculation.set(future);
                                JobScheduler.getScheduler().schedule(this, 1, TimeUnit.SECONDS);
                                return;
                            }
                            reported.compareAndSet(false, true);

                            //noinspection SSBasedInspection
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    String productName = ApplicationNamesInfo.getInstance().getFullProductName();
                                    String message = IdeBundle.message("low.disk.space.message", productName);
                                    if (fileUsableSpace < 100 * 1024) {
                                        LOG.warn(message);
                                        Messages.showErrorDialog(message, "Fatal Configuration Problem");
                                        reported.compareAndSet(true, false);
                                        restart(timeout);
                                    }
                                    else {
                                        GROUP.createNotification(message, file.getPath(), NotificationType.ERROR, null).whenExpired(new Runnable() {
                                            @Override
                                            public void run() {
                                                reported.compareAndSet(true, false);
                                                restart(timeout);
                                            }
                                        }).notify(null);
                                    }
                                }
                            });
                        }
                        else {
                            restart(timeout);
                        }
                    }
                    catch (Exception ex) {
                        LOG.error(ex);
                    }
                }
            }

            private boolean notificationsComponentIsLoaded() {
                return ApplicationManager.getApplication().runReadAction(new Computable<NotificationsConfiguration>() {
                    @Override
                    public NotificationsConfiguration compute() {
                        return NotificationsConfiguration.getNotificationsConfiguration();
                    }
                }) != null;
            }

            private void restart(long timeout) {
                JobScheduler.getScheduler().schedule(this, timeout, TimeUnit.SECONDS);
            }
        }, 1, TimeUnit.SECONDS);
    }

}
