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
package com.gome.maven.pom.references;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.pom.PomTarget;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiTarget;

/**
 * @author peter
 */
public abstract class PomService {

    private static PomService getInstance(Project project) {
        return ServiceManager.getService(project, PomService.class);
    }

    
    protected abstract PsiElement convertToPsi( PomTarget target);

    public static PsiElement convertToPsi( Project project,  PomTarget target) {
        return getInstance(project).convertToPsi(target);
    }

    public static PsiElement convertToPsi( PsiTarget target) {
        return getInstance(target.getNavigationElement().getProject()).convertToPsi((PomTarget)target);
    }

}
