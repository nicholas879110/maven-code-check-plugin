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
package com.gome.maven.find.findUsages;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.util.NullableComputable;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiCompiledElement;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.PsiReferenceService;
import com.gome.maven.psi.impl.search.PsiSearchHelperImpl;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usageView.UsageInfoFactory;
import com.gome.maven.util.Processor;

import java.util.Collection;

public class FindUsagesHelper {
    protected static boolean processUsagesInText( final PsiElement element,
                                                  Collection<String> stringToSearch,
                                                  GlobalSearchScope searchScope,
                                                  Processor<UsageInfo> processor) {
        final TextRange elementTextRange = ApplicationManager.getApplication().runReadAction(new NullableComputable<TextRange>() {
            @Override
            public TextRange compute() {
                if (!element.isValid() || element instanceof PsiCompiledElement) return null;
                return element.getTextRange();
            }
        });
        UsageInfoFactory factory = new UsageInfoFactory() {
            @Override
            public UsageInfo createUsageInfo( PsiElement usage, int startOffset, int endOffset) {
                if (elementTextRange != null
                        && usage.getContainingFile() == element.getContainingFile()
                        && elementTextRange.contains(startOffset)
                        && elementTextRange.contains(endOffset)) {
                    return null;
                }

                PsiReference someReference = usage.findReferenceAt(startOffset);
                if (someReference != null) {
                    PsiElement refElement = someReference.getElement();
                    for (PsiReference ref : PsiReferenceService.getService().getReferences(refElement, new PsiReferenceService.Hints(element, null))) {
                        if (element.getManager().areElementsEquivalent(ref.resolve(), element)) {
                            TextRange range = ref.getRangeInElement().shiftRight(refElement.getTextRange().getStartOffset() - usage.getTextRange().getStartOffset());
                            return new UsageInfo(usage, range.getStartOffset(), range.getEndOffset(), true);
                        }
                    }
                }

                return new UsageInfo(usage, startOffset, endOffset, true);
            }
        };
        for (String s : stringToSearch) {
            if (!PsiSearchHelperImpl.processTextOccurrences(element, s, searchScope, processor, factory)) return false;
        }
        return true;
    }
}
