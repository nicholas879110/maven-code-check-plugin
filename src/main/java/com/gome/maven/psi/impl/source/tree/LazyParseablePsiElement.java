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
package com.gome.maven.psi.impl.source.tree;

import com.gome.maven.ide.util.PsiNavigationSupport;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.CheckUtil;
import com.gome.maven.psi.impl.ResolveScopeManager;
import com.gome.maven.psi.impl.SharedPsiElementImplUtil;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.impl.source.codeStyle.CodeEditUtil;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.IncorrectOperationException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class LazyParseablePsiElement extends LazyParseableElement implements PsiElement, NavigationItem {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.LazyParseablePsiElement");

    public LazyParseablePsiElement( IElementType type, CharSequence buffer) {
        super(type, buffer);
        setPsi(this);
    }

    
    @Override
    public LazyParseablePsiElement clone() {
        LazyParseablePsiElement clone = (LazyParseablePsiElement)super.clone();
        clone.setPsi(clone);
        return clone;
    }

    @Override
    
    public PsiElement[] getChildren() {
        return getChildrenAsPsiElements((TokenSet)null, PsiElement.ARRAY_FACTORY);
    }

    
    protected <T> T findChildByClass(Class<T> aClass) {
        for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (aClass.isInstance(cur)) return (T)cur;
        }
        return null;
    }

    
    protected <T> T[] findChildrenByClass(Class<T> aClass) {
        List<T> result = new ArrayList<T>();
        for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (aClass.isInstance(cur)) result.add((T)cur);
        }
        return result.toArray((T[])Array.newInstance(aClass, result.size()));
    }

    @Override
    public PsiElement getFirstChild() {
        TreeElement child = getFirstChildNode();
        if (child == null) return null;
        return child.getPsi();
    }

    @Override
    public PsiElement getLastChild() {
        TreeElement child = getLastChildNode();
        if (child == null) return null;
        return child.getPsi();
    }

    @Override
    public void acceptChildren( PsiElementVisitor visitor) {
        PsiElement child = getFirstChild();
        while (child != null) {
            child.accept(visitor);
            child = child.getNextSibling();
        }
    }

    @Override
    public PsiElement getParent() {
        final CompositeElement treeParent = getTreeParent();
        if (treeParent == null) return null;
        if (treeParent instanceof PsiElement) return (PsiElement)treeParent;
        return treeParent.getPsi();
    }

    @Override
    public PsiElement getNextSibling() {
        return SharedImplUtil.getNextSibling(this);
    }

    @Override
    public PsiElement getPrevSibling() {
        return SharedImplUtil.getPrevSibling(this);
    }

    @Override
    public PsiFile getContainingFile() {
        PsiFile file = SharedImplUtil.getContainingFile(this);
        if (file == null) throw new PsiInvalidElementAccessException(this);
        return file;
    }

    @Override
    public PsiElement findElementAt(int offset) {
        ASTNode leaf = findLeafElementAt(offset);
        return SourceTreeToPsiMap.treeElementToPsi(leaf);
    }

    @Override
    public PsiReference findReferenceAt(int offset) {
        return SharedPsiElementImplUtil.findReferenceAt(this, offset);
    }

    @Override
    public PsiElement copy() {
        ASTNode elementCopy = copyElement();
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    public boolean isValid() {
        return SharedImplUtil.isValid(this);
    }

    @Override
    public boolean isWritable() {
        return SharedImplUtil.isWritable(this);
    }

    @Override
    public PsiReference getReference() {
        return null;
    }

    @Override
    
    public PsiReference[] getReferences() {
        return SharedPsiElementImplUtil.getReferences(this);
    }

    @Override
    public PsiElement add( PsiElement element) throws IncorrectOperationException {
        return addInnerBefore(element, null);
    }

    @Override
    public PsiElement addBefore( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        return addInnerBefore(element, anchor);
    }

    @Override
    public PsiElement addAfter( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        TreeElement treeElement = addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
        return ChangeUtil.decodeInformation(treeElement).getPsi();

    }

    @Override
    public final void checkAdd( PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    @Override
    public final PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, null, null);
    }

    @Override
    public final PsiElement addRangeBefore( PsiElement first,  PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
    }

    @Override
    public final PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
    }

    @Override
    public void delete() throws IncorrectOperationException {
        LOG.assertTrue(getTreeParent() != null, "Parent not found for " + this);
        CheckUtil.checkWritable(this);
        getTreeParent().deleteChildInternal(this);
        invalidate();
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    @Override
    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        ASTNode firstElement = SourceTreeToPsiMap.psiElementToTree(first);
        ASTNode lastElement = SourceTreeToPsiMap.psiElementToTree(last);
        LOG.assertTrue(firstElement.getTreeParent() == this);
        LOG.assertTrue(lastElement.getTreeParent() == this);
        CodeEditUtil.removeChildren(this, firstElement, lastElement);
    }

    @Override
    public PsiElement replace( PsiElement newElement) throws IncorrectOperationException {
        return SharedImplUtil.doReplace(this, this, newElement);
    }

    @Override
    public void accept( PsiElementVisitor visitor) { //TODO: remove this method!!
        visitor.visitElement(this);
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,
                                        ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        return true;
    }

    public String toString() {
        return "PsiElement" + "(" + getElementType().toString() + ")";
    }

    @Override
    public PsiElement getContext() {
        return getParent();
    }

    @Override
    
    public PsiElement getNavigationElement() {
        return this;
    }

    @Override
    public PsiElement getOriginalElement() {
        return this;
    }

    @Override
    public boolean isPhysical() {
        PsiFile file = getContainingFile();
        return file != null && file.isPhysical();
    }

    @Override
    
    public GlobalSearchScope getResolveScope() {
        assert isValid();
        return ResolveScopeManager.getElementResolveScope(this);
    }

    @Override
    
    public SearchScope getUseScope() {
        return ResolveScopeManager.getElementUseScope(this);
    }

    @Override
    public ItemPresentation getPresentation() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        PsiNavigationSupport.getInstance().getDescriptor(this).navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return PsiNavigationSupport.getInstance().canNavigate(this);
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    
    public Project getProject() {
        final PsiManager manager = getManager();
        if (manager == null) throw new PsiInvalidElementAccessException(this);

        return manager.getProject();
    }

    @Override
    
    public Language getLanguage() {
        return getElementType().getLanguage();
    }

    @Override
    
    public ASTNode getNode() {
        return this;
    }

    private PsiElement addInnerBefore(final PsiElement element, final PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        TreeElement treeElement = addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
        if (treeElement != null) return ChangeUtil.decodeInformation(treeElement).getPsi();
        throw new IncorrectOperationException("Element cannot be added");
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {
        return this == another;
    }
}

