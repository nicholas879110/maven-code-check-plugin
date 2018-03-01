/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.refactoring;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiPackage;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.MultiMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a destination of Move Classes/Packages refactoring.
 * Destination of Move refactoring is generally a single package,
 * and various <code>MoveDestination</code>s control how moved items
 * will be layouted in directories corresponding to target packages.
 *
 * Instances of this interface can be obtained via methods of {@link RefactoringFactory}.
 *
 * @see JavaRefactoringFactory#createSourceFolderPreservingMoveDestination(String)
 * @see JavaRefactoringFactory#createSourceRootMoveDestination(java.lang.String, com.gome.maven.openapi.vfs.VirtualFile)
 *  @author dsl
 */
public interface MoveDestination {
    /**
     * Invoked in command & write action
     */
    PsiDirectory getTargetDirectory(PsiDirectory source) throws IncorrectOperationException;
    /**
     * Invoked in command & write action
     */
    PsiDirectory getTargetDirectory(PsiFile source) throws IncorrectOperationException;

    PackageWrapper getTargetPackage();

    PsiDirectory getTargetIfExists(PsiDirectory source);
    PsiDirectory getTargetIfExists(PsiFile source);

    
    String verify(PsiFile source);
    
    String verify(PsiDirectory source);
    
    String verify(PsiPackage source);

    void analyzeModuleConflicts(final Collection<PsiElement> elements, MultiMap<PsiElement,String> conflicts, final UsageInfo[] usages);

    boolean isTargetAccessible(Project project, VirtualFile place);
}
