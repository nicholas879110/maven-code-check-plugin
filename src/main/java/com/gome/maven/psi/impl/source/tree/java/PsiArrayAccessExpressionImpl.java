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

public class PsiArrayAccessExpressionImpl extends ExpressionPsiElement implements PsiArrayAccessExpression, Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiArrayAccessExpressionImpl");

    public PsiArrayAccessExpressionImpl() {
        super(ARRAY_ACCESS_EXPRESSION);
    }

    @Override
    
    public PsiExpression getArrayExpression() {
        return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.ARRAY);
    }

    @Override
    public PsiExpression getIndexExpression() {
        return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.INDEX);
    }

    @Override
    public PsiType getType() {
        PsiType arrayType = getArrayExpression().getType();
        if (!(arrayType instanceof PsiArrayType)) return null;
        return GenericsUtil.getVariableTypeByExpressionType(((PsiArrayType)arrayType).getComponentType(), false);
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.ARRAY:
                return getFirstChildNode();

            case ChildRole.INDEX:
            {
                ASTNode lbracket = findChildByRole(ChildRole.LBRACKET);
                if (lbracket == null) return null;
                for(ASTNode child = lbracket.getTreeNext(); child != null; child = child.getTreeNext()){
                    if (EXPRESSION_BIT_SET.contains(child.getElementType())){
                        return child;
                    }
                }
                return null;
            }

            case ChildRole.LBRACKET:
                return findChildByType(LBRACKET);

            case ChildRole.RBRACKET:
                return findChildByType(RBRACKET);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == LBRACKET) {
            return ChildRole.LBRACKET;
        }
        else if (i == RBRACKET) {
            return ChildRole.RBRACKET;
        }
        else {
            if (EXPRESSION_BIT_SET.contains(child.getElementType())) {
                return child == getFirstChildNode() ? ChildRole.ARRAY : ChildRole.INDEX;
            }
            else {
                return ChildRoleBase.NONE;
            }
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitArrayAccessExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiArrayAccessExpression:" + getText();
    }
}

