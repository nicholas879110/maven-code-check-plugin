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
package com.gome.maven.psi.impl.source.resolve;

import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.SmartPsiElementPointer;

/**
 * @author max
 */
public class FileContextUtil {
    public static final Key<SmartPsiElementPointer> INJECTED_IN_ELEMENT = Key.create("injectedIn");
    public static final Key<PsiFile> CONTAINING_FILE_KEY = Key.create("CONTAINING_FILE_KEY");

    private FileContextUtil() { }

    
    public static PsiElement getFileContext( PsiFile file) {
        SmartPsiElementPointer pointer = file.getUserData(INJECTED_IN_ELEMENT);
        return pointer == null ? null : pointer.getElement();
    }

    
    public static PsiFile getContextFile( PsiElement element) {
        if (!element.isValid()) return null;
        PsiFile file = element.getContainingFile();
        if (file == null) return null;
        PsiElement context = file.getContext();
        if (context == null) {
            return file;
        }
        else {
            return getContextFile(context);
        }
    }
}