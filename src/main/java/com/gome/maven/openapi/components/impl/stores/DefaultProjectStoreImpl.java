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
package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.components.StateStorage.SaveSession;
import com.gome.maven.openapi.project.impl.ProjectImpl;
import com.gome.maven.openapi.project.impl.ProjectManagerImpl;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;


import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DefaultProjectStoreImpl extends ProjectStoreImpl {
    private final ProjectManagerImpl myProjectManager;
     private static final String ROOT_TAG_NAME = "defaultProject";

    public DefaultProjectStoreImpl( ProjectImpl project,  ProjectManagerImpl projectManager,  PathMacroManager pathMacroManager) {
        super(project, pathMacroManager);

        myProjectManager = projectManager;
    }

    
    Element getStateCopy() {
        final Element element = myProjectManager.getDefaultProjectRootElement();
        return element != null ? (Element) element.clone() : null;
    }

    
    @Override
    protected StateStorageManager createStateStorageManager() {
        final XmlElementStorage storage = new XmlElementStorage("", RoamingType.DISABLED, myPathMacroManager.createTrackingSubstitutor(),
                ROOT_TAG_NAME, null) {
            @Override
            
            protected Element loadLocalData() {
                return getStateCopy();
            }

            
            @Override
            protected XmlElementStorageSaveSession createSaveSession( StorageData storageData) {
                return new XmlElementStorageSaveSession(storageData) {
                    @Override
                    protected void doSave( Element element) {
                        // we must set empty element instead of null as indicator - ProjectManager state is ready to save
                        myProjectManager.setDefaultProjectRootElement(element == null ? new Element("empty") : element);
                    }

                    // we must not collapse paths here, because our solution is just a big hack
                    // by default, getElementToSave() returns collapsed paths -> setDefaultProjectRootElement -> project manager writeExternal -> save -> compare old and new - diff because old has expanded, but new collapsed
                    // -> needless save
                    @Override
                    protected boolean isCollapsePathsOnSave() {
                        return false;
                    }
                };
            }

            @Override
            
            protected StorageData createStorageData() {
                return new BaseStorageData(ROOT_TAG_NAME);
            }
        };

        //noinspection deprecation
        return new StateStorageManager() {
            @Override
            public void addMacro( String macro,  String expansion) {
                throw new UnsupportedOperationException("Method addMacro not implemented in " + getClass());
            }

            @Override
            
            public TrackingPathMacroSubstitutor getMacroSubstitutor() {
                return null;
            }

            @Override
            
            public StateStorage getStateStorage( Storage storageSpec) {
                return storage;
            }

            
            @Override
            public StateStorage getStateStorage( String fileSpec,  RoamingType roamingType) {
                return storage;
            }

            
            @Override
            public Couple<Collection<FileBasedStorage>> getCachedFileStateStorages( Collection<String> changed,  Collection<String> deleted) {
                return new Couple<Collection<FileBasedStorage>>(Collections.<FileBasedStorage>emptyList(), Collections.<FileBasedStorage>emptyList());
            }

            @Override
            public void clearStateStorage( String file) {
            }

            
            @Override
            public ExternalizationSession startExternalization() {
                StateStorage.ExternalizationSession externalizationSession = storage.startExternalization();
                return externalizationSession == null ? null : new MyExternalizationSession(externalizationSession);
            }

            
            @Override
            public String expandMacros( String file) {
                throw new UnsupportedOperationException("Method expandMacros not implemented in " + getClass());
            }

            
            @Override
            public String collapseMacros( String path) {
                throw new UnsupportedOperationException("Method collapseMacros not implemented in " + getClass());
            }

            @Override
            
            public StateStorage getOldStorage( Object component,  String componentName,  StateStorageOperation operation) {
                return storage;
            }

            @Override
            public void setStreamProvider( StreamProvider streamProvider) {
                throw new UnsupportedOperationException("Method setStreamProvider not implemented in " + getClass());
            }

            
            @Override
            public StreamProvider getStreamProvider() {
                throw new UnsupportedOperationException("Method getStreamProviders not implemented in " + getClass());
            }

            
            @Override
            public Collection<String> getStorageFileNames() {
                throw new UnsupportedOperationException("Method getStorageFileNames not implemented in " + getClass());
            }
        };
    }

    @Override
    public void load() throws IOException {
        if (myProjectManager.getDefaultProjectRootElement() != null) {
            super.load();
        }
    }

    private static class MyExternalizationSession implements StateStorageManager.ExternalizationSession {
         final StateStorage.ExternalizationSession externalizationSession;

        public MyExternalizationSession( StateStorage.ExternalizationSession externalizationSession) {
            this.externalizationSession = externalizationSession;
        }

        @Override
        public void setState( Storage[] storageSpecs,  Object component,  String componentName,  Object state) {
            externalizationSession.setState(component, componentName, state, null);
        }

        @Override
        public void setStateInOldStorage( Object component,  String componentName,  Object state) {
            externalizationSession.setState(component, componentName, state, null);
        }

        
        @Override
        public List<SaveSession> createSaveSessions() {
            return ContainerUtil.createMaybeSingletonList(externalizationSession.createSaveSession());
        }
    }
}
