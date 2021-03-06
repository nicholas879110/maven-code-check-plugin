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
package com.gome.maven.packaging.artifacts;

import com.gome.maven.packaging.elements.CompositePackagingElement;

/**
 * @author nik
 */
public interface ModifiableArtifactModel extends ArtifactModel {

    
    ModifiableArtifact addArtifact(final  String name,  ArtifactType artifactType);

    
    ModifiableArtifact addArtifact(final  String name,  ArtifactType artifactType, CompositePackagingElement<?> rootElement);

    void removeArtifact( Artifact artifact);

    
    ModifiableArtifact getOrCreateModifiableArtifact( Artifact artifact);

    
    Artifact getModifiableCopy(Artifact artifact);

    void addListener( ArtifactListener listener);

    void removeListener( ArtifactListener listener);


    boolean isModified();

    void commit();

    void dispose();
}
