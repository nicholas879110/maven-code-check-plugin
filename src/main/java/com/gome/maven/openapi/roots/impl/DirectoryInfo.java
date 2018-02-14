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

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.vfs.VirtualFile;

public abstract class DirectoryInfo {
    /**
     * @return {@code true} if located under project content or library roots and not excluded or ignored
     */
    public abstract boolean isInProject();

    /**
     * @return {@code true} if located under ignored directory
     */
    public abstract boolean isIgnored();

    /**
     * @return {@code true} if located project content, output or library root but excluded from the project
     */
    public abstract boolean isExcluded();

    public abstract boolean isInModuleSource();

    public abstract boolean isInLibrarySource();

    
    public abstract VirtualFile getSourceRoot();

    public abstract int getSourceRootTypeId();

    public boolean hasLibraryClassRoot() {
        return getLibraryClassRoot() != null;
    }

    public abstract VirtualFile getLibraryClassRoot();

    
    public abstract VirtualFile getContentRoot();

    
    public abstract Module getModule();
}
