/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.psi.impl.source;

import com.gome.maven.extapi.psi.StubBasedPsiElementBase;
import com.gome.maven.ide.util.PsiNavigationSupport;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.StubBasedPsiElement;
import com.gome.maven.psi.impl.CheckUtil;
import com.gome.maven.psi.impl.source.codeStyle.CodeEditUtil;
import com.gome.maven.psi.impl.source.tree.ChangeUtil;
import com.gome.maven.psi.impl.source.tree.CompositeElement;
import com.gome.maven.psi.impl.source.tree.SharedImplUtil;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.stubs.IStubElementType;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.util.IncorrectOperationException;

public abstract class JavaStubPsiElement<T extends StubElement> extends StubBasedPsiElementBase<T> implements StubBasedPsiElement<T> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.JavaStubPsiElement");

    public JavaStubPsiElement( T stub,  IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public JavaStubPsiElement( ASTNode node) {
        super(node);
    }

    @Override
    
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }

    @Override
    public PsiElement getParent() {
        return getParentByStub();
    }

    @Override
    public int getTextOffset() {
        return calcTreeElement().getTextOffset();
    }

    protected CompositeElement calcTreeElement() {
        return (CompositeElement)getNode();
    }

    @Override
    public PsiElement add( PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, null, null);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    public PsiElement addBefore( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    public PsiElement addAfter( PsiElement element,  PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    public final void checkAdd( PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    @Override
    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, null, null);
    }

    @Override
    public PsiElement addRangeBefore( PsiElement first,  PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
    }

    @Override
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
    }

    @Override
    public void delete() throws IncorrectOperationException {
        ASTNode treeElement = calcTreeElement();
        LOG.assertTrue(treeElement.getTreeParent() != null);
        CheckUtil.checkWritable(this);
        ((CompositeElement)treeElement.getTreeParent()).deleteChildInternal(treeElement);
    }

    @Override
    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        if (first == null) {
            LOG.assertTrue(last == null);
            return;
        }
        ASTNode firstElement = SourceTreeToPsiMap.psiElementToTree(first);
        ASTNode lastElement = SourceTreeToPsiMap.psiElementToTree(last);
        CompositeElement treeElement = calcTreeElement();
        LOG.assertTrue(firstElement.getTreeParent() == treeElement);
        LOG.assertTrue(lastElement.getTreeParent() == treeElement);
        CodeEditUtil.removeChildren(treeElement, firstElement, lastElement);
    }

    @Override
    public PsiElement replace( PsiElement newElement) throws IncorrectOperationException {
        CompositeElement treeElement = calcTreeElement();
        return SharedImplUtil.doReplace(this, treeElement, newElement);
    }

    @Override
    public void navigate(boolean requestFocus) {
        final Navigatable navigatable = PsiNavigationSupport.getInstance().getDescriptor(this);
        if (navigatable != null) {
            navigatable.navigate(requestFocus);
        }
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
    public void acceptChildren( PsiElementVisitor visitor) {
        SharedImplUtil.acceptChildren(visitor, calcTreeElement());
    }

    @Override
    protected Object clone() {
        CompositeElement treeElement = calcTreeElement();
        CompositeElement treeElementClone
                = (CompositeElement)(treeElement.getTreeParent() != null ? treeElement.copyElement() : (ASTNode)treeElement.clone());
    /*
    if (treeElementClone.getPsiElement() != null) {
      return treeElementClone.getPsiElement();
    }
    */
        return cloneImpl(treeElementClone);
    }

    protected StubBasedPsiElementBase cloneImpl( CompositeElement treeElementClone) {
        StubBasedPsiElementBase clone = (StubBasedPsiElementBase)super.clone();
        clone.setNode(treeElementClone);
        treeElementClone.setPsi(clone);
        return clone;
    }

    @Override
    public void subtreeChanged() {
        final CompositeElement compositeElement = calcTreeElement();
        if (compositeElement != null) compositeElement.clearCaches();
        super.subtreeChanged();
    }

    @Override
    
    public PsiElement[] getChildren() {
        PsiElement psiChild = getFirstChild();
        if (psiChild == null) return EMPTY_ARRAY;

        int count = 0;
        while (psiChild != null) {
            count++;
            psiChild = psiChild.getNextSibling();
        }

        PsiElement[] answer = new PsiElement[count];
        count = 0;
        psiChild = getFirstChild();
        while (psiChild != null) {
            answer[count++] = psiChild;
            psiChild = psiChild.getNextSibling();
        }

        return answer;
    }
}
