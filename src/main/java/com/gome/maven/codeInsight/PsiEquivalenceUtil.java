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

package com.gome.maven.codeInsight;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.psi.PsiComment;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.PsiWhiteSpace;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.PairConsumer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author ven
 */
public class PsiEquivalenceUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.PsiEquivalenceUtil");

    public static boolean areElementsEquivalent( PsiElement element1,
                                                 PsiElement element2,
                                                 Comparator<PsiElement> resolvedElementsComparator,
                                                boolean areCommentsSignificant) {
        return areElementsEquivalent(element1, element2, new ReferenceComparator(resolvedElementsComparator), null, null, areCommentsSignificant);
    }

    public static boolean areElementsEquivalent( PsiElement element1,
                                                 PsiElement element2,
                                                 Comparator<PsiElement> resolvedElementsComparator,
                                                 Comparator<PsiElement> leafElementsComparator) {
        return areElementsEquivalent(element1, element2, new ReferenceComparator(resolvedElementsComparator), leafElementsComparator, null, false);
    }

    private static class ReferenceComparator implements Comparator<PsiReference> {
        private  Comparator<PsiElement> myResolvedElementsComparator;

        ReferenceComparator( Comparator<PsiElement> resolvedElementsComparator) {
            myResolvedElementsComparator = resolvedElementsComparator;
        }

        @Override
        public int compare(PsiReference ref1, PsiReference ref2) {
            PsiElement resolved1 = ref1.resolve();
            PsiElement resolved2 = ref2.resolve();
            return Comparing.equal(resolved1, resolved2) ||
                    myResolvedElementsComparator != null && myResolvedElementsComparator.compare(resolved1, resolved2) == 0 ? 0 : 1;
        }
    }

    public static boolean areElementsEquivalent( PsiElement element1,
                                                 PsiElement element2,
                                                 Comparator<PsiReference> referenceComparator,
                                                 Comparator<PsiElement> leafElementsComparator,
                                                 Condition<PsiElement> isElementSignificantCondition,
                                                boolean areCommentsSignificant) {
        if(element1 == element2) return true;
        ASTNode node1 = element1.getNode();
        ASTNode node2 = element2.getNode();
        if (node1 == null || node2 == null) return false;
        if (node1.getElementType() != node2.getElementType()) return false;

        PsiElement[] children1 = getFilteredChildren(element1, isElementSignificantCondition, areCommentsSignificant);
        PsiElement[] children2 = getFilteredChildren(element2, isElementSignificantCondition, areCommentsSignificant);
        if (children1.length != children2.length) return false;

        for (int i = 0; i < children1.length; i++) {
            PsiElement child1 = children1[i];
            PsiElement child2 = children2[i];
            if (!areElementsEquivalent(child1, child2, referenceComparator,
                    leafElementsComparator, isElementSignificantCondition, areCommentsSignificant)) return false;
        }

        if (children1.length == 0) {
            if (leafElementsComparator != null) {
                if (leafElementsComparator.compare(element1, element2) != 0) return false;
            }
            else {
                if (!element1.textMatches(element2)) return false;
            }
        }

        PsiReference ref1 = element1.getReference();
        if (ref1 != null) {
            PsiReference ref2 = element2.getReference();
            if (ref2 == null) return false;
            if (referenceComparator.compare(ref1, ref2) != 0) return false;
        }
        return true;
    }

    public static boolean areElementsEquivalent( PsiElement element1,  PsiElement element2) {
        return areElementsEquivalent(element1, element2, null, false);
    }

    public static PsiElement[] getFilteredChildren( final PsiElement element,
                                                    Condition<PsiElement> isElementSignificantCondition,
                                                   boolean areCommentsSignificant) {
        ASTNode[] children1 = element.getNode().getChildren(null);
        ArrayList<PsiElement> array = new ArrayList<PsiElement>();
        for (ASTNode node : children1) {
            final PsiElement child = node.getPsi();
            if (!(child instanceof PsiWhiteSpace) && (areCommentsSignificant || !(child instanceof PsiComment)) &&
                    (isElementSignificantCondition == null || isElementSignificantCondition.value(child))) {
                array.add(child);
            }
        }
        return PsiUtilCore.toPsiElementArray(array);
    }

    public static void findChildRangeDuplicates(PsiElement first, PsiElement last,
                                                final List<Couple<PsiElement>> result,
                                                PsiElement scope) {
        findChildRangeDuplicates(first, last, scope, new PairConsumer<PsiElement, PsiElement>() {
            @Override
            public void consume(final PsiElement start, final PsiElement end) {
                result.add(Couple.of(start, end));
            }
        });
    }

    public static void findChildRangeDuplicates(PsiElement first, PsiElement last, PsiElement scope,
                                                PairConsumer<PsiElement, PsiElement> consumer) {
        LOG.assertTrue(first.getParent() == last.getParent());
        LOG.assertTrue(!(first instanceof PsiWhiteSpace) && !(last instanceof PsiWhiteSpace));
        addRangeDuplicates(scope, first, last, consumer);
    }

    private static void addRangeDuplicates(final PsiElement scope,
                                           final PsiElement first,
                                           final PsiElement last,
                                           final PairConsumer<PsiElement, PsiElement> result) {
        final PsiElement[] children = getFilteredChildren(scope, null, true);
        NextChild:
        for (int i = 0; i < children.length;) {
            PsiElement child = children[i];
            if (child != first) {
                int j = i;
                PsiElement next = first;
                do {
                    if (!areElementsEquivalent(children[j], next)) break;
                    j++;
                    if (next == last) {
                        result.consume(child, children[j - 1]);
                        i = j + 1;
                        continue NextChild;
                    }
                    next = PsiTreeUtil.skipSiblingsForward(next, PsiWhiteSpace.class);
                }
                while (true);

                if (i == j) {
                    addRangeDuplicates(child, first, last, result);
                }
            }

            i++;
        }
    }
}
