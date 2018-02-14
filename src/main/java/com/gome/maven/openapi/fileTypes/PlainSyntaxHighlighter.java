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
package com.gome.maven.openapi.fileTypes;

import com.gome.maven.lexer.EmptyLexer;
import com.gome.maven.lexer.Lexer;
import com.gome.maven.openapi.editor.HighlighterColors;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.psi.tree.IElementType;

public class PlainSyntaxHighlighter implements SyntaxHighlighter {
    private static final TextAttributesKey[] ATTRS = {HighlighterColors.TEXT};

    @Override
    
    public Lexer getHighlightingLexer() {
        return new EmptyLexer();
    }

    @Override
    
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return ATTRS;
    }
}