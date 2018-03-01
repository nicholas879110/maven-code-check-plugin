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
import com.gome.maven.psi.JavaElementVisitor;
import com.gome.maven.psi.PsiBlockStatement;
import com.gome.maven.psi.PsiCodeBlock;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.impl.source.Constants;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.CompositePsiElement;

public class PsiBlockStatementImpl extends CompositePsiElement implements PsiBlockStatement {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiBlockStatementImpl");

    public PsiBlockStatementImpl() {
        super(JavaElementType.BLOCK_STATEMENT);
    }

    @Override
    
    public PsiCodeBlock getCodeBlock() {
        return (PsiCodeBlock)findChildByRoleAsPsiElement(ChildRole.BLOCK);
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.BLOCK:
                return findChildByType(JavaElementType.CODE_BLOCK);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        if (child.getElementType() == JavaElementType.CODE_BLOCK) {
            return ChildRole.BLOCK;
        }
        else {
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitBlockStatement(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiBlockStatement";
    }
}
