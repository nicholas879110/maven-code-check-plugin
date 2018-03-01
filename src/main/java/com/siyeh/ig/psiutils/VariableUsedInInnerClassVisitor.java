/*
 * Copyright 2003-2013 Dave Griffith, Bas Leijdekkers
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
import com.gome.maven.psi.util.PsiTreeUtil;

class VariableUsedInInnerClassVisitor extends JavaRecursiveElementVisitor {

     private final PsiVariable variable;
    private boolean usedInInnerClass = false;
    private boolean inInnerClass = false;

    public VariableUsedInInnerClassVisitor( PsiVariable variable) {
        this.variable = variable;
    }

    @Override
    public void visitElement( PsiElement element) {
        if (!usedInInnerClass) {
            super.visitElement(element);
        }
    }

    @Override
    public void visitClass( PsiClass aClass) {
        if (usedInInnerClass) {
            return;
        }
        final boolean wasInInnerClass = inInnerClass;
        if (!inInnerClass) {
            inInnerClass = true;
            if (aClass instanceof PsiAnonymousClass) {
                final PsiExpressionList argumentList = ((PsiAnonymousClass)aClass).getArgumentList();
                if (argumentList != null) {
                    for (PsiClass localAndAnonymousClasses : PsiTreeUtil.findChildrenOfType(argumentList, PsiClass.class)) {
                        localAndAnonymousClasses.accept(this);
                    }
                }
            }
            PsiElement child = aClass.getLBrace();
            while (child != null) {
                child.accept(this);
                child = child.getNextSibling();
            }
        } else {
            inInnerClass = true;
            super.visitClass(aClass);
        }
        inInnerClass = wasInInnerClass;
    }

    @Override
    public void visitReferenceExpression( PsiReferenceExpression referenceExpression) {
        if (usedInInnerClass) {
            return;
        }
        super.visitReferenceExpression(referenceExpression);
        if (!inInnerClass) {
            return;
        }
        final PsiElement target = referenceExpression.resolve();
        if (variable.equals(target)) {
            usedInInnerClass = true;
        }
    }

    public boolean isUsedInInnerClass() {
        return usedInInnerClass;
    }
}