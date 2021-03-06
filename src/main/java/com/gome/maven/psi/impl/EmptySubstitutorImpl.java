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
package com.gome.maven.psi.impl;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.*;

import java.util.Collections;
import java.util.Map;

/**
 *  @author dsl
 */
public final class EmptySubstitutorImpl extends EmptySubstitutor {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.EmptySubstitutorImpl");
    @Override
    public PsiType substitute( PsiTypeParameter typeParameter){
        return JavaPsiFacade.getInstance(typeParameter.getProject()).getElementFactory().createType(typeParameter);
    }

    @Override
    public PsiType substitute(PsiType type){
        return type;
    }

    @Override
    public PsiType substituteWithBoundsPromotion( PsiTypeParameter typeParameter) {
        return JavaPsiFacade.getInstance(typeParameter.getProject()).getElementFactory().createType(typeParameter);
    }

    
    @Override
    public PsiSubstitutor put( PsiTypeParameter classParameter, PsiType mapping){
        if (mapping != null && !mapping.isValid()) {
            LOG.error("Invalid type in substitutor: " + mapping);
        }
        return new PsiSubstitutorImpl(classParameter, mapping);
    }

    
    @Override
    public PsiSubstitutor putAll( PsiClass parentClass, PsiType[] mappings){
        if(!parentClass.hasTypeParameters()) return this;
        return new PsiSubstitutorImpl(parentClass, mappings);
    }

    
    @Override
    public PsiSubstitutor putAll( PsiSubstitutor another) {
        return another;
    }

    @Override
    
    public Map<PsiTypeParameter, PsiType> getSubstitutionMap() {
        return Collections.emptyMap();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void ensureValid() { }
}
