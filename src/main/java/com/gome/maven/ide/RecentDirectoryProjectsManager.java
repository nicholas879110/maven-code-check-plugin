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
package com.gome.maven.ide;

import com.gome.maven.openapi.components.RoamingType;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.platform.PlatformProjectOpenProcessor;
import com.gome.maven.platform.ProjectBaseDirectory;
import com.gome.maven.util.messages.MessageBus;

@State(
        name = "RecentDirectoryProjectsManager",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/recentProjectDirectories.xml", roamingType = RoamingType.DISABLED),
                @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true)
        }
)
public class RecentDirectoryProjectsManager extends RecentProjectsManagerBase {
    public RecentDirectoryProjectsManager(MessageBus messageBus) {
        super(messageBus);
    }

    @Override
    
    protected String getProjectPath( Project project) {
        final ProjectBaseDirectory baseDir = ProjectBaseDirectory.getInstance(project);
        final VirtualFile baseDirVFile = baseDir.getBaseDir() != null ? baseDir.getBaseDir() : project.getBaseDir();
        return baseDirVFile != null ? FileUtil.toSystemDependentName(baseDirVFile.getPath()) : null;
    }

    @Override
    protected void doOpenProject( String projectPath, Project projectToClose, boolean forceOpenInNewFrame) {
        final VirtualFile projectDir = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(projectPath));
        if (projectDir != null) {
            PlatformProjectOpenProcessor.doOpenProject(projectDir, projectToClose, forceOpenInNewFrame, -1, null, true);
        }
    }
}
