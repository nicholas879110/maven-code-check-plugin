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
package com.gome.maven.lang.findUsages;

import com.gome.maven.lang.cacheBuilder.WordsScanner;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNamedElement;

/**
 * The default empty implementation of the {@link FindUsagesProvider} interface.
 * @author max
 */
public class EmptyFindUsagesProvider implements FindUsagesProvider {

    
    public WordsScanner getWordsScanner() {
        return null;
    }

    public boolean canFindUsagesFor( PsiElement psiElement) {
        return false;
    }

    
    public String getHelpId( PsiElement psiElement) {
        return null;
    }

    
    public String getType( PsiElement element) {
        return "";
    }

    
    public String getDescriptiveName( PsiElement element) {
        return getNodeText(element, true);
    }

    
    public String getNodeText( PsiElement element, boolean useFullName) {
        if (element instanceof PsiNamedElement) {
            final String name = ((PsiNamedElement)element).getName();
            if (name != null) {
                return name;
            }
        }
        return "";
    }
}
