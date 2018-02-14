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

package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.codeInspection.QuickFix;
import com.gome.maven.openapi.command.undo.UndoUtil;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author max
 */
public class QuickFixWrapper implements IntentionAction {
    private static final Logger LOG = Logger.getInstance("com.intellij.codeInspection.ex.QuickFixWrapper");

    private final ProblemDescriptor myDescriptor;
    private final int myFixNumber;


    
    public static IntentionAction wrap( ProblemDescriptor descriptor, int fixNumber) {
        LOG.assertTrue(fixNumber >= 0, fixNumber);
        QuickFix[] fixes = descriptor.getFixes();
        LOG.assertTrue(fixes != null && fixes.length > fixNumber);

        final QuickFix fix = fixes[fixNumber];
        return fix instanceof IntentionAction ? (IntentionAction)fix : new QuickFixWrapper(descriptor, fixNumber);
    }

    private QuickFixWrapper( ProblemDescriptor descriptor, int fixNumber) {
        myDescriptor = descriptor;
        myFixNumber = fixNumber;
    }

    @Override
    
    public String getText() {
        return getFamilyName();
    }

    @Override
    
    public String getFamilyName() {
        return myDescriptor.getFixes()[myFixNumber].getName();
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        PsiElement psiElement = myDescriptor.getPsiElement();
        if (psiElement == null || !psiElement.isValid()) return false;
        final LocalQuickFix fix = getFix();
        return !(fix instanceof IntentionAction) || ((IntentionAction)fix).isAvailable(project, editor, file);
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        //if (!CodeInsightUtil.prepareFileForWrite(file)) return;
        // consider all local quick fixes do it themselves

        final PsiElement element = myDescriptor.getPsiElement();
        final PsiFile fileForUndo = element == null ? null : element.getContainingFile();
        LocalQuickFix fix = getFix();
        fix.applyFix(project, myDescriptor);
        DaemonCodeAnalyzer.getInstance(project).restart();
        if (fileForUndo != null && !fileForUndo.equals(file)) {
            UndoUtil.markPsiFileForUndo(fileForUndo);
        }
    }

    @Override
    public boolean startInWriteAction() {
        final LocalQuickFix fix = getFix();
        return !(fix instanceof IntentionAction) || ((IntentionAction)fix).startInWriteAction();
    }

    public LocalQuickFix getFix() {
        return (LocalQuickFix)myDescriptor.getFixes()[myFixNumber];
    }

    public String toString() {
        return getText();
    }
}
