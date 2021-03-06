/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.psi;

import com.gome.maven.util.ProcessingContext;

/**
 * Allows to inject additional references into an element that supports reference contributors.
 * Register it via {@link PsiReferenceContributor} or {@link PsiReferenceProviderBean#EP_NAME}
 *
 * Note that, if you're implementing a custom language, it won't by default support references registered through PsiReferenceContributor.
 * If you want to support that, you need to call
 * {@link com.gome.maven.psi.impl.source.resolve.reference.ReferenceProvidersRegistry#getReferencesFromProviders(PsiElement)} from your implementation
 * of PsiElement.getReferences().
 *
 * @author ik
 */
public abstract class PsiReferenceProvider {
    public static final PsiReferenceProvider[] EMPTY_ARRAY = new PsiReferenceProvider[0];

    
    public abstract PsiReference[] getReferencesByElement( PsiElement element,  final ProcessingContext context);

    public boolean acceptsHints( final PsiElement element,  PsiReferenceService.Hints hints) {
        final PsiElement target = hints.target;
        return target == null || acceptsTarget(target);
    }
    public boolean acceptsTarget( PsiElement target) {
        return true;
    }

}
