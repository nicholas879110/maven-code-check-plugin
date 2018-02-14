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

import com.gome.maven.compiler.ant.GenerationOptions;
import com.gome.maven.compiler.ant.Generator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.packaging.artifacts.Artifact;

/**
 * @author nik
 */
public interface ArtifactAntGenerationContext {

    void runBeforeCurrentArtifact(Generator generator);

    void runBeforeBuild(Generator generator);

    void runAfterBuild(Generator generator);

    String createNewTempFileProperty( String basePropertyName,  String fileName);

    String getModuleOutputPath( String moduleName);

    String getModuleTestOutputPath( String moduleName);

    String getSubstitutedPath( String path);

    String getArtifactOutputProperty( Artifact artifact);

    Project getProject();

    GenerationOptions getGenerationOptions();

    String getConfiguredArtifactOutputProperty( Artifact artifact);
}
