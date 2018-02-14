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
package com.gome.maven.packaging.ui;

import com.gome.maven.facet.Facet;
import com.gome.maven.openapi.module.ModifiableModuleModel;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.roots.ModifiableRootModel;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.packaging.artifacts.Artifact;
import com.gome.maven.packaging.artifacts.ArtifactType;
import com.gome.maven.packaging.artifacts.ModifiableArtifactModel;
import com.gome.maven.packaging.elements.CompositePackagingElement;
import com.gome.maven.packaging.elements.PackagingElementResolvingContext;

import java.util.List;

/**
 * @author nik
 */
public interface ArtifactEditorContext extends PackagingElementResolvingContext {

    void queueValidation();

    
    ArtifactType getArtifactType();

    
    ModifiableArtifactModel getOrCreateModifiableArtifactModel();

    
    ModifiableModuleModel getModifiableModuleModel();

    
    ModifiableRootModel getOrCreateModifiableRootModel( Module module);

    
    ManifestFileConfiguration getManifestFile(CompositePackagingElement<?> element, ArtifactType artifactType);


    CompositePackagingElement<?> getRootElement( Artifact artifact);

    void editLayout( Artifact artifact, Runnable runnable);

    ArtifactEditor getOrCreateEditor(Artifact originalArtifact);

    ArtifactEditor getThisArtifactEditor();

    void selectArtifact( Artifact artifact);

    void selectFacet( Facet<?> facet);

    void selectModule( Module module);

    void selectLibrary( Library library);


    List<Artifact> chooseArtifacts(List<? extends Artifact> artifacts, String title);

    List<Module> chooseModules(List<Module> modules, final String title);

    List<Library> chooseLibraries(String title);

    Artifact getArtifact();
}
