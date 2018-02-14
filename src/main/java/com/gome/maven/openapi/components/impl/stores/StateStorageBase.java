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

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.components.StateStorage;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import org.jdom.Element;

public abstract class StateStorageBase<T extends StorageDataBase> implements StateStorage {
    protected static final Logger LOG = Logger.getInstance(StateStorageBase.class);

    private boolean mySavingDisabled = false;
    protected final TrackingPathMacroSubstitutor myPathMacroSubstitutor;

    protected T myStorageData;

    protected StateStorageBase( TrackingPathMacroSubstitutor trackingPathMacroSubstitutor) {
        myPathMacroSubstitutor = trackingPathMacroSubstitutor;
    }

    @Override
    
    public final <S> S getState(Object component,  String componentName,  Class<S> stateClass,  S mergeInto) {
        return deserializeState(getStateAndArchive(getStorageData(), component, componentName), stateClass, mergeInto);
    }

    
    protected <S> S deserializeState( Element serializedState,  Class <S> stateClass,  S mergeInto) {
        return DefaultStateSerializer.deserializeState(serializedState, stateClass, mergeInto);
    }

    
    protected abstract Element getStateAndArchive( T storageData, Object component,  String componentName);

    @Override
    public final boolean hasState( Object component,  String componentName, Class<?> aClass, boolean reloadData) {
        return getStorageData(reloadData).hasState(componentName);
    }

    
    public final T getStorageData() {
        return getStorageData(false);
    }

    
    protected final T getStorageData(boolean reload) {
        if (myStorageData != null && !reload) {
            return myStorageData;
        }

        myStorageData = loadData();
        return myStorageData;
    }

    protected abstract T loadData();

    public final void disableSaving() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Disabled saving for " + toString());
        }
        mySavingDisabled = true;
    }

    public final void enableSaving() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Enabled saving " + toString());
        }
        mySavingDisabled = false;
    }

    protected final boolean checkIsSavingDisabled() {
        if (mySavingDisabled && LOG.isDebugEnabled()) {
            LOG.debug("Saving disabled for " + toString());
        }
        return mySavingDisabled;
    }
}