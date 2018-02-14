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

import com.gome.maven.ide.impl.ProjectUtil;
import com.gome.maven.openapi.components.RoamingType;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.messages.MessageBus;

@State(
        name = "RecentProjectsManager",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/recentProjects.xml", roamingType = RoamingType.DISABLED),
                @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true)
        }
)
public class RecentProjectsManagerImpl extends RecentProjectsManagerBase {
    public RecentProjectsManagerImpl(MessageBus messageBus) {
        super(messageBus);
    }

    @Override
    protected String getProjectPath( Project project) {
        return project.getPresentableUrl();
    }

    @Override
    protected void doOpenProject( String projectPath, Project projectToClose, boolean forceOpenInNewFrame) {
        ProjectUtil.openProject(projectPath, projectToClose, forceOpenInNewFrame);
    }
}
