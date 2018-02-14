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

import com.gome.maven.application.options.PathMacrosImpl;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.application.impl.ApplicationImpl;
import com.gome.maven.openapi.components.PathMacroManager;
import com.gome.maven.openapi.components.StateStorageOperation;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.util.NamedJDOMExternalizable;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.messages.MessageBus;


import java.io.IOException;

class ApplicationStoreImpl extends ComponentStoreImpl implements IApplicationStore {
    private static final Logger LOG = Logger.getInstance(ApplicationStoreImpl.class);

    private static final String DEFAULT_STORAGE_SPEC = StoragePathMacros.APP_CONFIG + "/" + PathManager.DEFAULT_OPTIONS_FILE_NAME + DirectoryStorageData.DEFAULT_EXT;
    private static final String ROOT_ELEMENT_NAME = "application";

    private final ApplicationImpl myApplication;
    private final StateStorageManager myStateStorageManager;

    private String myConfigPath;

    // created from PicoContainer
    @SuppressWarnings({"UnusedDeclaration"})
    public ApplicationStoreImpl(final ApplicationImpl application, PathMacroManager pathMacroManager) {
        myApplication = application;
        myStateStorageManager = new StateStorageManagerImpl(pathMacroManager.createTrackingSubstitutor(), ROOT_ELEMENT_NAME, application, application.getPicoContainer()) {
            private boolean myConfigDirectoryRefreshed;

            @Override
            protected StorageData createStorageData( String fileSpec,  String filePath) {
                return new StorageData(ROOT_ELEMENT_NAME);
            }

            
            @Override
            protected String getOldStorageSpec( Object component,  String componentName,  StateStorageOperation operation) {
                if (component instanceof NamedJDOMExternalizable) {
                    return StoragePathMacros.APP_CONFIG + '/' + ((NamedJDOMExternalizable)component).getExternalFileName() + DirectoryStorageData.DEFAULT_EXT;
                }
                else {
                    return DEFAULT_STORAGE_SPEC;
                }
            }

            @Override
            protected TrackingPathMacroSubstitutor getMacroSubstitutor( final String fileSpec) {
                if (fileSpec.equals(StoragePathMacros.APP_CONFIG + '/' + PathMacrosImpl.EXT_FILE_NAME + DirectoryStorageData.DEFAULT_EXT)) return null;
                return super.getMacroSubstitutor(fileSpec);
            }

            @Override
            protected boolean isUseXmlProlog() {
                return false;
            }

            @Override
            protected void beforeFileBasedStorageCreate() {
                if (!myConfigDirectoryRefreshed && (application.isUnitTestMode() || application.isDispatchThread())) {
                    try {
                        VirtualFile configDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(getConfigPath());
                        if (configDir != null) {
                            VfsUtil.markDirtyAndRefresh(false, true, true, configDir);
                        }
                    }
                    finally {
                        myConfigDirectoryRefreshed = true;
                    }
                }
            }
        };
    }

    @Override
    public void load() throws IOException {
        long t = System.currentTimeMillis();
        myApplication.init();
        t = System.currentTimeMillis() - t;
        LOG.info(myApplication.getComponentConfigurations().length + " application components initialized in " + t + " ms");
    }

    @Override
    public void setOptionsPath( String path) {
        myStateStorageManager.addMacro(StoragePathMacros.APP_CONFIG, path);
    }

    @Override
    public void setConfigPath( final String configPath) {
        myStateStorageManager.addMacro(StoragePathMacros.ROOT_CONFIG, configPath);
        myConfigPath = configPath;
    }

    @Override
    
    public String getConfigPath() {
        String configPath = myConfigPath;
        if (configPath == null) {
            // unrealistic case, but we keep backward compatibility
            configPath = PathManager.getConfigPath();
        }
        return configPath;
    }

    @Override
    
    protected MessageBus getMessageBus() {
        return myApplication.getMessageBus();
    }

    
    @Override
    public StateStorageManager getStateStorageManager() {
        return myStateStorageManager;
    }

    
    @Override
    protected PathMacroManager getPathMacroManagerForDefaults() {
        return null;
    }
}
