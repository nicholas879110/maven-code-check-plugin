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

import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.refactoring.MoveDestination;
import com.gome.maven.refactoring.PackageWrapper;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.util.RefactoringUtil;
import com.gome.maven.util.IncorrectOperationException;

/**
 *  @author dsl
 */
public abstract class AutocreatingMoveDestination implements MoveDestination {
    protected final PackageWrapper myPackage;
    protected final PsiManager myManager;
    protected final ProjectFileIndex myFileIndex;

    public AutocreatingMoveDestination(PackageWrapper targetPackage) {
        myPackage = targetPackage;
        myManager = myPackage.getManager();
        myFileIndex = ProjectRootManager.getInstance(myManager.getProject()).getFileIndex();
    }

    public abstract PackageWrapper getTargetPackage();

    public abstract PsiDirectory getTargetDirectory(PsiDirectory source) throws IncorrectOperationException;

    public abstract PsiDirectory getTargetDirectory(PsiFile source) throws IncorrectOperationException;


    protected String checkCanCreateInSourceRoot(final VirtualFile targetSourceRoot) {
        final String targetQName = myPackage.getQualifiedName();
        final String sourceRootPackage = myFileIndex.getPackageNameByDirectory(targetSourceRoot);
        if (!RefactoringUtil.canCreateInSourceRoot(sourceRootPackage, targetQName)) {
            return RefactoringBundle.message("source.folder.0.has.package.prefix.1", targetSourceRoot.getPresentableUrl(),
                    sourceRootPackage, targetQName);
        }
        return null;
    }
}
