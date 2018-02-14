/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.usages.impl;

import com.gome.maven.usages.Usage;
import com.gome.maven.usages.UsageGroup;
import com.gome.maven.usages.UsageTarget;
import com.gome.maven.usages.rules.UsageFilteringRule;
import com.gome.maven.usages.rules.UsageFilteringRuleEx;
import com.gome.maven.usages.rules.UsageGroupingRule;
import com.gome.maven.usages.rules.UsageGroupingRuleEx;
import com.gome.maven.util.Consumer;

/**
 * @author max
 */
class UsageNodeTreeBuilder {
    private final GroupNode myRoot;
    private final UsageTarget[] myTargets;
    private UsageGroupingRule[] myGroupingRules;
    private UsageFilteringRule[] myFilteringRules;

    UsageNodeTreeBuilder( UsageTarget[] targets,
                          UsageGroupingRule[] groupingRules,
                          UsageFilteringRule[] filteringRules,
                          GroupNode root) {
        myTargets = targets;
        myGroupingRules = groupingRules;
        myFilteringRules = filteringRules;
        myRoot = root;
    }

    public void setGroupingRules( UsageGroupingRule[] rules) {
        myGroupingRules = rules;
    }

    public void setFilteringRules( UsageFilteringRule[] rules) {
        myFilteringRules = rules;
    }

    public boolean isVisible( Usage usage) {
        for (final UsageFilteringRule rule : myFilteringRules) {
            final boolean visible;
            if (rule instanceof UsageFilteringRuleEx) {
                visible = ((UsageFilteringRuleEx) rule).isVisible(usage, myTargets);
            }
            else {
                visible = rule.isVisible(usage);
            }
            if (!visible) {
                return false;
            }
        }
        return true;
    }

    
    UsageNode appendUsage( Usage usage,  Consumer<Runnable> edtQueue) {
        if (!isVisible(usage)) return null;

        GroupNode lastGroupNode = myRoot;
        for (int i = 0; i < myGroupingRules.length; i++) {
            final UsageGroupingRule rule = myGroupingRules[i];
            final UsageGroup group;
            if (rule instanceof UsageGroupingRuleEx) {
                group = ((UsageGroupingRuleEx) rule).groupUsage(usage, myTargets);
            }
            else {
                group = rule.groupUsage(usage);
            }
            if (group != null) {
                lastGroupNode = lastGroupNode.addGroup(group, i, edtQueue);
            }
        }

        return lastGroupNode.addUsage(usage, edtQueue);
    }
}
