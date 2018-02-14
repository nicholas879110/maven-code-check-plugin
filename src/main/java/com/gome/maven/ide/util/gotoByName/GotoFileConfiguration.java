/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.ide.util.gotoByName;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;

/**
 * Configuration for file type filtering popup in "Go to | File" action.
 *
 * @author Constantine.Plotnikov
 */
@State(
        name = "GotoFileConfiguration",
        storages = {@Storage(
                file = StoragePathMacros.WORKSPACE_FILE)})
public class GotoFileConfiguration extends ChooseByNameFilterConfiguration<FileType> {
    /**
     * Get configuration instance
     *
     * @param project a project instance
     * @return a configuration instance
     */
    public static GotoFileConfiguration getInstance(Project project) {
        return ServiceManager.getService(project, GotoFileConfiguration.class);
    }

    @Override
    protected String nameForElement(FileType type) {
        return type.getName();
    }
}
