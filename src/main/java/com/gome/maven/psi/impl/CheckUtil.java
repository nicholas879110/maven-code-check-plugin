/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.psi.impl;

import com.gome.maven.openapi.fileTypes.FileTypeRegistry;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileVisitor;
import com.gome.maven.psi.PsiBundle;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

public class CheckUtil {
    private CheckUtil() { }

    public static void checkWritable( final PsiElement element) throws IncorrectOperationException {
        if (!element.isWritable()) {
            if (element instanceof PsiDirectory) {
                throw new IncorrectOperationException(
                        PsiBundle.message("cannot.modify.a.read.only.directory", ((PsiDirectory)element).getVirtualFile().getPresentableUrl()));
            }
            else {
                PsiFile file = element.getContainingFile();
                if (file == null) {
                    throw new IncorrectOperationException();
                }
                final VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile == null) {
                    throw new IncorrectOperationException();
                }
                throw new IncorrectOperationException(PsiBundle.message("cannot.modify.a.read.only.file", virtualFile.getPresentableUrl()));
            }
        }
    }

    public static void checkDelete( final VirtualFile file) throws IncorrectOperationException {
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
            @Override
            public boolean visitFile( VirtualFile file) {
                if (FileTypeRegistry.getInstance().isFileIgnored(file)) {
                    return false;
                }
                if (!file.isWritable()) {
                    throw new IncorrectOperationException(PsiBundle.message("cannot.delete.a.read.only.file", file.getPresentableUrl()));
                }
                return true;
            }
        });
    }
}
