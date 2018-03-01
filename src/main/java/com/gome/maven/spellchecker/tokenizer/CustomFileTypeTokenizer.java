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
package com.gome.maven.spellchecker.tokenizer;

import com.gome.maven.ide.highlighter.custom.CustomFileTypeLexer;
import com.gome.maven.ide.highlighter.custom.SyntaxTable;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.CustomHighlighterTokenType;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.spellchecker.inspections.PlainTextSplitter;

/**
 * @author peter
 */
class CustomFileTypeTokenizer extends Tokenizer<PsiElement> {
    private final SyntaxTable mySyntaxTable;

    public CustomFileTypeTokenizer( SyntaxTable syntaxTable) {
        mySyntaxTable = syntaxTable;
    }

    @Override
    public void tokenize( PsiElement element, TokenConsumer consumer) {
        CustomFileTypeLexer lexer = new CustomFileTypeLexer(mySyntaxTable);
        String text = element.getText();
        lexer.start(text);
        while (true) {
            IElementType tokenType = lexer.getTokenType();
            if (tokenType == null) {
                break;
            }

            if (!isKeyword(tokenType)) {
                consumer.consumeToken(element, text, false, 0, new TextRange(lexer.getTokenStart(), lexer.getTokenEnd()), PlainTextSplitter.getInstance());
            }
            lexer.advance();
        }
    }

    private static boolean isKeyword(IElementType tokenType) {
        return tokenType == CustomHighlighterTokenType.KEYWORD_1 ||
                tokenType == CustomHighlighterTokenType.KEYWORD_2 ||
                tokenType == CustomHighlighterTokenType.KEYWORD_3 ||
                tokenType == CustomHighlighterTokenType.KEYWORD_4;
    }
}
