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
package com.gome.maven.openapi.vcs;

import com.gome.maven.notification.*;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.changes.ui.ChangesViewContentManager;

public class VcsNotifier {

    public static final NotificationGroup NOTIFICATION_GROUP_ID = NotificationGroup.toolWindowGroup(
            "Vcs Messages", ChangesViewContentManager.TOOLWINDOW_ID);
    public static final NotificationGroup IMPORTANT_ERROR_NOTIFICATION = new NotificationGroup(
            "Vcs Important Messages", NotificationDisplayType.STICKY_BALLOON, true);
    public static final NotificationGroup MINOR_NOTIFICATION = new NotificationGroup(
            "Vcs Minor Notifications", NotificationDisplayType.BALLOON, true);
    public static final NotificationGroup SILENT_NOTIFICATION = new NotificationGroup(
            "Vcs Silent Notifications", NotificationDisplayType.NONE, true);

    private final  Project myProject;


    public static VcsNotifier getInstance( Project project) {
        return ServiceManager.getService(project, VcsNotifier.class);
    }

    public VcsNotifier( Project project) {
        myProject = project;
    }

    
    private static Notification createNotification( NotificationGroup notificationGroup,
                                                    String title,  String message,  NotificationType type,
                                                    NotificationListener listener) {
        // title can be empty; message can't be neither null, nor empty
        if (StringUtil.isEmptyOrSpaces(message)) {
            message = title;
            title = "";
        }
        // if both title and message were empty, then it is a problem in the calling code => Notifications engine assertion will notify.
        return notificationGroup.createNotification(title, message, type, listener);
    }

    
    protected Notification notify( NotificationGroup notificationGroup,  String title,  String message,
                                   NotificationType type,  NotificationListener listener) {
        Notification notification = createNotification(notificationGroup, title, message, type, listener);
        notification.notify(myProject);
        return notification;
    }

    
    public Notification notifyError( String title,  String message) {
        return notifyError(title, message, null);
    }

    
    public Notification notifyError( String title,  String message,  NotificationListener listener) {
        return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.ERROR, listener);
    }

    
    public Notification notifyWeakError( String message) {
        return notify(NOTIFICATION_GROUP_ID, "", message, NotificationType.ERROR, null);
    }

    
    public Notification notifySuccess( String message) {
        return notifySuccess("", message);
    }

    
    public Notification notifySuccess( String title,  String message) {
        return notifySuccess(title, message, null);
    }

    
    public Notification notifySuccess( String title,  String message,  NotificationListener listener) {
        return notify(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION, listener);
    }

    
    public Notification notifyImportantInfo( String title,  String message) {
        return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.INFORMATION, null);
    }

    
    public Notification notifyImportantInfo( String title,  String message,  NotificationListener listener) {
        return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.INFORMATION, listener);
    }

    
    public Notification notifyInfo( String message) {
        return notifyInfo("", message);
    }

    
    public Notification notifyInfo( String title,  String message) {
        return notifyInfo(title, message, null);
    }

    
    public Notification notifyInfo( String title,  String message,  NotificationListener listener) {
        return notify(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION, listener);
    }

    
    public Notification notifyMinorWarning( String title,  String message) {
        return notifyMinorWarning(title, message, null);
    }

    
    public Notification notifyMinorWarning( String title,  String message,  NotificationListener listener) {
        return notify(MINOR_NOTIFICATION, title, message, NotificationType.WARNING, listener);
    }

    
    public Notification notifyWarning( String title,  String message) {
        return notifyWarning(title, message, null);
    }

    
    public Notification notifyWarning( String title,  String message,  NotificationListener listener) {
        return notify(NOTIFICATION_GROUP_ID, title, message, NotificationType.WARNING, listener);
    }

    
    public Notification notifyImportantWarning( String title,  String message) {
        return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.WARNING, null);
    }

    
    public Notification notifyImportantWarning( String title,  String message,  NotificationListener listener) {
        return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.WARNING, listener);
    }

    
    public Notification notifyMinorInfo( String title,  String message) {
        return notifyMinorInfo(title, message, null);
    }

    
    public Notification notifyMinorInfo( String title,  String message,  NotificationListener listener) {
        return notify(MINOR_NOTIFICATION, title, message, NotificationType.INFORMATION, listener);
    }

    public Notification logInfo( String title,  String message) {
        return notify(SILENT_NOTIFICATION, title, message, NotificationType.INFORMATION, null);
    }
}
