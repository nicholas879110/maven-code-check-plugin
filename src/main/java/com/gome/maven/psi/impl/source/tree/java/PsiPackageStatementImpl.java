/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.tree.ChildRoleBase;
import com.gome.maven.psi.tree.IElementType;

public class PsiPackageStatementImpl extends CompositePsiElement implements PsiPackageStatement {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.java.PsiPackageStatementImpl");

    public PsiPackageStatementImpl() {
        super(JavaElementType.PACKAGE_STATEMENT);
    }

    @Override
    public PsiJavaCodeReferenceElement getPackageReference() {
        return (PsiJavaCodeReferenceElement)findChildByRoleAsPsiElement(ChildRole.PACKAGE_REFERENCE);
    }

    @Override
    public String getPackageName() {
        PsiJavaCodeReferenceElement ref = getPackageReference();
        return ref == null ? null : JavaSourceUtil.getReferenceText(ref);
    }

    @Override
    public PsiModifierList getAnnotationList() {
        return (PsiModifierList)findChildByRoleAsPsiElement(ChildRole.MODIFIER_LIST);
    }

    @Override
    public ASTNode findChildByRole(int role) {
        LOG.assertTrue(ChildRole.isUnique(role));
        switch(role){
            default:
                return null;

            case ChildRole.PACKAGE_KEYWORD:
                return findChildByType(JavaTokenType.PACKAGE_KEYWORD);

            case ChildRole.PACKAGE_REFERENCE:
                return findChildByType(JavaElementType.JAVA_CODE_REFERENCE);

            case ChildRole.CLOSING_SEMICOLON:
                return TreeUtil.findChildBackward(this, JavaTokenType.SEMICOLON);

            case ChildRole.MODIFIER_LIST:
                return findChildByType(JavaElementType.MODIFIER_LIST);
        }
    }

    @Override
    public int getChildRole(ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this);
        IElementType i = child.getElementType();
        if (i == JavaTokenType.PACKAGE_KEYWORD) {
            return ChildRole.PACKAGE_KEYWORD;
        }
        else if (i == JavaElementType.JAVA_CODE_REFERENCE) {
            return ChildRole.PACKAGE_REFERENCE;
        }
        else if (i == JavaTokenType.SEMICOLON) {
            return ChildRole.CLOSING_SEMICOLON;
        }
        else if (i == JavaElementType.MODIFIER_LIST) {
            return ChildRole.MODIFIER_LIST;
        }
        else {
            return ChildRoleBase.NONE;
        }
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitPackageStatement(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "PsiPackageStatement:" + getPackageName();
    }
}
