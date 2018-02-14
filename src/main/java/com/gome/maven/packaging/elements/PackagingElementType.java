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

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.packaging.artifacts.Artifact;
import com.gome.maven.packaging.ui.ArtifactEditorContext;
import com.gome.maven.packaging.ui.PackagingElementPropertiesPanel;

import javax.swing.*;
import java.util.List;

/**
 * @author nik
 */
public abstract class PackagingElementType<E extends PackagingElement<?>> {
    public static final ExtensionPointName<PackagingElementType> EP_NAME = ExtensionPointName.create("com.intellij.packaging.elementType");
    private final String myId;
    private final String myPresentableName;

    protected PackagingElementType(  String id,  String presentableName) {
        myId = id;
        myPresentableName = presentableName;
    }

    public final String getId() {
        return myId;
    }

    public String getPresentableName() {
        return myPresentableName;
    }

    
    public Icon getCreateElementIcon() {
        return null;
    }

    public abstract boolean canCreate( ArtifactEditorContext context,  Artifact artifact);

    
    public abstract List<? extends PackagingElement<?>> chooseAndCreate( ArtifactEditorContext context,  Artifact artifact,
                                                                         CompositePackagingElement<?> parent);

    
    public abstract E createEmpty( Project project);

    protected static <T extends PackagingElementType<?>> T getInstance(final Class<T> aClass) {
        for (PackagingElementType type : Extensions.getExtensions(EP_NAME)) {
            if (aClass.isInstance(type)) {
                return aClass.cast(type);
            }
        }
        throw new AssertionError();
    }

    
    public PackagingElementPropertiesPanel createElementPropertiesPanel( E element,  ArtifactEditorContext context) {
        return null;
    }
}
