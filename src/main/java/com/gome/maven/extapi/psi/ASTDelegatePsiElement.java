/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.gome.maven.extapi.psi;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiInvalidElementAccessException;
import com.gome.maven.psi.impl.CheckUtil;
import com.gome.maven.psi.impl.PsiElementBase;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.codeStyle.CodeEditUtil;
import com.gome.maven.psi.impl.source.tree.ChangeUtil;
import com.gome.maven.psi.impl.source.tree.CompositeElement;
import com.gome.maven.psi.impl.source.tree.SharedImplUtil;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.Function;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ASTDelegatePsiElement extends PsiElementBase {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.extapi.psi.ASTDelegatePsiElement");

    private static final List EMPTY = Collections.emptyList();

    @Override
    public PsiFile getContainingFile() {
        return SharedImplUtil.getContainingFile(getNode());
    }

    @Override
    public PsiManagerEx getManager() {
        PsiElement parent = this;

        while (parent instanceof ASTDelegatePsiElement) {
            parent = parent.getParent();
        }

        if (parent == null) {
            throw new PsiInvalidElementAccessException(this);
        }

        return (PsiManagerEx)parent.getManager();
    }

    @Override
   
    public PsiElement[] getChildren() {
        PsiElement psiChild = getFirstChild();
        if (psiChild == null) return PsiElement.EMPTY_ARRAY;

        List<PsiElement> result = new ArrayList<PsiElement>();
        while (psiChild != null) {
            if (psiChild.getNode() instanceof CompositeElement) {
                result.add(psiChild);
            }
            psiChild = psiChild.getNextSibling();
        }
        return PsiUtilCore.toPsiElementArray(result);
    }

    @Override
    public PsiElement getFirstChild() {
        return SharedImplUtil.getFirstChild(getNode());
    }

    @Override
    public PsiElement getLastChild() {
        return SharedImplUtil.getLastChild(getNode());
    }

    @Override
    public PsiElement getNextSibling() {
        return SharedImplUtil.getNextSibling(getNode());
    }

    @Override
    public PsiElement getPrevSibling() {
        return SharedImplUtil.getPrevSibling(getNode());
    }

    @Override
    public TextRange getTextRange() {
        return getNode().getTextRange();
    }

    @Override
    public int getStartOffsetInParent() {
        return getNode().getStartOffset() - getNode().getTreeParent().getStartOffset();
    }

    @Override
    public int getTextLength() {
        return getNode().getTextLength();
    }

    @Override
    public PsiElement findElementAt(int offset) {
        ASTNode treeElement = getNode().findLeafElementAt(offset);
        return SourceTreeToPsiMap.treeElementToPsi(treeElement);
    }

    @Override
    public int getTextOffset() {
        return getNode().getStartOffset();
    }

    @Override
    public String getText() {
        return getNode().getText();
    }

    @Override
   
    public char[] textToCharArray() {
        return getNode().getText().toCharArray();
    }

    @Override
    public boolean textContains(char c) {
        return getNode().textContains(c);
    }

    @Override
    public <T> T getCopyableUserData( Key<T> key) {
        return getNode().getCopyableUserData(key);
    }

    @Override
    public <T> void putCopyableUserData( Key<T> key, T value) {
        getNode().putCopyableUserData(key, value);
    }

    @Override
   
    public abstract ASTNode getNode();

    public void subtreeChanged() {
    }

    @Override
   
    public Language getLanguage() {
        return getNode().getElementType().getLanguage();
    }

    
    protected <T extends PsiElement> T findChildByType(IElementType type) {
        ASTNode node = getNode().findChildByType(type);
        return node == null ? null : (T)node.getPsi();
    }

    
    protected <T extends PsiElement> T findLastChildByType(IElementType type) {
        PsiElement child = getLastChild();
        while (child != null) {
            final ASTNode node = child.getNode();
            if (node != null && node.getElementType() == type) return (T)child;
            child = child.getPrevSibling();
        }
        return null;
    }



   
    protected <T extends PsiElement> T findNotNullChildByType(IElementType type) {
        return notNullChild(this.<T>findChildByType(type));
    }

    
    protected <T extends PsiElement> T findChildByType(TokenSet type) {
        ASTNode node = getNode().findChildByType(type);
        return node == null ? null : (T)node.getPsi();
    }

   
    protected PsiElement findNotNullChildByType(TokenSet type) {
        return notNullChild(findChildByType(type));
    }

    
    protected PsiElement findChildByFilter(TokenSet tokenSet) {
        ASTNode[] nodes = getNode().getChildren(tokenSet);
        return nodes == null || nodes.length == 0 ? null : nodes[0].getPsi();
    }

   
    protected PsiElement findNotNullChildByFilter(TokenSet tokenSet) {
        return notNullChild(findChildByFilter(tokenSet));
    }

    protected <T extends PsiElement> T[] findChildrenByType(IElementType elementType, Class<T> arrayClass) {
        return ContainerUtil.map2Array(SharedImplUtil.getChildrenOfType(getNode(), elementType), arrayClass, new Function<ASTNode, T>() {
            @Override
            public T fun(final ASTNode s) {
                return (T)s.getPsi();
            }
        });
    }

    protected <T extends PsiElement> List<T> findChildrenByType(TokenSet elementType) {
        List<T> result = EMPTY;
        ASTNode child = getNode().getFirstChildNode();
        while (child != null) {
            final IElementType tt = child.getElementType();
            if (elementType.contains(tt)) {
                if (result == EMPTY) {
                    result = new ArrayList<T>();
                }
                result.add((T)child.getPsi());
            }
            child = child.getTreeNext();
        }
        return result;
    }

    protected <T extends PsiElement> List<T> findChildrenByType(IElementType elementType) {
        List<T> result = EMPTY;
        ASTNode child = getNode().getFirstChildNode();
        while (child != null) {
            if (elementType == child.getElementType()) {
                if (result == EMPTY) {
                    result = new ArrayList<T>();
                }
                result.add((T)child.getPsi());
            }
            child = child.getTreeNext();
        }
        return result;
    }

    protected <T extends PsiElement> T[] findChildrenByType(TokenSet elementType, Class<T> arrayClass) {
        return ContainerUtil.map2Array(getNode().getChildren(elementType), arrayClass, new Function<ASTNode, T>() {
            @Override
            public T fun(final ASTNode s) {
                return (T)s.getPsi();
            }
        });
    }

    @Override
    public PsiElement copy() {
        return getNode().copyElement().getPsi();
    }

    @Override
    public PsiElement add( PsiElement element) throws IncorrectOperationException {
        return addInnerBefore(element, null);
    }

    @Override
    public PsiElement addBefore( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        return addInnerBefore(element, anchor);
    }

    private PsiElement addInnerBefore(final PsiElement element, final PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        ASTNode treeElement = addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
        if (treeElement != null) {
            if (treeElement instanceof TreeElement) {
                return ChangeUtil.decodeInformation((TreeElement) treeElement).getPsi();
            }
            return treeElement.getPsi();
        }
        throw new IncorrectOperationException("Element cannot be added");
    }

    @Override
    public PsiElement addAfter( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        ASTNode treeElement = addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
        if (treeElement instanceof TreeElement) {
            return ChangeUtil.decodeInformation((TreeElement) treeElement).getPsi();
        }
        return treeElement.getPsi();
    }

    @Override
    public void checkAdd( final PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    public ASTNode addInternal(ASTNode first, ASTNode last, ASTNode anchor, Boolean before) {
        return CodeEditUtil.addChildren(getNode(), first, last, getAnchorNode(anchor, before));
    }

    @Override
    public PsiElement addRange(final PsiElement first, final PsiElement last) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, null, null);
    }

    @Override
    public PsiElement addRangeBefore( final PsiElement first, final PsiElement last, final PsiElement anchor)
            throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
    }

    @Override
    public PsiElement addRangeAfter(final PsiElement first, final PsiElement last, final PsiElement anchor) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
    }

    @Override
    public void delete() throws IncorrectOperationException {
        PsiElement parent = getParent();
        if (parent instanceof ASTDelegatePsiElement) {
            CheckUtil.checkWritable(this);
            ((ASTDelegatePsiElement)parent).deleteChildInternal(getNode());
        }
        else if (parent instanceof CompositeElement) {
            CheckUtil.checkWritable(this);
            ((CompositeElement)parent).deleteChildInternal(getNode());
        }
        else if (parent instanceof PsiFile) {
            CheckUtil.checkWritable(this);
            parent.deleteChildRange(this, this);
        }
        else {
            throw new UnsupportedOperationException(getClass().getName() + " under " + (parent == null ? "null" : parent.getClass().getName()));
        }
    }

    public void deleteChildInternal( ASTNode child) {
        CodeEditUtil.removeChild(getNode(), child);
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    @Override
    public void deleteChildRange(final PsiElement first, final PsiElement last) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        ASTNode firstElement = SourceTreeToPsiMap.psiElementToTree(first);
        ASTNode lastElement = SourceTreeToPsiMap.psiElementToTree(last);

        LOG.assertTrue(firstElement.getTreeParent() == getNode());
        LOG.assertTrue(lastElement.getTreeParent() == getNode());
        CodeEditUtil.removeChildren(getNode(), firstElement, lastElement);
    }

    @Override
    public PsiElement replace( final PsiElement newElement) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(newElement);
        if (getParent() instanceof ASTDelegatePsiElement) {
            final ASTDelegatePsiElement parentElement = (ASTDelegatePsiElement)getParent();
            parentElement.replaceChildInternal(this, elementCopy);
        }
        else {
            CodeEditUtil.replaceChild(getParent().getNode(), getNode(), elementCopy);
        }
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    public void replaceChildInternal(final PsiElement child, final TreeElement newElement) {
        CodeEditUtil.replaceChild(getNode(), child.getNode(), newElement);
    }

    private ASTNode getAnchorNode(final ASTNode anchor, final Boolean before) {
        ASTNode anchorBefore;
        if (anchor != null) {
            anchorBefore = before.booleanValue() ? anchor : anchor.getTreeNext();
        }
        else {
            if (before != null && !before.booleanValue()) {
                anchorBefore = getNode().getFirstChildNode();
            }
            else {
                anchorBefore = null;
            }
        }
        return anchorBefore;
    }
}
