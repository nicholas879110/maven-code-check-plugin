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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.paths.PsiDynaReference;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.ReferenceRange;
import com.gome.maven.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.gome.maven.util.PairConsumer;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author peter
 */
public class LegacyCompletionContributor extends CompletionContributor {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.completion.LegacyCompletionContributor");

    @Override
    public void fillCompletionVariants( CompletionParameters parameters,  CompletionResultSet _result) {
        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }
        CompletionData completionData = getCompletionData(parameters);
        if (completionData == null) return;

        final PsiElement insertedElement = parameters.getPosition();
        final CompletionResultSet result = _result.withPrefixMatcher(completionData.findPrefix(insertedElement, parameters.getOffset()));

        completeReference(parameters, result);

        final Set<LookupElement> lookupSet = new LinkedHashSet<LookupElement>();
        final Set<CompletionVariant> keywordVariants = new HashSet<CompletionVariant>();
        PsiFile file = parameters.getOriginalFile();
        completionData.addKeywordVariants(keywordVariants, insertedElement, file);
        completionData.completeKeywordsBySet(lookupSet, keywordVariants, insertedElement, result.getPrefixMatcher(), file);
        result.addAllElements(lookupSet);
    }

    public static boolean completeReference(final CompletionParameters parameters, final CompletionResultSet result) {
        final CompletionData completionData = getCompletionData(parameters);
        if (completionData == null) {
            return false;
        }

        final Ref<Boolean> hasVariants = Ref.create(false);
        processReferences(parameters, result, new PairConsumer<PsiReference, CompletionResultSet>() {
            @Override
            public void consume(final PsiReference reference, final CompletionResultSet resultSet) {
                final Set<LookupElement> lookupSet = new LinkedHashSet<LookupElement>();
                completionData
                        .completeReference(reference, lookupSet, parameters.getPosition(), parameters.getOriginalFile(), parameters.getOffset());
                for (final LookupElement item : lookupSet) {
                    if (resultSet.getPrefixMatcher().prefixMatches(item)) {
                        hasVariants.set(true);
                        resultSet.addElement(item);
                    }
                }
            }
        });
        return hasVariants.get().booleanValue();
    }

    private static CompletionData getCompletionData(CompletionParameters parameters) {
        final PsiElement position = parameters.getPosition();
        return CompletionUtil.getCompletionDataByElement(position, parameters.getOriginalFile());
    }

    public static void processReferences(final CompletionParameters parameters,
                                         final CompletionResultSet result,
                                         final PairConsumer<PsiReference, CompletionResultSet> consumer) {
        final int startOffset = parameters.getOffset();
        final PsiReference ref = parameters.getPosition().getContainingFile().findReferenceAt(startOffset);
        if (ref instanceof PsiMultiReference) {
            for (final PsiReference reference : CompletionData.getReferences((PsiMultiReference)ref)) {
                processReference(result, startOffset, consumer, reference);
            }
        }
        else if (ref instanceof PsiDynaReference) {
            for (final PsiReference reference : ((PsiDynaReference<?>)ref).getReferences()) {
                processReference(result, startOffset, consumer, reference);
            }
        }
        else if (ref != null) {
            processReference(result, startOffset, consumer, ref);
        }
    }

    private static void processReference(final CompletionResultSet result,
                                         final int startOffset,
                                         final PairConsumer<PsiReference, CompletionResultSet> consumer,
                                         final PsiReference reference) {
        PsiElement element = reference.getElement();
        final int offsetInElement = startOffset - element.getTextRange().getStartOffset();
        if (!ReferenceRange.containsOffsetInElement(reference, offsetInElement)) {
            return;
        }

        TextRange range = reference.getRangeInElement();
        try {
            final String prefix = element.getText().substring(range.getStartOffset(), offsetInElement);
            consumer.consume(reference, result.withPrefixMatcher(prefix));
        }
        catch (StringIndexOutOfBoundsException e) {
            LOG.error("Reference=" + reference +
                            "; element=" + element + " of " + element.getClass() +
                            "; range=" + range +
                            "; offset=" + offsetInElement,
                    e);
        }
    }


}