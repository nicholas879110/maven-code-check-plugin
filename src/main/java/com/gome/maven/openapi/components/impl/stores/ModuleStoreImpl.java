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

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleTypeManager;
import com.gome.maven.openapi.module.impl.ModuleImpl;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.PathUtilRt;
import com.gome.maven.util.messages.MessageBus;
import org.jdom.Attribute;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ModuleStoreImpl extends BaseFileConfigurableStoreImpl implements IModuleStore {
    private static final Logger LOG = Logger.getInstance(ModuleStoreImpl.class);

    private final ModuleImpl myModule;

    @SuppressWarnings({"UnusedDeclaration"})
    public ModuleStoreImpl( ModuleImpl module,  PathMacroManager pathMacroManager) {
        super(pathMacroManager);

        myModule = module;
    }

    
    @Override
    protected FileBasedStorage getMainStorage() {
        FileBasedStorage storage = (FileBasedStorage)getStateStorageManager().getStateStorage(StoragePathMacros.MODULE_FILE, RoamingType.PER_USER);
        assert storage != null;
        return storage;
    }

    @Override
    protected Project getProject() {
        return myModule.getProject();
    }

    @Override
    public void load() throws IOException {
        super.load();

        String moduleTypeId = getMainStorageData().myOptions.get(Module.ELEMENT_TYPE);
        myModule.setOption(Module.ELEMENT_TYPE, ModuleTypeManager.getInstance().findByID(moduleTypeId).getId());

        if (ApplicationManager.getApplication().isHeadlessEnvironment() || ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        final TrackingPathMacroSubstitutor substitutor = getStateStorageManager().getMacroSubstitutor();
        if (substitutor != null) {
            final Collection<String> macros = substitutor.getUnknownMacros(null);
            if (!macros.isEmpty()) {
                final Project project = myModule.getProject();
                StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
                    @Override
                    public void run() {
                        StorageUtil.notifyUnknownMacros(substitutor, project, null);
                    }
                });
            }
        }
    }

    @Override
    public ModuleFileData getMainStorageData() {
        return (ModuleFileData)super.getMainStorageData();
    }

    static class ModuleFileData extends BaseStorageData {
        private final Map<String, String> myOptions;
        private final Module myModule;

        private boolean dirty = true;

        public ModuleFileData( String rootElementName,  Module module) {
            super(rootElementName);

            myModule = module;
            myOptions = new TreeMap<String, String>();
        }

        public boolean isDirty() {
            return dirty;
        }

        private ModuleFileData( ModuleFileData storageData) {
            super(storageData);

            myModule = storageData.myModule;
            dirty = storageData.dirty;
            myOptions = new TreeMap<String, String>(storageData.myOptions);
        }

        @Override
        public void load( Element rootElement,  PathMacroSubstitutor pathMacroSubstitutor, boolean intern) {
            super.load(rootElement, pathMacroSubstitutor, intern);

            for (Attribute attribute : (List<Attribute>)rootElement.getAttributes()) {
                if (!attribute.getName().equals(VERSION_OPTION)) {
                    myOptions.put(attribute.getName(), attribute.getValue());
                }
            }

            dirty = false;
        }

        @Override
        protected void writeOptions( Element root,  String versionString) {
            if (!myOptions.isEmpty()) {
                for (Map.Entry<String, String> entry : myOptions.entrySet()) {
                    root.setAttribute(entry.getKey(), entry.getValue());
                }
            }
            // need be last for compat reasons
            super.writeOptions(root, versionString);

            dirty = false;
        }

        @Override
        public StorageData clone() {
            return new ModuleFileData(this);
        }

        
        @Override
        public Set<String> getChangedComponentNames( StorageData newStorageData,  PathMacroSubstitutor substitutor) {
            final ModuleFileData data = (ModuleFileData)newStorageData;
            if (!myOptions.equals(data.myOptions)) {
                return null;
            }
            return super.getChangedComponentNames(newStorageData, substitutor);
        }

        public void setOption( String optionName,  String optionValue) {
            if (!optionValue.equals(myOptions.put(optionName, optionValue))) {
                dirty = true;
            }
        }

        public void clearOption( String optionName) {
            if (myOptions.remove(optionName) != null) {
                dirty = true;
            }
        }

        
        public String getOptionValue( String optionName) {
            return myOptions.get(optionName);
        }
    }

    @Override
    public void setModuleFilePath( String filePath) {
        final String path = filePath.replace(File.separatorChar, '/');
        LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        final StateStorageManager storageManager = getStateStorageManager();
        storageManager.clearStateStorage(StoragePathMacros.MODULE_FILE);
        storageManager.addMacro(StoragePathMacros.MODULE_FILE, path);
    }

    @Override
    
    public VirtualFile getModuleFile() {
        return getMainStorage().getVirtualFile();
    }

    @Override
    
    public String getModuleFilePath() {
        return getMainStorage().getFilePath();
    }

    @Override
    
    public String getModuleFileName() {
        return PathUtilRt.getFileName(getMainStorage().getFilePath());
    }

    @Override
    public void setOption( String optionName,  String optionValue) {
        try {
            getMainStorageData().setOption(optionName,  optionValue);
        }
        catch (StateStorageException e) {
            LOG.error(e);
        }
    }

    @Override
    public void clearOption( String optionName) {
        try {
            getMainStorageData().clearOption(optionName);
        }
        catch (StateStorageException e) {
            LOG.error(e);
        }
    }

    @Override
    public String getOptionValue( String optionName) {
        try {
            return getMainStorageData().getOptionValue(optionName);
        }
        catch (StateStorageException e) {
            LOG.error(e);
            return null;
        }
    }

    @Override
    protected boolean optimizeTestLoading() {
        return ((ProjectEx)myModule.getProject()).isOptimiseTestLoadSpeed();
    }

    
    @Override
    protected MessageBus getMessageBus() {
        return myModule.getMessageBus();
    }

    
    @Override
    protected StateStorageManager createStateStorageManager() {
        return new ModuleStateStorageManager(myPathMacroManager.createTrackingSubstitutor(), myModule);
    }
}
