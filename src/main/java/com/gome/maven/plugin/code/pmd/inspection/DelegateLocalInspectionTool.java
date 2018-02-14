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
 * @date 2017/07/19
 */
public class DelegateLocalInspectionTool extends LocalInspectionTool implements AliBaseInspection {

    private LocalInspectionTool forJavassist;
    private LocalInspectionTool localInspectionTool;

    public boolean runForWholeFile() {
        return this.localInspectionTool.runForWholeFile();
    }

    public ProblemDescriptor[] checkFile( File file, boolean isOnTheFly) {
        Intrinsics.checkParameterIsNotNull(file, "file");
        return this.localInspectionTool.checkFile(file, isOnTheFly);
    }

    public String getStaticDescription() {
        return this.localInspectionTool.getStaticDescription();
    }

    public String ruleName() {
        LocalInspectionTool var10000 = this.localInspectionTool;
        if (this.localInspectionTool == null) {
            throw new ClassCastException("null cannot be cast to non-null type com.alibaba.p3c.idea.inspection.AliBaseInspection");
        } else {
            return ((AliBaseInspection) var10000).ruleName();
        }
    }


    public String getDisplayName() {
        String var10000 = this.localInspectionTool.getDisplayName();
        Intrinsics.checkExpressionValueIsNotNull(var10000, "localInspectionTool.displayName");
        return var10000;
    }


    public HighlightDisplayLevel getDefaultLevel() {
        HighlightDisplayLevel var10000 = this.localInspectionTool.getDefaultLevel();
        Intrinsics.checkExpressionValueIsNotNull(var10000, "localInspectionTool.defaultLevel");
        return var10000;
    }

    public String getGroupDisplayName() {
        return GROUP_NAME;
    }

    public boolean isEnabledByDefault() {
        return true;
    }


    public String getShortName() {
        String var10000 = this.localInspectionTool.getShortName();
        Intrinsics.checkExpressionValueIsNotNull(var10000, "localInspectionTool.shortName");
        return var10000;
    }



//    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
//        Intrinsics.checkParameterIsNotNull(holder, "holder");
//        Intrinsics.checkParameterIsNotNull(session, "session");
//        AliLocalInspectionToolProvider.ShouldInspectChecker var10000 = AliLocalInspectionToolProvider.javaShouldInspectChecker;
//        PsiFile var10001 = holder.getFile();
//        Intrinsics.checkExpressionValueIsNotNull(var10001, "holder.file");
//        PsiElementVisitor var4;
//        if (!var10000.shouldInspect(var10001)) {
//            var4 = PsiElementVisitor.EMPTY_VISITOR;
//            Intrinsics.checkExpressionValueIsNotNull(PsiElementVisitor.EMPTY_VISITOR, "PsiElementVisitor.EMPTY_VISITOR");
//            return var4;
//        } else {
//            var4 = this.localInspectionTool.buildVisitor(holder, isOnTheFly, session);
//            Intrinsics.checkExpressionValueIsNotNull(var4, "localInspectionTool.buil…der, isOnTheFly, session)");
//            return var4;
//        }
//    }

    public DelegateLocalInspectionTool() {
        LocalInspectionTool var10001 = this.forJavassist;
        if (this.forJavassist != null) {
            this.localInspectionTool = var10001;
        } else {
            throw new IllegalStateException();
        }
    }
}