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
package com.gome.maven.find.findUsages;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.usages.ConfigurableUsageTarget;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

public class UsageHistory {
    // the last element is the most recent
    private final List<ConfigurableUsageTarget> myHistory = ContainerUtil.createLockFreeCopyOnWriteList();

    public void add( ConfigurableUsageTarget usageTarget) {
        final String descriptiveName = usageTarget.getLongDescriptiveName();
        ContainerUtil.retainAll(myHistory, new Condition<ConfigurableUsageTarget>() {
            @Override
            public boolean value(ConfigurableUsageTarget existing) {
                return !existing.getLongDescriptiveName().equals(descriptiveName);
            }
        });
        myHistory.add(usageTarget);

        // todo configure history depth limit
        if (myHistory.size() > 15) {
            myHistory.remove(0);
        }
    }

    
    public List<ConfigurableUsageTarget> getAll() {
        removeInvalidElementsFromHistory();
        return Collections.unmodifiableList(myHistory);
    }

    private void removeInvalidElementsFromHistory() {
        for (ConfigurableUsageTarget target : myHistory) {
            if (!target.isValid()) myHistory.remove(target);
        }
    }

}
