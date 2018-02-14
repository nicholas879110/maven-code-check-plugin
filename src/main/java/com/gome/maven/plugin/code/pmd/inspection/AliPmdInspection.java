/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.plugin.code.pmd.inspection;


import com.alibaba.p3c.pmd.lang.java.util.NumberConstants;
import com.gome.maven.plugin.code.pmd.util.HighlightDisplayLevel;
import net.sourceforge.pmd.Rule;

import java.io.File;

;

/**
 * @author caikang
 * @date 2016/12/16
 */
public class AliPmdInspection extends LocalInspectionTool implements AliBaseInspection, PmdRuleInspectionIdentify {


    private String ruleName;

    private String staticDescription;

    private String displayName;

    private ShouldInspectChecker shouldInspectChecker;

    private HighlightDisplayLevel defaultLevel;

    private Rule rule;

    public AliPmdInspection( String ruleName) {
        this.ruleName = ruleName;
        staticDescription = RuleInspectionUtils.getRuleStaticDescription(ruleName);
        RuleInfo ruleInfo = AliLocalInspectionToolProvider.getRuleInfoMap().get(ruleName);
        shouldInspectChecker = ruleInfo.getShouldInspectChecker();
        rule = ruleInfo.getRule();
        displayName = rule.getMessage();
        defaultLevel = RuleInspectionUtils.getHighlightDisplayLevel(rule.getPriority());
    }


    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @Override
    public ProblemDescriptor[] checkFile(File file, boolean isOnTheFly) {
        if (!shouldInspectChecker.shouldInspect(file)) {
            return null;
        }
        return AliPmdInspectionInvoker.invokeInspection(file, rule, isOnTheFly);
    }

    @Override
    public String getStaticDescription() {
        return staticDescription;
    }

    @Override
    public String ruleName() {
        return ruleName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel(){
        return defaultLevel;
    }

    @Override
    public String getGroupDisplayName() {
        return AliBaseInspection.GROUP_NAME;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }


    @Override
    public String getShortName() {
        String shortName = "Alibaba" + ruleName;
        int index = shortName.lastIndexOf("Rule");
        if (index > NumberConstants.INDEX_0) {
            shortName = shortName.substring(NumberConstants.INDEX_0, index);
        }
        return shortName;
    }
}
