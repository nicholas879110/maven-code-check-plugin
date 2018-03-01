/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.ide.navigationToolbar;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.Processor;

import java.util.Collection;
import java.util.Collections;

/**
 * @author gregsh
 */
public abstract class AbstractNavBarModelExtension implements NavBarModelExtension {
    
    @Override
    public abstract String getPresentableText(Object object);

    
    @Override
    public PsiElement adjustElement(PsiElement psiElement) {
        return psiElement;
    }

    
    @Override
    public PsiElement getParent(PsiElement psiElement) {
        return null;
    }

    
    @Override
    public Collection<VirtualFile> additionalRoots(Project project) {
        return Collections.emptyList();
    }

    public boolean processChildren(Object object, Object rootElement, Processor<Object> processor) {
        return true;
    }
}
