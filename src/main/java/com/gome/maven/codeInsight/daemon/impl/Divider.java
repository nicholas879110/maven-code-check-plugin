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
package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.ProperTextRange;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.containers.Stack;
import gnu.trove.TIntStack;

import java.util.List;

/**
 * User: cdr
 */
public class Divider {
    private static final int STARTING_TREE_HEIGHT = 10;

    public static void divideInsideAndOutside( PsiFile file,
                                              int startOffset,
                                              int endOffset,
                                               TextRange range,
                                               List<PsiElement> inside,
                                               List<ProperTextRange> insideRanges,
                                               List<PsiElement> outside,
                                               List<ProperTextRange> outsideRanges,
                                              boolean includeParents,
                                               Condition<PsiFile> filter) {
        final FileViewProvider viewProvider = file.getViewProvider();
        for (Language language : viewProvider.getLanguages()) {
            final PsiFile psiRoot = viewProvider.getPsi(language);
            if (filter.value(psiRoot)) {
                divideInsideAndOutside(psiRoot, startOffset, endOffset, range, inside, insideRanges, outside, outsideRanges, includeParents);
            }
        }
    }

    private static final PsiElement HAVE_TO_GET_CHILDREN = PsiUtilCore.NULL_PSI_ELEMENT;
    private static void divideInsideAndOutside( PsiFile root,
                                               int startOffset,
                                               int endOffset,
                                                TextRange range,
                                                List<PsiElement> inside,
                                                List<ProperTextRange> insideRanges,
                                                List<PsiElement> outside,
                                                List<ProperTextRange> outsideRanges,
                                               boolean includeParents) {
        final int currentOffset = root.getTextRange().getStartOffset();
        final Condition<PsiElement>[] filters = Extensions.getExtensions(CollectHighlightsUtil.EP_NAME);

        int offset = currentOffset;

        final TIntStack starts = new TIntStack(STARTING_TREE_HEIGHT);
        starts.push(startOffset);
        final Stack<PsiElement> elements = new Stack<PsiElement>(STARTING_TREE_HEIGHT);
        final Stack<PsiElement> children = new Stack<PsiElement>(STARTING_TREE_HEIGHT);
        PsiElement element = root;

        PsiElement child = HAVE_TO_GET_CHILDREN;
        while (true) {
            ProgressManager.checkCanceled();

            for (Condition<PsiElement> filter : filters) {
                if (!filter.value(element)) {
                    assert child == HAVE_TO_GET_CHILDREN;
                    child = null; // do not want to process children
                    break;
                }
            }

            boolean startChildrenVisiting;
            if (child == HAVE_TO_GET_CHILDREN) {
                startChildrenVisiting = true;
                child = element.getFirstChild();
            }
            else {
                startChildrenVisiting = false;
            }

            if (child == null) {
                if (startChildrenVisiting) {
                    // leaf element
                    offset += element.getTextLength();
                }

                int start = starts.pop();
                if (startOffset <= start && offset <= endOffset) {
                    if (range.containsRange(start, offset)) {
                        inside.add(element);
                        insideRanges.add(new ProperTextRange(start, offset));
                    }
                    else {
                        outside.add(element);
                        outsideRanges.add(new ProperTextRange(start, offset));
                    }
                }

                if (elements.isEmpty()) break;
                element = elements.pop();
                child = children.pop();
            }
            else {
                // composite element
                if (offset > endOffset) break;
                children.push(child.getNextSibling());
                starts.push(offset);
                elements.push(element);
                element = child;
                child = HAVE_TO_GET_CHILDREN;
            }
        }

        if (includeParents) {
            PsiElement parent = !outside.isEmpty() ? outside.get(outside.size() - 1) :
                    !inside.isEmpty() ? inside.get(inside.size() - 1) :
                            CollectHighlightsUtil.findCommonParent(root, startOffset, endOffset);
            while (parent != null && !(parent instanceof PsiFile)) {
                parent = parent.getParent();
                if (parent != null) {
                    outside.add(parent);
                    TextRange textRange = parent.getTextRange();
                    assert textRange != null : "Text range for " + parent + " is null. " + parent.getClass() +"; root: "+root+": "+root.getVirtualFile();
                    outsideRanges.add(ProperTextRange.create(textRange));
                }
            }
        }

        assert inside.size() == insideRanges.size();
        assert outside.size() == outsideRanges.size();
    }
}
