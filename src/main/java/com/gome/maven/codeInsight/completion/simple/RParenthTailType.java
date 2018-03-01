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
package com.gome.maven.codeInsight.completion.simple;

import com.gome.maven.codeInsight.TailType;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.HighlighterIterator;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.CommonCodeStyleSettings;
import com.gome.maven.psi.impl.source.tree.ElementType;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.java.IJavaElementType;
import com.gome.maven.psi.util.PsiTreeUtil;

/**
 * @author peter
 */
public abstract class RParenthTailType extends TailType {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.completion.simple.RParenthSimpleTailType");

    private static TextRange getRangeToCheckParensBalance(PsiFile file, final Document document, int startOffset){
        PsiElement element = file.findElementAt(startOffset);
        element = PsiTreeUtil.getParentOfType(element, PsiStatement.class, false);
        if (element != null) {
            final PsiElement parent = element.getParent();
            if (parent instanceof PsiLoopStatement) {
                element = parent;
            }
        }
        return element == null ? new TextRange(0, document.getTextLength()) : element.getTextRange();
    }

    protected abstract boolean isSpaceWithinParentheses(CommonCodeStyleSettings styleSettings, Editor editor, final int tailOffset);

    @Override
    public int processTail(final Editor editor, int tailOffset) {
        return addRParenth(editor, tailOffset, isSpaceWithinParentheses(getLocalCodeStyleSettings(editor, tailOffset), editor, tailOffset));
    }

    public static int addRParenth(Editor editor, int offset, boolean spaceWithinParens) {
        int existingRParenthOffset = getExistingRParenthOffset(editor, offset);

        if (existingRParenthOffset < 0){
            if (spaceWithinParens){
                offset = insertChar(editor, offset, ' ');
            }
            editor.getDocument().insertString(offset, ")");
            return moveCaret(editor, offset, 1);
        }
        if (spaceWithinParens && offset == existingRParenthOffset) {
            existingRParenthOffset = insertChar(editor, offset, ' ');
        }
        return moveCaret(editor, existingRParenthOffset, 1);
    }


    public String toString() {
        return "RParenth";
    }

    private static int getExistingRParenthOffset(final Editor editor, final int tailOffset) {
        final Document document = editor.getDocument();
        if (tailOffset >= document.getTextLength()) return -1;

        final CharSequence charsSequence = document.getCharsSequence();
        EditorHighlighter highlighter = ((EditorEx) editor).getHighlighter();

        int existingRParenthOffset = -1;
        for(HighlighterIterator iterator = highlighter.createIterator(tailOffset); !iterator.atEnd(); iterator.advance()){
            final IElementType tokenType = iterator.getTokenType();

            if ((!(tokenType instanceof IJavaElementType) || !ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(tokenType)) &&
                    tokenType != TokenType.WHITE_SPACE) {
                final int start = iterator.getStart();
                if (iterator.getEnd() == start + 1 &&  ')' == charsSequence.charAt(start)) {
                    existingRParenthOffset = start;
                }
                break;
            }
        }

        if (existingRParenthOffset >= 0){
            final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(editor.getProject());
            psiDocumentManager.commitDocument(document);
            TextRange range = getRangeToCheckParensBalance(psiDocumentManager.getPsiFile(document), document, editor.getCaretModel().getOffset());
            int balance = calcParensBalance(document, highlighter, range.getStartOffset(), range.getEndOffset());
            if (balance > 0){
                return -1;
            }
        }
        return existingRParenthOffset;
    }

    private static int calcParensBalance(Document document, EditorHighlighter highlighter, int rangeStart, int rangeEnd){
        LOG.assertTrue(0 <= rangeStart);
        LOG.assertTrue(rangeStart <= rangeEnd);
        LOG.assertTrue(rangeEnd <= document.getTextLength());

        HighlighterIterator iterator = highlighter.createIterator(rangeStart);
        int balance = 0;
        while(!iterator.atEnd() && iterator.getStart() < rangeEnd){
            IElementType tokenType = iterator.getTokenType();
            if (tokenType == JavaTokenType.LPARENTH){
                balance++;
            }
            else if (tokenType == JavaTokenType.RPARENTH){
                balance--;
            }
            iterator.advance();
        }
        return balance;
    }

}
