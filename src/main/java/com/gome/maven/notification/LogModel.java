/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.notification;

import com.gome.maven.notification.impl.NotificationsConfigurationImpl;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Trinity;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.Topic;
import com.gome.maven.util.ui.UIUtil;
import gnu.trove.THashMap;

import java.util.*;

/**
 * @author peter
 */
public class LogModel implements Disposable {
    public static final Topic<Runnable> LOG_MODEL_CHANGED = Topic.create("LOG_MODEL_CHANGED", Runnable.class, Topic.BroadcastDirection.NONE);

    private final List<Notification> myNotifications = new ArrayList<Notification>();
    private final Map<Notification, Long> myStamps = Collections.synchronizedMap(new WeakHashMap<Notification, Long>());
    private final Map<Notification, String> myStatuses = Collections.synchronizedMap(new WeakHashMap<Notification, String>());
    private Trinity<Notification, String, Long> myStatusMessage;
    private final Project myProject;
    final Map<Notification, Runnable> removeHandlers = new THashMap<Notification, Runnable>();

    LogModel( Project project,  Disposable parentDisposable) {
        myProject = project;
        Disposer.register(parentDisposable, this);
    }

    void addNotification(Notification notification) {
        long stamp = System.currentTimeMillis();
        NotificationDisplayType type = NotificationsConfigurationImpl.getSettings(notification.getGroupId()).getDisplayType();
        if (notification.isImportant() || (type != NotificationDisplayType.NONE && type != NotificationDisplayType.TOOL_WINDOW)) {
            synchronized (myNotifications) {
                myNotifications.add(notification);
            }
        }
        myStamps.put(notification, stamp);
        myStatuses.put(notification, EventLog.formatForLog(notification, "").status);
        setStatusMessage(notification, stamp);
        fireModelChanged();
    }

    private static void fireModelChanged() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(LOG_MODEL_CHANGED).run();
    }

    List<Notification> takeNotifications() {
        final ArrayList<Notification> result;
        synchronized (myNotifications) {
            result = getNotifications();
            myNotifications.clear();
        }
        fireModelChanged();
        return result;
    }

    void setStatusMessage( Notification statusMessage, long stamp) {
        synchronized (myNotifications) {
            if (myStatusMessage != null && myStatusMessage.first == statusMessage) return;
            if (myStatusMessage == null && statusMessage == null) return;

            myStatusMessage = statusMessage == null ? null : Trinity.create(statusMessage, myStatuses.get(statusMessage), stamp);
        }
        StatusBar.Info.set("", myProject, EventLog.LOG_REQUESTOR);
    }

    
    Trinity<Notification, String, Long> getStatusMessage() {
        synchronized (myNotifications) {
            return myStatusMessage;
        }
    }

    void logShown() {
        for (Notification notification : getNotifications()) {
            if (!notification.isImportant()) {
                removeNotification(notification);
            }
        }
        setStatusToImportant();
    }

    public ArrayList<Notification> getNotifications() {
        synchronized (myNotifications) {
            return new ArrayList<Notification>(myNotifications);
        }
    }

    
    public Long getNotificationTime(Notification notification) {
        return myStamps.get(notification);
    }

    public void removeNotification(Notification notification) {
        synchronized (myNotifications) {
            myNotifications.remove(notification);
        }

        Runnable handler = removeHandlers.remove(notification);
        if (handler != null) {
            UIUtil.invokeLaterIfNeeded(handler);
        }

        Trinity<Notification, String, Long> oldStatus = getStatusMessage();
        if (oldStatus != null && notification == oldStatus.first) {
            setStatusToImportant();
        }
        fireModelChanged();
    }

    private void setStatusToImportant() {
        ArrayList<Notification> notifications = getNotifications();
        Collections.reverse(notifications);
        Notification message = ContainerUtil.find(notifications, new Condition<Notification>() {
            @Override
            public boolean value(Notification notification) {
                return notification.isImportant();
            }
        });
        if (message == null) {
            setStatusMessage(null, 0);
        }
        else {
            Long notificationTime = getNotificationTime(message);
            assert notificationTime != null;
            setStatusMessage(message, notificationTime);
        }
    }

    public Project getProject() {
        //noinspection ConstantConditions
        return myProject;
    }

    @Override
    public void dispose() {
    }
}
