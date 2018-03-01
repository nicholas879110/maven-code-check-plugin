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

package com.gome.maven.extapi.psi;

import com.gome.maven.ide.util.PsiNavigationSupport;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.ElementBase;
import com.gome.maven.psi.impl.ResolveScopeManager;
import com.gome.maven.psi.impl.SharedPsiElementImplUtil;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.util.IncorrectOperationException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public abstract class PsiElementBase extends ElementBase implements NavigatablePsiElement {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.extapi.psi.PsiElementBase");

    @Override
    public PsiElement copy() {
        return (PsiElement)clone();
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
    public void checkAdd( PsiElement element) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiElement addRangeBefore( PsiElement first,  PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public void delete() throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiElement replace( PsiElement newElement) throws IncorrectOperationException {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PsiReference getReference() {
        return null;
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,  ResolveState state, PsiElement lastParent,  PsiElement place) {
        return true;
    }

    @Override
    
    public Project getProject() {
        final PsiManager manager = getManager();
        if (manager == null) {
            throw new PsiInvalidElementAccessException(this);
        }

        return manager.getProject();
    }

    @Override
    public PsiManager getManager() {
        final PsiElement parent = getParent();
        return parent != null ? parent.getManager() : null;
    }

    @Override
    public PsiFile getContainingFile() {
        final PsiElement parent = getParent();
        if (parent == null) throw new PsiInvalidElementAccessException(this);
        return parent.getContainingFile();
    }

    @Override
    public PsiReference findReferenceAt(int offset) {
        return SharedPsiElementImplUtil.findReferenceAt(this, offset);
    }

    @Override
    
    public PsiElement getNavigationElement() {
        return this;
    }

    @Override
    public PsiElement getOriginalElement() {
        return this;
    }

    //Q: get rid of these methods?
    @Override
    public boolean textMatches( CharSequence text) {
        return Comparing.equal(getText(), text, true);
    }

    @Override
    public boolean textMatches( PsiElement element) {
        return getText().equals(element.getText());
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        visitor.visitElement(this);
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
    public boolean isValid() {
        final PsiElement parent = getParent();
        return parent != null && parent.isValid();
    }

    @Override
    public boolean isWritable() {
        final PsiElement parent = getParent();
        return parent != null && parent.isWritable();
    }

    @Override
    
    public PsiReference[] getReferences() {
        return SharedPsiElementImplUtil.getReferences(this);
    }

    @Override
    public PsiElement getContext() {
        return getParent();
    }

    @Override
    public boolean isPhysical() {
        final PsiElement parent = getParent();
        return parent != null && parent.isPhysical();
    }

    @Override
    
    public GlobalSearchScope getResolveScope() {
        return ResolveScopeManager.getElementResolveScope(this);
    }

    @Override
    
    public SearchScope getUseScope() {
        return ResolveScopeManager.getElementUseScope(this);
    }

    /**
     * Returns the UI presentation data for the PSI element.
     *
     * @return null, unless overridden in a subclass. 
     */
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
        if (descriptor != null) descriptor.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(this);
        return descriptor != null && descriptor.canNavigateToSource();
    }

    
    protected <T> T[] findChildrenByClass(Class<T> aClass) {
        List<T> result = new ArrayList<T>();
        for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (aClass.isInstance(cur)) result.add((T)cur);
        }
        return result.toArray((T[]) Array.newInstance(aClass, result.size()));
    }

    
    protected <T> T findChildByClass(Class<T> aClass) {
        for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (aClass.isInstance(cur)) return (T)cur;
        }
        return null;
    }

    
    protected <T> T findNotNullChildByClass(Class<T> aClass) {
        return notNullChild(findChildByClass(aClass));
    }

    
    protected <T> T notNullChild(T child) {
        if (child == null) {
            LOG.error(getText() + "\n parent=" + getParent().getText());
        }
        return child;
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {
        return this == another;
    }
}
