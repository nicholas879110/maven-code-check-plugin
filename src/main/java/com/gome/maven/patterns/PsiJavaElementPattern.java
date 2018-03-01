/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.patterns;

import com.gome.maven.psi.*;
import com.gome.maven.util.ProcessingContext;

/**
 * @author peter
 */
public class PsiJavaElementPattern<T extends PsiElement,Self extends PsiJavaElementPattern<T,Self>> extends PsiElementPattern<T,Self> {
     private static final String VALUE = "value";

    public PsiJavaElementPattern(final Class<T> aClass) {
        super(aClass);
    }

    public PsiJavaElementPattern( final InitialPatternCondition<T> condition) {
        super(condition);
    }

    public Self annotationParam( final String annotationQualifiedName,  final String parameterName) {
        return annotationParam(StandardPatterns.string().equalTo(annotationQualifiedName), parameterName);
    }

    public Self annotationParam( final String annotationQualifiedName) {
        return annotationParam(annotationQualifiedName, VALUE);
    }

    public Self annotationParam(final ElementPattern<String> annotationQualifiedName,  final String parameterName) {
        return withParent(
                PsiJavaPatterns.psiNameValuePair().withName(parameterName).withParent(
                        PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
                                PsiJavaPatterns.psiAnnotation().qName(annotationQualifiedName))));
    }

    public Self insideAnnotationParam(final ElementPattern<String> annotationQualifiedName,  final String parameterName) {
        return inside(true,
                PsiJavaPatterns.psiNameValuePair().withName(parameterName).withParent(
                        PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
                                PsiJavaPatterns.psiAnnotation().qName(annotationQualifiedName))));
    }

    public Self insideAnnotationParam(final ElementPattern<String> annotationQualifiedName) {
        return insideAnnotationParam(annotationQualifiedName, VALUE);
    }

    public Self nameIdentifierOf(final Class<? extends PsiMember> aClass) {
        return nameIdentifierOf(StandardPatterns.instanceOf(aClass));
    }

    public Self nameIdentifierOf(final ElementPattern<? extends PsiMember> pattern) {
        return with(new PatternCondition<T>("nameIdentifierOf") {
            @Override
            public boolean accepts( final T t, final ProcessingContext context) {
                if (!(t instanceof PsiIdentifier)) return false;

                final PsiElement parent = t.getParent();
                if (parent instanceof PsiClass && t != ((PsiClass) parent).getNameIdentifier()) return false;
                if (parent instanceof PsiMethod && t != ((PsiMethod) parent).getNameIdentifier()) return false;
                if (parent instanceof PsiVariable && t != ((PsiVariable) parent).getNameIdentifier()) return false;

                return pattern.accepts(parent, context);
            }
        });
    }

    public Self methodCallParameter(final int index, final ElementPattern<? extends PsiMethod> methodPattern) {
        return with(new PatternCondition<T>("methodCallParameter") {
            @Override
            public boolean accepts( final T literal, final ProcessingContext context) {
                final PsiElement parent = literal.getParent();
                if (parent instanceof PsiExpressionList) {
                    final PsiExpressionList psiExpressionList = (PsiExpressionList)parent;
                    final PsiExpression[] psiExpressions = psiExpressionList.getExpressions();
                    if (!(psiExpressions.length > index && psiExpressions[index] == literal)) return false;

                    final PsiElement element = psiExpressionList.getParent();
                    if (element instanceof PsiMethodCallExpression) {
                        final JavaResolveResult[] results = ((PsiMethodCallExpression)element).getMethodExpression().multiResolve(false);
                        for (JavaResolveResult result : results) {
                            final PsiElement psiElement = result.getElement();
                            if (methodPattern.accepts(psiElement, context)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });
    }

    public Self constructorParameter(final int index, final String... fqns) {
        return with(new PatternCondition<T>("methodCallParameter") {
            @Override
            public boolean accepts( final T literal, final ProcessingContext context) {
                final PsiElement parent = literal.getParent();
                if (parent instanceof PsiExpressionList) {
                    final PsiExpressionList psiExpressionList = (PsiExpressionList)parent;
                    final PsiExpression[] psiExpressions = psiExpressionList.getExpressions();
                    if (!(psiExpressions.length > index && psiExpressions[index] == literal)) return false;

                    final PsiElement element = psiExpressionList.getParent();
                    if (element instanceof PsiNewExpression) {
                        PsiJavaCodeReferenceElement reference = ((PsiNewExpression)element).getClassOrAnonymousClassReference();
                        if (reference != null) {
                            String qualifiedName = reference.getQualifiedName();
                            for (String fqn : fqns) {
                                if( fqn.equals(qualifiedName)) return true;
                            }
                        }
                    }
                }
                return false;
            }
        });
    }

    public static class Capture<T extends PsiElement> extends PsiJavaElementPattern<T, Capture<T>> {
        public Capture(final Class<T> aClass) {
            super(aClass);
        }

        public Capture( final InitialPatternCondition<T> condition) {
            super(condition);
        }
    }



}