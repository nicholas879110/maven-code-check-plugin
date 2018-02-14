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
package com.gome.maven.internal.statistic;

import com.gome.maven.featureStatistics.FeatureUsageTracker;
import com.gome.maven.featureStatistics.FeatureUsageTrackerImpl;
import com.gome.maven.internal.statistic.beans.ConvertUsagesUtil;
import com.gome.maven.internal.statistic.beans.GroupDescriptor;
import com.gome.maven.internal.statistic.beans.UsageDescriptor;
import com.gome.maven.internal.statistic.connect.RemotelyConfigurableStatisticsService;
import com.gome.maven.internal.statistic.connect.StatisticsConnectionService;
import com.gome.maven.internal.statistic.connect.StatisticsHttpClientSender;
import com.gome.maven.internal.statistic.connect.StatisticsService;
import com.gome.maven.internal.statistic.persistence.SentUsagesPersistence;
import com.gome.maven.internal.statistic.persistence.UsageStatisticsPersistenceComponent;
import com.gome.maven.openapi.application.impl.ApplicationInfoImpl;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.KeyedExtensionCollector;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.util.Time;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StatisticsUploadAssistant {
    private static final Logger LOG = Logger.getInstance(StatisticsUploadAssistant.class);

    public String getData() {
        return getData(Collections.<String>emptySet());
    }

    public static boolean showNotification() {
        return UsageStatisticsPersistenceComponent.getInstance().isShowNotification() &&
                (System.currentTimeMillis() - Time.DAY > ((FeatureUsageTrackerImpl)FeatureUsageTracker.getInstance()).getFirstRunTime());
    }

    public static boolean isTimeToSend() {
        return isTimeToSend(UsageStatisticsPersistenceComponent.getInstance());
    }

    public static boolean isTimeToSend(UsageStatisticsPersistenceComponent settings) {
        final long timeDelta = System.currentTimeMillis() - settings.getLastTimeSent();

        return Math.abs(timeDelta) > settings.getPeriod().getMillis();
    }

    public static boolean isSendAllowed() {
        return isSendAllowed(UsageStatisticsPersistenceComponent.getInstance());
    }

    public static boolean isSendAllowed(final SentUsagesPersistence settings) {
        return settings != null && settings.isAllowed();
    }

    public String getData( Set<String> disabledGroups) {
        return getDataString(disabledGroups);
    }

    public static void updateSentTime() {
        UsageStatisticsPersistenceComponent.getInstance().setSentTime(System.currentTimeMillis());
    }

    
    public static String getDataString( Set<String> disabledGroups) {
        return getDataString(disabledGroups, 0);
    }

    
    public static String getDataString( Set<String> disabledGroups,
                                       int maxSize) {
        return getDataString(getAllUsages(disabledGroups), maxSize);
    }

    public static <T extends UsageDescriptor> String getDataString( Map<GroupDescriptor, Set<T>> usages, int maxSize) {
        if (usages.isEmpty()) {
            return "";
        }

        String dataStr = ConvertUsagesUtil.convertUsages(usages);
        return maxSize > 0 && dataStr.getBytes(CharsetToolkit.UTF8_CHARSET).length > maxSize ? ConvertUsagesUtil.cutDataString(dataStr, maxSize) : dataStr;
    }

    
    public static Map<GroupDescriptor, Set<UsageDescriptor>> getAllUsages( Set<String> disabledGroups) {
        Map<GroupDescriptor, Set<UsageDescriptor>> usageDescriptors = new LinkedHashMap<GroupDescriptor, Set<UsageDescriptor>>();
        for (UsagesCollector usagesCollector : UsagesCollector.EP_NAME.getExtensions()) {
            GroupDescriptor groupDescriptor = usagesCollector.getGroupId();
            if (!disabledGroups.contains(groupDescriptor.getId())) {
                try {
                    usageDescriptors.put(groupDescriptor, usagesCollector.getUsages());
                }
                catch (CollectUsagesException e) {
                    LOG.info(e);
                }
            }
        }
        return usageDescriptors;
    }

    private static final KeyedExtensionCollector<StatisticsService, String> COLLECTOR;

    static {
        COLLECTOR = new KeyedExtensionCollector<StatisticsService, String>("com.intellij.statisticsService");
    }

    public static StatisticsService getStatisticsService() {
        String key = ((ApplicationInfoImpl)ApplicationInfoImpl.getShadowInstance()).getStatisticsServiceKey();
        StatisticsService service = key == null ? null : COLLECTOR.findSingle(key);
        if (service != null) {
            return service;
        }

        return new RemotelyConfigurableStatisticsService(new StatisticsConnectionService(),
                new StatisticsHttpClientSender(),
                new StatisticsUploadAssistant());
    }
}
