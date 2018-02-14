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
package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.analysis.AnalysisScope;
import com.gome.maven.analysis.AnalysisScopeBundle;
import com.gome.maven.analysis.AnalysisUIOptions;
import com.gome.maven.analysis.BaseAnalysisActionDialog;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.actions.CleanupIntention;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;

/**
 * Created by anna on 5/13/2014.
 */
class CleanupOnScopeIntention extends CleanupIntention {
    static final CleanupOnScopeIntention INSTANCE = new CleanupOnScopeIntention();

    private CleanupOnScopeIntention() {}

    
    @Override
    protected AnalysisScope getScope(final Project project, final PsiFile file) {
        final Module module = ModuleUtilCore.findModuleForPsiElement(file);
        AnalysisScope analysisScope = new AnalysisScope(file);
        final VirtualFile virtualFile = file.getVirtualFile();
        if (file.isPhysical() || virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            analysisScope = new AnalysisScope(project);
        }
        final BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(
                AnalysisScopeBundle.message("specify.analysis.scope", InspectionsBundle.message("inspection.action.title")),
                AnalysisScopeBundle.message("analysis.scope.title", InspectionsBundle.message("inspection.action.noun")),
                project,
                analysisScope,
                module != null ? module.getName() : null,
                true, AnalysisUIOptions.getInstance(project), file);
        if (!dlg.showAndGet()) {
            return null;
        }
        final AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
        return dlg.getScope(uiOptions, analysisScope, project, module);
    }
}
