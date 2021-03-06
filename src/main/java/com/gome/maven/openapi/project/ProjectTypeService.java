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
package com.gome.maven.openapi.project;

import com.gome.maven.openapi.components.*;

/**
 * @author Dmitry Avdeev
 */

@State(
        name = "ProjectType",
        storages = {
                @Storage(
                        id = "other",
                        file = StoragePathMacros.PROJECT_FILE
                )
        }
)
public class ProjectTypeService implements PersistentStateComponent<ProjectType> {

    private ProjectType myProjectType;

    
    public static ProjectType getProjectType( Project project) {
        ProjectType projectType;
        if (project != null) {
            projectType = getInstance(project).myProjectType;
            if (projectType != null) return projectType;
        }
        return DefaultProjectTypeEP.getDefaultProjectType();
    }

    public static void setProjectType( Project project,  ProjectType projectType) {
        getInstance(project).loadState(projectType);
    }

    private static ProjectTypeService getInstance( Project project) {
        return ServiceManager.getService(project, ProjectTypeService.class);
    }

    
    @Override
    public ProjectType getState() {
        return myProjectType;
    }

    @Override
    public void loadState(ProjectType state) {
        myProjectType = state;
    }
}
