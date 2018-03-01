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
import com.gome.maven.psi.impl.source.tree.JavaDocElementType;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.javadoc.PsiDocTag;
import com.gome.maven.psi.tree.IElementType;

class ClsDocCommentImpl extends ClsElementImpl implements PsiDocComment, JavaTokenType, PsiJavaToken {
    private final PsiDocCommentOwner myParent;
    private final PsiDocTag[] myTags;

    ClsDocCommentImpl( PsiDocCommentOwner parent) {
        myParent = parent;
        myTags = new PsiDocTag[]{new ClsDocTagImpl(this, "@deprecated")};
    }

    @Override
    public void appendMirrorText(final int indentLevel,  final StringBuilder buffer) {
        buffer.append("/**");
        for (PsiDocTag tag : getTags()) {
            goNextLine(indentLevel + 1, buffer);
            buffer.append("* ");
            buffer.append(tag.getText());
        }
        goNextLine(indentLevel + 1, buffer);
        buffer.append("*/");
    }

    @Override
    public void setMirror( TreeElement element) throws InvalidMirrorException {
        setMirrorCheckingType(element, JavaDocElementType.DOC_COMMENT);
    }

    @Override
    
    public PsiElement[] getChildren() {
        return getTags();
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public PsiDocCommentOwner getOwner() {
        return myParent;
    }

    @Override
    
    public PsiElement[] getDescriptionElements() {
        return EMPTY_ARRAY;
    }

    @Override
    
    public PsiDocTag[] getTags() {
        return myTags;
    }

    @Override
    public PsiDocTag findTagByName( String name) {
        return name.equals("deprecated") ? getTags()[0] : null;
    }

    @Override
    
    public PsiDocTag[] findTagsByName( String name) {
        return name.equals("deprecated") ? getTags() : PsiDocTag.EMPTY_ARRAY;
    }

    @Override
    public IElementType getTokenType() {
        return JavaDocElementType.DOC_COMMENT;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitDocComment(this);
        }
        else {
            visitor.visitElement(this);
        }
    }
}
