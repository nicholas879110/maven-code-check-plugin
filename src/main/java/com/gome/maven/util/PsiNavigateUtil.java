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

package com.gome.maven.util;

import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilCore;

public class PsiNavigateUtil {
    public static void navigate( final PsiElement psiElement) {
        if (psiElement != null && psiElement.isValid()) {
            final PsiElement navigationElement = psiElement.getNavigationElement();
            final int offset = navigationElement instanceof PsiFile ? -1 : navigationElement.getTextOffset();

            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(psiElement);
            if (virtualFile != null && virtualFile.isValid()) {
                new OpenFileDescriptor(navigationElement.getProject(), virtualFile, offset).navigate(true);
            }
            else if (navigationElement instanceof Navigatable) {
                ((Navigatable)navigationElement).navigate(true);
            }
        }
    }
}