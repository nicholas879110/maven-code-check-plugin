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

import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.actions.CodeCleanupAction;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.profile.codeInspection.InspectionProfileManager;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.profile.codeInspection.ui.ProjectInspectionToolsConfigurable;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

/**
 * Created by anna on 5/13/2014.
 */
class EditCleanupProfileIntentionAction implements IntentionAction {
    static final EditCleanupProfileIntentionAction INSTANCE = new EditCleanupProfileIntentionAction();
    private EditCleanupProfileIntentionAction() {}

    @Override
    
    public String getText() {
        return getFamilyName();
    }

    @Override
    
    public String getFamilyName() {
        return "Edit cleanup profile settings";
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(project);
        final ProjectInspectionToolsConfigurable configurable =
                new ProjectInspectionToolsConfigurable(InspectionProfileManager.getInstance(), profileManager) {
                    @Override
                    protected boolean acceptTool(InspectionToolWrapper entry) {
                        return super.acceptTool(entry) && entry.isCleanupTool();
                    }

                    @Override
                    public String getDisplayName() {
                        return CodeCleanupAction.CODE_CLEANUP_INSPECTIONS_DISPLAY_NAME;
                    }
                };
        ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
