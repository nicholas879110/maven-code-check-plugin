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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.openapi.util.AtomicNotNullLazyValue;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.java.stubs.PsiAnnotationStub;
import com.gome.maven.psi.impl.meta.MetaRegistry;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author ven
 */
public class ClsAnnotationImpl extends ClsRepositoryPsiElement<PsiAnnotationStub> implements PsiAnnotation, Navigatable {
    private final NotNullLazyValue<ClsJavaCodeReferenceElementImpl> myReferenceElement;
    private final NotNullLazyValue<ClsAnnotationParameterListImpl> myParameterList;

    public ClsAnnotationImpl(final PsiAnnotationStub stub) {
        super(stub);
        myReferenceElement = new AtomicNotNullLazyValue<ClsJavaCodeReferenceElementImpl>() {
            
            @Override
            protected ClsJavaCodeReferenceElementImpl compute() {
                String text = PsiTreeUtil.getRequiredChildOfType(getStub().getPsiElement(), PsiJavaCodeReferenceElement.class).getText();
                return new ClsJavaCodeReferenceElementImpl(ClsAnnotationImpl.this, text);
            }
        };
        myParameterList = new AtomicNotNullLazyValue<ClsAnnotationParameterListImpl>() {
            
            @Override
            protected ClsAnnotationParameterListImpl compute() {
                PsiAnnotationParameterList paramList = PsiTreeUtil.getRequiredChildOfType(getStub().getPsiElement(), PsiAnnotationParameterList.class);
                return new ClsAnnotationParameterListImpl(ClsAnnotationImpl.this, paramList.getAttributes());
            }
        };
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        buffer.append("@").append(myReferenceElement.getValue().getCanonicalText());
        appendText(getParameterList(), indentLevel, buffer);
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
        return new PsiElement[]{myReferenceElement.getValue(), getParameterList()};
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
        return myParameterList.getValue();
    }

    @Override
    
    public String getQualifiedName() {
        return myReferenceElement.getValue().getCanonicalText();
    }

    @Override
    public PsiJavaCodeReferenceElement getNameReferenceElement() {
        return myReferenceElement.getValue();
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

    @Override
    public PsiAnnotationOwner getOwner() {
        return (PsiAnnotationOwner)getParent();//todo
    }
}
