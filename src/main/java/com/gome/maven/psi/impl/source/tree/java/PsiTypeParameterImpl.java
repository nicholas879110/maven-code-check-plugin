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
package com.gome.maven.psi.impl.source.tree.java;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.InheritanceImplUtil;
import com.gome.maven.psi.impl.PsiClassImplUtil;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.PsiSuperMethodImplUtil;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiTypeParameterListStub;
import com.gome.maven.psi.impl.java.stubs.PsiTypeParameterStub;
import com.gome.maven.psi.impl.light.LightEmptyImplementsList;
import com.gome.maven.psi.impl.source.JavaStubPsiElement;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.IncorrectOperationException;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * @author dsl
 */
public class PsiTypeParameterImpl extends JavaStubPsiElement<PsiTypeParameterStub> implements PsiTypeParameter {
    public PsiTypeParameterImpl(final PsiTypeParameterStub stub) {
        super(stub, JavaStubElementTypes.TYPE_PARAMETER);
    }

    public PsiTypeParameterImpl(final ASTNode node) {
        super(node);
    }

    @Override
    public String getQualifiedName() {
        return null;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAnnotationType() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    
    public PsiField[] getFields() {
        return PsiField.EMPTY_ARRAY;
    }

    @Override
    
    public PsiMethod[] getMethods() {
        return PsiMethod.EMPTY_ARRAY;
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
    public PsiField findFieldByName(String name, boolean checkBases) {
        return PsiClassImplUtil.findFieldByName(this, name, checkBases);
    }

    @Override
    
    public PsiMethod[] findMethodsByName(String name, boolean checkBases) {
        return PsiClassImplUtil.findMethodsByName(this, name, checkBases);
    }

    @Override
    
    public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(String name, boolean checkBases) {
        return PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases);
    }

    @Override
    
    public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
        return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD);
    }

    @Override
    public PsiClass findInnerClassByName(String name, boolean checkBases) {
        return PsiClassImplUtil.findInnerByName(this, name, checkBases);
    }

    @Override
    public PsiTypeParameterList getTypeParameterList() {
        return null;
    }

    @Override
    public boolean hasTypeParameters() {
        return false;
    }

    // very special method!
    @Override
    public PsiElement getScope() {
        return getParent().getParent();
    }

    @Override
    public boolean isInheritorDeep(PsiClass baseClass, PsiClass classToByPass) {
        return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
    }

    @Override
    public boolean isInheritor( PsiClass baseClass, boolean checkDeep) {
        return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
    }

    @Override
    public PsiTypeParameterListOwner getOwner() {
        final PsiElement parent = getParent();
        if (parent == null) throw new PsiInvalidElementAccessException(this);
        final PsiElement parentParent = parent.getParent();
        if (!(parentParent instanceof PsiTypeParameterListOwner)) {
            // Might be an error element;
            return PsiTreeUtil.getParentOfType(this, PsiTypeParameterListOwner.class);
        }

        return (PsiTypeParameterListOwner)parentParent;
    }


    @Override
    public int getIndex() {
        final PsiTypeParameterStub stub = getStub();
        if (stub != null) {
            final PsiTypeParameterListStub parentStub = (PsiTypeParameterListStub)stub.getParentStub();
            return parentStub.getChildrenStubs().indexOf(stub);
        }

        int ret = 0;
        PsiElement element = getPrevSibling();
        while (element != null) {
            if (element instanceof PsiTypeParameter) {
                ret++;
            }
            element = element.getPrevSibling();
        }
        return ret;
    }

    @Override
    
    public PsiIdentifier getNameIdentifier() {
        return PsiTreeUtil.getRequiredChildOfType(this, PsiIdentifier.class);
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,
                                        ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        return PsiClassImplUtil.processDeclarationsInClass(this, processor, state, null, lastParent, place, PsiUtil.getLanguageLevel(place), false);
    }

    @Override
    public String getName() {
        final PsiTypeParameterStub stub = getStub();
        if (stub != null) {
            return stub.getName();
        }

        return getNameIdentifier().getText();
    }

    @Override
    public PsiElement setName( String name) throws IncorrectOperationException {
        PsiImplUtil.setName(getNameIdentifier(), name);
        return this;
    }

    @Override
    
    public PsiMethod[] getConstructors() {
        return PsiMethod.EMPTY_ARRAY;
    }

    @Override
    public PsiDocComment getDocComment() {
        return null;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    
    public PsiReferenceList getExtendsList() {
        return getRequiredStubOrPsiChild(JavaStubElementTypes.EXTENDS_BOUND_LIST);
    }

    @Override
    public PsiReferenceList getImplementsList() {
        return new LightEmptyImplementsList(getManager());
    }

    @Override
    
    public PsiClassType[] getExtendsListTypes() {
        return PsiClassImplUtil.getExtendsListTypes(this);
    }

    @Override
    
    public PsiClassType[] getImplementsListTypes() {
        return PsiClassType.EMPTY_ARRAY;
    }

    @Override
    
    public PsiClass[] getInnerClasses() {
        return PsiClass.EMPTY_ARRAY;
    }

    @Override
    
    public PsiField[] getAllFields() {
        return PsiField.EMPTY_ARRAY;
    }

    @Override
    
    public PsiMethod[] getAllMethods() {
        return PsiMethod.EMPTY_ARRAY;
    }

    @Override
    
    public PsiClass[] getAllInnerClasses() {
        return PsiClass.EMPTY_ARRAY;
    }

    @Override
    
    public PsiClassInitializer[] getInitializers() {
        return PsiClassInitializer.EMPTY_ARRAY;
    }

    @Override
    
    public PsiTypeParameter[] getTypeParameters() {
        return PsiTypeParameter.EMPTY_ARRAY;
    }

    @Override
    public PsiClass getSuperClass() {
        return PsiClassImplUtil.getSuperClass(this);
    }

    @Override
    public PsiClass[] getInterfaces() {
        return PsiClassImplUtil.getInterfaces(this);
    }

    @Override
    
    public PsiClass[] getSupers() {
        return PsiClassImplUtil.getSupers(this);
    }

    @Override
    
    public PsiClassType[] getSuperTypes() {
        return PsiClassImplUtil.getSuperTypes(this);
    }

    @Override
    public PsiClass getContainingClass() {
        return null;
    }

    @Override
    
    public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
        return PsiSuperMethodImplUtil.getVisibleSignatures(this);
    }

    @Override
    public PsiModifierList getModifierList() {
        return null;
    }

    @Override
    public boolean hasModifierProperty( String name) {
        return false;
    }

    @Override
    public PsiJavaToken getLBrace() {
        return null;
    }

    @Override
    public PsiJavaToken getRBrace() {
        return null;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitTypeParameter(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override

    public String toString() {
        return "PsiTypeParameter:" + getName();
    }

    @Override
    public Icon getElementIcon(final int flags) {
        return PsiClassImplUtil.getClassIcon(flags, this);
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {
        return PsiClassImplUtil.isClassEquivalentTo(this, another);
    }

    @Override
    
    public SearchScope getUseScope() {
        return PsiClassImplUtil.getClassUseScope(this);
    }

    @Override
    
    public PsiAnnotation[] getAnnotations() {
        return getStubOrPsiChildren(JavaStubElementTypes.ANNOTATION, PsiAnnotation.ARRAY_FACTORY);
    }

    @Override
    public PsiAnnotation findAnnotation(  String qualifiedName) {
        return PsiImplUtil.findAnnotation(this, qualifiedName);
    }

    @Override
    
    public PsiAnnotation addAnnotation(  String qualifiedName) {
        throw new IncorrectOperationException();
    }

    @Override
    
    public PsiAnnotation[] getApplicableAnnotations() {
        return getAnnotations();
    }
}
