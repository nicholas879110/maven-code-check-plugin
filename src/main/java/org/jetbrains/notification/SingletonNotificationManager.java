package org.jetbrains.notification;

import com.gome.maven.notification.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.wm.ToolWindowManager;

import java.util.concurrent.atomic.AtomicReference;

public final class SingletonNotificationManager {
    private final AtomicReference<Notification> notification = new AtomicReference<Notification>();

    private final NotificationGroup group;
    private final NotificationType type;
    
    private final NotificationListener listener;

    private Runnable expiredListener;

    public SingletonNotificationManager( NotificationGroup group,  NotificationType type,  NotificationListener listener) {
        this.group = group;
        this.type = type;
        this.listener = listener;
    }

    public boolean notify( String title,  String content) {
        return notify(title, content, null);
    }

    public boolean notify( String title,  String content,  Project project) {
        return notify(title, content, listener, project);
    }

    public boolean notify( String content,  Project project) {
        return notify("", content, listener, project);
    }

    public boolean notify( String title,
                           String content,
                           NotificationListener listener,
                           Project project) {
        Notification oldNotification = notification.get();
        // !oldNotification.isExpired() is not enough - notification could be closed, but not expired
        if (oldNotification != null) {
            if (!oldNotification.isExpired() && (oldNotification.getBalloon() != null ||
                    (project != null &&
                            group.getDisplayType() == NotificationDisplayType.TOOL_WINDOW &&
                            ToolWindowManager.getInstance(project).getToolWindowBalloon(group.getToolWindowId()) != null))) {
                return false;
            }
            oldNotification.whenExpired(null);
            oldNotification.expire();
        }

        if (expiredListener == null) {
            expiredListener = new Runnable() {
                @Override
                public void run() {
                    Notification currentNotification = notification.get();
                    if (currentNotification != null && currentNotification.isExpired()) {
                        notification.compareAndSet(currentNotification, null);
                    }
                }
            };
        }

        Notification newNotification = group.createNotification(title, content, type, listener);
        newNotification.whenExpired(expiredListener);
        notification.set(newNotification);
        newNotification.notify(project);
        return true;
    }

    public void clear() {
        Notification oldNotification = notification.getAndSet(null);
        if (oldNotification != null) {
            oldNotification.whenExpired(null);
            oldNotification.expire();
        }
    }
}