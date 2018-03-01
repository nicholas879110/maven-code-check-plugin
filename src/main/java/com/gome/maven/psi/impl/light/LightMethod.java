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
package com.gome.maven.psi.impl.light;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.ItemPresentationProviders;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.ElementPresentationUtil;
import com.gome.maven.psi.impl.PsiClassImplUtil;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.util.MethodSignature;
import com.gome.maven.psi.util.MethodSignatureBackedByPsiMethod;
import com.gome.maven.ui.RowIcon;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;
import java.util.List;

/**
 * @author ven
 */
public class LightMethod extends LightElement implements PsiMethod {
    private final PsiMethod myMethod;
    private final PsiClass myContainingClass;

    public LightMethod( PsiManager manager,  PsiMethod method,  PsiClass containingClass) {
        this(manager, method, containingClass, JavaLanguage.INSTANCE);
    }

    public LightMethod( PsiManager manager,
                        PsiMethod method,
                        PsiClass containingClass,
                        Language language) {
        super(manager, language);
        myMethod = method;
        myContainingClass = containingClass;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public boolean hasTypeParameters() {
        return myMethod.hasTypeParameters();
    }

    @Override
     public PsiTypeParameter[] getTypeParameters() {
        return myMethod.getTypeParameters();
    }

    @Override
    public PsiTypeParameterList getTypeParameterList() {
        return myMethod.getTypeParameterList();
    }

    @Override
    public PsiDocComment getDocComment() {
        return myMethod.getDocComment();
    }

    @Override
    public boolean isDeprecated() {
        return myMethod.isDeprecated();
    }

    @Override
    public PsiElement setName( String name) throws IncorrectOperationException {
        return myMethod.setName(name);
    }

    @Override
    
    public String getName() {
        return myMethod.getName();
    }

    @Override
    
    public HierarchicalMethodSignature getHierarchicalMethodSignature() {
        return myMethod.getHierarchicalMethodSignature();
    }

    @Override
    public boolean hasModifierProperty( String name) {
        return myMethod.hasModifierProperty(name);
    }

    @Override
    public TextRange getTextRange() {
        return myMethod.getTextRange();
    }

    @Override
    
    public PsiModifierList getModifierList() {
        return myMethod.getModifierList();
    }

    @Override
    public PsiType getReturnType() {
        return myMethod.getReturnType();
    }

    @Override
    public PsiTypeElement getReturnTypeElement() {
        return myMethod.getReturnTypeElement();
    }

    @Override
    
    public PsiParameterList getParameterList() {
        return myMethod.getParameterList();
    }

    @Override
    
    public PsiReferenceList getThrowsList() {
        return myMethod.getThrowsList();
    }

    @Override
    public PsiCodeBlock getBody() {
        return myMethod.getBody();
    }

    @Override
    public boolean isConstructor() {
        return myMethod.isConstructor();
    }

    @Override
    public boolean isVarArgs() {
        return myMethod.isVarArgs();
    }

    @Override
    
    public MethodSignature getSignature( PsiSubstitutor substitutor) {
        return myMethod.getSignature(substitutor);
    }

    @Override
    public PsiIdentifier getNameIdentifier() {
        return myMethod.getNameIdentifier();
    }

    @Override
    
    public PsiMethod[] findSuperMethods() {
        return myMethod.findSuperMethods();
    }

    @Override
    
    public PsiMethod[] findSuperMethods(boolean checkAccess) {
        return myMethod.findSuperMethods(checkAccess);
    }

    @Override
    
    public PsiMethod[] findSuperMethods(PsiClass parentClass) {
        return myMethod.findSuperMethods(parentClass);
    }

    @Override
    
    public List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(boolean checkAccess) {
        return myMethod.findSuperMethodSignaturesIncludingStatic(checkAccess);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PsiMethod findDeepestSuperMethod() {
        return myMethod.findDeepestSuperMethod();
    }

    @Override
    
    public PsiMethod[] findDeepestSuperMethods() {
        return myMethod.findDeepestSuperMethods();
    }

    @Override
    public String getText() {
        return myMethod.getText();
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        myMethod.accept(visitor);
    }

    @Override
    public PsiElement copy() {
        return new LightMethod(myManager, (PsiMethod)myMethod.copy(), myContainingClass);
    }

    @Override
    public boolean isValid() {
        return myContainingClass.isValid();
    }

    @Override
    public PsiClass getContainingClass() {
        return myContainingClass;
    }

    @Override
    public PsiFile getContainingFile() {
        return myContainingClass.getContainingFile();
    }

    @Override
    public String toString() {
        return "PsiMethod:" + getName();
    }

    @Override
    protected boolean isVisibilitySupported() {
        return true;
    }

    @Override
    public Icon getElementIcon(final int flags) {
        Icon methodIcon = hasModifierProperty(PsiModifier.ABSTRACT) ? PlatformIcons.ABSTRACT_METHOD_ICON : PlatformIcons.METHOD_ICON;
        RowIcon baseIcon = ElementPresentationUtil.createLayeredIcon(methodIcon, this, false);
        return ElementPresentationUtil.addVisibilityIcon(this, flags, baseIcon);
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {
        return PsiClassImplUtil.isMethodEquivalentTo(this, another);
    }

    @Override
    
    public SearchScope getUseScope() {
        return PsiImplUtil.getMemberUseScope(this);
    }

    @Override
    public PsiElement getContext() {
        return getContainingClass();
    }
}
