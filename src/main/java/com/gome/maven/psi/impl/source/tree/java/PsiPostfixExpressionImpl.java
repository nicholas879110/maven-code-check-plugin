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
package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.ChildRoleBase;

public class PsiPostfixExpressionImpl extends ExpressionPsiElement implements PsiPostfixExpression {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiPostfixExpressionImpl");

    public PsiPostfixExpressionImpl() {
        super(JavaElementType.POSTFIX_EXPRESSION);
    }

    @Override
    
    public PsiExpression getOperand() {
        return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.OPERAND);
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
        return getOperand().getType();
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.OPERAND:
                return getFirstChildNode();

            case ChildRole.OPERATION_SIGN:
                return getLastChildNode();
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        if (child == getFirstChildNode()) return ChildRole.OPERAND;
        if (child == getLastChildNode()) return ChildRole.OPERATION_SIGN;
        return ChildRoleBase.NONE;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitPostfixExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiPostfixExpression:" + getText();
    }
}

