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

package com.gome.maven.psi.impl.source.tree;

import com.gome.maven.ide.util.PsiNavigationSupport;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.CheckUtil;
import com.gome.maven.psi.impl.ResolveScopeManager;
import com.gome.maven.psi.impl.SharedPsiElementImplUtil;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.util.CharTable;
import com.gome.maven.util.IncorrectOperationException;

public class LeafPsiElement extends LeafElement implements PsiElement, NavigationItem {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.LeafPsiElement");

    public LeafPsiElement( IElementType type, CharSequence text) {
        super(type, text);
    }

    @Deprecated
    public LeafPsiElement( IElementType type, CharSequence buffer, int startOffset, int endOffset, CharTable table) {
        super(type, table.intern(buffer, startOffset, endOffset));
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
    public void acceptChildren( PsiElementVisitor visitor) {
    }

    @Override
    public PsiElement getParent() {
        return SharedImplUtil.getParent(this);
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
        final PsiFile file = SharedImplUtil.getContainingFile(this);
        if (file == null || !file.isValid()) invalid();
        //noinspection ConstantConditions
        return file;
    }

    private void invalid() {
        ProgressIndicatorProvider.checkCanceled();

        final StringBuilder builder = new StringBuilder();
        TreeElement element = this;
        while (element != null) {
            if (element != this) builder.append(" / ");
            builder.append(element.getClass().getName()).append(':').append(element.getElementType());
            element = element.getTreeParent();
        }

        throw new PsiInvalidElementAccessException(this, builder.toString());
    }

    @Override
    public PsiElement findElementAt(int offset) {
        return this;
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
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement addBefore( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement addAfter( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public void checkAdd( PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement addRangeBefore( PsiElement first,  PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public void delete() throws IncorrectOperationException {
        LOG.assertTrue(getTreeParent() != null);
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
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement replace( PsiElement newElement) throws IncorrectOperationException {
        return SharedImplUtil.doReplace(this, this, newElement);
    }

    public String toString() {
        return "PsiElement" + "(" + getElementType().toString() + ")";
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        visitor.visitElement(this);
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,
                                        ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        return true;
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
        return ResolveScopeManager.getElementResolveScope(this);
    }

    @Override
    
    public SearchScope getUseScope() {
        return ResolveScopeManager.getElementUseScope(this);
    }

    @Override
    
    public Project getProject() {
        final PsiManager manager = getManager();
        if (manager == null) invalid();
        //noinspection ConstantConditions
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

    @Override
    public PsiElement getPsi() {
        return this;
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
        final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(this);
        if (descriptor != null) {
            descriptor.navigate(requestFocus);
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
    public boolean isEquivalentTo(final PsiElement another) {
        return this == another;
    }
}
