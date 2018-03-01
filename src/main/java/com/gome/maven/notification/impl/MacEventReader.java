/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.notification.impl;

import com.gome.maven.notification.Notification;
import com.gome.maven.notification.Notifications;
import com.gome.maven.notification.NotificationsAdapter;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.AbstractProjectComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.text.StringUtil;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MacEventReader {
    private static final int MAX_MESSAGE_LENGTH = 100;
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.notification.impl.MacEventReader");
    private static final NotificationsAdapter ourNotificationAdapter = new NotificationsAdapter() {
        @Override
        public void notify( Notification notification) {
            process(notification);
        }
    };

    private static ExecutorService ourService = null;

    MacEventReader() {
        if (SystemInfo.isMac) {
            ApplicationManager.getApplication().getMessageBus().connect().subscribe(Notifications.TOPIC, ourNotificationAdapter);
        }
    }

    private static void process(Notification notification) {
        if (!NotificationsConfigurationImpl.getSettings(notification.getGroupId()).isShouldReadAloud()) {
            return;
        }
        String message = notification.getTitle();
        if (message.isEmpty()) {
            message = notification.getContent();
        }
        message = StringUtil.stripHtml(message, false);
        if (message.length() > MAX_MESSAGE_LENGTH) {
            String[] words = message.split("\\s");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (sb.length() + word.length() >= MAX_MESSAGE_LENGTH - 1) break;
                if (sb.length() > 0) sb.append(' ');
                sb.append(word);
            }
            message = sb.toString();
        }

        if (!message.isEmpty()) {
            final String copy = message;
            getService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Runtime.getRuntime().exec("say " + copy).waitFor();
                    }
                    catch (IOException e) {
                        LOG.warn(e);
                    }
                    catch (InterruptedException e) {
                        LOG.warn(e);
                    }
                }
            });
        }
    }

    private static synchronized ExecutorService getService() {
        if (ourService == null) {
            ourService = Executors.newSingleThreadExecutor();
        }
        return ourService;
    }

    public static class ProjectTracker extends AbstractProjectComponent {
        public ProjectTracker( final Project project) {
            super(project);
            project.getMessageBus().connect(project).subscribe(Notifications.TOPIC, ourNotificationAdapter);
        }
    }
}


