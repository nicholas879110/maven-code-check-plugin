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

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiWhiteSpace;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.ReflectionUtil;

public abstract class CodeInsightUtilCore extends FileModificationService {
    public static <T extends PsiElement> T findElementInRange( PsiFile file,
                                                              int startOffset,
                                                              int endOffset,
                                                               Class<T> klass,
                                                               Language language) {
        PsiElement element1 = file.getViewProvider().findElementAt(startOffset, language);
        PsiElement element2 = file.getViewProvider().findElementAt(endOffset - 1, language);
        if (element1 instanceof PsiWhiteSpace) {
            startOffset = element1.getTextRange().getEndOffset();
            element1 = file.getViewProvider().findElementAt(startOffset, language);
        }
        if (element2 instanceof PsiWhiteSpace) {
            endOffset = element2.getTextRange().getStartOffset();
            element2 = file.getViewProvider().findElementAt(endOffset - 1, language);
        }
        if (element2 == null || element1 == null) return null;
        final PsiElement commonParent = PsiTreeUtil.findCommonParent(element1, element2);
        final T element =
                ReflectionUtil.isAssignable(klass, commonParent.getClass())
                        ? (T)commonParent : PsiTreeUtil.getParentOfType(commonParent, klass);
        if (element == null || element.getTextRange().getStartOffset() != startOffset || element.getTextRange().getEndOffset() != endOffset) {
            return null;
        }
        return element;
    }

    public static <T extends PsiElement> T forcePsiPostprocessAndRestoreElement( T element) {
        return forcePsiPostprocessAndRestoreElement(element, false);
    }

    public static <T extends PsiElement> T forcePsiPostprocessAndRestoreElement( T element,
                                                                                boolean useFileLanguage) {
        final PsiFile psiFile = element.getContainingFile();
        final Document document = psiFile.getViewProvider().getDocument();
        //if (document == null) return element;
        final Language language = useFileLanguage ? psiFile.getLanguage() : PsiUtilCore.getDialect(element);
        final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(psiFile.getProject());
        final RangeMarker rangeMarker = document.createRangeMarker(element.getTextRange());
        documentManager.doPostponedOperationsAndUnblockDocument(document);
        documentManager.commitDocument(document);

        T elementInRange = findElementInRange(psiFile, rangeMarker.getStartOffset(), rangeMarker.getEndOffset(),
                (Class<? extends T>)element.getClass(),
                language);
        rangeMarker.dispose();
        return elementInRange;
    }
}
