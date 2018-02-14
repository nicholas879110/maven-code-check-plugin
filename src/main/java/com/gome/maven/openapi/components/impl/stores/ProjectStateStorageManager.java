/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.project.impl.ProjectImpl;
import org.jdom.Element;

import java.util.Map;

class ProjectStateStorageManager extends StateStorageManagerImpl {
    protected final ProjectImpl myProject;
     protected static final String ROOT_TAG_NAME = "project";

    public ProjectStateStorageManager(final TrackingPathMacroSubstitutor macroSubstitutor, ProjectImpl project) {
        super(macroSubstitutor, ROOT_TAG_NAME, project, project.getPicoContainer());
        myProject = project;
    }

    @Override
    protected StorageData createStorageData( String fileSpec,  String filePath) {
        if (fileSpec.equals(StoragePathMacros.PROJECT_FILE)) {
            return createIprStorageData(filePath);
        }
        else if (fileSpec.equals(StoragePathMacros.WORKSPACE_FILE)) {
            return new ProjectStoreImpl.WsStorageData(ROOT_TAG_NAME, myProject);
        }
        else {
            return new ProjectStoreImpl.ProjectStorageData(ROOT_TAG_NAME, myProject);
        }
    }

    protected StorageData createIprStorageData( String filePath) {
        return new ProjectStoreImpl.IprStorageData(ROOT_TAG_NAME, myProject);
    }

    @Override
    protected String getOldStorageSpec( Object component,  String componentName,  StateStorageOperation operation) {
        final ComponentConfig config = myProject.getConfig(component.getClass());
        assert config != null : "Couldn't find old storage for " + component.getClass().getName();

        final boolean workspace = isWorkspace(config.options);
        String fileSpec = workspace ? StoragePathMacros.WORKSPACE_FILE : StoragePathMacros.PROJECT_FILE;
        StateStorage storage = getStateStorage(fileSpec, workspace ? RoamingType.DISABLED : RoamingType.PER_USER);
        if (operation == StateStorageOperation.READ && storage != null && workspace && !storage.hasState(component, componentName, Element.class, false)) {
            fileSpec = StoragePathMacros.PROJECT_FILE;
        }
        return fileSpec;
    }

    private static boolean isWorkspace(final Map options) {
        return options != null && Boolean.parseBoolean((String)options.get(ProjectStoreImpl.OPTION_WORKSPACE));
    }

    
    @Override
    protected StateStorage.Listener createStorageTopicListener() {
        return myProject.getMessageBus().syncPublisher(StateStorage.PROJECT_STORAGE_TOPIC);
    }
}
