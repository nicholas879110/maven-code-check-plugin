/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.roots.impl.storage;

import com.gome.maven.openapi.components.StateStorage;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.roots.ModifiableRootModel;
import com.gome.maven.openapi.roots.ModuleRootModel;

import java.io.IOException;
import java.util.List;

/**
 * @author Vladislav.Kaznacheev
 */
public interface ClasspathStorageProvider {
     ExtensionPointName<ClasspathStorageProvider> EXTENSION_POINT_NAME =
            new ExtensionPointName<ClasspathStorageProvider>("com.gome.maven.classpathStorageProvider");

    
    String getID();

    
    String getDescription();

    void assertCompatible(final ModuleRootModel model) throws ConfigurationException;

    void detach( Module module);

    void moduleRenamed( Module module,  String newName);

    
    ClasspathConverter createConverter(Module module);

    String getContentRoot( ModuleRootModel model);

    void modulePathChanged(Module module, String path);

    interface ClasspathConverter {
        
        List<String> getFileUrls();

        
        StateStorage.ExternalizationSession startExternalization();

        void readClasspath( ModifiableRootModel model) throws IOException;
    }
}
