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

package com.gome.maven.codeInspection.ex;

import com.gome.maven.CommonBundle;
import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInspection.*;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiFile;

import javax.swing.*;
import java.io.IOException;

public class DisableInspectionToolAction extends IntentionAndQuickFixAction implements Iconable {
    private final String myToolId;
    public static final String NAME = InspectionsBundle.message("disable.inspection.action.name");

    public DisableInspectionToolAction(LocalInspectionTool tool) {
        myToolId = tool.getShortName();
    }

    public DisableInspectionToolAction(final HighlightDisplayKey key) {
        myToolId = key.toString();
    }

    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    
    public String getFamilyName() {
        return NAME;
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(project);
        InspectionProfile inspectionProfile = profileManager.getInspectionProfile();
        InspectionToolWrapper toolWrapper = inspectionProfile.getInspectionTool(myToolId, project);
        return toolWrapper == null || toolWrapper.getDefaultLevel() != HighlightDisplayLevel.NON_SWITCHABLE_ERROR;
    }

    @Override
    public void applyFix( Project project, PsiFile file,  Editor editor) {
        InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(project);
        InspectionProfile inspectionProfile = profileManager.getInspectionProfile();
        ModifiableModel model = inspectionProfile.getModifiableModel();
        model.disableTool(myToolId, file);
        try {
            model.commit();
        }
        catch (IOException e) {
            Messages.showErrorDialog(project, e.getMessage(), CommonBundle.getErrorTitle());
        }
        DaemonCodeAnalyzer.getInstance(project).restart();
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public Icon getIcon(int flags) {
        return AllIcons.Actions.Cancel;
    }
}
