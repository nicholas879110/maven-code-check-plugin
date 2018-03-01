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
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.psi.PsiThrowStatement;
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.CompositePsiElement;
import com.gome.maven.psi.impl.source.tree.TreeUtil;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.ChildRoleBase;


public class PsiThrowStatementImpl extends CompositePsiElement implements PsiThrowStatement, Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiThrowStatementImpl");

    public PsiThrowStatementImpl() {
        super(THROW_STATEMENT);
    }

    @Override
    public PsiExpression getException() {
        return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.EXCEPTION);
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.THROW_KEYWORD:
                return findChildByType(THROW_KEYWORD);

            case ChildRole.EXCEPTION:
                return findChildByType(EXPRESSION_BIT_SET);

            case ChildRole.CLOSING_SEMICOLON:
                return TreeUtil.findChildBackward(this, SEMICOLON);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == THROW_KEYWORD) {
            return ChildRole.THROW_KEYWORD;
        }
        else if (i == SEMICOLON) {
            return ChildRole.CLOSING_SEMICOLON;
        }
        else {
            if (EXPRESSION_BIT_SET.contains(child.getElementType())) {
                return ChildRole.EXCEPTION;
            }
            else {
                return ChildRoleBase.NONE;
            }
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitThrowStatement(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiThrowStatement";
    }
}
