/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.List;

public class DeleteRepeatedInterfaceFix implements IntentionAction {
    private final PsiTypeElement myConjunct;
    private final List<PsiTypeElement> myConjList;

    public DeleteRepeatedInterfaceFix(PsiTypeElement conjunct, List<PsiTypeElement> conjList) {
        myConjunct = conjunct;
        myConjList = conjList;
    }

    
    @Override
    public String getText() {
        return "Delete repeated '" + myConjunct.getText() + "'";
    }

    
    @Override
    public String getFamilyName() {
        return "Delete repeated interface";
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        for (PsiTypeElement element : myConjList) {
            if (!element.isValid()) return false;
        }
        return true;
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
        final PsiTypeCastExpression castExpression = PsiTreeUtil.getParentOfType(myConjunct, PsiTypeCastExpression.class);
        if (castExpression != null) {
            final PsiTypeElement castType = castExpression.getCastType();
            if (castType != null) {
                final PsiType type = castType.getType();
                if (type instanceof PsiIntersectionType) {
                    final String typeText = StringUtil.join(ContainerUtil.filter(myConjList, new Condition<PsiTypeElement>() {
                        @Override
                        public boolean value(PsiTypeElement element) {
                            return element != myConjunct;
                        }
                    }), new Function<PsiTypeElement, String>() {
                        @Override
                        public String fun(PsiTypeElement element) {
                            return element.getText();
                        }
                    }, " & ");
                    final PsiTypeCastExpression newCastExpr =
                            (PsiTypeCastExpression)JavaPsiFacade.getElementFactory(project).createExpressionFromText("(" + typeText + ")a", castType);
                    CodeStyleManager.getInstance(project).reformat(castType.replace(newCastExpr.getCastType()));
                }
            }
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
