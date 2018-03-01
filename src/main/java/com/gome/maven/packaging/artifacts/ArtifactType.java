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

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.packaging.elements.CompositePackagingElement;
import com.gome.maven.packaging.elements.PackagingElement;
import com.gome.maven.packaging.elements.PackagingElementOutputKind;
import com.gome.maven.packaging.elements.PackagingElementResolvingContext;
import com.gome.maven.packaging.ui.ArtifactProblemsHolder;
import com.gome.maven.packaging.ui.PackagingSourceItem;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class ArtifactType {
    public static final ExtensionPointName<ArtifactType> EP_NAME = ExtensionPointName.create("com.gome.maven.packaging.artifactType");
    private final String myId;
    private final String myTitle;

    protected ArtifactType( String id, String title) {
        myId = id;
        myTitle = title;
    }

    public final String getId() {
        return myId;
    }

    public String getPresentableName() {
        return myTitle;
    }

    
    public abstract Icon getIcon();

    
    public String getDefaultPathFor( PackagingSourceItem sourceItem) {
        return getDefaultPathFor(sourceItem.getKindOfProducedElements());
    }

    
    public abstract String getDefaultPathFor( PackagingElementOutputKind kind);

    public boolean isSuitableItem( PackagingSourceItem sourceItem) {
        return true;
    }

    public static ArtifactType[] getAllTypes() {
        return Extensions.getExtensions(EP_NAME);
    }

    
    public static ArtifactType findById(  String id) {
        for (ArtifactType type : getAllTypes()) {
            if (id.equals(type.getId())) {
                return type;
            }
        }
        return null;
    }

    
    public abstract CompositePackagingElement<?> createRootElement( String artifactName);

    
    public List<? extends ArtifactTemplate> getNewArtifactTemplates( PackagingElementResolvingContext context) {
        return Collections.emptyList();
    }

    public void checkRootElement( CompositePackagingElement<?> rootElement,  Artifact artifact,  ArtifactProblemsHolder manager) {
    }

    
    public List<? extends PackagingElement<?>> getSubstitution( Artifact artifact,  PackagingElementResolvingContext context,
                                                                ArtifactType parentType) {
        return null;
    }
}
