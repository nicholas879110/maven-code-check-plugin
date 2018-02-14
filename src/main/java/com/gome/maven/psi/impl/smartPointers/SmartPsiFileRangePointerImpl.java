/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.smartPointers;

import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ProperTextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.FreeThreadedFileViewProvider;

/**
 * User: cdr
 */
class SmartPsiFileRangePointerImpl extends SmartPsiElementPointerImpl<PsiFile> implements SmartPsiFileRange {
    SmartPsiFileRangePointerImpl( PsiFile containingFile,  ProperTextRange range) {
        super(containingFile, createElementInfo(containingFile, range), PsiFile.class);
    }

    
    private static SmartPointerElementInfo createElementInfo( PsiFile containingFile,  ProperTextRange range) {
        Project project = containingFile.getProject();
        if (containingFile.getViewProvider() instanceof FreeThreadedFileViewProvider) {
            PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(project).getInjectionHost(containingFile);
            if (host != null) {
                SmartPsiElementPointer<PsiLanguageInjectionHost> hostPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(host);
                return new InjectedSelfElementInfo(project, containingFile, range, containingFile, hostPointer);
            }
        }
        if (range.equals(containingFile.getTextRange())) return new FileElementInfo(containingFile);
        return new SelfElementInfo(project, range, PsiElement.class, containingFile, containingFile.getLanguage());
    }

    @Override
    public PsiFile getElement() {
        if (getRange() == null) return null; // range is invalid
        return getContainingFile();
    }
}
