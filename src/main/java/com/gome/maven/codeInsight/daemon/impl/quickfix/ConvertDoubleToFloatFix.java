/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.util.IncorrectOperationException;

/**
 * User: anna
 * Date: 2/10/12
 */
public class ConvertDoubleToFloatFix implements IntentionAction {
    private final PsiExpression myExpression;

    public ConvertDoubleToFloatFix(PsiExpression expression) {
        myExpression = expression;
    }

    
    @Override
    public String getText() {
        return "Convert '" + myExpression.getText() + "' to float";
    }

    
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        if (myExpression.isValid()) {
            if (!StringUtil.endsWithIgnoreCase(myExpression.getText(), "f")) {
                final PsiLiteralExpression expression = (PsiLiteralExpression)createFloatingPointExpression(project);
                final Object value = expression.getValue();
                return value instanceof Float && !((Float)value).isInfinite() && !(((Float)value).floatValue() == 0 && !TypeConversionUtil.isFPZero(expression.getText()));
            }
        }
        return false;
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        myExpression.replace(createFloatingPointExpression(project));
    }

    private PsiExpression createFloatingPointExpression(Project project) {
        final String text = myExpression.getText();
        if (StringUtil.endsWithIgnoreCase(text, "d")) {
            return JavaPsiFacade.getElementFactory(project).createExpressionFromText(text.substring(0, text.length() - 1) + "f", myExpression);
        } else {
            return JavaPsiFacade.getElementFactory(project).createExpressionFromText(text + "f", myExpression);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static void registerIntentions( JavaResolveResult[] candidates,
                                           PsiExpressionList list,
                                           HighlightInfo highlightInfo,
                                          TextRange fixRange) {
        if (candidates.length == 0) return;
        PsiExpression[] expressions = list.getExpressions();
        for (JavaResolveResult candidate : candidates) {
            registerIntention(expressions, highlightInfo, fixRange, candidate, list);
        }
    }

    private static void registerIntention( PsiExpression[] expressions,
                                           HighlightInfo highlightInfo,
                                          TextRange fixRange,
                                           JavaResolveResult candidate,
                                           PsiElement context) {
        if (!candidate.isStaticsScopeCorrect()) return;
        PsiMethod method = (PsiMethod)candidate.getElement();
        if (method != null && context.getManager().isInProject(method)) {
            final PsiParameter[] parameters = method.getParameterList().getParameters();
            if (parameters.length == expressions.length) {
                for (int i = 0, length = parameters.length; i < length; i++) {
                    PsiParameter parameter = parameters[i];
                    final PsiExpression expression = expressions[i];
                    if (expression instanceof PsiLiteralExpression && PsiType.FLOAT.equals(parameter.getType()) && PsiType.DOUBLE.equals(expression.getType())) {
                        QuickFixAction.registerQuickFixAction(highlightInfo, fixRange, new ConvertDoubleToFloatFix(expression));
                    }
                }
            }
        }
    }
}
