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
package com.gome.maven.openapi.project.ex;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.vfs.VirtualFile;
import org.jdom.JDOMException;


import java.io.IOException;
import java.util.Collection;

public abstract class ProjectManagerEx extends ProjectManager {
    public static ProjectManagerEx getInstanceEx() {
        return (ProjectManagerEx)ApplicationManager.getApplication().getComponent(ProjectManager.class);
    }

    /**
     * @param filePath path to .ipr file or directory where .idea directory is located
     */
    
    public abstract Project newProject(final String projectName,  String filePath, boolean useDefaultProjectSettings, boolean isDummy);

    
    public abstract Project loadProject( String filePath) throws IOException, JDOMException, InvalidDataException;

    public abstract boolean openProject(Project project);

    public abstract boolean isProjectOpened(Project project);

    public abstract boolean canClose(Project project);

    public abstract void saveChangedProjectFile( VirtualFile file,  Project project);

    public abstract void blockReloadingProjectOnExternalChanges();
    public abstract void unblockReloadingProjectOnExternalChanges();

    public abstract void openTestProject( Project project);

    // returns remaining open test projects
    public abstract Collection<Project> closeTestProject( Project project);

    // returns true on success
    public abstract boolean closeAndDispose( Project project);

    
    @Override
    public Project createProject(String name, String path) {
        return newProject(name, path, true, false);
    }

    
    public abstract Project convertAndLoadProject(String filePath) throws IOException;
}
