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
package com.gome.maven.psi.search;

import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.PsiReferenceService;
import com.gome.maven.psi.ReferenceRange;
import com.gome.maven.util.Processor;

import java.util.List;

/**
 * @author peter
 */
public final class SingleTargetRequestResultProcessor extends RequestResultProcessor {
    private static final PsiReferenceService ourReferenceService = PsiReferenceService.getService();
    private final PsiElement myTarget;

    public SingleTargetRequestResultProcessor( PsiElement target) {
        super(target);
        myTarget = target;
    }

    @Override
    public boolean processTextOccurrence( PsiElement element, int offsetInElement,  final Processor<PsiReference> consumer) {
        if (!myTarget.isValid()) {
            return false;
        }

        final List<PsiReference> references = ourReferenceService.getReferences(element,
                new PsiReferenceService.Hints(myTarget, offsetInElement));
        for (PsiReference ref : references) {
            ProgressManager.checkCanceled();
            if (ReferenceRange.containsOffsetInElement(ref, offsetInElement) && ref.isReferenceTo(myTarget) && !consumer.process(ref)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public String toString() {
        return "SingleTarget: " + myTarget;
    }
}
