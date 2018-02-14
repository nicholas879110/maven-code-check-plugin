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


import com.gome.maven.plugin.code.pmd.util.HighlightDisplayLevel;
import com.gome.maven.plugin.code.pmd.util.Intrinsics;

import java.io.File;

/**
 * @author caikang
 * @date 2017/02/28
 */
public class DelegatePmdInspection extends LocalInspectionTool implements AliBaseInspection, PmdRuleInspectionIdentify {

    private String ruleName;
    private AliPmdInspection aliPmdInspection;

    public boolean runForWholeFile() {
        return this.aliPmdInspection.runForWholeFile();
    }

    public ProblemDescriptor[] checkFile(File file, boolean isOnTheFly) {
        return this.aliPmdInspection.checkFile(file, isOnTheFly);
    }

    public String getStaticDescription() {
        return this.aliPmdInspection.getStaticDescription();
    }


    public String ruleName() {
        String var10000 = this.ruleName;
        if (this.ruleName == null) {
            Intrinsics.throwNpe();
        }

        return var10000;
    }

    public String getDisplayName() {
        return this.aliPmdInspection.getDisplayName();
    }


    public HighlightDisplayLevel getDefaultLevel() {
        return this.aliPmdInspection.getDefaultLevel();
    }

    public String getGroupDisplayName() {
        return this.aliPmdInspection.getGroupDisplayName();
    }

    public boolean isEnabledByDefault() {
        return this.aliPmdInspection.isEnabledByDefault();
    }


    public String getShortName() {
        return this.aliPmdInspection.getShortName();
    }


    public DelegatePmdInspection() {
        if (this.ruleName == null) {
            Intrinsics.throwNpe();
        }
        AliPmdInspection var10001 = new AliPmdInspection(this.ruleName);
        this.aliPmdInspection = var10001;
    }

}
