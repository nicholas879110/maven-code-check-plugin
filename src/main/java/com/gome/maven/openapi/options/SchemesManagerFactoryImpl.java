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
package com.gome.maven.openapi.options;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.impl.ApplicationImpl;
import com.gome.maven.openapi.components.RoamingType;
import com.gome.maven.openapi.components.SettingsSavingComponent;
import com.gome.maven.openapi.components.impl.stores.StateStorageManager;
import com.gome.maven.openapi.components.impl.stores.StreamProvider;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.lang.CompoundRuntimeException;

import java.io.File;
import java.util.List;

final class SchemesManagerFactoryImpl extends SchemesManagerFactory implements SettingsSavingComponent {
    private static final Logger LOG = Logger.getInstance(SchemesManagerFactoryImpl.class);

    private final List<SchemesManagerImpl> myRegisteredManagers = ContainerUtil.createLockFreeCopyOnWriteList();

    
    @Override
    public <T extends Scheme, E extends ExternalizableScheme> SchemesManager<T, E> createSchemesManager( String fileSpec,
                                                                                                         SchemeProcessor<E> processor,
                                                                                                         RoamingType roamingType) {
        StateStorageManager storageManager = ((ApplicationImpl)ApplicationManager.getApplication()).getStateStore().getStateStorageManager();
        String baseDirPath = storageManager.expandMacros(fileSpec);
        StreamProvider provider = storageManager.getStreamProvider();
        SchemesManagerImpl<T, E> manager = new SchemesManagerImpl<T, E>(fileSpec, processor, roamingType, provider, new File(baseDirPath));
        myRegisteredManagers.add(manager);
        return manager;
    }

    @Override
    public void updateConfigFilesFromStreamProviders() {
        for (SchemesManagerImpl registeredManager : myRegisteredManagers) {
            try {
                registeredManager.updateConfigFilesFromStreamProviders();
            }
            catch (Throwable e) {
                LOG.error("Cannot reload settings for " + registeredManager.getClass().getName(), e);
            }
        }
    }

    @Override
    public void save() {
        List<Throwable> errors = null;
        for (SchemesManager registeredManager : myRegisteredManagers) {
            try {
                registeredManager.save();
            }
            catch (Throwable e) {
                if (errors == null) {
                    errors = new SmartList<Throwable>();
                }
                errors.add(e);
            }
        }

        CompoundRuntimeException.doThrow(errors);
    }
}
