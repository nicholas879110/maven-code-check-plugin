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
package com.gome.maven.psi.impl.light;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiClassImplUtil;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Collection;
import java.util.List;

public abstract class AbstractLightClass extends LightElement implements PsiClass {
    protected AbstractLightClass(PsiManager manager, Language language) {
        super(manager, language);
    }

    protected AbstractLightClass(PsiManager manager) {
        super(manager, JavaLanguage.INSTANCE);
    }

    
    public abstract PsiClass getDelegate();

    
    public abstract PsiElement copy();

    @Override
    
    
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    
    public PsiModifierList getModifierList() {
        return getDelegate().getModifierList();
    }

    @Override
    public boolean hasModifierProperty(  String name) {
        return getDelegate().hasModifierProperty(name);
    }

    @Override
    
    public PsiDocComment getDocComment() {
        return null;
    }

    @Override
    public boolean isDeprecated() {
        return getDelegate().isDeprecated();
    }

    @Override
    public boolean hasTypeParameters() {
        return PsiImplUtil.hasTypeParameters(this);
    }

    @Override
    
    public PsiTypeParameterList getTypeParameterList() {
        return getDelegate().getTypeParameterList();
    }

    @Override
    
    public PsiTypeParameter[] getTypeParameters() {
        return getDelegate().getTypeParameters();
    }

    @Override
    
    
    public String getQualifiedName() {
        return getDelegate().getQualifiedName();
    }

    @Override
    public boolean isInterface() {
        return getDelegate().isInterface();
    }

    @Override
    public boolean isAnnotationType() {
        return getDelegate().isAnnotationType();
    }

    @Override
    public boolean isEnum() {
        return getDelegate().isEnum();
    }

    @Override
    
    public PsiReferenceList getExtendsList() {
        return getDelegate().getExtendsList();
    }

    @Override
    
    public PsiReferenceList getImplementsList() {
        return getDelegate().getImplementsList();
    }

    @Override
    
    public PsiClassType[] getExtendsListTypes() {
        return PsiClassImplUtil.getExtendsListTypes(this);
    }

    @Override
    
    public PsiClassType[] getImplementsListTypes() {
        return PsiClassImplUtil.getImplementsListTypes(this);
    }

    @Override
    
    public PsiClass getSuperClass() {
        return getDelegate().getSuperClass();
    }

    @Override
    public PsiClass[] getInterfaces() {
        return getDelegate().getInterfaces();
    }

    
    @Override
    public PsiElement getNavigationElement() {
        return getDelegate().getNavigationElement();
    }

    @Override
    
    public PsiClass[] getSupers() {
        return getDelegate().getSupers();
    }

    @Override
    
    public PsiClassType[] getSuperTypes() {
        return getDelegate().getSuperTypes();
    }

    @Override
    
    public PsiField[] getFields() {
        return getDelegate().getFields();
    }

    @Override
    
    public PsiMethod[] getMethods() {
        return getDelegate().getMethods();
    }

    @Override
    
    public PsiMethod[] getConstructors() {
        return getDelegate().getConstructors();
    }

    @Override
    
    public PsiClass[] getInnerClasses() {
        return getDelegate().getInnerClasses();
    }

    @Override
    
    public PsiClassInitializer[] getInitializers() {
        return getDelegate().getInitializers();
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,
                                        ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        return PsiClassImplUtil.processDeclarationsInClass(this, processor, state, null, lastParent, place, PsiUtil.getLanguageLevel(place), false);
    }

    @Override
    
    public PsiField[] getAllFields() {
        return getDelegate().getAllFields();
    }

    @Override
    
    public PsiMethod[] getAllMethods() {
        return getDelegate().getAllMethods();
    }

    @Override
    
    public PsiClass[] getAllInnerClasses() {
        return getDelegate().getAllInnerClasses();
    }

    @Override
    
    public PsiField findFieldByName( String name, boolean checkBases) {
        return PsiClassImplUtil.findFieldByName(this, name, checkBases);
    }

    @Override
    
    public PsiMethod findMethodBySignature(PsiMethod patternMethod, boolean checkBases) {
        return PsiClassImplUtil.findMethodBySignature(this, patternMethod, checkBases);
    }

    @Override
    
    public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
        return PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases);
    }

    @Override
    
    public PsiMethod[] findMethodsByName( String name, boolean checkBases) {
        return PsiClassImplUtil.findMethodsByName(this, name, checkBases);
    }

    @Override
    
    public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName( String name, boolean checkBases) {
        return PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases);
    }

    @Override
    
    public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
        return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD);
    }

    @Override
    
    public PsiClass findInnerClassByName( String name, boolean checkBases) {
        return getDelegate().findInnerClassByName(name, checkBases);
    }

    @Override
    
    public PsiElement getLBrace() {
        return getDelegate().getLBrace();
    }

    @Override
    
    public PsiElement getRBrace() {
        return getDelegate().getRBrace();
    }

    @Override
    
    public PsiIdentifier getNameIdentifier() {
        return getDelegate().getNameIdentifier();
    }

    @Override
    public PsiElement getScope() {
        return getDelegate().getScope();
    }

    @Override
    public boolean isInheritor( PsiClass baseClass, boolean checkDeep) {
        return getDelegate().isInheritor(baseClass, checkDeep);
    }

    @Override
    public boolean isInheritorDeep(PsiClass baseClass,  PsiClass classToByPass) {
        return getDelegate().isInheritorDeep(baseClass, classToByPass);
    }

    @Override
    
    public PsiClass getContainingClass() {
        return getDelegate().getContainingClass();
    }

    @Override
    
    public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
        return getDelegate().getVisibleSignatures();
    }

    @Override
    public PsiElement setName(  String name) throws IncorrectOperationException {
        return getDelegate().setName(name);
    }

    @Override
    public String toString() {
        return "PsiClass:" + getName();
    }

    @Override
    public String getText() {
        return getDelegate().getText();
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitClass(this);
        } else {
            visitor.visitElement(this);
        }
    }

    @Override
    public PsiFile getContainingFile() {
        return getDelegate().getContainingFile();
    }

    @Override
    public PsiElement getContext() {
        return getDelegate();
    }

    @Override
    public boolean isValid() {
        return getDelegate().isValid();
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return this == another ||
                (another instanceof AbstractLightClass && getDelegate().isEquivalentTo(((AbstractLightClass)another).getDelegate())) ||
                getDelegate().isEquivalentTo(another);
    }

}
