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
import com.gome.maven.psi.JavaElementVisitor;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.PsiExpressionList;
import com.gome.maven.psi.PsiExpressionListStatement;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.CompositePsiElement;
import com.gome.maven.psi.impl.source.tree.TreeUtil;
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.ChildRoleBase;

public class PsiExpressionListStatementImpl extends CompositePsiElement implements PsiExpressionListStatement, Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiExpressionListStatementImpl");

    public PsiExpressionListStatementImpl() {
        super(EXPRESSION_LIST_STATEMENT);
    }

    @Override
    public PsiExpressionList getExpressionList() {
        return (PsiExpressionList)findChildByRoleAsPsiElement(ChildRole.EXPRESSION_LIST);
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.EXPRESSION_LIST:
                return findChildByType(EXPRESSION_LIST);

            case ChildRole.CLOSING_SEMICOLON:
                return TreeUtil.findChildBackward(this, SEMICOLON);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == EXPRESSION_LIST) {
            return ChildRole.EXPRESSION_LIST;
        }
        else if (i == SEMICOLON) {
            return ChildRole.CLOSING_SEMICOLON;
        }
        else {
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitExpressionListStatement(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiExpressionListStatement";
    }
}
