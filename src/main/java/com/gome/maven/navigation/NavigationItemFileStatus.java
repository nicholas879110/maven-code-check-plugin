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
package com.gome.maven.navigation;

import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;

/**
 * @author yole
 */
public class NavigationItemFileStatus {
    private NavigationItemFileStatus() {
    }

    public static FileStatus get(NavigationItem item) {
        if (item instanceof PsiElement) {
            return getPsiElementFileStatus((PsiElement)item);
        }
        if (item instanceof AbstractTreeNode) {
            return ((AbstractTreeNode) item).getFileStatus();
        }
        if (item instanceof PsiElementNavigationItem) {
            PsiElement target = ((PsiElementNavigationItem) item).getTargetElement();
            if (target != null) return getPsiElementFileStatus(target);
        }
        return FileStatus.NOT_CHANGED;
    }

    private static FileStatus getPsiElementFileStatus( PsiElement psiElement) {
        if (!psiElement.isPhysical()) return FileStatus.NOT_CHANGED;
        PsiFile contFile = psiElement.getContainingFile();
        if (contFile == null) return FileStatus.NOT_CHANGED;
        VirtualFile vFile = contFile.getVirtualFile();
        return vFile == null ? FileStatus.NOT_CHANGED : FileStatusManager.getInstance(psiElement.getProject()).getStatus(vFile);
    }
}
