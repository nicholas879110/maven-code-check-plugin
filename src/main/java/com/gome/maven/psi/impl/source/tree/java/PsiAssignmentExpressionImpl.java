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
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.ElementType;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;

public class PsiAssignmentExpressionImpl extends ExpressionPsiElement implements PsiAssignmentExpression {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiAssignmentExpressionImpl");

    public PsiAssignmentExpressionImpl() {
        super(JavaElementType.ASSIGNMENT_EXPRESSION);
    }

    @Override
    
    public PsiExpression getLExpression() {
        return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.LOPERAND);
    }

    @Override
    public PsiExpression getRExpression() {
        return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.ROPERAND);
    }

    @Override
    
    public PsiJavaToken getOperationSign() {
        return (PsiJavaToken)findChildByRoleAsPsiElement(ChildRole.OPERATION_SIGN);
    }

    @Override
    
    public IElementType getOperationTokenType() {
        return getOperationSign().getTokenType();
    }

    @Override
    public PsiType getType() {
        return getLExpression().getType();
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch (role) {
            default:
                return null;

            case ChildRole.LOPERAND:
                return getFirstChildNode();

            case ChildRole.ROPERAND:
                return ElementType.EXPRESSION_BIT_SET.contains(getLastChildNode().getElementType()) ? getLastChildNode() : null;

            case ChildRole.OPERATION_SIGN:
                return findChildByType(OUR_OPERATIONS_BIT_SET);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        if (ElementType.EXPRESSION_BIT_SET.contains(child.getElementType())) {
            if (child == getFirstChildNode()) return ChildRole.LOPERAND;
            if (child == getLastChildNode()) return ChildRole.ROPERAND;
            return ChildRoleBase.NONE;
        }
        else if (OUR_OPERATIONS_BIT_SET.contains(child.getElementType())) {
            return ChildRole.OPERATION_SIGN;
        }
        else {
            return ChildRoleBase.NONE;
        }
    }

    private static final TokenSet OUR_OPERATIONS_BIT_SET = TokenSet.create(JavaTokenType.EQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ,
            JavaTokenType.PERCEQ, JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ,
            JavaTokenType.LTLTEQ, JavaTokenType.GTGTEQ, JavaTokenType.GTGTGTEQ,
            JavaTokenType.ANDEQ, JavaTokenType.OREQ, JavaTokenType.XOREQ);

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitAssignmentExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiAssignmentExpression:" + getText();
    }
}

