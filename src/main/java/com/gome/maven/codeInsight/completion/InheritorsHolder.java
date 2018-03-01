/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.AutoCompletionPolicy;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
public class InheritorsHolder implements Consumer<LookupElement> {
    private final PsiElement myPosition;
    private final Set<String> myAddedClasses = new HashSet<String>();
    private final CompletionResultSet myResult;

    public InheritorsHolder(PsiElement position, CompletionResultSet result) {
        myPosition = position;
        myResult = result;
    }

    @Override
    public void consume(LookupElement lookupElement) {
        final Object object = lookupElement.getObject();
        if (object instanceof PsiClass) {
            registerClass((PsiClass)object);
        }
        myResult.addElement(AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(lookupElement));
    }

    public void registerClass(PsiClass psiClass) {
        ContainerUtil.addIfNotNull(myAddedClasses, getClassName(psiClass));
    }

    
    private static String getClassName(PsiClass psiClass) {
        String name = psiClass.getQualifiedName();
        return name == null ? psiClass.getName() : name;
    }

    public boolean alreadyProcessed( LookupElement element) {
        final Object object = element.getObject();
        return object instanceof PsiClass && alreadyProcessed((PsiClass)object);
    }

    public boolean alreadyProcessed( PsiClass object) {
        final String name = getClassName(object);
        return name == null || myAddedClasses.contains(name);
    }
}
