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
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.util.Function;
import com.gome.maven.util.IncorrectOperationException;

import java.util.List;

/**
 * User: anna
 * Date: 10/31/13
 */
public class FlipIntersectionSidesFix implements IntentionAction {
    private static final Logger LOG = Logger.getInstance("#" + FlipIntersectionSidesFix.class.getName());
    private final String myClassName;
    private final List<PsiTypeElement> myConjuncts;
    private final PsiTypeElement myConjunct;
    private final PsiTypeElement myCastTypeElement;

    public FlipIntersectionSidesFix(String className,
                                     List<PsiTypeElement> conjList,
                                    PsiTypeElement conjunct,
                                    PsiTypeElement castTypeElement) {
        myClassName = className;
        myConjuncts = conjList;
        LOG.assertTrue(!conjList.isEmpty());
        myConjunct = conjunct;
        myCastTypeElement = castTypeElement;
    }

    
    @Override
    public String getText() {
        return "Move '" + myClassName + "' to the beginning";
    }

    
    @Override
    public String getFamilyName() {
        return "Move to front";
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        for (PsiTypeElement typeElement : myConjuncts) {
            if (!typeElement.isValid()) return false;
        }
        return !Comparing.strEqual(myConjunct.getText(), myConjuncts.get(0).getText());
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
        myConjuncts.remove(myConjunct);
        myConjuncts.add(0, myConjunct);

        final String intersectionTypeText = StringUtil.join(myConjuncts, new Function<PsiTypeElement, String>() {
            @Override
            public String fun(PsiTypeElement element) {
                return element.getText();
            }
        }, " & ");
        final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        final PsiTypeCastExpression fixedCast =
                (PsiTypeCastExpression)elementFactory.createExpressionFromText("(" + intersectionTypeText + ") a", myCastTypeElement);
        final PsiTypeElement fixedCastCastType = fixedCast.getCastType();
        LOG.assertTrue(fixedCastCastType != null);
        final PsiElement flippedTypeElement = myCastTypeElement.replace(fixedCastCastType);
        CodeStyleManager.getInstance(project).reformat(flippedTypeElement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
