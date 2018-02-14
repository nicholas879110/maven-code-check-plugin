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
package com.gome.maven.psi.impl.source.resolve.reference.impl;

import com.gome.maven.psi.ElementManipulator;
import com.gome.maven.psi.ElementManipulators;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.impl.source.resolve.ResolveCache;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author peter
 */
public abstract class CachingReference implements PsiReference {
    @Override
    public PsiElement resolve(){
        return ResolveCache.getInstance(getElement().getProject()).resolveWithCaching(this, MyResolver.INSTANCE, false, false);
    }

    
    public abstract PsiElement resolveInner();

    @Override
    public boolean isReferenceTo(final PsiElement element) {
        return getElement().getManager().areElementsEquivalent(resolve(), element);
    }

    @Override
    public boolean isSoft(){
        return false;
    }

    
    public static <T extends PsiElement> ElementManipulator<T> getManipulator(T currentElement){
        ElementManipulator<T> manipulator = ElementManipulators.getManipulator(currentElement);
        if (manipulator == null) {
            throw new IncorrectOperationException("Manipulator for this element is not defined: " + currentElement + "; " + currentElement.getClass());
        }
        return manipulator;
    }

    private static class MyResolver implements ResolveCache.Resolver {
        private static final MyResolver INSTANCE = new MyResolver();
        @Override
        
        public PsiElement resolve( PsiReference ref, boolean incompleteCode) {
            return ((CachingReference)ref).resolveInner();
        }
    }

}
