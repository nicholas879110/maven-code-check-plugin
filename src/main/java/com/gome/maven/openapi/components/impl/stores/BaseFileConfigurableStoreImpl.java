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

import com.gome.maven.openapi.components.PathMacroManager;
import com.gome.maven.openapi.components.PathMacroSubstitutor;
import com.gome.maven.openapi.project.impl.ProjectManagerImpl;
import com.gome.maven.util.SmartList;
import org.jdom.Element;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class BaseFileConfigurableStoreImpl extends ComponentStoreImpl {
     protected static final String VERSION_OPTION = "version";
     public static final String ATTRIBUTE_NAME = "name";

    private static final List<String> ourConversionProblemsStorage = new SmartList<String>();

    private StateStorageManager myStateStorageManager;
    protected final PathMacroManager myPathMacroManager;

    protected BaseFileConfigurableStoreImpl( PathMacroManager pathMacroManager) {
        myPathMacroManager = pathMacroManager;
    }

    protected static class BaseStorageData extends StorageData {
        private int myVersion = ProjectManagerImpl.CURRENT_FORMAT_VERSION;

        public BaseStorageData( String rootElementName) {
            super(rootElementName);
        }

        protected BaseStorageData(BaseStorageData storageData) {
            super(storageData);
        }

        @Override
        public void load( Element rootElement,  PathMacroSubstitutor pathMacroSubstitutor, boolean intern) {
            super.load(rootElement, pathMacroSubstitutor, intern);

            String v = rootElement.getAttributeValue(VERSION_OPTION);
            myVersion = v == null ? ProjectManagerImpl.CURRENT_FORMAT_VERSION : Integer.parseInt(v);
        }

        @Override
        
        protected final Element save( Map<String, Element> newLiveStates) {
            Element root = super.save(newLiveStates);
            if (root == null) {
                root = new Element(myRootElementName);
            }
            writeOptions(root, Integer.toString(myVersion));
            return root;
        }

        protected void writeOptions( Element root,  String versionString) {
            root.setAttribute(VERSION_OPTION, versionString);
        }

        @Override
        public StorageData clone() {
            return new BaseStorageData(this);
        }

        
        @Override
        public Set<String> getChangedComponentNames( StorageData newStorageData,  PathMacroSubstitutor substitutor) {
            BaseStorageData data = (BaseStorageData)newStorageData;
            if (myVersion != data.myVersion) {
                return null;
            }
            return super.getChangedComponentNames(newStorageData, substitutor);
        }
    }

    
    protected abstract XmlElementStorage getMainStorage();

    
    static List<String> getConversionProblemsStorage() {
        return ourConversionProblemsStorage;
    }

    @Override
    public void load() throws IOException {
        getMainStorageData(); //load it
    }

    public BaseStorageData getMainStorageData() {
        return (BaseStorageData)getMainStorage().getStorageData();
    }

    
    @Override
    protected final PathMacroManager getPathMacroManagerForDefaults() {
        return myPathMacroManager;
    }

    
    @Override
    public final StateStorageManager getStateStorageManager() {
        if (myStateStorageManager == null) {
            myStateStorageManager = createStateStorageManager();
        }
        return myStateStorageManager;
    }

    
    protected abstract StateStorageManager createStateStorageManager();
}
