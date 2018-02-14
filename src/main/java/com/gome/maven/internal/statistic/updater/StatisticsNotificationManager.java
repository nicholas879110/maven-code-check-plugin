package com.gome.maven.internal.statistic.updater;

import com.gome.maven.internal.statistic.configurable.StatisticsConfigurable;
import com.gome.maven.internal.statistic.connect.StatisticsService;
import com.gome.maven.internal.statistic.persistence.UsageStatisticsPersistenceComponent;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.notification.Notifications;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.wm.IdeFrame;
import com.gome.maven.openapi.wm.ex.WindowManagerEx;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

public class StatisticsNotificationManager {

    public static final String GROUP_DISPLAY_ID = "IDE Usage Statistics";

    private StatisticsNotificationManager() {
    }

    public static void showNotification( StatisticsService statisticsService) {
        MyNotificationListener listener =
                new MyNotificationListener(statisticsService, UsageStatisticsPersistenceComponent.getInstance());

        Notifications.Bus.notify(statisticsService.createNotification(GROUP_DISPLAY_ID, listener));
    }

    private static class MyNotificationListener implements NotificationListener {
        private StatisticsService myStatisticsService;
        private final UsageStatisticsPersistenceComponent mySettings;

        public MyNotificationListener( StatisticsService statisticsService,
                                       UsageStatisticsPersistenceComponent settings) {
            myStatisticsService = statisticsService;
            mySettings = settings;
        }

        @Override
        public void hyperlinkUpdate( Notification notification,  HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                final String description = event.getDescription();
                if ("allow".equals(description)) {
                    mySettings.setAllowed(true);
                    mySettings.setShowNotification(false);
                    notification.expire();

                    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                        @Override
                        public void run() {
                            myStatisticsService.send();
                        }
                    });
                }
                else if ("decline".equals(description)) {
                    mySettings.setAllowed(false);
                    mySettings.setShowNotification(false);
                    notification.expire();
                }
                else if ("settings".equals(description)) {
                    final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
                    IdeFrame ideFrame = WindowManagerEx.getInstanceEx().findFrameFor(null);
                    util.editConfigurable((JFrame)ideFrame, new StatisticsConfigurable(true));
                    notification.expire();
                }
            }
        }
    }
}
