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
package com.gome.maven.notification;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.Balloon;
import com.gome.maven.openapi.ui.popup.JBPopupAdapter;
import com.gome.maven.openapi.ui.popup.LightweightWindowEvent;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.reference.SoftReference;

import javax.swing.*;
import java.lang.ref.WeakReference;

/**
 * @author spleaner
 */
public class Notification {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.notification.Notification");

    private final String myGroupId;
    private final String myContent;
    private final NotificationType myType;
    private final NotificationListener myListener;
    private final String myTitle;
    private boolean myExpired;
    private Runnable myWhenExpired;
    private Boolean myImportant;
    private WeakReference<Balloon> myBalloonRef;

    public Notification( String groupDisplayId,  String title,  String content,  NotificationType type) {
        this(groupDisplayId, title, content, type, null);
    }

    /**
     * @param groupDisplayId this should be a human-readable, capitalized string like "Facet Detector".
     *                       It will appear in "Notifications" configurable.
     * @param title          notification title
     * @param content        notification content
     * @param type           notification type
     * @param listener       notification lifecycle listener
     */
    public Notification( String groupDisplayId,
                         String title,
                         String content,
                         NotificationType type,
                         NotificationListener listener) {
        myGroupId = groupDisplayId;
        myTitle = title;
        myContent = content;
        myType = type;
        myListener = listener;

        LOG.assertTrue(!StringUtil.isEmptyOrSpaces(myContent), "Notification should have content, groupId: " + myGroupId);
    }

    @SuppressWarnings("MethodMayBeStatic")
    
    public Icon getIcon() {
        return null;
    }

    
    public String getGroupId() {
        return myGroupId;
    }

    
    public String getTitle() {
        return myTitle;
    }

    
    public String getContent() {
        return myContent;
    }

    
    public NotificationListener getListener() {
        return myListener;
    }

    
    public NotificationType getType() {
        return myType;
    }

    public boolean isExpired() {
        return myExpired;
    }

    public void expire() {
        NotificationsManager.getNotificationsManager().expire(this);
        hideBalloon();
        myExpired = true;

        Runnable whenExpired = myWhenExpired;
        if (whenExpired != null) whenExpired.run();
    }

    public Notification whenExpired( Runnable whenExpired) {
        myWhenExpired = whenExpired;
        return this;
    }

    public void hideBalloon() {
        if (myBalloonRef != null) {
            final Balloon balloon = myBalloonRef.get();
            if (balloon != null) {
                balloon.hide();
            }
            myBalloonRef = null;
        }
    }

    public void setBalloon( final Balloon balloon) {
        hideBalloon();
        myBalloonRef = new WeakReference<Balloon>(balloon);
        balloon.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                if (SoftReference.dereference(myBalloonRef) == balloon) {
                    myBalloonRef = null;
                }
            }
        });
    }

    
    public Balloon getBalloon() {
        return SoftReference.dereference(myBalloonRef);
    }

    public void notify( Project project) {
        Notifications.Bus.notify(this, project);
    }

    public Notification setImportant(boolean important) {
        myImportant = important;
        return this;
    }

    public boolean isImportant() {
        if (myImportant != null) {
            return myImportant;
        }

        return getListener() != null;
    }
}
