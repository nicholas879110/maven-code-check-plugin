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
package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;

public class PsiSuperExpressionImpl extends ExpressionPsiElement implements PsiSuperExpression, Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiSuperExpressionImpl");

    public PsiSuperExpressionImpl() {
        super(SUPER_EXPRESSION);
    }

    @Override
    public PsiJavaCodeReferenceElement getQualifier() {
        return (PsiJavaCodeReferenceElement)findChildByRoleAsPsiElement(ChildRole.QUALIFIER);
    }

    @Override
    public PsiType getType() {
        final PsiJavaCodeReferenceElement qualifier = getQualifier();
        if (qualifier != null) {
            final PsiElement aClass = qualifier.resolve();
            if (!(aClass instanceof PsiClass)) return null;
            return getSuperType((PsiClass)aClass, PsiUtil.isLanguageLevel8OrHigher(this));
        }

        for (PsiElement scope = getContext(); scope != null; scope = scope.getContext()) {
            if (scope instanceof PsiClass) {
                final PsiClass aClass = (PsiClass)scope;
                return getSuperType(aClass, false);
            }
            if (scope instanceof PsiExpressionList && scope.getParent() instanceof PsiAnonymousClass) {
                //noinspection AssignmentToForLoopParameter
                scope = scope.getParent();
            }
            else if (scope instanceof JavaCodeFragment) {
                PsiType fragmentSuperType = ((JavaCodeFragment)scope).getSuperType();
                if (fragmentSuperType != null) return fragmentSuperType;
            }
        }

        return null;
    }


    private PsiType getSuperType(PsiClass aClass, boolean checkImmediateSuperInterfaces) {
        if (CommonClassNames.JAVA_LANG_OBJECT.equals(aClass.getQualifiedName())) return null;

        final PsiClass containingClass = checkImmediateSuperInterfaces ? PsiTreeUtil.getParentOfType(this, PsiClass.class) : null;
        if (containingClass != null) {
            final PsiClassType[] superTypes;
            if (containingClass.isInterface()) {
                superTypes = containingClass.getExtendsListTypes();
            }
            else if (containingClass instanceof PsiAnonymousClass) {
                superTypes = new PsiClassType[]{((PsiAnonymousClass)containingClass).getBaseClassType()};
            }
            else {
                superTypes = containingClass.getImplementsListTypes();
            }

            for (PsiClassType superType : superTypes) {
                final PsiClass superClass = superType.resolve();
                if (superClass != null && superClass.isInterface() && aClass.equals(superClass)) return superType;
            }
        }

        if (aClass.isInterface()) {
            return PsiType.getJavaLangObject(getManager(), getResolveScope());
        }

        if (aClass instanceof PsiAnonymousClass) {
            final PsiClassType baseClassType = ((PsiAnonymousClass)aClass).getBaseClassType();
            final PsiClass psiClass = baseClassType.resolve();
            return psiClass != null && !psiClass.isInterface() ? baseClassType : PsiType.getJavaLangObject(getManager(), getResolveScope());
        }

        final PsiClassType[] superTypes = aClass.getExtendsListTypes();
        return superTypes.length == 0 ? PsiType.getJavaLangObject(getManager(), getResolveScope()) : superTypes[0];
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch (role) {
            default:
                return null;

            case ChildRole.QUALIFIER:
                return getFirstChildNode().getElementType() == JAVA_CODE_REFERENCE ? getFirstChildNode() : null;

            case ChildRole.DOT:
                return findChildByType(DOT);

            case ChildRole.SUPER_KEYWORD:
                return getLastChildNode();
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == JAVA_CODE_REFERENCE) {
            return ChildRole.QUALIFIER;
        }
        else if (i == DOT) {
            return ChildRole.DOT;
        }
        else if (i == SUPER_KEYWORD) {
            return ChildRole.SUPER_KEYWORD;
        }
        else {
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitSuperExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiSuperExpression:" + getText();
    }
}
