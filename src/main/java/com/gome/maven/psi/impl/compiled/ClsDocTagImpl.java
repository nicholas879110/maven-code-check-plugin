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

import com.gome.maven.psi.JavaElementVisitor;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.source.tree.JavaDocElementType;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.javadoc.PsiDocTag;
import com.gome.maven.psi.javadoc.PsiDocTagValue;
import com.gome.maven.util.IncorrectOperationException;

class ClsDocTagImpl extends ClsElementImpl implements PsiDocTag {
    private final ClsDocCommentImpl myDocComment;
    private final PsiElement myNameElement;

    public ClsDocTagImpl(ClsDocCommentImpl docComment,  String name) {
        myDocComment = docComment;
        myNameElement = new NameElement(this, name);
    }

    @Override
    public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        buffer.append(myNameElement.getText());
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, JavaDocElementType.DOC_TAG);
    }

    @Override
    public String getText() {
        return myNameElement.getText();
    }

    @Override
    
    public char[] textToCharArray() {
        return myNameElement.textToCharArray();
    }

    @Override
    
    public String getName() {
        return getNameElement().getText().substring(1);
    }

    @Override
    public boolean textMatches( CharSequence text) {
        return myNameElement.textMatches(text);
    }

    @Override
    public boolean textMatches( PsiElement element) {
        return myNameElement.textMatches(element);
    }

    @Override
    public int getTextLength() {
        return myNameElement.getTextLength();
    }

    @Override
    
    public PsiElement[] getChildren() {
        return new PsiElement[]{myNameElement};
    }

    @Override
    public PsiElement getParent() {
        return getContainingComment();
    }

    @Override
    public PsiDocComment getContainingComment() {
        return myDocComment;
    }

    @Override
    public PsiElement getNameElement() {
        return myNameElement;
    }

    @Override
    public PsiElement[] getDataElements() {
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    public PsiDocTagValue getValueElement() {
        return null;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitDocTag(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public PsiElement setName( String name) throws IncorrectOperationException {
        PsiImplUtil.setName(getNameElement(), name);
        return this;
    }

    private static class NameElement extends ClsElementImpl {
        private final ClsDocTagImpl myParent;
        private final String myText;

        public NameElement(ClsDocTagImpl parent, String text) {
            myParent = parent;
            myText = text;
        }

        @Override
        public String getText() {
            return myText;
        }

        @Override
        
        public char[] textToCharArray() {
            return myText.toCharArray();
        }

        @Override
        
        public PsiElement[] getChildren() {
            return PsiElement.EMPTY_ARRAY;
        }

        @Override
        public void appendMirrorText(int indentLevel,  StringBuilder buffer) {
        }

        @Override
        public void setMirror( TreeElement element) throws InvalidMirrorException {
            setMirrorCheckingType(element, null);
        }

        @Override
        public PsiElement getParent() {
            return myParent;
        }

        @Override
        public void accept( PsiElementVisitor visitor) {
            visitor.visitElement(this);
        }
    }
}
