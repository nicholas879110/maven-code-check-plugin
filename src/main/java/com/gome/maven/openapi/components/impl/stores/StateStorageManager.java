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
import com.gome.maven.openapi.util.Couple;


import java.util.Collection;
import java.util.List;

public interface StateStorageManager {
    void addMacro( String macro,  String expansion);

    
    TrackingPathMacroSubstitutor getMacroSubstitutor();

    
    StateStorage getStateStorage( Storage storageSpec);

    
    StateStorage getStateStorage( String fileSpec,  RoamingType roamingType);

    
    Couple<Collection<FileBasedStorage>> getCachedFileStateStorages( Collection<String> changed,  Collection<String> deleted);

    
    Collection<String> getStorageFileNames();

    void clearStateStorage( String file);

    
    ExternalizationSession startExternalization();

    
    StateStorage getOldStorage( Object component,  String componentName,  StateStorageOperation operation);

    
    String expandMacros( String file);

    
    String collapseMacros( String path);

    void setStreamProvider( StreamProvider streamProvider);

    
    StreamProvider getStreamProvider();

    interface ExternalizationSession {
        void setState( Storage[] storageSpecs,  Object component,  String componentName,  Object state);

        void setStateInOldStorage( Object component,  String componentName,  Object state);

        /**
         * return empty list if nothing to save
         */
        
        List<StateStorage.SaveSession> createSaveSessions();
    }
}