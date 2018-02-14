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

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ModificationTracker;
import com.gome.maven.packaging.elements.CompositePackagingElement;
import com.gome.maven.packaging.elements.PackagingElement;
import com.gome.maven.packaging.elements.PackagingElementResolvingContext;
import com.gome.maven.util.messages.Topic;

import java.util.Collection;
import java.util.Comparator;

/**
 * @author nik
 */
public abstract class ArtifactManager implements ArtifactModel {
    public static final Topic<ArtifactListener> TOPIC = Topic.create("artifacts changes", ArtifactListener.class);
    public static final Comparator<Artifact> ARTIFACT_COMPARATOR = new Comparator<Artifact>() {
        public int compare(Artifact o1, Artifact o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

    public static ArtifactManager getInstance( Project project) {
        return project.getComponent(ArtifactManager.class);
    }

    public abstract Artifact[] getSortedArtifacts();

    public abstract ModifiableArtifactModel createModifiableModel();

    public abstract PackagingElementResolvingContext getResolvingContext();

    
    public abstract Artifact addArtifact(  String name,  ArtifactType type,  CompositePackagingElement<?> root);

    public abstract void addElementsToDirectory( Artifact artifact,  String relativePath,
                                                 Collection<? extends PackagingElement<?>> elements);

    public abstract void addElementsToDirectory( Artifact artifact,  String relativePath,
                                                 PackagingElement<?> element);

    public abstract ModificationTracker getModificationTracker();
}
