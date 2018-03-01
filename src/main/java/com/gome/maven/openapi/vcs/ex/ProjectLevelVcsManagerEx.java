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
package com.gome.maven.openapi.vcs.ex;

import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.update.ActionInfo;
import com.gome.maven.openapi.vcs.update.UpdateInfoTree;
import com.gome.maven.openapi.vcs.update.UpdatedFiles;
import com.gome.maven.ui.content.ContentManager;

import java.util.List;

public abstract class ProjectLevelVcsManagerEx extends ProjectLevelVcsManager {
    public static ProjectLevelVcsManagerEx getInstanceEx(Project project) {
        return (ProjectLevelVcsManagerEx) PeriodicalTasksCloser.getInstance().safeGetComponent(project, ProjectLevelVcsManager.class);
    }

    public abstract ContentManager getContentManager();

    
    public abstract VcsShowSettingOption getOptions(VcsConfiguration.StandardOption option);

    
    public abstract VcsShowConfirmationOptionImpl getConfirmation(VcsConfiguration.StandardConfirmation option);

    public abstract List<VcsShowOptionsSettingImpl> getAllOptions();

    public abstract List<VcsShowConfirmationOptionImpl> getAllConfirmations();

    public abstract void notifyDirectoryMappingChanged();

    public abstract UpdateInfoTree showUpdateProjectInfo(UpdatedFiles updatedFiles,
                                                         String displayActionName,
                                                         ActionInfo actionInfo,
                                                         boolean canceled);

    public abstract void fireDirectoryMappingsChanged();

    public abstract String haveDefaultMapping();
}
