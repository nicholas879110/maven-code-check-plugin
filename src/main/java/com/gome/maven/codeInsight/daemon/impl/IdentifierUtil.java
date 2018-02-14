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
package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNameIdentifierOwner;
import com.gome.maven.psi.PsiNamedElement;

public class IdentifierUtil {
    
    public static PsiElement getNameIdentifier( PsiElement element) {
        if (element instanceof PsiNameIdentifierOwner) {
            return ((PsiNameIdentifierOwner)element).getNameIdentifier();
        }

        if (element.isPhysical() &&
                element instanceof PsiNamedElement &&
                element.getContainingFile() != null &&
                element.getTextRange() != null) {
            // Quite hacky way to get name identifier. Depends on getTextOffset overriden properly.
            final PsiElement potentialIdentifier = element.findElementAt(element.getTextOffset() - element.getTextRange().getStartOffset());
            if (potentialIdentifier != null && Comparing.equal(potentialIdentifier.getText(), ((PsiNamedElement)element).getName(), false)) {
                return potentialIdentifier;
            }
        }

        return null;
    }
}
