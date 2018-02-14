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
package com.gome.maven.psi.search.scope.packageSet;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;

/**
 * User: anna
 */
public abstract class PackageSetBase implements PackageSet {
    /**
     * @see PackageSetBase#contains(VirtualFile, Project, NamedScopesHolder)
     */
    @Deprecated
    public abstract boolean contains(VirtualFile file, NamedScopesHolder holder);

    public boolean contains(VirtualFile file,  Project project,  NamedScopesHolder holder) {
        return contains(file, holder);
    }

    @Override
    public boolean contains( PsiFile file, NamedScopesHolder holder) {
        return contains(file.getVirtualFile(), file.getProject(), holder);
    }

    /**
     * @see PackageSetBase#getPsiFile(com.gome.maven.openapi.vfs.VirtualFile, com.gome.maven.psi.search.scope.packageSet.NamedScopesHolder)
     */
    @Deprecated
    
    public static PsiFile getPsiFile(VirtualFile file, NamedScopesHolder holder) {
        return PsiManager.getInstance(holder.getProject()).findFile(file);
    }

    
    public static PsiFile getPsiFile( VirtualFile file,  Project project) {
        return PsiManager.getInstance(project).findFile(file);
    }
}
