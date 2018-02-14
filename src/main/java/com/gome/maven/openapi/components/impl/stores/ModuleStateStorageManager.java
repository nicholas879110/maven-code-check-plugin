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

import com.gome.maven.openapi.components.StateStorage;
import com.gome.maven.openapi.components.StateStorageOperation;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.module.impl.ModuleImpl;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

class ModuleStateStorageManager extends StateStorageManagerImpl {
     private static final String ROOT_TAG_NAME = "module";
    private final ModuleImpl myModule;

    public ModuleStateStorageManager( TrackingPathMacroSubstitutor pathMacroManager,  ModuleImpl module) {
        super(pathMacroManager, ROOT_TAG_NAME, module, module.getPicoContainer());

        myModule = module;
    }

    @Override
    protected StorageData createStorageData( String fileSpec,  String filePath) {
        return new ModuleStoreImpl.ModuleFileData(ROOT_TAG_NAME, myModule);
    }

    
    @Override
    public ExternalizationSession startExternalization() {
        return new StateStorageManagerExternalizationSession() {
            
            @Override
            public List<StateStorage.SaveSession> createSaveSessions() {
                final ModuleStoreImpl.ModuleFileData data = myModule.getStateStore().getMainStorageData();
                List<StateStorage.SaveSession> sessions = super.createSaveSessions();
                if (!data.isDirty()) {
                    return sessions;
                }

                return ContainerUtil.concat(sessions, Collections.singletonList(new StateStorage.SaveSession() {
                    @Override
                    public void save() {
                        if (data.isDirty()) {
                            myModule.getStateStore().getMainStorage().forceSave();
                        }
                    }
                }));
            }
        };
    }

    
    @Override
    protected String getOldStorageSpec( Object component,  String componentName,  StateStorageOperation operation) {
        return StoragePathMacros.MODULE_FILE;
    }

    
    @Override
    protected StateStorage.Listener createStorageTopicListener() {
        return myModule.getProject().getMessageBus().syncPublisher(StateStorage.PROJECT_STORAGE_TOPIC);
    }
}
