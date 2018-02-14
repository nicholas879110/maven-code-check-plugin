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

package com.gome.maven.facet;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.UserDataHolderBase;

/**
 * Represents a specific instance of facet
 *
 * @see FacetType
 *
 * @author nik
 */
public class Facet<C extends FacetConfiguration> extends UserDataHolderBase implements UserDataHolder, Disposable {
    public static final Facet[] EMPTY_ARRAY = new Facet[0];
     private final FacetType myFacetType;
     private final Module myModule;
     private final C myConfiguration;
    private final Facet myUnderlyingFacet;
    private String myName;
    private boolean isDisposed;

    public Facet( final FacetType facetType,  final Module module,  final String name,  final C configuration, Facet underlyingFacet) {
        myName = name;
        myFacetType = facetType;
        myModule = module;
        myConfiguration = configuration;
        myUnderlyingFacet = underlyingFacet;
        Disposer.register(myModule, this);
    }

    
    public final FacetType getType() {
        return myFacetType;
    }

    public final FacetTypeId getTypeId() {
        return myFacetType.getId();
    }

    public final Facet getUnderlyingFacet() {
        return myUnderlyingFacet;
    }

    
    public final C getConfiguration() {
        return myConfiguration;
    }

    
    public final Module getModule() {
        return myModule;
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    /**
     * Called when the module containing this facet is initialized
     */
    public void initFacet() {
    }

    /**
     * Called when the module containing this facet is disposed
     */
    public void disposeFacet() {
    }

    @Override
    public final void dispose() {
        assert !isDisposed;
        isDisposed = true;
        disposeFacet();
    }

    public final int hashCode() {
        return super.hashCode();
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    
    public final String getName() {
        return myName;
    }

    /**
     * Use {@link com.gome.maven.facet.ModifiableFacetModel#rename} to rename facets
     */
    final void setName( final String name) {
        myName = name;
    }

    @Override
    public String toString() {
        return getName() + " (" + getModule().getName() + ")";
    }
}