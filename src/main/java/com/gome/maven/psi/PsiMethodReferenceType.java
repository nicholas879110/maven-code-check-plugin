/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * A type which represents a function denoted by a method reference.
 */
public class PsiMethodReferenceType extends PsiType {
    
    private final PsiMethodReferenceExpression myReference;

    public PsiMethodReferenceType( final PsiMethodReferenceExpression reference) {
        super(PsiAnnotation.EMPTY_ARRAY);
        myReference = reference;
    }

    
    @Override
    public String getPresentableText() {
        return "<method reference>";
    }

    
    @Override
    public String getCanonicalText() {
        return getPresentableText();
    }

    
    @Override
    public String getInternalCanonicalText() {
        return getPresentableText();
    }

    @Override
    public boolean isValid() {
        return myReference.isValid();
    }

    @Override
    public boolean equalsToText(  final String text) {
        return false;
    }

    @Override
    public <A> A accept( final PsiTypeVisitor<A> visitor) {
        return visitor.visitMethodReferenceType(this);
    }

    @Override
    public GlobalSearchScope getResolveScope() {
        return null;
    }

    
    @Override
    public PsiType[] getSuperTypes() {
        return PsiType.EMPTY_ARRAY;
    }

    public PsiMethodReferenceExpression getExpression() {
        return myReference;
    }
}
