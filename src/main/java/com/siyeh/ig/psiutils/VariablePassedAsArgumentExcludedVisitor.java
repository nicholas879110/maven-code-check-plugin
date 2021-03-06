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
package com.siyeh.ig.psiutils;

import com.gome.maven.psi.*;

import java.util.Set;

class VariablePassedAsArgumentExcludedVisitor extends JavaRecursiveElementVisitor {

    
    private final PsiVariable variable;
    private final Set<String> excludes;
    private final boolean myBuilderPattern;

    private boolean passed = false;

    public VariablePassedAsArgumentExcludedVisitor( PsiVariable variable,  Set<String> excludes, boolean builderPattern) {
        this.variable = variable;
        this.excludes = excludes;
        myBuilderPattern = builderPattern;
    }

    @Override
    public void visitElement( PsiElement element) {
        if (passed) {
            return;
        }
        super.visitElement(element);
    }

    @Override
    public void visitCallExpression(PsiCallExpression callExpression) {
        if (passed) {
            return;
        }
        super.visitCallExpression(callExpression);
        visitCall(callExpression);
    }

    @Override
    public void visitEnumConstant(PsiEnumConstant enumConstant) {
        if (passed) {
            return;
        }
        super.visitEnumConstant(enumConstant);
        visitCall(enumConstant);
    }

    private void visitCall(PsiCall call) {
        final PsiExpressionList argumentList = call.getArgumentList();
        if (argumentList == null) {
            return;
        }
        for (PsiExpression argument : argumentList.getExpressions()) {
            if (!VariableAccessUtils.mayEvaluateToVariable(argument, variable, myBuilderPattern)) {
                continue;
            }
            final PsiMethod method = call.resolveMethod();
            if (method != null) {
                final PsiClass aClass = method.getContainingClass();
                if (aClass != null) {
                    final String name = aClass.getQualifiedName();
                    if (excludes.contains(name)) {
                        continue;
                    }
                }
            }
            passed = true;
        }
    }

    public boolean isPassed() {
        return passed;
    }
}