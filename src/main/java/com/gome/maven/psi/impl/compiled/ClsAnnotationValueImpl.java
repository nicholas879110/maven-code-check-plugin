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

import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.meta.MetaRegistry;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author ven
 */
public abstract class ClsAnnotationValueImpl extends ClsElementImpl implements PsiAnnotation, Navigatable {
    private final ClsElementImpl myParent;
    private final ClsJavaCodeReferenceElementImpl myReferenceElement;
    private final ClsAnnotationParameterListImpl myParameterList;

    @SuppressWarnings("AbstractMethodCallInConstructor")
    public ClsAnnotationValueImpl( ClsElementImpl parent) {
        myParent = parent;
        myReferenceElement = createReference();
        myParameterList = createParameterList();
    }

    protected abstract ClsAnnotationParameterListImpl createParameterList();

    protected abstract ClsJavaCodeReferenceElementImpl createReference();

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        buffer.append("@").append(myReferenceElement.getCanonicalText());
        myParameterList.appendMirrorText(indentLevel, buffer);
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, null);

        PsiAnnotation mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);
        setMirror(getNameReferenceElement(), mirror.getNameReferenceElement());
        setMirror(getParameterList(), mirror.getParameterList());
    }

    @Override
    
    public PsiElement[] getChildren() {
        return new PsiElement[]{myReferenceElement, myParameterList};
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitAnnotation(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    
    public PsiAnnotationParameterList getParameterList() {
        return myParameterList;
    }

    @Override
    
    public String getQualifiedName() {
        return myReferenceElement != null ? myReferenceElement.getCanonicalText() : null;
    }

    @Override
    public PsiJavaCodeReferenceElement getNameReferenceElement() {
        return myReferenceElement;
    }

    @Override
    public PsiAnnotationMemberValue findAttributeValue(String attributeName) {
        return PsiImplUtil.findAttributeValue(this, attributeName);
    }

    @Override
    
    public PsiAnnotationMemberValue findDeclaredAttributeValue( final String attributeName) {
        return PsiImplUtil.findDeclaredAttributeValue(this, attributeName);
    }

    @Override
    public <T extends PsiAnnotationMemberValue> T setDeclaredAttributeValue( String attributeName, T value) {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public String getText() {
        final StringBuilder buffer = new StringBuilder();
        appendMirrorText(0, buffer);
        return buffer.toString();
    }

    @Override
    public PsiMetaData getMetaData() {
        return MetaRegistry.getMetaBase(this);
    }
}
