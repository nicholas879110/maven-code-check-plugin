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
package com.gome.maven.psi.impl.light;

import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.infos.CandidateInfo;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.util.IncorrectOperationException;

public class LightClassReference extends LightElement implements PsiJavaCodeReferenceElement {
    private final String myText;
    private final String myClassName;
    private final PsiElement myContext;
    private final GlobalSearchScope myResolveScope;
    private final PsiClass myRefClass;
    private final PsiSubstitutor mySubstitutor;

    private LightReferenceParameterList myParameterList;

    private LightClassReference( PsiManager manager,   String text,   String className, PsiSubstitutor substitutor,  GlobalSearchScope resolveScope) {
        super(manager, JavaLanguage.INSTANCE);
        myText = text;
        myClassName = className;
        myResolveScope = resolveScope;

        myContext = null;
        myRefClass = null;
        mySubstitutor = substitutor;
    }

    public LightClassReference( PsiManager manager,   String text,   String className,  GlobalSearchScope resolveScope) {
        this (manager, text, className, null, resolveScope);
    }

    public LightClassReference( PsiManager manager,   String text,   String className, PsiSubstitutor substitutor, PsiElement context) {
        super(manager, JavaLanguage.INSTANCE);
        myText = text;
        myClassName = className;
        mySubstitutor = substitutor;
        myContext = context;

        myResolveScope = null;
        myRefClass = null;
    }

    public LightClassReference( PsiManager manager,   String text,  PsiClass refClass) {
        this(manager, text, refClass, null);
    }

    public LightClassReference( PsiManager manager,   String text,  PsiClass refClass, PsiSubstitutor substitutor) {
        super(manager, JavaLanguage.INSTANCE);
        myText = text;
        myRefClass = refClass;

        myResolveScope = null;
        myClassName = null;
        myContext = null;
        mySubstitutor = substitutor;
    }

    @Override
    public PsiElement resolve() {
        if (myClassName != null) {
            final JavaPsiFacade facade = JavaPsiFacade.getInstance(getProject());
            if (myContext != null) {
                return facade.getResolveHelper().resolveReferencedClass(myClassName, myContext);
            }
            else {
                return facade.findClass(myClassName, myResolveScope);
            }
        }
        else {
            return myRefClass;
        }
    }

    @Override
    
    public JavaResolveResult advancedResolve(boolean incompleteCode){
        final PsiElement resolved = resolve();
        if (resolved == null) {
            return JavaResolveResult.EMPTY;
        }
        PsiSubstitutor substitutor = mySubstitutor;
        if (substitutor == null) {
            if (resolved instanceof PsiClass) {
                substitutor = JavaPsiFacade.getInstance(myManager.getProject()).getElementFactory().createRawSubstitutor((PsiClass) resolved);
            } else {
                substitutor = PsiSubstitutor.EMPTY;
            }
        }
        return new CandidateInfo(resolved, substitutor);
    }

    @Override
    
    public JavaResolveResult[] multiResolve(boolean incompleteCode){
        final JavaResolveResult result = advancedResolve(incompleteCode);
        if(result != JavaResolveResult.EMPTY) return new JavaResolveResult[]{result};
        return JavaResolveResult.EMPTY_ARRAY;
    }

    @Override
    public void processVariants( PsiScopeProcessor processor){
        throw new RuntimeException("Variants are not available for light references");
    }

    @Override
    public PsiElement getReferenceNameElement() {
        return null;
    }

    @Override
    public PsiReferenceParameterList getParameterList() {
        if (myParameterList == null) {
            myParameterList = new LightReferenceParameterList(myManager, PsiTypeElement.EMPTY_ARRAY);
        }
        return myParameterList;
    }

    @Override
    public String getQualifiedName() {
        if (myClassName != null) {
            if (myContext != null) {
                PsiClass psiClass = (PsiClass)resolve();
                if (psiClass != null) {
                    return psiClass.getQualifiedName();
                }
            }
            return myClassName;
        }
        return myRefClass.getQualifiedName();
    }

    @Override
    public String getReferenceName() {
        if (myClassName != null){
            return PsiNameHelper.getShortClassName(myClassName);
        }
        else{
            if (myRefClass instanceof PsiAnonymousClass){
                return ((PsiAnonymousClass)myRefClass).getBaseClassReference().getReferenceName();
            }
            else{
                return myRefClass.getName();
            }
        }
    }

    @Override
    public String getText() {
        return myText;
    }

    @Override
    public PsiReference getReference() {
        return this;
    }

    @Override
    
    public String getCanonicalText() {
        String name = getQualifiedName();
        if (name == null) return null;
        PsiType[] types = getTypeParameters();
        if (types.length == 0) return name;

        StringBuffer buf = new StringBuffer();
        buf.append(name);
        buf.append('<');
        for (int i = 0; i < types.length; i++) {
            if (i > 0) buf.append(',');
            buf.append(types[i].getCanonicalText());
        }
        buf.append('>');

        return buf.toString();
    }

    @Override
    public PsiElement copy() {
        if (myClassName != null) {
            if (myContext != null) {
                return new LightClassReference(myManager, myText, myClassName, mySubstitutor, myContext);
            }
            else{
                return new LightClassReference(myManager, myText, myClassName, mySubstitutor, myResolveScope);
            }
        }
        else {
            return new LightClassReference(myManager, myText, myRefClass, mySubstitutor);
        }
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        //TODO?
        throw new UnsupportedOperationException();
    }

    @Override
    public PsiElement bindToElement( PsiElement element) throws IncorrectOperationException {
        //TODO?
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitReferenceElement(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString() {
        return "LightClassReference:" + myText;
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        return element instanceof PsiClass && getManager().areElementsEquivalent(resolve(), element);
    }

    @Override
    
    public Object[] getVariants() {
        throw new RuntimeException("Variants are not available for light references");
    }

    @Override
    public boolean isSoft(){
        return false;
    }

    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getTextLength());
    }

    @Override
    public PsiElement getElement() {
        return this;
    }

    @Override
    public boolean isValid() {
        return myRefClass == null || myRefClass.isValid();
    }

    @Override
    
    public PsiType[] getTypeParameters() {
        return PsiType.EMPTY_ARRAY;
    }

    @Override
    public PsiElement getQualifier() {
        return null;
    }

    @Override
    public boolean isQualified() {
        return false;
    }
}
