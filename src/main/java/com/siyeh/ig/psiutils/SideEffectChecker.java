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

import com.gome.maven.codeInspection.dataFlow.ControlFlowAnalyzer;
import com.gome.maven.psi.*;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.PropertyUtil;
import gnu.trove.THashSet;

import java.util.*;

public class SideEffectChecker {
    private static final Set<String> ourSideEffectFreeClasses = new THashSet<String>(Arrays.asList(
            Object.class.getName(),
            Short.class.getName(),
            Character.class.getName(),
            Byte.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Float.class.getName(),
            Double.class.getName(),
            String.class.getName(),
            StringBuffer.class.getName(),
            Boolean.class.getName(),

            ArrayList.class.getName(),
            Date.class.getName(),
            HashMap.class.getName(),
            HashSet.class.getName(),
            Hashtable.class.getName(),
            LinkedHashMap.class.getName(),
            LinkedHashSet.class.getName(),
            LinkedList.class.getName(),
            Stack.class.getName(),
            TreeMap.class.getName(),
            TreeSet.class.getName(),
            Vector.class.getName(),
            WeakHashMap.class.getName()));

    private SideEffectChecker() {
    }

    public static boolean mayHaveSideEffects( PsiExpression exp) {
        final SideEffectsVisitor visitor = new SideEffectsVisitor();
        exp.accept(visitor);
        return visitor.mayHaveSideEffects();
    }

    public static boolean checkSideEffects( PsiExpression element,  List<PsiElement> sideEffects) {
        final SideEffectsVisitor visitor = new SideEffectsVisitor();
        element.accept(visitor);
        if (visitor.sideEffect != null) {
            sideEffects.add(visitor.sideEffect);
            return true;
        }
        return false;
    }

    private static class SideEffectsVisitor extends JavaRecursiveElementWalkingVisitor {
        PsiElement sideEffect;

        @Override
        public void visitElement( PsiElement element) {
            if (sideEffect == null) {
                super.visitElement(element);
            }
        }

        @Override
        public void visitAssignmentExpression(
                 PsiAssignmentExpression expression) {
            if (sideEffect != null) {
                return;
            }
            super.visitAssignmentExpression(expression);
            sideEffect = expression;
        }

        @Override
        public void visitMethodCallExpression(
                 PsiMethodCallExpression expression) {
            if (sideEffect != null) {
                return;
            }
            super.visitMethodCallExpression(expression);
            final PsiMethod method = expression.resolveMethod();
            if (method != null && (PropertyUtil.isSimpleGetter(method) || ControlFlowAnalyzer.isPure(method))) {
                return;
            }

            sideEffect = expression;
        }

        @Override
        public void visitNewExpression( PsiNewExpression expression) {
            if (sideEffect != null) {
                return;
            }
            super.visitNewExpression(expression);
            sideEffect = isSideEffectFreeConstructor(expression) ? null : expression;
        }

        @Override
        public void visitPostfixExpression(
                 PsiPostfixExpression expression) {
            if (sideEffect != null) {
                return;
            }
            super.visitPostfixExpression(expression);
            final IElementType tokenType = expression.getOperationTokenType();
            if (tokenType.equals(JavaTokenType.PLUSPLUS) ||
                    tokenType.equals(JavaTokenType.MINUSMINUS)) {
                sideEffect = expression;
            }
        }

        @Override
        public void visitPrefixExpression(
                 PsiPrefixExpression expression) {
            if (sideEffect != null) {
                return;
            }
            super.visitPrefixExpression(expression);
            final IElementType tokenType = expression.getOperationTokenType();
            if (tokenType.equals(JavaTokenType.PLUSPLUS) ||
                    tokenType.equals(JavaTokenType.MINUSMINUS)) {
                sideEffect = expression;
            }
        }

        public boolean mayHaveSideEffects() {
            return sideEffect != null;
        }
    }

    private static boolean isSideEffectFreeConstructor( PsiNewExpression newExpression) {
        PsiJavaCodeReferenceElement classReference = newExpression.getClassReference();
        PsiClass aClass = classReference == null ? null : (PsiClass)classReference.resolve();
        String qualifiedName = aClass == null ? null : aClass.getQualifiedName();
        if (qualifiedName == null) return false;
        if (ourSideEffectFreeClasses.contains(qualifiedName)) return true;

        PsiFile file = aClass.getContainingFile();
        PsiDirectory directory = file.getContainingDirectory();
        PsiPackage classPackage = directory == null ? null : JavaDirectoryService.getInstance().getPackage(directory);
        String packageName = classPackage == null ? null : classPackage.getQualifiedName();

        // all Throwable descendants from java.lang are side effects free
        if (CommonClassNames.DEFAULT_PACKAGE.equals(packageName) || "java.io".equals(packageName)) {
            PsiClass throwableClass = JavaPsiFacade.getInstance(aClass.getProject()).findClass("java.lang.Throwable", aClass.getResolveScope());
            if (throwableClass != null && com.gome.maven.psi.util.InheritanceUtil.isInheritorOrSelf(aClass, throwableClass, true)) {
                return true;
            }
        }
        return false;
    }
}
