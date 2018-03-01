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

package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.messages.MessageBus;

import java.io.IOException;

/**
 * @author Gregory.Shrago
 */
public interface FilePropertyPusher<T> {
    ExtensionPointName<FilePropertyPusher> EP_NAME = ExtensionPointName.create("com.gome.maven.filePropertyPusher");

    void initExtra( Project project,  MessageBus bus,  Engine languageLevelUpdater);
    
    Key<T> getFileDataKey();
    boolean pushDirectoriesOnly();

    
    T getDefaultValue();

    
    T getImmediateValue( Project project,  VirtualFile file);

    
    T getImmediateValue( Module module);

    boolean acceptsFile( VirtualFile file);
    boolean acceptsDirectory( VirtualFile file,  Project project);

    void persistAttribute( Project project,  VirtualFile fileOrDir,  T value) throws IOException;

    interface Engine {
        void pushAll();
        void pushRecursively(VirtualFile vile, Project project);
    }

    void afterRootsChanged( Project project);
}
