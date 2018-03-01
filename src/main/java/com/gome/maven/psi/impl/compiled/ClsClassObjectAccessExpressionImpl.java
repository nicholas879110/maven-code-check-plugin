/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.ElementBase;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.ui.RowIcon;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;

/**
 * @author ven
 */
public class ClsClassObjectAccessExpressionImpl extends ClsElementImpl implements PsiClassObjectAccessExpression {
    private final ClsElementImpl myParent;
    private final ClsTypeElementImpl myTypeElement;

    public ClsClassObjectAccessExpressionImpl(ClsElementImpl parent, String canonicalClassText) {
        myParent = parent;
        myTypeElement = new ClsTypeElementImpl(this, canonicalClassText, ClsTypeElementImpl.VARIANCE_NONE);
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        myTypeElement.appendMirrorText(0, buffer);
        buffer.append('.').append(PsiKeyword.CLASS);
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, null);
        setMirror(getOperand(), SourceTreeToPsiMap.<PsiClassObjectAccessExpression>treeToPsiNotNull(element).getOperand());
    }

    @Override
    
    public PsiElement[] getChildren() {
        return new PsiElement[]{myTypeElement};
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitClassObjectAccessExpression(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    
    public PsiTypeElement getOperand() {
        return myTypeElement;
    }

    @Override
    public PsiType getType() {
        return PsiImplUtil.getType(this);
    }

    @Override
    public String getText() {
        final StringBuilder buffer = new StringBuilder();
        appendMirrorText(0, buffer);
        return buffer.toString();
    }

    @Override
    public Icon getElementIcon(final int flags) {
        final RowIcon rowIcon = ElementBase.createLayeredIcon(this, PlatformIcons.FIELD_ICON, 0);
        rowIcon.setIcon(PlatformIcons.PUBLIC_ICON, 1);
        return rowIcon;
    }
}
