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

import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInsight.intention.HighPriorityAction;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.InspectionProfile;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.LocalInspectionTool;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.profile.codeInspection.InspectionProfileManager;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.profile.codeInspection.ui.ErrorsConfigurable;
import com.gome.maven.profile.codeInspection.ui.IDEInspectionToolsConfigurable;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

import javax.swing.*;

/**
 * User: anna
 * Date: Feb 7, 2005
 */
public class EditInspectionToolsSettingsAction implements IntentionAction, Iconable, HighPriorityAction {
    private final String myShortName;

    public EditInspectionToolsSettingsAction( LocalInspectionTool tool) {
        myShortName = tool.getShortName();
    }

    public EditInspectionToolsSettingsAction( HighlightDisplayKey key) {
        myShortName = key.toString();
    }

    @Override
    
    public String getText() {
        return InspectionsBundle.message("edit.options.of.reporter.inspection.text");
    }

    @Override
    
    public String getFamilyName() {
        return InspectionsBundle.message("edit.options.of.reporter.inspection.family");
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(file.getProject());
        InspectionProfile inspectionProfile = projectProfileManager.getInspectionProfile();
        editToolSettings(project,
                inspectionProfile, true,
                myShortName);
    }

    public boolean editToolSettings(final Project project,
                                    final InspectionProfileImpl inspectionProfile,
                                    final boolean canChooseDifferentProfiles) {
        return editToolSettings(project,
                inspectionProfile,
                canChooseDifferentProfiles,
                myShortName);
    }

    public static boolean editToolSettings(final Project project,
                                           final InspectionProfile inspectionProfile,
                                           final boolean canChooseDifferentProfile,
                                           final String selectedToolShortName) {
        final ShowSettingsUtil settingsUtil = ShowSettingsUtil.getInstance();
        final ErrorsConfigurable errorsConfigurable;
        if (!canChooseDifferentProfile) {
            errorsConfigurable = new IDEInspectionToolsConfigurable(InspectionProjectProfileManager.getInstance(project), InspectionProfileManager.getInstance());
        }
        else {
            errorsConfigurable = ErrorsConfigurable.SERVICE.createConfigurable(project);
        }
        return settingsUtil.editConfigurable(project, errorsConfigurable, new Runnable() {
            @Override
            public void run() {
                errorsConfigurable.selectProfile(inspectionProfile);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        errorsConfigurable.selectInspectionTool(selectedToolShortName);
                    }
                });
            }
        });

    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public Icon getIcon(int flags) {
        return AllIcons.General.Settings;
    }
}
