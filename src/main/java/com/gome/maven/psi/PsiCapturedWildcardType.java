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
package com.gome.maven.psi;

import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * @author ven
 */
public class PsiCapturedWildcardType extends PsiType.Stub {
     private final PsiWildcardType myExistential;
     private final PsiElement myContext;
     private final PsiTypeParameter myParameter;

    private PsiType myUpperBound;

    
    public static PsiCapturedWildcardType create( PsiWildcardType existential,  PsiElement context) {
        return create(existential, context, null);
    }

    
    public static PsiCapturedWildcardType create( PsiWildcardType existential,
                                                  PsiElement context,
                                                  PsiTypeParameter parameter) {
        return new PsiCapturedWildcardType(existential, context, parameter);
    }

    private PsiCapturedWildcardType( PsiWildcardType existential,
                                     PsiElement context,
                                     PsiTypeParameter parameter) {
        super(PsiAnnotation.EMPTY_ARRAY);
        myExistential = existential;
        myContext = context;
        myParameter = parameter;
        if (parameter != null) {
            final PsiClassType[] boundTypes = parameter.getExtendsListTypes();
            if (boundTypes.length > 0) {
                PsiType result = null;
                for (PsiType type : boundTypes) {
                    if (result == null) {
                        result = type;
                    }
                    else {
                        result = GenericsUtil.getGreatestLowerBound(result, type);
                    }
                }
                myUpperBound = result;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PsiCapturedWildcardType)) {
            return false;
        }

        final PsiCapturedWildcardType captured = (PsiCapturedWildcardType)o;
        if (!myContext.equals(captured.myContext) || !myExistential.equals(captured.myExistential)) {
            return false;
        }

        if ((myContext instanceof PsiReferenceExpression || myContext instanceof PsiMethodCallExpression) && !Comparing.equal(myParameter, captured.myParameter)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return myExistential.hashCode() + 31 * myContext.hashCode();
    }

    
    @Override
    public String getPresentableText() {
        return "capture of " + myExistential.getPresentableText();
    }

    
    @Override
    public String getCanonicalText(boolean annotated) {
        return myExistential.getCanonicalText(annotated);
    }

    
    @Override
    public String getInternalCanonicalText() {
        return "capture<" + myExistential.getInternalCanonicalText() + '>';
    }

    @Override
    public boolean isValid() {
        return myExistential.isValid();
    }

    @Override
    public boolean equalsToText( String text) {
        return false;
    }

    @Override
    public <A> A accept( PsiTypeVisitor<A> visitor) {
        return visitor.visitCapturedWildcardType(this);
    }

    
    @Override
    public GlobalSearchScope getResolveScope() {
        return myExistential.getResolveScope();
    }

    @Override
    
    public PsiType[] getSuperTypes() {
        return myExistential.getSuperTypes();
    }

    public PsiType getLowerBound () {
        return myExistential.isSuper() ? myExistential.getBound() : NULL;
    }

    public PsiType getUpperBound () {
        final PsiType bound = myExistential.getBound();
        if (myExistential.isExtends()) {
            return bound;
        }
        else if (bound instanceof PsiCapturedWildcardType) {
            return PsiWildcardType.createSuper(myContext.getManager(), ((PsiCapturedWildcardType)bound).getUpperBound());
        }
        else {
            return myUpperBound != null ? myUpperBound : PsiType.getJavaLangObject(myContext.getManager(), getResolveScope());
        }
    }

    public void setUpperBound(PsiType upperBound) {
        myUpperBound = upperBound;
    }

    
    public PsiWildcardType getWildcard() {
        return myExistential;
    }

    
    public PsiElement getContext() {
        return myContext;
    }

    public PsiTypeParameter getTypeParameter() {
        return myParameter;
    }
}
