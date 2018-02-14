/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.updateSettings.impl;

import com.gome.maven.ide.AppLifecycleListener;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationInfo;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.net.NetUtils;
import com.gome.maven.util.text.DateFormatUtil;

/**
 * @author yole
 */
public class UpdateCheckerComponent implements ApplicationComponent {
    private static final long CHECK_INTERVAL = DateFormatUtil.DAY;

    private final Alarm myCheckForUpdatesAlarm = new Alarm(Alarm.ThreadToUse.SHARED_THREAD);
    private final Runnable myCheckRunnable = new Runnable() {
        @Override
        public void run() {
            UpdateChecker.updateAndShowResult().doWhenDone(new Runnable() {
                @Override
                public void run() {
                    queueNextCheck(CHECK_INTERVAL);
                }
            });
        }
    };
    private final UpdateSettings mySettings;

    public UpdateCheckerComponent( Application app,  UpdateSettings settings) {
        mySettings = settings;

        if (mySettings.isSecureConnection() && !NetUtils.isSniEnabled()) {
            app.invokeLater(new Runnable() {
                @Override
                public void run() {
                    String title = IdeBundle.message("update.notifications.title");
                    boolean tooOld = !SystemInfo.isJavaVersionAtLeast("1.7");
                    String message = IdeBundle.message(tooOld ? "update.sni.not.available.notification" : "update.sni.disabled.notification");
                    UpdateChecker.NOTIFICATIONS.createNotification(title, message, NotificationType.ERROR, null).notify(null);
                }
            }, ModalityState.NON_MODAL);
        }

        scheduleOnStartCheck(app);
    }

    @Override
    public void initComponent() {
        PluginsAdvertiser.ensureDeleted();
    }

    private void scheduleOnStartCheck( Application app) {
        if (!mySettings.isCheckNeeded() || mySettings.isSecureConnection() && !NetUtils.isSniEnabled()) {
            return;
        }

        app.getMessageBus().connect(app).subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener.Adapter() {
            @Override
            public void appFrameCreated(String[] commandLineArgs,  Ref<Boolean> willOpenProject) {
                String currentBuild = ApplicationInfo.getInstance().getBuild().asString();
                long timeToNextCheck = mySettings.getLastTimeChecked() + CHECK_INTERVAL - System.currentTimeMillis();

                if (StringUtil.compareVersionNumbers(mySettings.getLasBuildChecked(), currentBuild) < 0 || timeToNextCheck <= 0) {
                    myCheckRunnable.run();
                }
                else {
                    queueNextCheck(timeToNextCheck);
                }
            }
        });
    }

    private void queueNextCheck(long interval) {
        myCheckForUpdatesAlarm.addRequest(myCheckRunnable, interval);
    }

    @Override
    public void disposeComponent() {
        Disposer.dispose(myCheckForUpdatesAlarm);
    }

    
    @Override
    public String getComponentName() {
        return "UpdateCheckerComponent";
    }

    public void queueNextCheck() {
        queueNextCheck(CHECK_INTERVAL);
    }

    public void cancelChecks() {
        myCheckForUpdatesAlarm.cancelAllRequests();
    }
}
