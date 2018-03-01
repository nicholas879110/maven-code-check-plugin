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

/*
 * @author max
 */
package com.gome.maven.psi.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFileFilter;
import com.gome.maven.psi.JavaPsiFacade;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.search.GlobalSearchScope;

public abstract class JavaPsiFacadeEx extends JavaPsiFacade {
    
    public static JavaPsiFacadeEx getInstanceEx( Project project) {
        return (JavaPsiFacadeEx)getInstance(project);
    }

    
    
    public PsiClass findClass( String qualifiedName) {
        return findClass(qualifiedName, GlobalSearchScope.allScope(getProject()));
    }

    
    public abstract void setAssertOnFileLoadingFilter( VirtualFileFilter filter, Disposable parentDisposable);
}