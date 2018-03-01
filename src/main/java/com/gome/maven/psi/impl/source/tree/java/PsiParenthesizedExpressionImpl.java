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
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.ChildRoleBase;

public class PsiParenthesizedExpressionImpl extends ExpressionPsiElement implements PsiParenthesizedExpression, Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiParenthesizedExpressionImpl");

    public PsiParenthesizedExpressionImpl() {
        super(PARENTH_EXPRESSION);
    }

    @Override

    public PsiExpression getExpression() {
        return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.EXPRESSION);
    }

    @Override
    public PsiType getType() {
        PsiExpression expr = getExpression();
        if (expr == null) return null;
        return expr.getType();
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.LPARENTH:
                return findChildByType(LPARENTH);

            case ChildRole.RPARENTH:
                return findChildByType(RPARENTH);

            case ChildRole.EXPRESSION:
                return findChildByType(EXPRESSION_BIT_SET);

        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == LPARENTH) {
            return ChildRole.LPARENTH;
        }
        else if (i == RPARENTH) {
            return ChildRole.RPARENTH;
        }
        else {
            if (EXPRESSION_BIT_SET.contains(child.getElementType())) {
                return ChildRole.EXPRESSION;
            }
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitParenthesizedExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiParenthesizedExpression:" + getText();
    }
}

