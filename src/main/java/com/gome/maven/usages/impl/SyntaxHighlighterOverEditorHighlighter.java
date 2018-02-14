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
package com.gome.maven.usages.impl;

import com.gome.maven.lexer.LayeredLexer;
import com.gome.maven.lexer.Lexer;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.ex.util.LayeredHighlighterIterator;
import com.gome.maven.openapi.editor.ex.util.LayeredLexerEditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighterFactory;
import com.gome.maven.openapi.editor.highlighter.HighlighterIterator;
import com.gome.maven.openapi.fileTypes.PlainSyntaxHighlighter;
import com.gome.maven.openapi.fileTypes.PlainTextFileType;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighter;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.impl.search.LexerEditorHighlighterLexer;
import com.gome.maven.psi.tree.IElementType;

/**
 * Created by Maxim.Mossienko on 7/31/2014.
 */
public class SyntaxHighlighterOverEditorHighlighter implements SyntaxHighlighter {
    private final Lexer lexer;
    private LayeredHighlighterIterator layeredHighlighterIterator = null;
    private final SyntaxHighlighter highlighter;

    public SyntaxHighlighterOverEditorHighlighter(SyntaxHighlighter _highlighter, VirtualFile file, Project project) {
        if (file.getFileType() == PlainTextFileType.INSTANCE) { // optimization for large files, PlainTextSyntaxHighlighterFactory is slow
            highlighter = new PlainSyntaxHighlighter();
            lexer = highlighter.getHighlightingLexer();
        } else {
            highlighter = _highlighter;
            LayeredLexer.ourDisableLayersFlag.set(Boolean.TRUE);
            EditorHighlighter editorHighlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file);

            try {
                if (editorHighlighter instanceof LayeredLexerEditorHighlighter) {
                    lexer = new LexerEditorHighlighterLexer(editorHighlighter, false);
                }
                else {
                    lexer = highlighter.getHighlightingLexer();
                }
            }
            finally {
                LayeredLexer.ourDisableLayersFlag.set(null);
            }
        }
    }

    
    @Override
    public Lexer getHighlightingLexer() {
        return lexer;
    }

    
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        final SyntaxHighlighter activeSyntaxHighlighter =
                layeredHighlighterIterator != null ? layeredHighlighterIterator.getActiveSyntaxHighlighter() : highlighter;
        return activeSyntaxHighlighter.getTokenHighlights(tokenType);
    }

    public void restart( CharSequence text) {
        lexer.start(text);

        if (lexer instanceof LexerEditorHighlighterLexer) {
            HighlighterIterator iterator = ((LexerEditorHighlighterLexer)lexer).getHighlighterIterator();
            if (iterator instanceof LayeredHighlighterIterator) {
                layeredHighlighterIterator = (LayeredHighlighterIterator)iterator;
            } else {
                layeredHighlighterIterator = null;
            }
        }
    }

    public void resetPosition(int startOffset) {
        if (lexer instanceof LexerEditorHighlighterLexer) {
            ((LexerEditorHighlighterLexer)lexer).resetPosition(startOffset);

            HighlighterIterator iterator = ((LexerEditorHighlighterLexer)lexer).getHighlighterIterator();
            if (iterator instanceof LayeredHighlighterIterator) {
                layeredHighlighterIterator = (LayeredHighlighterIterator)iterator;
            } else {
                layeredHighlighterIterator = null;
            }
        } else {
            CharSequence text = lexer.getBufferSequence();
            lexer.start(text, startOffset, text.length());
        }
    }
}
