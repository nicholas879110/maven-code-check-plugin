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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.core.JavaCoreBundle;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleSettingsFacade;
import com.gome.maven.psi.impl.PsiElementBase;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Arrays;
import java.util.List;

public abstract class ClsElementImpl extends PsiElementBase implements PsiCompiledElement {
    public static final Key<PsiCompiledElement> COMPILED_ELEMENT = Key.create("COMPILED_ELEMENT");

    protected static final String CAN_NOT_MODIFY_MESSAGE = JavaCoreBundle.message("psi.error.attempt.to.edit.class.file");

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.compiled.ClsElementImpl");

    private volatile TreeElement myMirror = null;

    @Override
    
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }

    @Override
    public PsiManager getManager() {
        return getParent().getManager();
    }

    @Override
    public PsiFile getContainingFile() {
        PsiElement parent = getParent();
        if (parent == null) {
            throw new PsiInvalidElementAccessException(this);
        }
        return parent.getContainingFile();
    }

    @Override
    public final boolean isWritable() {
        return false;
    }

    @Override
    public boolean isPhysical() {
        return true;
    }

    @Override
    public boolean isValid() {
        PsiElement parent = getParent();
        return parent != null && parent.isValid();
    }

    @Override
    public PsiElement copy() {
        return this;
    }

    
    protected PsiElement[] getChildren( PsiElement... children) {
        if (children == null) {
            return PsiElement.EMPTY_ARRAY;
        }

        List<PsiElement> list = ContainerUtil.newArrayListWithCapacity(children.length);
        for (PsiElement child : children) {
            if (child != null) {
                list.add(child);
            }
        }
        return PsiUtilCore.toPsiElementArray(list);
    }

    @Override
    public void checkAdd( PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public PsiElement add( PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public PsiElement addBefore( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public PsiElement addAfter( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public void delete() throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    @Override
    public PsiElement replace( PsiElement newElement) throws IncorrectOperationException {
        throw new IncorrectOperationException(CAN_NOT_MODIFY_MESSAGE);
    }

    public abstract void appendMirrorText(int indentLevel,  StringBuilder buffer);

    protected int getIndentSize() {
        return JavaCodeStyleSettingsFacade.getInstance(getProject()).getIndentSize();
    }

    public abstract void setMirror( TreeElement element) throws InvalidMirrorException;

    @Override
    public PsiElement getMirror() {
        TreeElement mirror = myMirror;
        if (mirror == null) {
            ((ClsFileImpl)getContainingFile()).getMirror();
            mirror = myMirror;
        }
        return SourceTreeToPsiMap.treeElementToPsi(mirror);
    }

    @Override
    public final TextRange getTextRange() {
        PsiElement mirror = getMirror();
        return mirror != null ? mirror.getTextRange() : TextRange.EMPTY_RANGE;
    }

    @Override
    public final int getStartOffsetInParent() {
        PsiElement mirror = getMirror();
        return mirror != null ? mirror.getStartOffsetInParent() : -1;
    }

    @Override
    public int getTextLength() {
        String text = getText();
        return text == null ? 0 : text.length();
    }

    @Override
    public PsiElement findElementAt(int offset) {
        PsiElement mirror = getMirror();
        if (mirror == null) return null;
        PsiElement mirrorAt = mirror.findElementAt(offset);
        while (true) {
            if (mirrorAt == null || mirrorAt instanceof PsiFile) return null;
            PsiElement elementAt = mirrorToElement(mirrorAt);
            if (elementAt != null) return elementAt;
            mirrorAt = mirrorAt.getParent();
        }
    }

    @Override
    public PsiReference findReferenceAt(int offset) {
        PsiElement mirror = getMirror();
        if (mirror == null) return null;
        PsiReference mirrorRef = mirror.findReferenceAt(offset);
        if (mirrorRef == null) return null;
        PsiElement mirrorElement = mirrorRef.getElement();
        PsiElement element = mirrorToElement(mirrorElement);
        if (element == null) return null;
        return element.getReference();
    }

    
    private PsiElement mirrorToElement(PsiElement mirror) {
        final PsiElement m = getMirror();
        if (m == mirror) return this;

        PsiElement[] children = getChildren();
        if (children.length == 0) return null;

        for (PsiElement child : children) {
            ClsElementImpl clsChild = (ClsElementImpl)child;
            if (PsiTreeUtil.isAncestor(clsChild.getMirror(), mirror, false)) {
                PsiElement element = clsChild.mirrorToElement(mirror);
                if (element != null) return element;
            }
        }

        return null;
    }

    @Override
    public final int getTextOffset() {
        PsiElement mirror = getMirror();
        return mirror != null ? mirror.getTextOffset() : -1;
    }

    @Override
    public String getText() {
        PsiElement mirror = getMirror();
        if (mirror != null) return mirror.getText();

        StringBuilder buffer = new StringBuilder();
        appendMirrorText(0, buffer);
        LOG.warn("Mirror wasn't set for " + this + " in " + getContainingFile() + ", expected text '" + buffer + "'");
        return buffer.toString();
    }

    @Override
    
    public char[] textToCharArray() {
        PsiElement mirror = getMirror();
        return mirror != null ? mirror.textToCharArray() : ArrayUtil.EMPTY_CHAR_ARRAY;
    }

    @Override
    public boolean textMatches( CharSequence text) {
        return getText().equals(text.toString());
    }

    @Override
    public boolean textMatches( PsiElement element) {
        return getText().equals(element.getText());
    }

    @Override
    public ASTNode getNode() {
        return null;
    }

    protected static void goNextLine(int indentLevel,  StringBuilder buffer) {
        buffer.append('\n');
        for (int i = 0; i < indentLevel; i++) buffer.append(' ');
    }

    protected static void appendText( PsiElement stub, int indentLevel,  StringBuilder buffer) {
        ((ClsElementImpl)stub).appendMirrorText(indentLevel, buffer);
    }

    protected static final String NEXT_LINE = "go_to_next_line_and_indent";

    protected static void appendText( PsiElement stub, int indentLevel,  StringBuilder buffer,  String separator) {
        if (stub == null) return;
        int pos = buffer.length();
        ((ClsElementImpl)stub).appendMirrorText(indentLevel, buffer);
        if (buffer.length() != pos) {
            if (separator == NEXT_LINE) {
                goNextLine(indentLevel, buffer);
            }
            else {
                buffer.append(separator);
            }
        }
    }

    protected void setMirrorCheckingType( TreeElement element,  IElementType type) throws InvalidMirrorException {
        // uncomment for extended consistency check
        //if (myMirror != null) {
        //  throw new InvalidMirrorException("Mirror should be null: " + myMirror);
        //}

        if (type != null && element.getElementType() != type) {
            throw new InvalidMirrorException(element.getElementType() + " != " + type);
        }

        element.getPsi().putUserData(COMPILED_ELEMENT, this);
        myMirror = element;
    }

    protected static <T extends  PsiElement> void setMirror( T stub,  T mirror) throws InvalidMirrorException {
        if (stub == null || mirror == null) {
            throw new InvalidMirrorException(stub, mirror);
        }
        ((ClsElementImpl)stub).setMirror(SourceTreeToPsiMap.psiToTreeNotNull(mirror));
    }

    protected static <T extends  PsiElement> void setMirrorIfPresent( T stub,  T mirror) throws InvalidMirrorException {
        if ((stub == null) != (mirror == null)) {
            throw new InvalidMirrorException(stub, mirror);
        }
        else if (stub != null) {
            ((ClsElementImpl)stub).setMirror(SourceTreeToPsiMap.psiToTreeNotNull(mirror));
        }
    }

    protected static <T extends  PsiElement> void setMirrors( T[] stubs,  T[] mirrors) throws InvalidMirrorException {
        setMirrors(Arrays.asList(stubs), Arrays.asList(mirrors));
    }

    protected static <T extends  PsiElement> void setMirrors( List<T> stubs,  T[] mirrors) throws InvalidMirrorException {
        setMirrors(stubs, Arrays.asList(mirrors));
    }

    protected static <T extends  PsiElement> void setMirrors( List<T> stubs,  List<T> mirrors) throws InvalidMirrorException {
        if (stubs.size() != mirrors.size()) {
            throw new InvalidMirrorException(stubs, mirrors);
        }
        for (int i = 0; i < stubs.size(); i++) {
            setMirror(stubs.get(i), mirrors.get(i));
        }
    }

    protected static class InvalidMirrorException extends RuntimeException {
        public InvalidMirrorException(  String message) {
            super(message);
        }

        public InvalidMirrorException( PsiElement stubElement,  PsiElement mirrorElement) {
            this("stub:" + stubElement + "; mirror:" + mirrorElement);
        }

        public InvalidMirrorException( PsiElement[] stubElements,  PsiElement[] mirrorElements) {
            this("stub:" + Arrays.toString(stubElements) + "; mirror:" + Arrays.toString(mirrorElements));
        }

        public InvalidMirrorException( List<? extends PsiElement> stubElements,  List<? extends PsiElement> mirrorElements) {
            this("stub:" + stubElements + "; mirror:" + mirrorElements);
        }
    }
}
