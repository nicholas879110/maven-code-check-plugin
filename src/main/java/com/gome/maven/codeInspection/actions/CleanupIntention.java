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

package com.gome.maven.codeInspection.actions;

import com.gome.maven.analysis.AnalysisScope;
import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInsight.intention.LowPriorityAction;
import com.gome.maven.codeInspection.InspectionManager;
import com.gome.maven.codeInspection.InspectionProfile;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.ex.GlobalInspectionContextBase;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

public abstract class CleanupIntention implements IntentionAction, LowPriorityAction {

    protected CleanupIntention() {}

    @Override
    
    public String getText() {
        return getFamilyName();
    }

    @Override
    
    public String getFamilyName() {
        return InspectionsBundle.message("cleanup.in.scope");
    }

    @Override
    public void invoke( final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(file)) return;
        final InspectionManager managerEx = InspectionManager.getInstance(project);
        final GlobalInspectionContextBase globalContext = (GlobalInspectionContextBase)managerEx.createNewGlobalContext(false);
        final AnalysisScope scope = getScope(project, file);
        if (scope != null) {
            final InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
            globalContext.codeCleanup(project, scope, profile, getText(), null, false);
        }
    }

    
    protected abstract AnalysisScope getScope(Project project, PsiFile file);

    @Override
    public boolean isAvailable( final Project project, final Editor editor, final PsiFile file) {
        return true;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
