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

package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Query;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

public abstract class DirectoryIndex {
    public static DirectoryIndex getInstance(Project project) {
        assert !project.isDefault() : "Must not call DirectoryIndex for default project";
        return ServiceManager.getService(project, DirectoryIndex.class);
    }

    /**
     * The same as {@link #getInfoForFile} but works only for directories or file roots and returns {@code null} for directories
     * which aren't included in project content or libraries
     * @deprecated use {@link #getInfoForFile(com.gome.maven.openapi.vfs.VirtualFile)} instead
     */
    @Deprecated
    public abstract DirectoryInfo getInfoForDirectory( VirtualFile dir);

    
    public abstract DirectoryInfo getInfoForFile( VirtualFile file);

    
    public abstract JpsModuleSourceRootType<?> getSourceRootType( DirectoryInfo info);

    
    public abstract
    Query<VirtualFile> getDirectoriesByPackageName( String packageName, boolean includeLibrarySources);

    
    public abstract String getPackageName( VirtualFile dir);

    /**
     * @return true
     */
    @Deprecated
    public boolean isInitialized() {
        return true;
    }

    
    public abstract OrderEntry[] getOrderEntries( DirectoryInfo info);
}
