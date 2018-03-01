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

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Map;

/**
 * @author peter
 */
public final class NotificationGroup {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.notification.NotificationGroup");
    private static final Map<String, NotificationGroup> ourRegisteredGroups = ContainerUtil.newConcurrentMap();

     private final String myDisplayId;
     private final NotificationDisplayType myDisplayType;
    private final boolean myLogByDefault;
     private final String myToolWindowId;

    public NotificationGroup( String displayId,  NotificationDisplayType defaultDisplayType, boolean logByDefault) {
        this(displayId, defaultDisplayType, logByDefault, null);
    }

    public NotificationGroup( String displayId,
                              NotificationDisplayType defaultDisplayType,
                             boolean logByDefault,
                              String toolWindowId) {
        myDisplayId = displayId;
        myDisplayType = defaultDisplayType;
        myLogByDefault = logByDefault;
        myToolWindowId = toolWindowId;

        if (ourRegisteredGroups.containsKey(displayId)) {
            LOG.info("Notification group " + displayId + " is already registered", new Throwable());
        }
        ourRegisteredGroups.put(displayId, this);
    }

    
    public static NotificationGroup balloonGroup( String displayId) {
        return new NotificationGroup(displayId, NotificationDisplayType.BALLOON, true);
    }

    
    public static NotificationGroup logOnlyGroup( String displayId) {
        return new NotificationGroup(displayId, NotificationDisplayType.NONE, true);
    }

    
    public static NotificationGroup toolWindowGroup( String displayId,  String toolWindowId, final boolean logByDefault) {
        return new NotificationGroup(displayId, NotificationDisplayType.TOOL_WINDOW, logByDefault, toolWindowId);
    }

    
    public static NotificationGroup toolWindowGroup( String displayId,  String toolWindowId) {
        return toolWindowGroup(displayId, toolWindowId, true);
    }

    
    public String getDisplayId() {
        return myDisplayId;
    }

    public Notification createNotification( final String content,  final MessageType type) {
        return createNotification(content, type.toNotificationType());
    }

    
    public Notification createNotification( final String content,  final NotificationType type) {
        return createNotification("", content, type, null);
    }

    
    public Notification createNotification( final String title,
                                            final String content,
                                            final NotificationType type,
                                            NotificationListener listener) {
        return new Notification(myDisplayId, title, content, type, listener);
    }

    
    public NotificationDisplayType getDisplayType() {
        return myDisplayType;
    }

    public boolean isLogByDefault() {
        return myLogByDefault;
    }

    
    public String getToolWindowId() {
        return myToolWindowId;
    }

    
    public static NotificationGroup findRegisteredGroup(String displayId) {
        return ourRegisteredGroups.get(displayId);
    }

    
    public static Iterable<NotificationGroup> getAllRegisteredGroups() {
        return ourRegisteredGroups.values();
    }

}
