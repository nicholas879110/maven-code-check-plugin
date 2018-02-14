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

/*
 * @author max
 */
package com.gome.maven.psi.search;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.project.Project;

public class EverythingGlobalScope extends GlobalSearchScope {
    public EverythingGlobalScope(Project project) {
        super(project);
    }

    public EverythingGlobalScope() {
    }

    @Override
    public int compare( final VirtualFile file1,  final VirtualFile file2) {
        return 0;
    }

    @Override
    public boolean contains( final VirtualFile file) {
        return true;
    }

    @Override
    public boolean isSearchInLibraries() {
        return true;
    }

    @Override
    public boolean isForceSearchingInLibrarySources() {
        return true;
    }

    @Override
    public boolean isSearchInModuleContent( final Module aModule) {
        return true;
    }

    @Override
    public boolean isSearchOutsideRootModel() {
        return true;
    }

    
    @Override
    public GlobalSearchScope union( SearchScope scope) {
        return this;
    }

    
    @Override
    public SearchScope intersectWith( SearchScope scope2) {
        return scope2;
    }
}