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

package com.gome.maven.application.options.colors;

import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.util.scopeChooser.EditScopesDialog;
import com.gome.maven.ide.util.scopeChooser.ScopeChooserConfigurable;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.options.ex.Settings;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ScopeColorsPageFactory implements ColorAndFontPanelFactory {
    
    @Override
    public NewColorAndFontPanel createPanel( ColorAndFontOptions options) {
        final JPanel scopePanel = createChooseScopePanel();
        return NewColorAndFontPanel.create(new PreviewPanel.Empty(){
            @Override
            public Component getPanel() {
                return scopePanel;
            }

        }, ColorAndFontOptions.SCOPES_GROUP, options, null, null);
    }

    
    @Override
    public String getPanelDisplayName() {
        return ColorAndFontOptions.SCOPES_GROUP;
    }

    private static JPanel createChooseScopePanel() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        JPanel panel = new JPanel(new GridBagLayout());
        //panel.setBorder(new LineBorder(Color.red));
        if (projects.length == 0) return panel;
        GridBagConstraints gc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0);
        final Project contextProject = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext());
        final Project project = contextProject != null ? contextProject : projects[0];

        JButton button = new JButton("Manage Scopes...");
        button.setPreferredSize(new Dimension(230, button.getPreferredSize().height));
        panel.add(button, gc);
        gc.gridx = GridBagConstraints.REMAINDER;
        gc.weightx = 1;
        panel.add(new JPanel(), gc);

        gc.gridy++;
        gc.gridx=0;
        gc.weighty = 1;
        panel.add(new JPanel(), gc);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e) {
                Settings settings = Settings.KEY.getData(DataManager.getInstance().getDataContext());
                if (settings != null) {
                    try {
                        if (settings.select(settings.find(ScopeChooserConfigurable.PROJECT_SCOPES)).isRejected()) {
                            EditScopesDialog.showDialog(project, null);
                        }
                    } catch (IllegalStateException ex) {
                        EditScopesDialog.showDialog(project, null);
                    }
                }
            }
        });
        return panel;
    }
}
