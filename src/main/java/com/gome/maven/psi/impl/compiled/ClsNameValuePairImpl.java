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
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author ven
 */
public class ClsNameValuePairImpl extends ClsElementImpl implements PsiNameValuePair {
    private final ClsElementImpl myParent;
    private final ClsIdentifierImpl myNameIdentifier;
    private final PsiAnnotationMemberValue myMemberValue;

    public ClsNameValuePairImpl( ClsElementImpl parent,  String name,  PsiAnnotationMemberValue value) {
        myParent = parent;
        myNameIdentifier = name != null ? new ClsIdentifierImpl(this, name) : null;
        myMemberValue = ClsParsingUtil.getMemberValue(value, this);
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        appendText(myNameIdentifier, 0, buffer, " = ");
        appendText(myMemberValue, 0, buffer);
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, null);

        PsiNameValuePair mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);
        setMirrorIfPresent(getNameIdentifier(), mirror.getNameIdentifier());
        setMirrorIfPresent(getValue(), mirror.getValue());
    }

    @Override
    
    public PsiElement[] getChildren() {
        if (myNameIdentifier != null) {
            return new PsiElement[]{myNameIdentifier, myMemberValue};
        }
        else {
            return new PsiElement[]{myMemberValue};
        }
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitNameValuePair(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public PsiIdentifier getNameIdentifier() {
        return myNameIdentifier;
    }

    @Override
    public String getName() {
        return myNameIdentifier != null ? myNameIdentifier.getText() : null;
    }

    @Override
    public String getLiteralValue() {
        return null;
    }

    @Override
    public PsiAnnotationMemberValue getValue() {
        return myMemberValue;
    }

    @Override
    
    public PsiAnnotationMemberValue setValue( PsiAnnotationMemberValue newValue) {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }
}
