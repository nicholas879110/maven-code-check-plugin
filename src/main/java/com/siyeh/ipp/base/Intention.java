/*
 * Copyright 2003-2014 Dave Griffith, Bas Leijdekkers
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
package com.siyeh.ipp.base;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.siyeh.IntentionPowerPackBundle;
import com.siyeh.ig.psiutils.ComparisonUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import com.siyeh.ipp.psiutils.BoolUtils;

public abstract class Intention extends BaseElementAtCaretIntentionAction {

    private final PsiElementPredicate predicate;

    /**
     * @noinspection AbstractMethodCallInConstructor, OverridableMethodCallInConstructor
     */
    protected Intention() {
        predicate = getElementPredicate();
    }

    @Override
    public void invoke( Project project, Editor editor,  PsiElement element){
        if (prepareForWriting() && !FileModificationService.getInstance().preparePsiElementsForWrite(element)) {
            return;
        }
        final PsiElement matchingElement = findMatchingElement(element, editor);
        if (matchingElement == null) {
            return;
        }
        processIntention(editor, matchingElement);
    }

    protected boolean prepareForWriting() {
        return true;
    }

    protected abstract void processIntention( PsiElement element);

    protected void processIntention(Editor editor,  PsiElement element) {
        processIntention(element);
    }

    
    protected abstract PsiElementPredicate getElementPredicate();

    protected static void replaceExpressionWithNegatedExpression( PsiExpression newExpression,  PsiExpression expression){
        final Project project = expression.getProject();
        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiExpression expressionToReplace = expression;
        final String newExpressionText = newExpression.getText();
        final String expString;
        if (BoolUtils.isNegated(expression)) {
            expressionToReplace = BoolUtils.findNegation(expression);
            expString = newExpressionText;
        }
        else if (ComparisonUtils.isComparison(newExpression)) {
            final PsiBinaryExpression binaryExpression = (PsiBinaryExpression)newExpression;
            final String negatedComparison = ComparisonUtils.getNegatedComparison(binaryExpression.getOperationTokenType());
            final PsiExpression lhs = binaryExpression.getLOperand();
            final PsiExpression rhs = binaryExpression.getROperand();
            assert rhs != null;
            expString = lhs.getText() + negatedComparison + rhs.getText();
        }
        else {
            if (ParenthesesUtils.getPrecedence(newExpression) > ParenthesesUtils.PREFIX_PRECEDENCE) {
                expString = "!(" + newExpressionText + ')';
            }
            else {
                expString = '!' + newExpressionText;
            }
        }
        final PsiExpression newCall = factory.createExpressionFromText(expString, expression);
        assert expressionToReplace != null;
        final PsiElement insertedElement = expressionToReplace.replace(newCall);
        final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        codeStyleManager.reformat(insertedElement);
    }

    protected static void replaceExpressionWithNegatedExpressionString( String newExpression,  PsiExpression expression) {
        final Project project = expression.getProject();
        final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        final PsiElementFactory factory = psiFacade.getElementFactory();
        PsiExpression expressionToReplace = expression;
        final String expString;
        if (BoolUtils.isNegated(expression)) {
            expressionToReplace = BoolUtils.findNegation(expressionToReplace);
            expString = newExpression;
        }
        else {
            PsiElement parent = expressionToReplace.getParent();
            while (parent instanceof PsiParenthesizedExpression) {
                expressionToReplace = (PsiExpression)parent;
                parent = parent.getParent();
            }
            expString = "!(" + newExpression + ')';
        }
        final PsiExpression newCall = factory.createExpressionFromText(expString, expression);
        assert expressionToReplace != null;
        final PsiElement insertedElement = expressionToReplace.replace(newCall);
        final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        codeStyleManager.reformat(insertedElement);
    }

    
    PsiElement findMatchingElement( PsiElement element, Editor editor) {
        while (element != null) {
            if (!JavaLanguage.INSTANCE.equals(element.getLanguage())) {
                break;
            }
            if (predicate instanceof PsiElementEditorPredicate) {
                if (((PsiElementEditorPredicate)predicate).satisfiedBy(element, editor)) {
                    return element;
                }
            }
            else if (predicate.satisfiedBy(element)) {
                return element;
            }
            element = element.getParent();
            if (element instanceof PsiFile) {
                break;
            }
        }
        return null;
    }

    @Override
    public boolean isAvailable( Project project, Editor editor,  PsiElement element) {
        return findMatchingElement(element, editor) != null;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    private String getPrefix() {
        final Class<? extends Intention> aClass = getClass();
        final String name = aClass.getSimpleName();
        final StringBuilder buffer = new StringBuilder(name.length() + 10);
        buffer.append(Character.toLowerCase(name.charAt(0)));
        for (int i = 1; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                buffer.append('.');
                buffer.append(Character.toLowerCase(c));
            }
            else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    @Override
    
    public String getText() {
        //noinspection UnresolvedPropertyKey
        return IntentionPowerPackBundle.message(getPrefix() + ".name");
    }

    
    public String getFamilyName() {
        //noinspection UnresolvedPropertyKey
        return IntentionPowerPackBundle.defaultableMessage(getPrefix() + ".family.name");
    }
}