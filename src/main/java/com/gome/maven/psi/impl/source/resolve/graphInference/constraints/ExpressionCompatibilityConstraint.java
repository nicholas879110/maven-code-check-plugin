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
package com.gome.maven.psi.impl.source.resolve.graphInference.constraints;

import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.resolve.graphInference.InferenceSession;
import com.gome.maven.psi.impl.source.resolve.graphInference.InferenceVariable;
import com.gome.maven.psi.impl.source.resolve.graphInference.PsiPolyExpressionUtil;
import com.gome.maven.psi.infos.MethodCandidateInfo;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.TypeConversionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: anna
 */
public class ExpressionCompatibilityConstraint extends InputOutputConstraintFormula {
    private final PsiExpression myExpression;
    private PsiType myT;

    public ExpressionCompatibilityConstraint( PsiExpression expression,  PsiType type) {
        myExpression = expression;
        myT = type;
    }

    @Override
    public boolean reduce(InferenceSession session, List<ConstraintFormula> constraints) {
        if (!PsiPolyExpressionUtil.isPolyExpression(myExpression)) {
            if (session.isProperType(myT)) {
                return TypeConversionUtil.areTypesAssignmentCompatible(myT, myExpression);
            }

            final PsiType exprType = myExpression.getType();

            if (exprType instanceof PsiLambdaParameterType) {
                return false;
            }

            if (exprType != null && exprType != PsiType.NULL) {
                constraints.add(new TypeCompatibilityConstraint(myT, exprType));
            }
            return true;
        }
        if (myExpression instanceof PsiParenthesizedExpression) {
            final PsiExpression expression = ((PsiParenthesizedExpression)myExpression).getExpression();
            if (expression != null) {
                constraints.add(new ExpressionCompatibilityConstraint(expression, myT));
                return true;
            }
        }

        if (myExpression instanceof PsiConditionalExpression) {
            final PsiExpression thenExpression = ((PsiConditionalExpression)myExpression).getThenExpression();
            if (thenExpression != null) {
                constraints.add(new ExpressionCompatibilityConstraint(thenExpression, myT));
            }

            final PsiExpression elseExpression = ((PsiConditionalExpression)myExpression).getElseExpression();
            if (elseExpression != null) {
                constraints.add(new ExpressionCompatibilityConstraint(elseExpression, myT));
            }
            return true;
        }

        if (myExpression instanceof PsiCallExpression) {
            final PsiExpressionList argumentList = ((PsiCallExpression)myExpression).getArgumentList();
            if (argumentList != null) {
                final MethodCandidateInfo.CurrentCandidateProperties candidateProperties = MethodCandidateInfo.getCurrentMethod(((PsiCallExpression)myExpression).getArgumentList());
                final JavaResolveResult resolveResult;
                PsiType returnType = null;
                PsiTypeParameter[] typeParams = null;
                if (candidateProperties != null) {
                    resolveResult = null;
                }
                else {
                    if (myExpression instanceof PsiNewExpression) {
                        final PsiJavaCodeReferenceElement classReference = ((PsiNewExpression)myExpression).getClassOrAnonymousClassReference();
                        final PsiElement psiClass = classReference != null ? classReference.resolve() : null;
                        if (psiClass instanceof PsiClass) {

                            final JavaPsiFacade facade = JavaPsiFacade.getInstance(myExpression.getProject());
                            resolveResult = facade.getResolveHelper().resolveConstructor(facade.getElementFactory().createType((PsiClass)psiClass).rawType(), argumentList, myExpression);

                            returnType = JavaPsiFacade.getElementFactory(argumentList.getProject()).createType((PsiClass)psiClass, PsiSubstitutor.EMPTY);
                            typeParams = ((PsiClass)psiClass).getTypeParameters();
                        }
                        else {
                            resolveResult = JavaResolveResult.EMPTY;
                        }
                    }
                    else {
                        resolveResult = ((PsiCallExpression)myExpression).resolveMethodGenerics();
                    }
                }
                final PsiMethod method = candidateProperties != null ? candidateProperties.getMethod() : (PsiMethod)resolveResult.getElement();

                if (method != null && !method.isConstructor()) {
                    returnType = method.getReturnType();
                    if (returnType != null) {
                        typeParams = method.getTypeParameters();
                    }
                }

                if (typeParams != null) {
                    PsiSubstitutor siteSubstitutor =
                            resolveResult instanceof MethodCandidateInfo && method != null && !method.isConstructor() ? ((MethodCandidateInfo)resolveResult).getSiteSubstitutor() : candidateProperties != null ? candidateProperties.getSubstitutor() : PsiSubstitutor.EMPTY;
                    final InferenceSession callSession = new InferenceSession(typeParams, siteSubstitutor, myExpression.getManager(), myExpression);
                    callSession.propagateVariables(session.getInferenceVariables());
                    if (method != null) {
                        final PsiExpression[] args = argumentList.getExpressions();
                        final PsiParameter[] parameters = method.getParameterList().getParameters();
                        callSession.initExpressionConstraints(parameters, args, myExpression, method, resolveResult instanceof MethodCandidateInfo && ((MethodCandidateInfo)resolveResult).isVarargs() ||
                                candidateProperties != null && candidateProperties.isVarargs());
                    }
                    final boolean accepted = callSession.repeatInferencePhases(true);
                    if (!accepted) {
                        return false;
                    }
                    callSession.registerReturnTypeConstraints(siteSubstitutor.substitute(returnType), myT);
                    if (callSession.repeatInferencePhases(true)) {
                        session.registerNestedSession(callSession);
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }

        if (myExpression instanceof PsiMethodReferenceExpression) {
            constraints.add(new PsiMethodReferenceCompatibilityConstraint(((PsiMethodReferenceExpression)myExpression), myT));
            return true;
        }

        if (myExpression instanceof PsiLambdaExpression) {
            constraints.add(new LambdaExpressionCompatibilityConstraint((PsiLambdaExpression)myExpression, myT));
            return true;
        }


        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExpressionCompatibilityConstraint that = (ExpressionCompatibilityConstraint)o;

        if (!myExpression.equals(that.myExpression)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return myExpression.hashCode();
    }

    @Override
    public PsiExpression getExpression() {
        return myExpression;
    }

    @Override
    public PsiType getT() {
        return myT;
    }

    @Override
    protected void setT(PsiType t) {
        myT = t;
    }

    @Override
    protected InputOutputConstraintFormula createSelfConstraint(PsiType type, PsiExpression expression) {
        return new ExpressionCompatibilityConstraint(expression, type);
    }

    protected void collectReturnTypeVariables(InferenceSession session,
                                              PsiExpression psiExpression,
                                              PsiType returnType,
                                              Set<InferenceVariable> result) {
        if (psiExpression instanceof PsiLambdaExpression) {
            if (returnType != PsiType.VOID) {
                final List<PsiExpression> returnExpressions = LambdaUtil.getReturnExpressions((PsiLambdaExpression)psiExpression);
                for (PsiExpression expression : returnExpressions) {
                    final Set<InferenceVariable> resultInputVars = createSelfConstraint(returnType, expression).getInputVariables(session);
                    if (resultInputVars != null) {
                        result.addAll(resultInputVars);
                    }
                }
            }
        }
    }
}
