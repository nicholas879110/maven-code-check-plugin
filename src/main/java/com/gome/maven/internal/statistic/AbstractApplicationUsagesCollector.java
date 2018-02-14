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

import com.gome.maven.internal.statistic.beans.UsageDescriptor;
import com.gome.maven.internal.statistic.persistence.ApplicationStatisticsPersistence;
import com.gome.maven.internal.statistic.persistence.ApplicationStatisticsPersistenceComponent;
import com.gome.maven.internal.statistic.persistence.CollectedUsages;
import com.gome.maven.internal.statistic.persistence.UsageStatisticsPersistenceComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.containers.ObjectIntHashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectIntProcedure;

import java.util.Collections;
import java.util.Set;

public abstract class AbstractApplicationUsagesCollector extends UsagesCollector {
    private static final Logger LOG = Logger.getInstance(AbstractApplicationUsagesCollector.class);

    public void persistProjectUsages( Project project) {
        try {
            persistProjectUsages(project, new CollectedUsages(getProjectUsages(project), System.currentTimeMillis()));
        }
        catch (CollectUsagesException e) {
            LOG.info(e);
        }
    }

    public void persistProjectUsages( Project project,  CollectedUsages usages) {
        persistProjectUsages(project, usages, ApplicationStatisticsPersistenceComponent.getInstance());
    }

    public void persistProjectUsages( Project project,
                                      CollectedUsages usages,
                                      ApplicationStatisticsPersistence persistence) {
        persistence.persistUsages(getGroupId(), project, usages);
    }

    
    public Set<UsageDescriptor> getApplicationUsages() {
        return getApplicationUsages(ApplicationStatisticsPersistenceComponent.getInstance());
    }

    
    public Set<UsageDescriptor> getApplicationUsages( ApplicationStatisticsPersistence persistence) {
        ObjectIntHashMap<String> result = new ObjectIntHashMap<String>();
        long lastTimeSent = UsageStatisticsPersistenceComponent.getInstance().getLastTimeSent();
        for (CollectedUsages usageDescriptors : persistence.getApplicationData(getGroupId()).values()) {
            if (!usageDescriptors.usages.isEmpty() && usageDescriptors.collectionTime > lastTimeSent) {
                result.ensureCapacity(usageDescriptors.usages.size());
                for (UsageDescriptor usageDescriptor : usageDescriptors.usages) {
                    String key = usageDescriptor.getKey();
                    result.put(key, result.get(key, 0) + usageDescriptor.getValue());
                }
            }
        }

        if (result.isEmpty()){
            return Collections.emptySet();
        }
        else {
            final THashSet<UsageDescriptor> descriptors = new THashSet<UsageDescriptor>(result.size());
            result.forEachEntry(new TObjectIntProcedure<String>() {
                @Override
                public boolean execute(String key, int value) {
                    descriptors.add(new UsageDescriptor(key, value));
                    return true;
                }
            });
            return descriptors;
        }
    }

    @Override
    
    public Set<UsageDescriptor> getUsages() throws CollectUsagesException {
        return getApplicationUsages();
    }

    
    public abstract Set<UsageDescriptor> getProjectUsages( Project project) throws CollectUsagesException;
}
