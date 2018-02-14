/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.ide.actions;

import com.gome.maven.ide.projectView.impl.ProjectRootsUtil;
import com.gome.maven.openapi.actionSystem.ActionPlaces;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * @author Konstantin Bulenkov
 */
public class OpenModuleSettingsAction extends EditSourceAction {
    @Override
    public void update(AnActionEvent event) {
        super.update(event);
        if (!isModuleInProjectViewPopup(event)) {
            event.getPresentation().setEnabledAndVisible(false);
        }
    }

    protected static boolean isModuleInProjectViewPopup(AnActionEvent e) {
        if (ActionPlaces.PROJECT_VIEW_POPUP.equals(e.getPlace())) {
            final Project project = getEventProject(e);
            final Module module = LangDataKeys.MODULE.getData(e.getDataContext());
            if (project != null && module != null) {
                final VirtualFile moduleFolder = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
                if (moduleFolder == null) {
                    return false;
                }
                if (ProjectRootsUtil.isModuleContentRoot(moduleFolder, project) || ProjectRootsUtil.isModuleSourceRoot(moduleFolder, project)) {
                    return true;
                }
            }
        }
        return false;
    }
}