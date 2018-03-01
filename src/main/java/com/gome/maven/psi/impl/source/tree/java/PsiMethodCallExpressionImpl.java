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
package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.projectRoots.JavaSdkVersion;
import com.gome.maven.openapi.projectRoots.JavaVersionService;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.DebugUtil;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.source.resolve.JavaResolveCache;
import com.gome.maven.psi.impl.source.resolve.graphInference.InferenceSession;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.ElementType;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.infos.MethodCandidateInfo;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.PsiTypesUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.util.Function;

public class PsiMethodCallExpressionImpl extends ExpressionPsiElement implements PsiMethodCallExpression {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiMethodCallExpressionImpl");

    public PsiMethodCallExpressionImpl() {
        super(JavaElementType.METHOD_CALL_EXPRESSION);
    }

    @Override
    public PsiType getType() {
        return JavaResolveCache.getInstance(getProject()).getType(this, ourTypeEvaluator);
    }

    @Override
    public PsiMethod resolveMethod() {
        return (PsiMethod)getMethodExpression().resolve();
    }

    @Override
    
    public JavaResolveResult resolveMethodGenerics() {
        return getMethodExpression().advancedResolve(false);
    }

    @Override
    public void removeChild( ASTNode child) {
        if (child == getArgumentList()) {
            LOG.error("Cannot delete argument list since it will break contract on argument list notnullity");
        }
        super.removeChild(child);
    }

    @Override
    
    public PsiReferenceParameterList getTypeArgumentList() {
        PsiReferenceExpression expression = getMethodExpression();
        PsiReferenceParameterList result = expression.getParameterList();
        if (result != null) return result;
        LOG.error("Invalid method call expression. Children:\n" + DebugUtil.psiTreeToString(expression, false));
        return result;
    }

    @Override
    
    public PsiType[] getTypeArguments() {
        return getMethodExpression().getTypeParameters();
    }

    @Override
    
    public PsiReferenceExpression getMethodExpression() {
        return (PsiReferenceExpression)findChildByRoleAsPsiElement(ChildRole.METHOD_EXPRESSION);
    }

    @Override
    
    public PsiExpressionList getArgumentList() {
        PsiExpressionList list = (PsiExpressionList)findChildByRoleAsPsiElement(ChildRole.ARGUMENT_LIST);
        if (list == null) {
            LOG.error("Invalid PSI for'" + getText() + ". Parent:" + DebugUtil.psiToString(getParent(), false));
        }
        return list;
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch (role) {
            default:
                return null;

            case ChildRole.METHOD_EXPRESSION:
                return getFirstChildNode();

            case ChildRole.ARGUMENT_LIST:
                return findChildByType(JavaElementType.EXPRESSION_LIST);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == JavaElementType.EXPRESSION_LIST) {
            return ChildRole.ARGUMENT_LIST;
        }
        else {
            if (ElementType.EXPRESSION_BIT_SET.contains(child.getElementType())) {
                return ChildRole.METHOD_EXPRESSION;
            }
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitMethodCallExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiMethodCallExpression:" + getText();
    }

    private static final TypeEvaluator ourTypeEvaluator = new TypeEvaluator();

    private static class TypeEvaluator implements Function<PsiMethodCallExpression, PsiType> {
        @Override
        
        public PsiType fun(final PsiMethodCallExpression call) {
            PsiReferenceExpression methodExpression = call.getMethodExpression();
            PsiType theOnly = null;
            final JavaResolveResult[] results = methodExpression.multiResolve(false);
            LanguageLevel languageLevel = PsiUtil.getLanguageLevel(call);
            for (int i = 0; i < results.length; i++) {
                final PsiType type = getResultType(call, methodExpression, results[i], languageLevel);
                if (type == null) {
                    return null;
                }

                if (i == 0) {
                    theOnly = type;
                }
                else if (!theOnly.equals(type)) {
                    return null;
                }
            }

            return theOnly;
        }

        
        private static PsiType getResultType(PsiMethodCallExpression call,
                                             PsiReferenceExpression methodExpression,
                                             JavaResolveResult result,
                                              final LanguageLevel languageLevel) {
            final PsiMethod method = (PsiMethod)result.getElement();
            if (method == null) return null;

            boolean is15OrHigher = languageLevel.compareTo(LanguageLevel.JDK_1_5) >= 0;
            final PsiType getClassReturnType = PsiTypesUtil.patchMethodGetClassReturnType(call, methodExpression, method,
                    new Condition<IElementType>() {
                        @Override
                        public boolean value(IElementType type) {
                            return type != JavaElementType.CLASS;
                        }
                    }, languageLevel);

            if (getClassReturnType != null) {
                return getClassReturnType;
            }

            PsiType ret = method.getReturnType();
            if (ret == null) return null;
            if (ret instanceof PsiClassType) {
                ret = ((PsiClassType)ret).setLanguageLevel(languageLevel);
            }
            if (is15OrHigher) {
                return captureReturnType(call, method, ret, result, languageLevel);
            }
            return TypeConversionUtil.erasure(ret);
        }
    }

    public static PsiType captureReturnType(PsiMethodCallExpression call,
                                            PsiMethod method,
                                            PsiType ret,
                                            JavaResolveResult result,
                                            LanguageLevel languageLevel) {
        PsiSubstitutor substitutor = result.getSubstitutor();
        PsiType substitutedReturnType = substitutor.substitute(ret);
        if (substitutedReturnType == null) {
            return TypeConversionUtil.erasure(ret);
        }

        if (InferenceSession.wasUncheckedConversionPerformed(call)) {
            // 18.5.2
            // if unchecked conversion was necessary, then this substitution provides the parameter types of the invocation type, 
            // while the return type and thrown types are given by the erasure of m's type (without applying θ').
            return TypeConversionUtil.erasure(substitutedReturnType);
        }

        //15.12.2.6. Method Invocation Type
        // If unchecked conversion was necessary for the method to be applicable, 
        // the parameter types of the invocation type are the parameter types of the method's type,
        // and the return type and thrown types are given by the erasures of the return type and thrown types of the method's type.
        if (!languageLevel.isAtLeast(LanguageLevel.JDK_1_8) &&
                (method.hasTypeParameters() || JavaVersionService.getInstance().isAtLeast(call, JavaSdkVersion.JDK_1_8)) &&
                result instanceof MethodCandidateInfo && ((MethodCandidateInfo)result).isApplicable()) {
            final PsiType[] args = call.getArgumentList().getExpressionTypes();
            final boolean allowUncheckedConversion = false;
            final int applicabilityLevel = PsiUtil.getApplicabilityLevel(method, substitutor, args, languageLevel, allowUncheckedConversion, true);
            if (applicabilityLevel == MethodCandidateInfo.ApplicabilityLevel.NOT_APPLICABLE) {
                return TypeConversionUtil.erasure(substitutedReturnType);
            }
        }

        if (PsiUtil.isRawSubstitutor(method, substitutor)) {
            final PsiType returnTypeErasure = TypeConversionUtil.erasure(ret);
            if (Comparing.equal(TypeConversionUtil.erasure(substitutedReturnType), returnTypeErasure)) {
                return returnTypeErasure;
            }
        }
        PsiType lowerBound = PsiType.NULL;
        if (substitutedReturnType instanceof PsiCapturedWildcardType) {
            lowerBound = ((PsiCapturedWildcardType)substitutedReturnType).getLowerBound();
        } else if (substitutedReturnType instanceof PsiWildcardType) {
            lowerBound = ((PsiWildcardType)substitutedReturnType).getSuperBound();
        }
        if (lowerBound != PsiType.NULL) { //? super
            final PsiClass containingClass = method.getContainingClass();
            final PsiExpression qualifierExpression = call.getMethodExpression().getQualifierExpression();
            final PsiClass childClass = qualifierExpression != null ? PsiUtil.resolveClassInClassTypeOnly(qualifierExpression.getType()) : null;
            if (containingClass != null && childClass != null) {
                final PsiType typeInChildClassTypeParams = TypeConversionUtil.getSuperClassSubstitutor(containingClass, childClass, PsiSubstitutor.EMPTY).substitute(ret);
                final PsiClass substituted = PsiUtil.resolveClassInClassTypeOnly(typeInChildClassTypeParams);
                if (substituted instanceof PsiTypeParameter) {
                    final PsiClassType[] extendsListTypes = substituted.getExtendsListTypes();
                    if (extendsListTypes.length == 1) {
                        return extendsListTypes[0];
                    }
                }
            }
        }
        return PsiImplUtil.normalizeWildcardTypeByPosition(substitutedReturnType, call);
    }
}

