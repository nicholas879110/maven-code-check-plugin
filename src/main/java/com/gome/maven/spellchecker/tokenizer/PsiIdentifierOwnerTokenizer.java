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
package com.gome.maven.spellchecker.tokenizer;

import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNameIdentifierOwner;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.spellchecker.inspections.IdentifierSplitter;


public class PsiIdentifierOwnerTokenizer extends Tokenizer<PsiNameIdentifierOwner> {
    @Override
    public void tokenize( PsiNameIdentifierOwner element, TokenConsumer consumer) {
        PsiElement identifier = element.getNameIdentifier();
        if (identifier == null) {
            return;
        }
        PsiElement parent = element;
        final TextRange range = identifier.getTextRange();
        if (range.isEmpty()) return;

        int offset = range.getStartOffset() - parent.getTextRange().getStartOffset();
        if(offset < 0 ) {
            parent = PsiTreeUtil.findCommonParent(identifier, element);
            offset = range.getStartOffset() - parent.getTextRange().getStartOffset();
        }
        String text = identifier.getText();
        consumer.consumeToken(parent, text, true, offset, TextRange.allOf(text), IdentifierSplitter.getInstance());
    }
}
