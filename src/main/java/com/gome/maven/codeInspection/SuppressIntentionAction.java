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

/*
 * User: anna
 * Date: 24-Dec-2007
 */
package com.gome.maven.codeInspection;

import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.editor.CaretModel;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.IncorrectOperationException;

import javax.swing.*;

public abstract class SuppressIntentionAction implements Iconable, IntentionAction {
    private String myText = "";
    public static SuppressIntentionAction[] EMPTY_ARRAY = new SuppressIntentionAction[0];

    @Override
    public Icon getIcon(int flags) {
        return AllIcons.General.InspectionsOff;
    }

    @Override
    
    public String getText() {
        return myText;
    }

    protected void setText( String text) {
        myText = text;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @Override
    public String toString() {
        return getText();
    }

    @Override
    public final void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!file.getManager().isInProject(file)) return;
        final PsiElement element = getElement(editor, file);
        if (element != null) {
            invoke(project, editor, element);
        }
    }

    /**
     * Invokes intention action for the element under caret.
     *
     * @param project the project in which the file is opened.
     * @param editor  the editor for the file.
     * @param element the element under cursor.
     * @throws com.gome.maven.util.IncorrectOperationException
     *
     */
    public abstract void invoke( Project project, Editor editor,  PsiElement element) throws IncorrectOperationException;

    @Override
    public final boolean isAvailable( Project project, Editor editor, PsiFile file) {
        if (file == null) return false;
        final PsiManager manager = file.getManager();
        if (manager == null) return false;
        if (!manager.isInProject(file)) return false;
        final PsiElement element = getElement(editor, file);
        return element != null && isAvailable(project, editor, element);
    }

    /**
     * Checks whether this intention is available at a caret offset in file.
     * If this method returns true, a light bulb for this intention is shown.
     *
     * @param project the project in which the availability is checked.
     * @param editor  the editor in which the intention will be invoked.
     * @param element the element under caret.
     * @return true if the intention is available, false otherwise.
     */
    public abstract boolean isAvailable( Project project, Editor editor,  PsiElement element);

    
    private static PsiElement getElement( Editor editor,  PsiFile file) {
        CaretModel caretModel = editor.getCaretModel();
        int position = caretModel.getOffset();
        return file.findElementAt(position);
    }
}
