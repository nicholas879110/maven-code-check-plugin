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
import com.gome.maven.psi.impl.DebugUtil;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.tree.IElementType;

public class PsiExpressionStatementImpl extends CompositePsiElement implements PsiExpressionStatement {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiExpressionStatementImpl");

    public PsiExpressionStatementImpl() {
        super(JavaElementType.EXPRESSION_STATEMENT);
    }

    @Override
    
    public PsiExpression getExpression() {
        PsiExpression expression = (PsiExpression)SourceTreeToPsiMap.treeElementToPsi(findChildByType(ElementType.EXPRESSION_BIT_SET));
        if (expression != null) return expression;
        LOG.error("Illegal PSI: \n" + DebugUtil.psiToString(getParent(), false));
        return null;
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.EXPRESSION:
                return findChildByType(ElementType.EXPRESSION_BIT_SET);

            case ChildRole.CLOSING_SEMICOLON:
                return TreeUtil.findChildBackward(this, JavaTokenType.SEMICOLON);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == JavaTokenType.SEMICOLON) {
            return ChildRole.CLOSING_SEMICOLON;
        }
        else {
            if (ElementType.EXPRESSION_BIT_SET.contains(child.getElementType())) {
                return ChildRole.EXPRESSION;
            }
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitExpressionStatement(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiExpressionStatement";
    }

    @Override
    public void deleteChildInternal( ASTNode child) {
        if (getChildRole(child) == ChildRole.EXPRESSION) {
            getTreeParent().deleteChildInternal(this);
        }
        else {
            super.deleteChildInternal(child);
        }
    }
}
