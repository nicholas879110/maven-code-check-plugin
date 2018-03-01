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
package com.gome.maven.codeInsight.daemon.impl.quickfix;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.refactoring.util.RefactoringChangeUtil;
import com.gome.maven.util.IncorrectOperationException;

/**
 * User: anna
 */
public class QualifyWithThisFix implements IntentionAction {
    private final PsiClass myContainingClass;
    private final PsiElement myExpression;

    public QualifyWithThisFix( PsiClass containingClass,  PsiElement expression) {
        myContainingClass = containingClass;
        myExpression = expression;
    }

    
    @Override
    public String getText() {
        return "Qualify with " + myContainingClass.getName() + ".this";
    }

    
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
        final PsiThisExpression thisExpression =
                RefactoringChangeUtil.createThisExpression(PsiManager.getInstance(project), myContainingClass);
        ((PsiReferenceExpression)myExpression).setQualifierExpression(thisExpression);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
