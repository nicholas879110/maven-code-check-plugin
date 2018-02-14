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

/**
 * @author nik
 */
public abstract class ArtifactPropertiesProvider {
    public static final ExtensionPointName<ArtifactPropertiesProvider> EP_NAME = ExtensionPointName.create("com.intellij.packaging.artifactPropertiesProvider");
    private final String myId;

    protected ArtifactPropertiesProvider(  String id) {
        myId = id;
    }

    public final String getId() {
        return myId;
    }

    public boolean isAvailableFor( ArtifactType type) {
        return true;
    }

    
    public abstract ArtifactProperties<?> createProperties( ArtifactType artifactType);

    public static ArtifactPropertiesProvider[] getProviders() {
        return Extensions.getExtensions(EP_NAME);
    }


    public static ArtifactPropertiesProvider findById(  String id) {
        for (ArtifactPropertiesProvider provider : getProviders()) {
            if (provider.getId().equals(id)) {
                return provider;
            }
        }
        return null;
    }
}
