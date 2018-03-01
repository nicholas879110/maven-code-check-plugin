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
package com.siyeh.ig;

import com.gome.maven.codeInspection.ProblemHighlightType;
import com.gome.maven.codeInspection.ProblemsHolder;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;

public abstract class BaseInspectionVisitor extends JavaElementVisitor {

    private BaseInspection inspection = null;
    private boolean onTheFly = false;
    private ProblemsHolder holder = null;

    final void setInspection(BaseInspection inspection) {
        this.inspection = inspection;
    }

    final void setOnTheFly(boolean onTheFly) {
        this.onTheFly = onTheFly;
    }

    public final boolean isOnTheFly() {
        return onTheFly;
    }

    protected final void registerNewExpressionError(
             PsiNewExpression expression, Object... infos) {
        final PsiJavaCodeReferenceElement classReference =
                expression.getClassOrAnonymousClassReference();
        if (classReference == null) {
            registerError(expression, infos);
        }
        else {
            registerError(classReference, infos);
        }
    }

    protected final void registerMethodCallError(
             PsiMethodCallExpression expression,
             Object... infos) {
        final PsiReferenceExpression methodExpression =
                expression.getMethodExpression();
        final PsiElement nameToken = methodExpression.getReferenceNameElement();
        if (nameToken == null) {
            registerError(expression, infos);
        }
        else {
            registerError(nameToken, infos);
        }
    }

    protected final void registerStatementError( PsiStatement statement,
                                                Object... infos) {
        final PsiElement statementToken = statement.getFirstChild();
        if (statementToken == null) {
            registerError(statement, infos);
        }
        else {
            registerError(statementToken, infos);
        }
    }

    protected final void registerClassError( PsiClass aClass,
                                            Object... infos) {
        PsiElement nameIdentifier;
        if (aClass instanceof PsiEnumConstantInitializer) {
            final PsiEnumConstantInitializer enumConstantInitializer =
                    (PsiEnumConstantInitializer)aClass;
            final PsiEnumConstant enumConstant =
                    enumConstantInitializer.getEnumConstant();
            nameIdentifier = enumConstant.getNameIdentifier();
        }
        else if (aClass instanceof PsiAnonymousClass) {
            final PsiAnonymousClass anonymousClass = (PsiAnonymousClass)aClass;
            nameIdentifier = anonymousClass.getBaseClassReference();
        }
        else {
            nameIdentifier = aClass.getNameIdentifier();
        }
        if (nameIdentifier != null && !nameIdentifier.isPhysical()) {
            nameIdentifier = nameIdentifier.getNavigationElement();
        }
        if (nameIdentifier == null || !nameIdentifier.isPhysical()) {
            registerError(aClass.getContainingFile(), infos);
        }
        else {
            registerError(nameIdentifier, infos);
        }
    }

    protected final void registerMethodError( PsiMethod method,
                                             Object... infos) {
        final PsiElement nameIdentifier = method.getNameIdentifier();
        if (nameIdentifier == null) {
            registerError(method.getContainingFile(), infos);
        }
        else {
            registerError(nameIdentifier, infos);
        }
    }

    protected final void registerVariableError( PsiVariable variable,
                                               Object... infos) {
        final PsiElement nameIdentifier = variable.getNameIdentifier();
        if (nameIdentifier == null) {
            registerError(variable, infos);
        }
        else {
            registerError(nameIdentifier, infos);
        }
    }

    protected final void registerTypeParameterError(
             PsiTypeParameter typeParameter, Object... infos) {
        final PsiElement nameIdentifier = typeParameter.getNameIdentifier();
        if (nameIdentifier == null) {
            registerError(typeParameter, infos);
        }
        else {
            registerError(nameIdentifier, infos);
        }
    }

    protected final void registerFieldError( PsiField field,
                                            Object... infos) {
        final PsiElement nameIdentifier = field.getNameIdentifier();
        registerError(nameIdentifier, infos);
    }

    protected final void registerModifierError(
             String modifier,  PsiModifierListOwner parameter,
            Object... infos) {
        final PsiModifierList modifiers = parameter.getModifierList();
        if (modifiers == null) {
            return;
        }
        final PsiElement[] children = modifiers.getChildren();
        for (final PsiElement child : children) {
            final String text = child.getText();
            if (modifier.equals(text)) {
                registerError(child, infos);
            }
        }
    }

    protected final void registerClassInitializerError(
             PsiClassInitializer initializer, Object... infos) {
        final PsiCodeBlock body = initializer.getBody();
        final PsiJavaToken lBrace = body.getLBrace();
        if (lBrace == null) {
            registerError(initializer, infos);
        }
        else {
            registerError(lBrace, infos);
        }
    }

    protected final void registerError( PsiElement location,
                                       Object... infos) {
        registerError(location, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, infos);
    }

    protected final void registerError( PsiElement location,
                                       final ProblemHighlightType highlightType,
                                       Object... infos) {
        if (location.getTextLength() == 0 && !(location instanceof PsiFile)) {
            return;
        }
        final InspectionGadgetsFix[] fixes = createFixes(infos);
        for (InspectionGadgetsFix fix : fixes) {
            fix.setOnTheFly(onTheFly);
        }
        final String description = inspection.buildErrorString(infos);
        holder.registerProblem(location, description, highlightType, fixes);
    }

    protected final void registerErrorAtOffset( PsiElement location, int offset, int length, Object... infos) {
        registerErrorAtOffset(location, offset, length, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, infos);
    }

    protected final void registerErrorAtOffset( PsiElement location, int offset, int length,
                                               ProblemHighlightType highlightType,
                                               Object... infos) {
        if (location.getTextLength() == 0 || length == 0) {
            return;
        }
        final InspectionGadgetsFix[] fixes = createFixes(infos);
        for (InspectionGadgetsFix fix : fixes) {
            fix.setOnTheFly(onTheFly);
        }
        final String description = inspection.buildErrorString(infos);
        final TextRange range = new TextRange(offset, offset + length);
        holder.registerProblem(location, description, highlightType, range, fixes);
    }

    
    private InspectionGadgetsFix[] createFixes(Object... infos) {
        if (!onTheFly && inspection.buildQuickFixesOnlyForOnTheFlyErrors()) {
            return InspectionGadgetsFix.EMPTY_ARRAY;
        }
        final InspectionGadgetsFix[] fixes = inspection.buildFixes(infos);
        if (fixes.length > 0) {
            return fixes;
        }
        final InspectionGadgetsFix fix = inspection.buildFix(infos);
        if (fix == null) {
            return InspectionGadgetsFix.EMPTY_ARRAY;
        }
        return new InspectionGadgetsFix[]{fix};
    }

    @Override
    public void visitReferenceExpression(PsiReferenceExpression expression) {
        visitExpression(expression);
    }

    public final void setProblemsHolder(ProblemsHolder holder) {
        this.holder = holder;
    }
}