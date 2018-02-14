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

package com.gome.maven.codeInsight.editorActions.smartEnter;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.text.CharArrayUtil;

/**
 * @author max
 */
public abstract class SmartEnterProcessor {
    public abstract boolean process( final Project project,  final Editor editor,  final PsiFile psiFile);

    public boolean processAfterCompletion( final Editor editor,  final PsiFile psiFile) {
        return process(psiFile.getProject(), editor, psiFile);
    }

    protected void reformat(PsiElement atCaret) throws IncorrectOperationException {
        final TextRange range = atCaret.getTextRange();
        final PsiFile file = atCaret.getContainingFile();
        final PsiFile baseFile = file.getViewProvider().getPsi(file.getViewProvider().getBaseLanguage());
        CodeStyleManager.getInstance(atCaret.getProject()).reformatText(baseFile, range.getStartOffset(), range.getEndOffset());
    }

    protected RangeMarker createRangeMarker(final PsiElement elt) {
        final PsiFile psiFile = elt.getContainingFile();
        final PsiDocumentManager instance = PsiDocumentManager.getInstance(elt.getProject());
        final Document document = instance.getDocument(psiFile);
        return document.createRangeMarker(elt.getTextRange());
    }

    
    protected PsiElement getStatementAtCaret(Editor editor, PsiFile psiFile) {
        int caret = editor.getCaretModel().getOffset();

        final Document doc = editor.getDocument();
        CharSequence chars = doc.getCharsSequence();
        int offset = caret == 0 ? 0 : CharArrayUtil.shiftBackward(chars, caret - 1, " \t");
        if (doc.getLineNumber(offset) < doc.getLineNumber(caret)) {
            offset = CharArrayUtil.shiftForward(chars, caret, " \t");
        }

        return psiFile.findElementAt(offset);
    }

    protected static boolean isUncommited( final Project project) {
        return PsiDocumentManager.getInstance(project).hasUncommitedDocuments();
    }

    protected void commit( final Editor editor) {
        final Project project = editor.getProject();
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

        //some psi operations may block the document, unblock here
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
    }

}
