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

import com.gome.maven.openapi.module.Module;
import com.gome.maven.util.messages.Topic;

/**
 * @author nik
 */
public abstract class FacetManager implements FacetModel {
    public static final Topic<FacetManagerListener> FACETS_TOPIC = Topic.create("facet changes", FacetManagerListener.class, Topic.BroadcastDirection.TO_PARENT);

    public static FacetManager getInstance( Module module) {
        return module.getComponent(FacetManager.class);
    }

    /**
     * Creates the interface for modifying set of facets in the module. Call {@link ModifiableFacetModel#commit()} when modification is finished
     * @return the modifiable facet model
     */
    
    public abstract ModifiableFacetModel createModifiableModel();

    
    public abstract <F extends Facet, C extends FacetConfiguration> F createFacet( FacetType<F, C> type,  String name,
                                                                                   C configuration,  Facet underlying);


    
    public abstract <F extends Facet, C extends FacetConfiguration> F createFacet( FacetType<F, C> type,  String name,
                                                                                   Facet underlying);

    
    public abstract <F extends Facet, C extends FacetConfiguration> F addFacet( FacetType<F, C> type,  String name,
                                                                                Facet underlying);

}
