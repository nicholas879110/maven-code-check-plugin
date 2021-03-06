/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiCompiledFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiTreeUtil;

/**
 * @author yole
 */
public class CompletionUtilCoreImpl {
    
    public static <T extends PsiElement> T getOriginalElement( T psi) {
        return getOriginalElement(psi, psi.getContainingFile());
    }

    
    public static <T extends PsiElement> T getOriginalElement( T psi, PsiFile containingFile) {
        if (containingFile == null) return psi;

        PsiFile originalFile = containingFile.getOriginalFile();
        if (originalFile != containingFile && !(originalFile instanceof PsiCompiledFile) && psi.getTextRange() != null) {
            TextRange range = psi.getTextRange();
            Integer start = range.getStartOffset();
            Integer end = range.getEndOffset();

            Document document = containingFile.getViewProvider().getDocument();
            if (document != null) {
                Document hostDocument = document instanceof DocumentWindow ? ((DocumentWindow)document).getDelegate() : document;
                OffsetTranslator translator = hostDocument.getUserData(OffsetTranslator.RANGE_TRANSLATION);
                if (translator != null) {
                    if (document instanceof DocumentWindow) {
                        TextRange translated = ((DocumentWindow)document).injectedToHost(new TextRange(start, end));
                        start = translated.getStartOffset();
                        end = translated.getEndOffset();
                    }

                    start = translator.translateOffset(start);
                    end = translator.translateOffset(end);
                    if (start == null || end == null) {
                        return null;
                    }

                    if (document instanceof DocumentWindow) {
                        start = ((DocumentWindow)document).hostToInjected(start);
                        end = ((DocumentWindow)document).hostToInjected(end);
                    }
                }
            }

            //noinspection unchecked
            return (T)PsiTreeUtil.findElementOfClassAtRange(originalFile, start, end, psi.getClass());
        }

        return psi;
    }
}
