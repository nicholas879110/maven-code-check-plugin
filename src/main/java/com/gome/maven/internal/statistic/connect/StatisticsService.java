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
package com.gome.maven.internal.statistic.connect;


import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.openapi.extensions.ExtensionPointName;

import java.util.Map;

public interface StatisticsService {

    ExtensionPointName<StatisticsServiceEP> EP_NAME = ExtensionPointName.create("com.gome.maven.statisticsService");

     String TITLE = "title";
     String DETAILS = "details";
     String ALLOW_CHECKBOX = "allow-checkbox";

    StatisticsResult send();

    Notification createNotification( String groupDisplayId,  NotificationListener listener);

    /**
     * Returns the custom text to be displayed in the statistics dialog. The keys in the map are control IDs ({@link #TITLE}, {@link #DETAILS}
     * and {@link #ALLOW_CHECKBOX} and the values are the text to be displayed in the dialog.
     *
     * @return the text override map, or null if the standard text should be used.
     */
    
    Map<String, String> getStatisticsConfigurationLabels();
}
