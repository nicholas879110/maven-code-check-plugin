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

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.refactoring.PackageWrapper;
import com.gome.maven.refactoring.util.RefactoringUtil;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.MultiMap;

import java.util.Collection;

/**
 *  @author dsl
 */
public class MultipleRootsMoveDestination extends AutocreatingMoveDestination {
    private static final Logger LOG = Logger.getInstance(
            "#com.gome.maven.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination");

    public MultipleRootsMoveDestination(PackageWrapper aPackage) {
        super(aPackage);
    }

    public PackageWrapper getTargetPackage() {
        return myPackage;
    }


    public PsiDirectory getTargetDirectory(PsiDirectory source) throws IncorrectOperationException {
        //if (JavaDirectoryService.getInstance().isSourceRoot(source)) return null;
        return getOrCreateDirectoryForSource(source.getVirtualFile());
    }

    public PsiDirectory getTargetDirectory(PsiFile source) throws IncorrectOperationException {
        return getOrCreateDirectoryForSource(source.getVirtualFile());
    }

    public PsiDirectory getTargetIfExists(PsiFile source) {
        return findTargetDirectoryForSource(source.getVirtualFile());
    }

    public String verify(PsiFile source) {
        VirtualFile virtualFile = source.getVirtualFile();
        if (virtualFile.isDirectory()) {
            virtualFile = virtualFile.getParent();
            LOG.assertTrue(virtualFile.isDirectory());
        }

        final VirtualFile sourceRootForFile = myFileIndex.getSourceRootForFile(virtualFile);
        if (sourceRootForFile == null) {
            return "";
        }
        return checkCanCreateInSourceRoot(sourceRootForFile);
    }

    
    public String verify(PsiDirectory source) {
        VirtualFile virtualFile = source.getVirtualFile();
        final VirtualFile sourceRootForFile = myFileIndex.getSourceRootForFile(virtualFile);
        if (sourceRootForFile == null) {
            return "";
        }
        if (virtualFile.equals(sourceRootForFile)) return null;
        return checkCanCreateInSourceRoot(sourceRootForFile);
    }

    
    public String verify(PsiPackage source) {
        PsiDirectory[] directories = source.getDirectories();
        for (final PsiDirectory directory : directories) {
            String s = verify(directory);
            if (s != null) return s;
        }
        return null;
    }

    public void analyzeModuleConflicts(final Collection<PsiElement> elements,
                                       MultiMap<PsiElement,String> conflicts, final UsageInfo[] usages) {
    }

    @Override
    public boolean isTargetAccessible(Project project, VirtualFile place) {
        return true;
    }

    public PsiDirectory getTargetIfExists(PsiDirectory source) {
        return findTargetDirectoryForSource(source.getVirtualFile());
    }


    private PsiDirectory findTargetDirectoryForSource(final VirtualFile file) {
        final VirtualFile sourceRoot = myFileIndex.getSourceRootForFile(file);
        LOG.assertTrue(sourceRoot != null);
        return RefactoringUtil.findPackageDirectoryInSourceRoot(myPackage, sourceRoot);
    }

    private PsiDirectory getOrCreateDirectoryForSource(final VirtualFile file)
            throws IncorrectOperationException {
        final VirtualFile sourceRoot = myFileIndex.getSourceRootForFile(file);
        LOG.assertTrue(sourceRoot != null);
        return RefactoringUtil.createPackageDirectoryInSourceRoot(myPackage, sourceRoot);
    }
}
