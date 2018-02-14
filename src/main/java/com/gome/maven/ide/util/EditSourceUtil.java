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
package com.gome.maven.ide.util;

import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.pom.PomTargetPsiElement;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilCore;

public class EditSourceUtil {
    private EditSourceUtil() { }


    public static Navigatable getDescriptor(final PsiElement element) {
        if (!canNavigate(element)) {
            return null;
        }
        if (element instanceof PomTargetPsiElement) {
            return ((PomTargetPsiElement)element).getTarget();
        }
        final PsiElement navigationElement = element.getNavigationElement();
        if (navigationElement instanceof PomTargetPsiElement) {
            return ((PomTargetPsiElement)navigationElement).getTarget();
        }
        final int offset = navigationElement instanceof PsiFile ? -1 : navigationElement.getTextOffset();
        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(navigationElement);
        if (virtualFile == null || !virtualFile.isValid()) {
            return null;
        }
        OpenFileDescriptor desc = new OpenFileDescriptor(navigationElement.getProject(), virtualFile, offset);
        desc.setUseCurrentWindow(FileEditorManager.USE_CURRENT_WINDOW.isIn(navigationElement));
        return desc;
    }

    public static boolean canNavigate(PsiElement element) {
        if (element == null || !element.isValid()) {
            return false;
        }

        VirtualFile file = PsiUtilCore.getVirtualFile(element.getNavigationElement());
        return file != null && file.isValid() && !file.is(VFileProperty.SPECIAL) && !VfsUtilCore.isBrokenLink(file);
    }

    public static void navigate(NavigationItem item, boolean requestFocus, boolean useCurrentWindow) {
        if (item instanceof UserDataHolder) {
            ((UserDataHolder)item).putUserData(FileEditorManager.USE_CURRENT_WINDOW, useCurrentWindow);
        }
        item.navigate(requestFocus);
        if (item instanceof UserDataHolder) {
            ((UserDataHolder)item).putUserData(FileEditorManager.USE_CURRENT_WINDOW, null);
        }
    }
}