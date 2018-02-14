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
package com.gome.maven.internal.statistic.persistence;

import com.gome.maven.ide.AppLifecycleListener;
import com.gome.maven.internal.statistic.AbstractApplicationUsagesCollector;
import com.gome.maven.internal.statistic.UsagesCollector;
import com.gome.maven.internal.statistic.beans.GroupDescriptor;
import com.gome.maven.internal.statistic.beans.UsageDescriptor;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.project.ProjectManagerAdapter;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.Function;
import gnu.trove.THashSet;
import org.jdom.Element;

import java.util.List;
import java.util.Map;
import java.util.Set;

@State(
        name = "StatisticsApplicationUsages",
        storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/statistics.application.usages.xml", roamingType = RoamingType.DISABLED)}
)
public class ApplicationStatisticsPersistenceComponent extends ApplicationStatisticsPersistence
        implements ApplicationComponent, PersistentStateComponent<Element> {
    private boolean persistOnClosing = !ApplicationManager.getApplication().isUnitTestMode();

    private final Alarm myAlarm;
    private final long PERSIST_PERIOD = 24*60*60*1000; //1 day

    private static final String TOKENIZER = ",";

    
    private static final String GROUP_TAG = "group";
    
    private static final String GROUP_NAME_ATTR = "name";

    
    private static final String PROJECT_TAG = "project";
    
    private static final String COLLECTION_TIME_TAG = "collectionTime";
    
    private static final String PROJECT_ID_ATTR = "id";
    
    private static final String VALUES_ATTR = "values";

    public ApplicationStatisticsPersistenceComponent() {
        myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, ApplicationManager.getApplication());
    }

    public static ApplicationStatisticsPersistenceComponent getInstance() {
        return ApplicationManager.getApplication().getComponent(ApplicationStatisticsPersistenceComponent.class);
    }

    @Override
    public void loadState(Element element) {
        for (Element groupElement : (List<Element>)element.getChildren(GROUP_TAG)) {
            GroupDescriptor groupDescriptor = GroupDescriptor.create(groupElement.getAttributeValue(GROUP_NAME_ATTR));
            List<Element> projectsList = groupElement.getChildren(PROJECT_TAG);
            for (Element projectElement : projectsList) {
                String projectId = projectElement.getAttributeValue(PROJECT_ID_ATTR);
                String frameworks = projectElement.getAttributeValue(VALUES_ATTR);
                if (!StringUtil.isEmptyOrSpaces(projectId) && !StringUtil.isEmptyOrSpaces(frameworks)) {
                    Set<UsageDescriptor> frameworkDescriptors = new THashSet<UsageDescriptor>();
                    for (String key : StringUtil.split(frameworks, TOKENIZER)) {
                        UsageDescriptor descriptor = getUsageDescriptor(key);
                        if (descriptor != null) {
                            frameworkDescriptors.add(descriptor);
                        }
                    }
                    long collectionTime;
                    try {
                        collectionTime = Long.valueOf(projectElement.getAttributeValue(COLLECTION_TIME_TAG));
                    } catch (NumberFormatException ignored) {
                        collectionTime = 0;
                    }
                    getApplicationData(groupDescriptor).put(projectId, new CollectedUsages(frameworkDescriptors, collectionTime));
                }
            }
        }
    }

    @Override
    public Element getState() {
        Element element = new Element("state");

        for (Map.Entry<GroupDescriptor, Map<String, CollectedUsages>> appData : getApplicationData().entrySet()) {
            Element groupElement = new Element(GROUP_TAG);
            groupElement.setAttribute(GROUP_NAME_ATTR, appData.getKey().getId());
            boolean isEmptyGroup = true;

            for (Map.Entry<String, CollectedUsages> projectData : appData.getValue().entrySet()) {
                Element projectElement = new Element(PROJECT_TAG);
                projectElement.setAttribute(PROJECT_ID_ATTR, projectData.getKey());
                final CollectedUsages projectDataValue = projectData.getValue();
                if (!projectDataValue.usages.isEmpty()) {
                    projectElement.setAttribute(VALUES_ATTR, joinUsages(projectDataValue.usages));
                    projectElement.setAttribute(COLLECTION_TIME_TAG, String.valueOf(projectDataValue.collectionTime));
                    groupElement.addContent(projectElement);
                    isEmptyGroup = false;
                }
            }

            if (!isEmptyGroup) {
                element.addContent(groupElement);
            }
        }

        return element;
    }

    private static UsageDescriptor getUsageDescriptor(String usage) {
        // for instance, usage can be: "_foo"(equals "_foo=1") or "_foo=2"
        try {
            final int i = usage.indexOf('=');
            if (i > 0 && i < usage.length() - 1) {
                String key = usage.substring(0, i).trim();
                String value = usage.substring(i + 1).trim();
                if (!StringUtil.isEmptyOrSpaces(key) && !StringUtil.isEmptyOrSpaces(value)) {
                    try {
                        final int count = Integer.parseInt(value);
                        if (count > 0) {
                            return new UsageDescriptor(key, count);
                        }
                    }
                    catch (NumberFormatException ignored) {
                    }
                }
            }
            return new UsageDescriptor(usage, 1);
        }
        catch (AssertionError e) {
            //escape loading of invalid usages
        }
        return null;
    }

    private static String joinUsages( Set<UsageDescriptor> usages) {
        // for instance, usage can be: "_foo"(equals "_foo=1") or "_foo=2"
        return StringUtil.join(usages, new Function<UsageDescriptor, String>() {
            @Override
            public String fun(UsageDescriptor usageDescriptor) {
                final String key = usageDescriptor.getKey();
                final int value = usageDescriptor.getValue();
                return value > 1 ? key + "=" + value : key;
            }
        }, TOKENIZER);
    }

    @Override
    
    
    public String getComponentName() {
        return "ApplicationStatisticsPersistenceComponent";
    }

    @Override
    public void initComponent() {
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener.Adapter() {
            @Override
            public void appClosing() {
                persistOpenedProjects();
                persistOnClosing = false;
            }
        });

        ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
            @Override
            public void projectClosing(Project project) {
                if (persistOnClosing && project != null) {
                    doPersistProjectUsages(project);
                }
            }
        });

        persistPeriodically();
    }

    private void persistPeriodically() {
        myAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                persistOpenedProjects();
                persistPeriodically();
            }
        }, PERSIST_PERIOD);
    }

    private static void persistOpenedProjects() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            doPersistProjectUsages(project);
        }
    }

    private static void doPersistProjectUsages( Project project) {
        synchronized (ApplicationStatisticsPersistenceComponent.class) {
            if (!project.isInitialized() || DumbService.isDumb(project)) {
                return;
            }

            for (UsagesCollector usagesCollector : Extensions.getExtensions(UsagesCollector.EP_NAME)) {
                if (usagesCollector instanceof AbstractApplicationUsagesCollector) {
                    ((AbstractApplicationUsagesCollector)usagesCollector).persistProjectUsages(project);
                }
            }
        }
    }

    @Override
    public void disposeComponent() {
    }
}
