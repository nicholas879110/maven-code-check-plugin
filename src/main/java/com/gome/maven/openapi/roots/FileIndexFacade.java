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
package com.gome.maven.openapi.roots;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ModificationTracker;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * @author yole
 */
public abstract class FileIndexFacade {
    protected final Project myProject;

    protected FileIndexFacade(final Project project) {
        myProject = project;
    }

    public static FileIndexFacade getInstance(Project project) {
        return ServiceManager.getService(project, FileIndexFacade.class);
    }

    public abstract boolean isInContent( VirtualFile file);
    public abstract boolean isInSource( VirtualFile file);
    public abstract boolean isInSourceContent( VirtualFile file);
    public abstract boolean isInLibraryClasses( VirtualFile file);

    public abstract boolean isInLibrarySource( VirtualFile file);
    public abstract boolean isExcludedFile( VirtualFile file);
    public abstract boolean isUnderIgnored( VirtualFile file);

    
    public abstract Module getModuleForFile( VirtualFile file);

    /**
     * Checks if <code>file</code> is an ancestor of <code>baseDir</code> and none of the files
     * between them are excluded from the project.
     *
     * @param baseDir the parent directory to check for ancestry.
     * @param child the child directory or file to check for ancestry.
     * @return true if it's a valid ancestor, false otherwise.
     */
    public abstract boolean isValidAncestor( VirtualFile baseDir,  VirtualFile child);

    public boolean shouldBeFound(GlobalSearchScope scope, VirtualFile virtualFile) {
        return scope.isSearchOutsideRootModel() || isInContent(virtualFile) || isInLibrarySource(virtualFile);
    }

     public abstract ModificationTracker getRootModificationTracker();
}
