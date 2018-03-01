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

/**
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Nov 13, 2002
 * Time: 3:26:50 PM
 * To change this template use Options | File Templates.
 */
package com.gome.maven.codeInsight.daemon.impl.quickfix;

import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.infos.CandidateInfo;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.refactoring.util.RefactoringChangeUtil;
import com.gome.maven.util.IncorrectOperationException;

import java.util.HashSet;
import java.util.Set;

public abstract class QualifyThisOrSuperArgumentFix implements IntentionAction {
    protected static final Logger LOG = Logger.getInstance("#" + QualifyThisOrSuperArgumentFix.class.getName());
    protected final PsiExpression myExpression;
    protected final PsiClass myPsiClass;
    private String myText;


    public QualifyThisOrSuperArgumentFix( PsiExpression expression,  PsiClass psiClass) {
        myExpression = expression;
        myPsiClass = psiClass;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    
    @Override
    public String getText() {
        return myText;
    }

    protected abstract String getQualifierText();
    protected abstract PsiExpression getQualifier(PsiManager manager);

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        if (!myExpression.isValid()) return false;
        if (!myPsiClass.isValid()) return false;
        myText = "Qualify " + getQualifierText() + " expression with \'" + myPsiClass.getQualifiedName() + "\'";
        return true;
    }

    
    @Override
    public String getFamilyName() {
        return "Qualify " + getQualifierText();
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        myExpression.replace(getQualifier(PsiManager.getInstance(project)));
    }
}
