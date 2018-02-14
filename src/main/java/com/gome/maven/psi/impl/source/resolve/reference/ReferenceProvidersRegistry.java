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

package com.gome.maven.psi.impl.source.resolve.reference;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.ProcessingContext;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 27.03.2003
 * Time: 17:13:45
 * To change this template use Options | File Templates.
 */
public abstract class ReferenceProvidersRegistry {
    public static final PsiReferenceProvider NULL_REFERENCE_PROVIDER = new PsiReferenceProvider() {
        
        @Override
        public PsiReference[] getReferencesByElement( PsiElement element,  ProcessingContext context) {
            return PsiReference.EMPTY_ARRAY;
        }
    };

    public static ReferenceProvidersRegistry getInstance() {
        return ServiceManager.getService(ReferenceProvidersRegistry.class);
    }

    
    public abstract PsiReferenceRegistrar getRegistrar( Language language);

    /**
     * @see #getReferencesFromProviders(com.gome.maven.psi.PsiElement)
     */
    @Deprecated
    
    public static PsiReference[] getReferencesFromProviders( PsiElement context,  Class clazz) {
        return getReferencesFromProviders(context, PsiReferenceService.Hints.NO_HINTS);
    }

    
    public static PsiReference[] getReferencesFromProviders( PsiElement context) {
        return getReferencesFromProviders(context, PsiReferenceService.Hints.NO_HINTS);
    }

    
    public static PsiReference[] getReferencesFromProviders( PsiElement context,  PsiReferenceService.Hints hints) {
        ProgressIndicatorProvider.checkCanceled();
        PsiUtilCore.ensureValid(context);

        ReferenceProvidersRegistry registry = getInstance();
        return registry.doGetReferencesFromProviders(context, hints);
    }

    
    protected abstract PsiReference[] doGetReferencesFromProviders( PsiElement context,  PsiReferenceService.Hints hints);
}
