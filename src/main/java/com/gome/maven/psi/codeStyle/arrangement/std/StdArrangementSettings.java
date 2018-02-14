/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.psi.codeStyle.arrangement.std;

import com.gome.maven.psi.codeStyle.arrangement.ArrangementSettings;
import com.gome.maven.psi.codeStyle.arrangement.ArrangementUtil;
import com.gome.maven.psi.codeStyle.arrangement.group.ArrangementGroupingRule;
import com.gome.maven.psi.codeStyle.arrangement.match.ArrangementMatchRule;
import com.gome.maven.psi.codeStyle.arrangement.match.ArrangementSectionRule;
import com.gome.maven.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.*;

/**
 * @author Denis Zhdanov
 * @since 9/17/12 11:53 AM
 */
public class StdArrangementSettings implements ArrangementSettings {
     private final   List<ArrangementSectionRule> mySectionRules     = new ArrayList<ArrangementSectionRule>();
     private final   List<ArrangementGroupingRule> myGroupings       = new ArrayList<ArrangementGroupingRule>();

    // cached values
     protected final List<StdArrangementMatchRule> myRulesByPriority =
            Collections.synchronizedList(new ArrayList<StdArrangementMatchRule>());

    public StdArrangementSettings() {
    }

    @SuppressWarnings("unchecked")
    public StdArrangementSettings( List<ArrangementSectionRule> rules) {
        this(Collections.EMPTY_LIST, rules);
    }

    public StdArrangementSettings( List<ArrangementGroupingRule> groupingRules,
                                   List<ArrangementSectionRule> sectionRules) {
        myGroupings.addAll(groupingRules);
        mySectionRules.addAll(sectionRules);
    }

    public static StdArrangementSettings createByMatchRules( List<ArrangementGroupingRule> groupingRules,
                                                             List<StdArrangementMatchRule> matchRules) {
        final List<ArrangementSectionRule> sectionRules = new ArrayList<ArrangementSectionRule>();
        for (StdArrangementMatchRule rule : matchRules) {
            sectionRules.add(ArrangementSectionRule.create(rule));
        }
        return new StdArrangementSettings(groupingRules, sectionRules);
    }

    
    protected List<ArrangementGroupingRule> cloneGroupings() {
        final ArrayList<ArrangementGroupingRule> groupings = new ArrayList<ArrangementGroupingRule>();
        for (ArrangementGroupingRule grouping : myGroupings) {
            groupings.add(grouping.clone());
        }
        return groupings;
    }

    
    protected List<ArrangementSectionRule> cloneSectionRules() {
        final ArrayList<ArrangementSectionRule> rules = new ArrayList<ArrangementSectionRule>();
        for (ArrangementSectionRule rule : mySectionRules) {
            rules.add(rule.clone());
        }
        return rules;
    }

    
    @Override
    public ArrangementSettings clone() {
        return new StdArrangementSettings(cloneGroupings(), cloneSectionRules());
    }

    @Override
    
    public List<ArrangementGroupingRule> getGroupings() {
        return myGroupings;
    }

    
    @Override
    public List<ArrangementSectionRule> getSections() {
        return mySectionRules;
    }

    
    @Override
    public List<StdArrangementMatchRule> getRules() {
        return ArrangementUtil.collectMatchRules(mySectionRules);
    }

    
    @Override
    public List<? extends ArrangementMatchRule> getRulesSortedByPriority() {
        synchronized (myRulesByPriority) {
            if (myRulesByPriority.isEmpty()) {
                for (ArrangementSectionRule rule : mySectionRules) {
                    myRulesByPriority.addAll(rule.getMatchRules());
                }
                ContainerUtil.sort(myRulesByPriority);
            }
        }
        return myRulesByPriority;
    }

    public void addRule( StdArrangementMatchRule rule) {
        addSectionRule(rule);
        myRulesByPriority.clear();
    }

    public void addSectionRule( StdArrangementMatchRule rule) {
        mySectionRules.add(ArrangementSectionRule.create(rule));
    }

    public void addGrouping( ArrangementGroupingRule rule) {
        myGroupings.add(rule);
    }

    @Override
    public int hashCode() {
        int result = mySectionRules.hashCode();
        result = 31 * result + myGroupings.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StdArrangementSettings settings = (StdArrangementSettings)o;

        if (!myGroupings.equals(settings.myGroupings)) return false;
        if (!mySectionRules.equals(settings.mySectionRules)) return false;

        return true;
    }
}
