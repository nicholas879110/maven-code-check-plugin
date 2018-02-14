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
package com.gome.maven.notification.impl;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.ui.UISettings;
import com.gome.maven.ide.ui.UISettingsListener;
import com.gome.maven.notification.EventLog;
import com.gome.maven.notification.LogModel;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.wm.CustomStatusBarWidget;
import com.gome.maven.openapi.wm.IconLikeCustomStatusBarWidget;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.ui.ClickListener;
import com.gome.maven.ui.LayeredIcon;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author spleaner
 */
public class IdeNotificationArea extends JLabel implements CustomStatusBarWidget, IconLikeCustomStatusBarWidget {
    public static final String WIDGET_ID = "Notifications";
    private StatusBar myStatusBar;

    public IdeNotificationArea() {
        UISettings.getInstance().addUISettingsListener(new UISettingsListener() {
            @Override
            public void uiSettingsChanged(UISettings source) {
                updateStatus();
            }
        }, this);
        new ClickListener() {
            @Override
            public boolean onClick( MouseEvent e, int clickCount) {
                EventLog.toggleLog(getProject(), null);
                return true;
            }
        }.installOn(this);

        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(LogModel.LOG_MODEL_CHANGED, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus();
                    }
                });
            }
        });
    }

    public WidgetPresentation getPresentation( PlatformType type) {
        return null;
    }

    public void dispose() {
    }

    public void install( StatusBar statusBar) {
        myStatusBar = statusBar;
        updateStatus();
    }

    
    private Project getProject() {
        return CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext((Component) myStatusBar));
    }

    
    public String ID() {
        return WIDGET_ID;
    }

    private void updateStatus() {
        final Project project = getProject();
        ArrayList<Notification> notifications = EventLog.getLogModel(project).getNotifications();
        applyIconToStatusAndToolWindow(project, createIconWithNotificationCount(notifications));

        int count = notifications.size();
        setToolTipText(count > 0 ? String.format("%s notification%s pending", count, count == 1 ? "" : "s") : "No new notifications");

        myStatusBar.updateWidget(ID());
    }

    private void applyIconToStatusAndToolWindow(Project project, LayeredIcon icon) {
        if (UISettings.getInstance().HIDE_TOOL_STRIPES || UISettings.getInstance().PRESENTATION_MODE) {
            setVisible(true);
            setIcon(icon);
        }
        else {
            ToolWindow eventLog = EventLog.getEventLog(project);
            if (eventLog != null) {
                eventLog.setIcon(icon);
            }
            setVisible(false);
        }
    }

    private LayeredIcon createIconWithNotificationCount(ArrayList<Notification> notifications) {
        LayeredIcon icon = new LayeredIcon(2);
        Icon statusIcon = getPendingNotificationsIcon(AllIcons.Ide.Notifications, getMaximumType(notifications));
        icon.setIcon(statusIcon, 0);
        if (notifications.size() > 0) {
            icon.setIcon(new TextIcon(this, String.valueOf(notifications.size())), 1, statusIcon.getIconWidth() - 2, 0);
        }
        return icon;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    private static Icon getPendingNotificationsIcon(Icon defIcon, final NotificationType maximumType) {
        if (maximumType != null) {
            switch (maximumType) {
                case WARNING: return AllIcons.Ide.Warning_notifications;
                case ERROR: return AllIcons.Ide.Error_notifications;
                case INFORMATION: return AllIcons.Ide.Info_notifications;
            }
        }
        return defIcon;
    }

    
    private static NotificationType getMaximumType(List<Notification> notifications) {
        NotificationType result = null;
        for (Notification notification : notifications) {
            if (NotificationType.ERROR == notification.getType()) {
                return NotificationType.ERROR;
            }

            if (NotificationType.WARNING == notification.getType()) {
                result = NotificationType.WARNING;
            }
            else if (result == null && NotificationType.INFORMATION == notification.getType()) {
                result = NotificationType.INFORMATION;
            }
        }

        return result;
    }

    private static class TextIcon implements Icon {
        private final String myStr;
        private final JComponent myComponent;
        private final int myWidth;

        public TextIcon(JComponent component,  String str) {
            myStr = str;
            myComponent = component;
            myWidth = myComponent.getFontMetrics(calcFont()).stringWidth(myStr) + 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TextIcon)) return false;

            TextIcon icon = (TextIcon)o;

            if (myWidth != icon.myWidth) return false;
            if (!myComponent.equals(icon.myComponent)) return false;
            if (!myStr.equals(icon.myStr)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = myStr.hashCode();
            result = 31 * result + myComponent.hashCode();
            result = 31 * result + myWidth;
            return result;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            UIUtil.applyRenderingHints(g);

            Font originalFont = g.getFont();
            Color originalColor = g.getColor();
            g.setFont(calcFont());
            y += getIconHeight() - g.getFontMetrics().getDescent();

            g.setColor(UIUtil.getLabelForeground());
            g.drawString(myStr, x, y);

            g.setFont(originalFont);
            g.setColor(originalColor);
        }

        private Font calcFont() {
            return myComponent.getFont().deriveFont(Font.BOLD).deriveFont((float) getIconHeight() * 3 / 5);
        }

        @Override
        public int getIconWidth() {
            return myWidth;
        }

        @Override
        public int getIconHeight() {
            return AllIcons.Ide.Notifications.getIconHeight();
        }
    }
}
