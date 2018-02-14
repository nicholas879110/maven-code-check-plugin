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
package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.openapi.components.StorageScheme;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.project.impl.ProjectImpl;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * @see com.gome.maven.openapi.project.ex.ProjectEx#getStateStore()
 */
public interface IProjectStore extends IComponentStore.Reloadable {
    boolean checkVersion();

    void setProjectFilePath( String filePath);

    
    VirtualFile getProjectBaseDir();

    
    String getProjectBasePath();

    
    String getProjectName();

    TrackingPathMacroSubstitutor[] getSubstitutors();

    
    StorageScheme getStorageScheme();

    
    String getPresentableUrl();

    
    VirtualFile getProjectFile();

    
    VirtualFile getWorkspaceFile();

    void loadProjectFromTemplate( ProjectImpl project);

    
    String getProjectFilePath();
}
