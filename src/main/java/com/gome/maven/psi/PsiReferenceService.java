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
package com.gome.maven.psi;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.util.Key;

import java.util.List;

/**
 * @author Gregory.Shrago
 */
public abstract class PsiReferenceService {

    public static final Key<Hints> HINTS = Key.create("HINTS");

    public static PsiReferenceService getService() {
        return ServiceManager.getService(PsiReferenceService.class);
    }

    /**
     * By default, return the same as {@link com.gome.maven.psi.PsiElement#getReferences()}.
     * For elements implementing {@link com.gome.maven.psi.ContributedReferenceHost} also run
     * the reference providers registered in {@link com.gome.maven.psi.PsiReferenceContributor}
     * extensions.
     * @param element PSI element to which the references will be bound
     * @param hints optional hints which are passed to {@link com.gome.maven.psi.PsiReferenceProvider#acceptsHints(PsiElement, com.gome.maven.psi.PsiReferenceService.Hints)} and
     * {@link com.gome.maven.psi.PsiReferenceProvider#acceptsTarget(PsiElement)} before the {@link com.gome.maven.patterns.ElementPattern} is matched, for performing
     * fail-fast checks in case the pattern takes long to match.
     * @return the references
     */
    
    public abstract List<PsiReference> getReferences( final PsiElement element,  final Hints hints);

    public PsiReference[] getContributedReferences( final PsiElement element) {
        final List<PsiReference> list = getReferences(element, Hints.NO_HINTS);
        return list.toArray(new PsiReference[list.size()]);
    }


    public static class Hints {
        public static final Hints NO_HINTS = new Hints();

         public final PsiElement target;
         public final Integer offsetInElement;

        public Hints() {
            target = null;
            offsetInElement = null;
        }

        public Hints( PsiElement target,  Integer offsetInElement) {
            this.target = target;
            this.offsetInElement = offsetInElement;
        }
    }
}
