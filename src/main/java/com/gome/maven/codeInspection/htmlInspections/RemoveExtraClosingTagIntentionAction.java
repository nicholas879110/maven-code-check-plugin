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

package com.gome.maven.codeInspection.htmlInspections;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.daemon.XmlErrorMessages;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiErrorElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.xml.XmlChildRole;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.psi.xml.XmlToken;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author spleaner
 */
public class RemoveExtraClosingTagIntentionAction implements LocalQuickFix, IntentionAction {
    @Override
    
    public String getFamilyName() {
        return XmlErrorMessages.message("remove.extra.closing.tag.quickfix");
    }

    @Override
    
    public String getName() {
        return XmlErrorMessages.message("remove.extra.closing.tag.quickfix");
    }


    @Override
    
    public String getText() {
        return getName();
    }

    @Override
    public boolean isAvailable( final Project project, final Editor editor, final PsiFile file) {
        return true;
    }

    @Override
    public void invoke( final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        final int offset = editor.getCaretModel().getOffset();
        final PsiElement psiElement = file.findElementAt(offset);
        if (psiElement == null || !psiElement.isValid() || !(psiElement instanceof XmlToken)) {
            return;
        }

        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
        doFix(psiElement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    private static void doFix( final PsiElement element) throws IncorrectOperationException {
        final XmlToken endNameToken = (XmlToken)element;
        final PsiElement tagElement = endNameToken.getParent();
        if (!(tagElement instanceof XmlTag) && !(tagElement instanceof PsiErrorElement)) return;

        if (tagElement instanceof PsiErrorElement) {
            tagElement.delete();
        }
        else {
            final ASTNode astNode = tagElement.getNode();
            if (astNode != null) {
                final ASTNode endTagStart = XmlChildRole.CLOSING_TAG_START_FINDER.findChild(astNode);
                if (endTagStart != null) {
                    final Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(tagElement.getContainingFile());
                    if (document != null) {
                        document.deleteString(endTagStart.getStartOffset(), tagElement.getLastChild().getTextRange().getEndOffset());
                    }
                }
            }
        }
    }

    @Override
    public void applyFix( final Project project,  final ProblemDescriptor descriptor) {
        final PsiElement element = descriptor.getPsiElement();
        if (!element.isValid() || !(element instanceof XmlToken)) return;
        if (!FileModificationService.getInstance().prepareFileForWrite(element.getContainingFile())) return;

        new WriteCommandAction(project) {
            @Override
            protected void run(final Result result) throws Throwable {
                doFix(element);
            }
        }.execute();
    }
}
