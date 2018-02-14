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

import com.gome.maven.openapi.components.RoamingType;
import com.gome.maven.openapi.components.StateStorageException;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class XmlElementStorage extends StateStorageBase<StorageData> {
     protected final String myRootElementName;
    protected final StreamProvider myStreamProvider;
    protected final String myFileSpec;
    protected boolean myBlockSavingTheContent = false;

    protected final RoamingType myRoamingType;

    protected XmlElementStorage( String fileSpec,
                                 RoamingType roamingType,
                                 TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                 String rootElementName,
                                 StreamProvider streamProvider) {
        super(pathMacroSubstitutor);

        myFileSpec = fileSpec;
        myRoamingType = roamingType == null ? RoamingType.PER_USER : roamingType;
        myRootElementName = rootElementName;
        myStreamProvider = myRoamingType == RoamingType.DISABLED ? null : streamProvider;
    }

    
    protected abstract Element loadLocalData();

    
    @Override
    protected Element getStateAndArchive( StorageData storageData, Object component,  String componentName) {
        return storageData.getStateAndArchive(componentName);
    }

    
    protected StorageData loadData() {
        StorageData result = createStorageData();
        Element element;
        // we don't use local data if has stream provider
        if (myStreamProvider != null && myStreamProvider.isEnabled()) {
            try {
                element = loadDataFromStreamProvider();
                if (element != null) {
                    loadState(result, element);
                }
            }
            catch (Exception e) {
                LOG.error(e);
                element = null;
            }
        }
        else {
            element = loadLocalData();
        }

        if (element != null) {
            loadState(result, element);
        }
        return result;
    }

    
    protected final Element loadDataFromStreamProvider() throws IOException, JDOMException {
        assert myStreamProvider != null;
        return JDOMUtil.load(myStreamProvider.loadContent(myFileSpec, myRoamingType));
    }

    protected final void loadState( StorageData result,  Element element) {
        result.load(element, myPathMacroSubstitutor, true);
    }

    
    protected StorageData createStorageData() {
        return new StorageData(myRootElementName);
    }

    public void setDefaultState( Element element) {
        myStorageData = createStorageData();
        loadState(myStorageData, element);
    }

    @Override
    
    public final XmlElementStorageSaveSession startExternalization() {
        return checkIsSavingDisabled() ? null : createSaveSession(getStorageData());
    }

    
    protected abstract XmlElementStorageSaveSession createSaveSession( StorageData storageData);

    
    protected final Element getElement( StorageData data, boolean collapsePaths,  Map<String, Element> newLiveStates) {
        Element element = data.save(newLiveStates);
        if (element == null || JDOMUtil.isEmpty(element)) {
            return null;
        }

        if (collapsePaths && myPathMacroSubstitutor != null) {
            try {
                myPathMacroSubstitutor.collapsePaths(element);
            }
            finally {
                myPathMacroSubstitutor.reset();
            }
        }

        return element;
    }

    @Override
    public void analyzeExternalChangesAndUpdateIfNeed( Collection<VirtualFile> changedFiles,  Set<String> componentNames) {
        StorageData oldData = myStorageData;
        StorageData newData = getStorageData(true);
        if (oldData == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("analyzeExternalChangesAndUpdateIfNeed: old data null, load new for " + toString());
            }
            componentNames.addAll(newData.getComponentNames());
        }
        else {
            Set<String> changedComponentNames = oldData.getChangedComponentNames(newData, myPathMacroSubstitutor);
            if (LOG.isDebugEnabled()) {
                LOG.debug("analyzeExternalChangesAndUpdateIfNeed: changedComponentNames + " + changedComponentNames + " for " + toString());
            }
            if (!ContainerUtil.isEmpty(changedComponentNames)) {
                componentNames.addAll(changedComponentNames);
            }
        }
    }

    protected abstract class XmlElementStorageSaveSession extends SaveSessionBase {
        private final StorageData myOriginalStorageData;
        private StorageData myCopiedStorageData;

        private final Map<String, Element> myNewLiveStates = new THashMap<String, Element>();

        public XmlElementStorageSaveSession( StorageData storageData) {
            myOriginalStorageData = storageData;
        }

        
        @Override
        public final SaveSession createSaveSession() {
            return checkIsSavingDisabled() || myCopiedStorageData == null ? null : this;
        }

        @Override
        protected void setSerializedState( Object component,  String componentName,  Element element) {
            if (myCopiedStorageData == null) {
                myCopiedStorageData = StorageData.setStateAndCloneIfNeed(componentName, element, myOriginalStorageData, myNewLiveStates);
            }
            else {
                myCopiedStorageData.setState(componentName, element, myNewLiveStates);
            }
        }

        public void forceSave() {
            LOG.assertTrue(myCopiedStorageData == null);

            if (myBlockSavingTheContent) {
                return;
            }

            try {
                doSave(getElement(myOriginalStorageData, isCollapsePathsOnSave(), Collections.<String, Element>emptyMap()));
            }
            catch (IOException e) {
                throw new StateStorageException(e);
            }
        }

        @Override
        public final void save() throws IOException {
            if (myBlockSavingTheContent) {
                return;
            }

            doSave(getElement(myCopiedStorageData, isCollapsePathsOnSave(), myNewLiveStates));
            myStorageData = myCopiedStorageData;
        }

        // only because default project store hack
        protected boolean isCollapsePathsOnSave() {
            return true;
        }

        protected abstract void doSave( Element element) throws IOException;

        protected void saveForProvider( BufferExposingByteArrayOutputStream content,  Element element) throws IOException {
            if (!myStreamProvider.isApplicable(myFileSpec, myRoamingType)) {
                return;
            }

            if (element == null) {
                myStreamProvider.delete(myFileSpec, myRoamingType);
            }
            else {
                doSaveForProvider(element, myRoamingType, content);
            }
        }

        private void doSaveForProvider( Element element,  RoamingType roamingType,  BufferExposingByteArrayOutputStream content) throws IOException {
            if (content == null) {
                StorageUtil.sendContent(myStreamProvider, myFileSpec, element, roamingType, true);
            }
            else {
                myStreamProvider.saveContent(myFileSpec, content.getInternalBuffer(), content.size(), myRoamingType, true);
            }
        }
    }
}
