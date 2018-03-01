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
package com.gome.maven.codeInsight;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.testIntegration.TestFramework;

/**
 * @author yole
 */
public abstract class TestFrameworks {
    public static TestFrameworks getInstance() {
        return ServiceManager.getService(TestFrameworks.class);
    }

    public abstract boolean isTestClass(PsiClass psiClass);
    public abstract boolean isPotentialTestClass(PsiClass psiClass);

    
    public abstract PsiMethod findOrCreateSetUpMethod(PsiClass psiClass);

    
    public abstract PsiMethod findSetUpMethod(PsiClass psiClass);

    
    public abstract PsiMethod findTearDownMethod(PsiClass psiClass);

    protected abstract boolean hasConfigMethods(PsiClass psiClass);

    public abstract boolean isTestMethod(PsiMethod method);

    public boolean isTestOrConfig(PsiClass psiClass) {
        return isTestClass(psiClass) || hasConfigMethods(psiClass);
    }

    
    public static TestFramework detectFramework(PsiClass psiClass) {
        for (TestFramework framework : Extensions.getExtensions(TestFramework.EXTENSION_NAME)) {
            if (framework.isTestClass(psiClass)) {
                return framework;
            }
        }
        return null;
    }
}
