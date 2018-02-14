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
package com.gome.maven.packaging.elements;

import com.gome.maven.compiler.ant.Generator;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.packaging.artifacts.ArtifactType;
import com.gome.maven.packaging.ui.ArtifactEditorContext;
import com.gome.maven.packaging.ui.PackagingElementPresentation;

import java.util.List;

/**
 * @author nik
 */
public abstract class PackagingElement<S> implements PersistentStateComponent<S> {
    private final PackagingElementType myType;

    protected PackagingElement(PackagingElementType type) {
        myType = type;
    }

    public abstract PackagingElementPresentation createPresentation( ArtifactEditorContext context);

    public final PackagingElementType getType() {
        return myType;
    }

    public abstract List<? extends Generator> computeAntInstructions( PackagingElementResolvingContext resolvingContext,  AntCopyInstructionCreator creator,
                                                                      ArtifactAntGenerationContext generationContext,
                                                                      ArtifactType artifactType);

    public abstract boolean isEqualTo( PackagingElement<?> element);

    
    public PackagingElementOutputKind getFilesKind(PackagingElementResolvingContext context) {
        return PackagingElementOutputKind.OTHER;
    }
}
