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
import com.gome.maven.psi.impl.source.tree.CompositePsiElement;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.tree.IElementType;

/**
 * @author dsl
 */
public class PsiForeachStatementImpl extends CompositePsiElement implements PsiForeachStatement, Constants {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiForeachStatementImpl");
    public PsiForeachStatementImpl() {
        super(FOREACH_STATEMENT);
    }

    @Override
    
    public PsiParameter getIterationParameter() {
        return (PsiParameter) findChildByRoleAsPsiElement(ChildRole.FOR_ITERATION_PARAMETER);
    }

    @Override
    public PsiExpression getIteratedValue() {
        return (PsiExpression) findChildByRoleAsPsiElement(ChildRole.FOR_ITERATED_VALUE);
    }

    @Override
    public PsiStatement getBody() {
        return (PsiStatement) findChildByRoleAsPsiElement(ChildRole.LOOP_BODY);
    }

    @Override
    
    public PsiJavaToken getLParenth() {
        return (PsiJavaToken) findChildByRoleAsPsiElement(ChildRole.LPARENTH);
    }

    @Override
    public PsiJavaToken getRParenth() {
        return (PsiJavaToken) findChildByRoleAsPsiElement(ChildRole.RPARENTH);
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));

        switch(role) {
            case ChildRole.LOOP_BODY:
                return PsiImplUtil.findStatementChild(this);

            case ChildRole.FOR_ITERATED_VALUE:
                return findChildByType(EXPRESSION_BIT_SET);

            case ChildRole.FOR_KEYWORD:
                return getFirstChildNode();

            case ChildRole.LPARENTH:
                return findChildByType(LPARENTH);

            case ChildRole.RPARENTH:
                return findChildByType(RPARENTH);

            case ChildRole.FOR_ITERATION_PARAMETER:
                return findChildByType(PARAMETER);

            case ChildRole.COLON:
                return findChildByType(COLON);

            default:
                return null;
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);

        IElementType i = child.getElementType();
        if (i == FOR_KEYWORD) {
            return ChildRole.FOR_KEYWORD;
        }
        else if (i == LPARENTH) {
            return ChildRole.LPARENTH;
        }
        else if (i == RPARENTH) {
            return ChildRole.RPARENTH;
        }
        else if (i == PARAMETER) {
            return ChildRole.FOR_ITERATION_PARAMETER;
        }
        else if (i == COLON) {
            return ChildRole.COLON;
        }
        else {
            if (EXPRESSION_BIT_SET.contains(child.getElementType())) {
                return ChildRole.FOR_ITERATED_VALUE;
            }
            else if (child.getPsi() instanceof PsiStatement) {
                return ChildRole.LOOP_BODY;
            }
            else {
                return ChildRoleBase.NONE;
            }
        }
    }

    public String toString() {
        return "PsiForeachStatement";
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,  ResolveState state, PsiElement lastParent,  PsiElement place) {
        processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, this);
        if (lastParent == null || lastParent.getParent() != this /*|| lastParent == getIteratedValue()*/)
            // Parent element should not see our vars
            return true;

        return processor.execute(getIterationParameter(), state);
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitForeachStatement(this);
        }
        else {
            visitor.visitElement(this);
        }
    }
}
