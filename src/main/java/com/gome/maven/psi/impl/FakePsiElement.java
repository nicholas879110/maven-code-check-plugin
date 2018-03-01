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

package com.gome.maven.psi.impl;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.util.IncorrectOperationException;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public abstract class FakePsiElement extends PsiElementBase implements PsiNamedElement, ItemPresentation {

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    @Override
    
    public Language getLanguage() {
        return Language.ANY;
    }

    @Override
    
    public PsiElement[] getChildren() {
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    
    public PsiElement getFirstChild() {
        return null;
    }

    @Override
    
    public PsiElement getLastChild() {
        return null;
    }

    @Override
    
    public PsiElement getNextSibling() {
        return null;
    }

    @Override
    
    public PsiElement getPrevSibling() {
        return null;
    }

    @Override
    
    public TextRange getTextRange() {
        return null;
    }

    @Override
    public int getStartOffsetInParent() {
        return 0;
    }

    @Override
    public int getTextLength() {
        return 0;
    }

    @Override
    
    public PsiElement findElementAt(int offset) {
        return null;
    }

    @Override
    public int getTextOffset() {
        return 0;
    }

    @Override
    

    public String getText() {
        return null;
    }

    @Override
    
    public char[] textToCharArray() {
        return new char[0];
    }

    @Override
    public boolean textContains(char c) {
        return false;
    }

    @Override
    
    public ASTNode getNode() {
        return null;
    }

    @Override
    public String getPresentableText() {
        return getName();
    }

    @Override
    
    public String getLocationString() {
        return null;
    }

    @Override
    public final Icon getIcon(final int flags) {
        return super.getIcon(flags);
    }

    @Override
    protected final Icon getElementIcon(final int flags) {
        return super.getElementIcon(flags);
    }

    @Override
    
    public Icon getIcon(boolean open) {
        return null;
    }

    @Override
    public PsiElement setName(  String name) throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement replace( PsiElement newElement) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public void checkAdd( PsiElement element) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiElement add( PsiElement element) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiElement addBefore( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiElement addAfter( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public void delete() throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiElement copy() {
        return (PsiElement)clone();
    }

    @Override
    public PsiManager getManager() {
        final PsiElement parent = getParent();
        return parent != null ? parent.getManager() : null;
    }
}
