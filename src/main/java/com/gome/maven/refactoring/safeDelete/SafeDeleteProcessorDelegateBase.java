/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.refactoring.safeDelete;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usages.UsageView;
import com.gome.maven.usages.UsageViewManager;
import com.gome.maven.usages.UsageViewPresentation;

import java.util.Collection;

/**
 * User: anna
 * Date: 12/1/11
 */
public abstract class SafeDeleteProcessorDelegateBase implements SafeDeleteProcessorDelegate {
    
    public abstract Collection<? extends PsiElement> getElementsToSearch(PsiElement element,  Module module, Collection<PsiElement> allElementsToDelete);
    @Override
    public Collection<? extends PsiElement> getElementsToSearch(PsiElement element, Collection<PsiElement> allElementsToDelete) {
        return getElementsToSearch(element, null, allElementsToDelete);
    }

    
    public UsageView showUsages(UsageInfo[] usages, UsageViewPresentation presentation, UsageViewManager manager, PsiElement[] elements) {
        return null;
    }
}
