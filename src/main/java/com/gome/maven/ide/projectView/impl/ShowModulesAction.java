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
package com.gome.maven.ide.projectView.impl;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.projectView.ProjectView;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.actionSystem.ToggleAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.util.PlatformUtils;

/**
 * @author anna
 * @since 8/5/11
 */
public abstract class ShowModulesAction extends ToggleAction {
    private final Project myProject;

    public ShowModulesAction(Project project) {
        super(IdeBundle.message("action.show.modules"), IdeBundle.message("action.description.show.modules"),
                AllIcons.ObjectBrowser.ShowModules);
        myProject = project;
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
        return ProjectView.getInstance(myProject).isShowModules(getId());
    }

    
    protected abstract String getId();

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
        final ProjectViewImpl projectView = (ProjectViewImpl)ProjectView.getInstance(myProject);
        projectView.setShowModules(flag, getId());
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        final Presentation presentation = e.getPresentation();
        final ProjectViewImpl projectView = (ProjectViewImpl)ProjectView.getInstance(myProject);
        presentation.setVisible(hasModules() && Comparing.strEqual(projectView.getCurrentViewId(), getId()));
    }

    private static boolean hasModules() {
        return PlatformUtils.isIntelliJ();
    }
}
