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
package com.gome.maven.refactoring.move.moveClassesOrPackages;

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
import com.gome.maven.refactoring.PackageWrapper;
import com.gome.maven.refactoring.util.RefactoringConflictsUtil;
import com.gome.maven.refactoring.util.RefactoringUtil;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.MultiMap;

import java.util.Collection;

/**
 *  @author dsl
 */
public class AutocreatingSingleSourceRootMoveDestination extends AutocreatingMoveDestination {
    private final VirtualFile mySourceRoot;

    public AutocreatingSingleSourceRootMoveDestination(PackageWrapper targetPackage,  VirtualFile sourceRoot) {
        super(targetPackage);
        mySourceRoot = sourceRoot;
    }

    public PackageWrapper getTargetPackage() {
        return myPackage;
    }

    public PsiDirectory getTargetIfExists(PsiDirectory source) {
        return RefactoringUtil.findPackageDirectoryInSourceRoot(myPackage, mySourceRoot);
    }

    public PsiDirectory getTargetIfExists(PsiFile source) {
        return RefactoringUtil.findPackageDirectoryInSourceRoot(myPackage, mySourceRoot);
    }

    public PsiDirectory getTargetDirectory(PsiDirectory source) throws IncorrectOperationException {
        return getDirectory();
    }

    public PsiDirectory getTargetDirectory(PsiFile source) throws IncorrectOperationException {
        return getDirectory();
    }

    
    public String verify(PsiFile source) {
        return checkCanCreateInSourceRoot(mySourceRoot);
    }

    public String verify(PsiDirectory source) {
        return checkCanCreateInSourceRoot(mySourceRoot);
    }

    public String verify(PsiPackage aPackage) {
        return checkCanCreateInSourceRoot(mySourceRoot);
    }

    public void analyzeModuleConflicts(final Collection<PsiElement> elements,
                                       MultiMap<PsiElement,String> conflicts, final UsageInfo[] usages) {
        RefactoringConflictsUtil.analyzeModuleConflicts(getTargetPackage().getManager().getProject(), elements, usages, mySourceRoot, conflicts);
    }

    @Override
    public boolean isTargetAccessible(Project project, VirtualFile place) {
        final boolean inTestSourceContent = ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(place);
        final Module module = ModuleUtil.findModuleForFile(place, project);
        if (mySourceRoot != null &&
                module != null &&
                !GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, inTestSourceContent).contains(mySourceRoot)) {
            return false;
        }
        return true;
    }

    PsiDirectory myTargetDirectory = null;
    private PsiDirectory getDirectory() throws IncorrectOperationException {
        if (myTargetDirectory == null) {
            myTargetDirectory = RefactoringUtil.createPackageDirectoryInSourceRoot(myPackage, mySourceRoot);
        }
        return myTargetDirectory;
    }
}
