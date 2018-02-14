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
package com.gome.maven.psi.impl;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * @author yole
 */
public abstract class ResolveScopeManager {
    
    public abstract GlobalSearchScope getResolveScope( PsiElement element);

    public abstract GlobalSearchScope getDefaultResolveScope(VirtualFile vFile);

    
    public abstract GlobalSearchScope getUseScope( PsiElement element);

    public static ResolveScopeManager getInstance(Project project) {
        return ServiceManager.getService(project, ResolveScopeManager.class);
    }

    
    public static GlobalSearchScope getElementUseScope( PsiElement element) {
        return getInstance(element.getProject()).getUseScope(element);
    }

    
    public static GlobalSearchScope getElementResolveScope( PsiElement element) {
        return getInstance(element.getProject()).getResolveScope(element);
    }
}
