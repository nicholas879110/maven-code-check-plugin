/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.ide;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.io.FileUtil;

import java.awt.event.InputEvent;
import java.io.File;

/**
 * @author yole
 */
public class ReopenProjectAction extends AnAction implements DumbAware {
    private final String myProjectPath;
    private final String myProjectName;

    public ReopenProjectAction(final String projectPath, final String projectName, final String displayName) {
        myProjectPath = projectPath;
        myProjectName = projectName;

        final Presentation presentation = getTemplatePresentation();
        String text = projectPath.equals(displayName) ? FileUtil.getLocationRelativeToUserHome(projectPath) : displayName;
        presentation.setText(text, false);
        presentation.setDescription(projectPath);
    }


    @Override
    public void actionPerformed(AnActionEvent e) {
        final int modifiers = e.getModifiers();
        final boolean forceOpenInNewFrame = (modifiers & InputEvent.CTRL_MASK) != 0 || (modifiers & InputEvent.SHIFT_MASK) != 0;
        Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (!new File(myProjectPath).exists()) {
            if (Messages.showDialog(project, "The path " + FileUtil.toSystemDependentName(myProjectPath) + " does not exist.\n" +
                            "If it is on a removable or network drive, please make sure that the drive is connected.",
                    "Reopen Project", new String[]{"OK", "&Remove From List"}, 0, Messages.getErrorIcon()) == 1) {
                RecentProjectsManager.getInstance().removePath(myProjectPath);
            }
            return;
        }
        RecentProjectsManagerBase.getInstanceEx().doOpenProject(myProjectPath, project, forceOpenInNewFrame);
    }

    public String getProjectPath() {
        return myProjectPath;
    }

    public String getProjectName() {
        return myProjectName;
    }
}
