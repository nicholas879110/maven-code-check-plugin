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

import com.gome.maven.codeInsight.daemon.QuickFixBundle;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.refactoring.util.RefactoringChangeUtil;
import com.gome.maven.util.IncorrectOperationException;

public class AddTypeArgumentsFix extends MethodArgumentFix {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.daemon.impl.quickfix.AddTypeArgumentsFix");

    private AddTypeArgumentsFix(PsiExpressionList list, int i, PsiType toType, final ArgumentFixerActionFactory factory) {
        super(list, i, toType, factory);
    }

    @Override

    public String getText() {
        if (myArgList.getExpressions().length == 1) {
            return QuickFixBundle.message("add.type.arguments.single.argument.text");
        }

        return QuickFixBundle.message("add.type.arguments.text", myIndex + 1);
    }

    private static class MyFixerActionFactory extends ArgumentFixerActionFactory {
        @Override
        public AddTypeArgumentsFix createFix(final PsiExpressionList list, final int i, final PsiType toType) {
            return new AddTypeArgumentsFix(list, i, toType, this);
        }

        @Override
        protected PsiExpression getModifiedArgument(final PsiExpression expression, final PsiType toType) throws IncorrectOperationException {
            if (!PsiUtil.isLanguageLevel5OrHigher(expression)) return null;

            if (expression instanceof PsiMethodCallExpression) {
                final PsiMethodCallExpression methodCall = (PsiMethodCallExpression)expression;
                final PsiReferenceParameterList list = methodCall.getMethodExpression().getParameterList();
                if (list == null || list.getTypeArguments().length > 0) return null;
                final JavaResolveResult resolveResult = methodCall.resolveMethodGenerics();
                final PsiElement element = resolveResult.getElement();
                if (element instanceof PsiMethod) {
                    final PsiMethod method = (PsiMethod)element;
                    final PsiType returnType = method.getReturnType();
                    if (returnType == null) return null;

                    final PsiTypeParameter[] typeParameters = method.getTypeParameters();
                    if (typeParameters.length > 0) {
                        PsiType[] mappings = PsiType.createArray(typeParameters.length);
                        PsiResolveHelper helper = JavaPsiFacade.getInstance(expression.getProject()).getResolveHelper();
                        LanguageLevel level = PsiUtil.getLanguageLevel(expression);
                        for (int i = 0; i < typeParameters.length; i++) {
                            PsiTypeParameter typeParameter = typeParameters[i];
                            final PsiType substitution = helper.getSubstitutionForTypeParameter(typeParameter, returnType, toType, false, level);
                            if (substitution == null || PsiType.NULL.equals(substitution)) return null;
                            mappings[i] = GenericsUtil.eliminateWildcards(substitution, false);
                        }

                        final PsiElementFactory factory = JavaPsiFacade.getInstance(expression.getProject()).getElementFactory();
                        PsiMethodCallExpression copy = (PsiMethodCallExpression)expression.copy();
                        final PsiReferenceExpression methodExpression = copy.getMethodExpression();
                        final PsiReferenceParameterList parameterList = methodExpression.getParameterList();
                        LOG.assertTrue(parameterList != null);
                        for (PsiType mapping : mappings) {
                            parameterList.add(factory.createTypeElement(mapping));
                        }
                        if (methodExpression.getQualifierExpression() == null) {
                            final PsiExpression qualifierExpression;
                            final PsiClass containingClass = method.getContainingClass();
                            LOG.assertTrue(containingClass != null);
                            if (method.hasModifierProperty(PsiModifier.STATIC)) {
                                qualifierExpression = factory.createReferenceExpression(containingClass);
                            } else {
                                qualifierExpression = RefactoringChangeUtil.createThisExpression(method.getManager(), null);
                            }
                            methodExpression.setQualifierExpression(qualifierExpression);
                        }

                        return (PsiExpression)JavaCodeStyleManager.getInstance(copy.getProject()).shortenClassReferences(copy);
                    }
                }
            }

            return null;
        }

        @Override
        public boolean areTypesConvertible(final PsiType exprType, final PsiType parameterType, final PsiElement context) {
            return !(exprType instanceof PsiPrimitiveType) && !(parameterType instanceof PsiPrimitiveType) || TypeConversionUtil.boxingConversionApplicable(exprType, parameterType);
        }
    }

    public static ArgumentFixerActionFactory REGISTRAR = new AddTypeArgumentsFix.MyFixerActionFactory();
}
