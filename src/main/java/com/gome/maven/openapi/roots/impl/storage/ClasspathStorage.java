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
package com.gome.maven.openapi.roots.impl.storage;

import com.gome.maven.application.options.PathMacrosCollector;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.components.impl.stores.IModuleStore;
import com.gome.maven.openapi.components.impl.stores.StateStorageBase;
import com.gome.maven.openapi.components.impl.stores.StorageDataBase;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.roots.ModifiableRootModel;
import com.gome.maven.openapi.roots.ModuleRootModel;
import com.gome.maven.openapi.roots.impl.ModuleRootManagerImpl;
import com.gome.maven.openapi.roots.impl.ModuleRootManagerImpl.ModuleRootManagerState;
import com.gome.maven.openapi.roots.impl.RootModelImpl;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.io.FileUtilRt;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileAdapter;
import com.gome.maven.openapi.vfs.VirtualFileEvent;
import com.gome.maven.openapi.vfs.tracker.VirtualFileTracker;
import com.gome.maven.util.PathUtil;
import org.jdom.Element;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClasspathStorage extends StateStorageBase<ClasspathStorage.MyStorageData> {
     public static final String SPECIAL_STORAGE = "special";

    private final ClasspathStorageProvider.ClasspathConverter myConverter;

    public ClasspathStorage( Module module,  IModuleStore moduleStore) {
        super(moduleStore.getStateStorageManager().getMacroSubstitutor());

        ClasspathStorageProvider provider = getProvider(ClassPathStorageUtil.getStorageType(module));
        assert provider != null;
        myConverter = provider.createConverter(module);
        assert myConverter != null;

        VirtualFileTracker virtualFileTracker = ServiceManager.getService(VirtualFileTracker.class);
        if (virtualFileTracker != null) {
            List<String> urls = myConverter.getFileUrls();
            for (String url : urls) {
                final Listener listener = module.getProject().getMessageBus().syncPublisher(PROJECT_STORAGE_TOPIC);
                virtualFileTracker.addTracker(url, new VirtualFileAdapter() {
                    @Override
                    public void contentsChanged( VirtualFileEvent event) {
                        listener.storageFileChanged(event, ClasspathStorage.this);
                    }
                }, true, module);
            }
        }
    }

    
    @Override
    protected <S> S deserializeState( Element serializedState,  Class<S> stateClass,  S mergeInto) {
        if (serializedState == null) {
            return null;
        }

        ModuleRootManagerState state = new ModuleRootManagerState();
        state.readExternal(serializedState);
        //noinspection unchecked
        return (S)state;
    }

    static class MyStorageData extends StorageDataBase {
        private boolean loaded;

        
        @Override
        public Set<String> getComponentNames() {
            return Collections.emptySet();
        }

        @Override
        public boolean hasState( String componentName) {
            return !loaded;
        }
    }

    
    @Override
    protected Element getStateAndArchive( MyStorageData storageData, Object component,  String componentName) {
        if (storageData.loaded) {
            return null;
        }

        Element element = new Element("component");
        try {
            ModifiableRootModel model = null;
            try {
                model = ((ModuleRootManagerImpl)component).getModifiableModel();
                // IDEA-137969 Eclipse integration: external remove of classpathentry is not synchronized
                model.clear();
                myConverter.readClasspath(model);
                ((RootModelImpl)model).writeExternal(element);
            }
            catch (WriteExternalException e) {
                LOG.error(e);
            }
            finally {
                if (model != null) {
                    model.dispose();
                }
            }

            if (myPathMacroSubstitutor != null) {
                myPathMacroSubstitutor.expandPaths(element);
                myPathMacroSubstitutor.addUnknownMacros("NewModuleRootManager", PathMacrosCollector.getMacroNames(element));
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        storageData.loaded = true;
        return element;
    }

    @Override
    protected MyStorageData loadData() {
        return new MyStorageData();
    }

    @Override
    
    public ExternalizationSession startExternalization() {
        return myConverter.startExternalization();
    }

    @Override
    public void analyzeExternalChangesAndUpdateIfNeed( Collection<VirtualFile> changedFiles,  Set<String> componentNames) {
        // if some file changed, so, changed
        componentNames.add("NewModuleRootManager");
        if (myStorageData != null) {
            myStorageData.loaded = false;
        }
    }

    
    public static ClasspathStorageProvider getProvider( String type) {
        if (type.equals(ClassPathStorageUtil.DEFAULT_STORAGE)) {
            return null;
        }

        for (ClasspathStorageProvider provider : ClasspathStorageProvider.EXTENSION_POINT_NAME.getExtensions()) {
            if (type.equals(provider.getID())) {
                return provider;
            }
        }
        return null;
    }

    
    public static String getModuleDir( Module module) {
        return PathUtil.getParentPath(FileUtilRt.toSystemIndependentName(module.getModuleFilePath()));
    }

    
    public static String getStorageRootFromOptions( Module module) {
        String moduleRoot = getModuleDir(module);
        String storageRef = module.getOptionValue(JpsProjectLoader.CLASSPATH_DIR_ATTRIBUTE);
        if (storageRef == null) {
            return moduleRoot;
        }

        storageRef = FileUtil.toSystemIndependentName(storageRef);
        if (SystemInfo.isWindows ? FileUtil.isAbsolutePlatformIndependent(storageRef) : FileUtil.isUnixAbsolutePath(storageRef)) {
            return storageRef;
        }
        else {
            return moduleRoot + '/' + storageRef;
        }
    }

    public static void setStorageType( ModuleRootModel model,  String storageId) {
        Module module = model.getModule();
        String oldStorageType = ClassPathStorageUtil.getStorageType(module);
        if (oldStorageType.equals(storageId)) {
            return;
        }

        ClasspathStorageProvider provider = getProvider(oldStorageType);
        if (provider != null) {
            provider.detach(module);
        }

        provider = getProvider(storageId);
        if (provider == null) {
            module.clearOption(JpsProjectLoader.CLASSPATH_ATTRIBUTE);
            module.clearOption(JpsProjectLoader.CLASSPATH_DIR_ATTRIBUTE);
        }
        else {
            module.setOption(JpsProjectLoader.CLASSPATH_ATTRIBUTE, storageId);
            module.setOption(JpsProjectLoader.CLASSPATH_DIR_ATTRIBUTE, provider.getContentRoot(model));
        }
    }

    public static void moduleRenamed( Module module,  String newName) {
        ClasspathStorageProvider provider = getProvider(ClassPathStorageUtil.getStorageType(module));
        if (provider != null) {
            provider.moduleRenamed(module, newName);
        }
    }

    public static void modulePathChanged(Module module, String path) {
        ClasspathStorageProvider provider = getProvider(ClassPathStorageUtil.getStorageType(module));
        if (provider != null) {
            provider.modulePathChanged(module, path);
        }
    }
}
