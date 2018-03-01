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
package com.gome.maven.lexer;

import com.gome.maven.lang.java.JavaParserDefinition;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.JavaDocTokenType;
import com.gome.maven.psi.JavaTokenType;
import com.gome.maven.psi.impl.source.tree.JavaDocElementType;
import com.gome.maven.psi.tree.IElementType;

/**
 * @author max
 */
public class JavaHighlightingLexer extends LayeredLexer {
    public JavaHighlightingLexer( LanguageLevel languageLevel) {
        super(JavaParserDefinition.createLexer(languageLevel));
        registerSelfStoppingLayer(new StringLiteralLexer('\"', JavaTokenType.STRING_LITERAL),
                new IElementType[]{JavaTokenType.STRING_LITERAL}, IElementType.EMPTY_ARRAY);

        registerSelfStoppingLayer(new StringLiteralLexer('\'', JavaTokenType.STRING_LITERAL),
                new IElementType[]{JavaTokenType.CHARACTER_LITERAL}, IElementType.EMPTY_ARRAY);

        LayeredLexer docLexer = new LayeredLexer(JavaParserDefinition.createDocLexer(languageLevel));

        HtmlHighlightingLexer lexer = new HtmlHighlightingLexer(null);
        lexer.setHasNoEmbeddments(true);
        docLexer.registerLayer(lexer, JavaDocTokenType.DOC_COMMENT_DATA);

        registerSelfStoppingLayer(docLexer, new IElementType[]{JavaDocElementType.DOC_COMMENT}, IElementType.EMPTY_ARRAY);
    }
}