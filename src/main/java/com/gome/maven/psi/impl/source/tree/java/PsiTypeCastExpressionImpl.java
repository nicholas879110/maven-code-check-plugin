/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.ChildRoleBase;

public class PsiTypeCastExpressionImpl extends ExpressionPsiElement implements PsiTypeCastExpression, Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiTypeCastExpressionImpl");

    public PsiTypeCastExpressionImpl() {
        super(TYPE_CAST_EXPRESSION);
    }

    @Override
    public PsiTypeElement getCastType() {
        return (PsiTypeElement)findChildByRoleAsPsiElement(ChildRole.TYPE);
    }

    @Override
    public PsiExpression getOperand() {
        return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.OPERAND);
    }

    @Override
     public PsiType getType() {
        final PsiTypeElement castType = getCastType();
        if (castType == null) return null;
        return PsiImplUtil.normalizeWildcardTypeByPosition(castType.getType(), this);
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.LPARENTH:
                return findChildByType(LPARENTH);

            case ChildRole.TYPE:
                return findChildByType(TYPE);

            case ChildRole.RPARENTH:
                return findChildByType(RPARENTH);

            case ChildRole.OPERAND:
                return findChildByType(EXPRESSION_BIT_SET);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        assert child.getTreeParent() == this: "child:"+child+"; child.getTreeParent():"+child.getTreeParent();
        IElementType i = child.getElementType();
        if (i == LPARENTH) {
            return ChildRole.LPARENTH;
        }
        else if (i == RPARENTH) {
            return ChildRole.RPARENTH;
        }
        else if (i == TYPE) {
            return ChildRole.TYPE;
        }
        else {
            if (EXPRESSION_BIT_SET.contains(child.getElementType())) {
                return ChildRole.OPERAND;
            }
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitTypeCastExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiTypeCastExpression:" + getText();
    }
}

