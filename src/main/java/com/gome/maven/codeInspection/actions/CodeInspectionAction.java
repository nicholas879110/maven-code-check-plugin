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
import com.gome.maven.analysis.BaseAnalysisAction;
import com.gome.maven.analysis.BaseAnalysisActionDialog;
import com.gome.maven.codeInspection.InspectionManager;
import com.gome.maven.codeInspection.InspectionProfile;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.ex.GlobalInspectionContextImpl;
import com.gome.maven.codeInspection.ex.InspectionManagerEx;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.options.ex.SingleConfigurableEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.profile.Profile;
import com.gome.maven.profile.ProfileManager;
import com.gome.maven.profile.codeInspection.InspectionProfileManager;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.profile.codeInspection.ui.ErrorsConfigurable;
import com.gome.maven.profile.codeInspection.ui.IDEInspectionToolsConfigurable;
import com.gome.maven.ui.ComboboxWithBrowseButton;
import com.gome.maven.ui.ListCellRendererWrapper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.TreeSet;

public class CodeInspectionAction extends BaseAnalysisAction {
    private GlobalInspectionContextImpl myGlobalInspectionContext = null;
    protected InspectionProfile myExternalProfile = null;

    public CodeInspectionAction() {
        super(InspectionsBundle.message("inspection.action.title"), InspectionsBundle.message("inspection.action.noun"));
    }

    public CodeInspectionAction(String title, String analysisNoon) {
        super(title, analysisNoon);
    }

    @Override
    protected void analyze( Project project,  AnalysisScope scope) {
        try {
            runInspections(project, scope);
        }
        finally {
            myGlobalInspectionContext = null;
            myExternalProfile = null;
        }
    }

    protected void runInspections(Project project, AnalysisScope scope) {
        scope.setSearchInLibraries(false);
        FileDocumentManager.getInstance().saveAllDocuments();
        final GlobalInspectionContextImpl inspectionContext = getGlobalInspectionContext(project);
        inspectionContext.setExternalProfile(myExternalProfile);
        inspectionContext.setCurrentScope(scope);
        inspectionContext.doInspections(scope);
    }


    private GlobalInspectionContextImpl getGlobalInspectionContext(Project project) {
        if (myGlobalInspectionContext == null) {
            myGlobalInspectionContext = ((InspectionManagerEx)InspectionManager.getInstance(project)).createNewGlobalContext(false);
        }
        return myGlobalInspectionContext;
    }

    @Override
    
    protected String getHelpTopic() {
        return "reference.dialogs.inspection.scope";
    }

    @Override
    protected void canceled() {
        super.canceled();
        myGlobalInspectionContext = null;
    }

    @Override
    protected JComponent getAdditionalActionSettings( final Project project, final BaseAnalysisActionDialog dialog) {
        final AdditionalPanel panel = new AdditionalPanel();
        final InspectionManagerEx manager = (InspectionManagerEx)InspectionManager.getInstance(project);
        final JComboBox profiles = panel.myBrowseProfilesCombo.getComboBox();
        profiles.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
                if (value instanceof Profile) {
                    Profile profile = (Profile)value;
                    setText(profile.getName());
                    setIcon(profile.isProjectLevel() ? AllIcons.General.ProjectSettings : AllIcons.General.Settings);
                }
            }
        });
        final InspectionProfileManager profileManager = InspectionProfileManager.getInstance();
        final InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(project);
        reloadProfiles(profiles, profileManager, projectProfileManager, manager);
        panel.myBrowseProfilesCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final IDEInspectionToolsConfigurable errorConfigurable = createConfigurable(projectProfileManager, profileManager);
                final MySingleConfigurableEditor editor = new MySingleConfigurableEditor(project, errorConfigurable, manager);
                errorConfigurable.selectProfile(((Profile)profiles.getSelectedItem()));
                if (editor.showAndGet()) {
                    reloadProfiles(profiles, profileManager, projectProfileManager, manager);
                }
                else {
                    //if profile was disabled and cancel after apply was pressed
                    final InspectionProfile profile = (InspectionProfile)profiles.getSelectedItem();
                    final boolean canExecute = profile != null && profile.isExecutable(project);
                    dialog.setOKActionEnabled(canExecute);
                }
            }
        });
        profiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myExternalProfile = (InspectionProfile)profiles.getSelectedItem();
                final boolean canExecute = myExternalProfile != null && myExternalProfile.isExecutable(project);
                dialog.setOKActionEnabled(canExecute);
                if (canExecute) {
                    manager.setProfile(myExternalProfile.getName());
                }
            }
        });
        final InspectionProfile profile = (InspectionProfile)profiles.getSelectedItem();
        dialog.setOKActionEnabled(profile != null && profile.isExecutable(project));
        return panel.myAdditionalPanel;
    }

    protected IDEInspectionToolsConfigurable createConfigurable(InspectionProjectProfileManager projectProfileManager,
                                                                InspectionProfileManager profileManager) {
        return new IDEInspectionToolsConfigurable(projectProfileManager, profileManager);
    }

    private void reloadProfiles(JComboBox profiles,
                                InspectionProfileManager inspectionProfileManager,
                                InspectionProjectProfileManager inspectionProjectProfileManager,
                                InspectionManagerEx inspectionManager) {
        final InspectionProfile selectedProfile = getGlobalInspectionContext(inspectionManager.getProject()).getCurrentProfile();
        final DefaultComboBoxModel model = (DefaultComboBoxModel)profiles.getModel();
        model.removeAllElements();
        fillModel(inspectionProfileManager, model);
        fillModel(inspectionProjectProfileManager, model);
        profiles.setSelectedItem(selectedProfile);
    }

    private static void fillModel(final ProfileManager inspectionProfileManager, final DefaultComboBoxModel model) {
        Collection<Profile> profiles = new TreeSet<Profile>(inspectionProfileManager.getProfiles());
        for (Profile profile : profiles) {
            model.addElement(profile);
        }
    }


    private static class AdditionalPanel {
        public ComboboxWithBrowseButton myBrowseProfilesCombo;
        public JPanel myAdditionalPanel;
    }

    private static class MySingleConfigurableEditor extends SingleConfigurableEditor {
        private final InspectionManagerEx myManager;

        public MySingleConfigurableEditor(final Project project, final ErrorsConfigurable configurable, InspectionManagerEx manager) {
            super(project, configurable, createDimensionKey(configurable));
            myManager = manager;
        }


        @Override
        protected void doOKAction() {
            final Object o = ((ErrorsConfigurable)getConfigurable()).getSelectedObject();
            if (o instanceof Profile) {
                myManager.setProfile(((Profile)o).getName());
            }
            super.doOKAction();
        }
    }
}
