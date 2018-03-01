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
package com.gome.maven.codeInspection;

import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

public abstract class LocalQuickFixAndIntentionActionOnPsiElement extends LocalQuickFixOnPsiElement implements IntentionAction {
    protected LocalQuickFixAndIntentionActionOnPsiElement( PsiElement element) {
        this(element, element);
    }
    protected LocalQuickFixAndIntentionActionOnPsiElement( PsiElement startElement,  PsiElement endElement) {
        super(startElement, endElement);
    }

    @Override
    public final void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (file == null||myStartElement==null) return;
        final PsiElement startElement = myStartElement.getElement();
        final PsiElement endElement = myEndElement == null ? startElement : myEndElement.getElement();
        if (startElement == null || endElement == null) return;
        invoke(project, file, editor, startElement, endElement);
    }

    @Override
    public final boolean isAvailable( Project project, Editor editor, PsiFile file) {
        if (myStartElement == null) return false;
        final PsiElement startElement = myStartElement.getElement();
        final PsiElement endElement = myEndElement == null ? startElement : myEndElement.getElement();
        return startElement != null &&
                endElement != null &&
                startElement.isValid() &&
                (endElement == startElement || endElement.isValid()) &&
                file != null &&
                isAvailable(project, file, startElement, endElement);
    }

    public abstract void invoke( Project project,
                                 PsiFile file,
                                 Editor editor,
                                 PsiElement startElement,
                                 PsiElement endElement);

    @Override
    public void invoke( Project project,  PsiFile file,  PsiElement startElement,  PsiElement endElement) {
        invoke(project, file, null, startElement, endElement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
