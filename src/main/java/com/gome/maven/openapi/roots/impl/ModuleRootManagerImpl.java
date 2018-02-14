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

package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.roots.ex.ProjectRootManagerEx;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ModuleRootManagerImpl extends ModuleRootManager implements ModuleComponent {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.impl.ModuleRootManagerImpl");

    private final Module myModule;
    private final ProjectRootManagerImpl myProjectRootManager;
    private final VirtualFilePointerManager myFilePointerManager;
    private RootModelImpl myRootModel;
    private boolean myIsDisposed = false;
    private boolean myLoaded = false;
    private boolean isModuleAdded = false;
    private final OrderRootsCache myOrderRootsCache;
    private final Map<RootModelImpl, Throwable> myModelCreations = new THashMap<RootModelImpl, Throwable>();


    public ModuleRootManagerImpl(Module module,
                                 ProjectRootManagerImpl projectRootManager,
                                 VirtualFilePointerManager filePointerManager) {
        myModule = module;
        myProjectRootManager = projectRootManager;
        myFilePointerManager = filePointerManager;

        myRootModel = new RootModelImpl(this, myProjectRootManager, myFilePointerManager);
        myOrderRootsCache = new OrderRootsCache(module);
    }

    @Override
    
    public Module getModule() {
        return myModule;
    }

    @Override
    
    public ModuleFileIndex getFileIndex() {
        return ModuleServiceManager.getService(myModule, ModuleFileIndex.class);
    }

    @Override
    
    public String getComponentName() {
        return "NewModuleRootManager";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
        myRootModel.dispose();
        myIsDisposed = true;

        if (Disposer.isDebugMode()) {
            final Set<Map.Entry<RootModelImpl, Throwable>> entries = myModelCreations.entrySet();
            for (final Map.Entry<RootModelImpl, Throwable> entry : new ArrayList<Map.Entry<RootModelImpl, Throwable>>(entries)) {
                System.err.println("***********************************************************************************************");
                System.err.println("***                        R O O T   M O D E L   N O T   D I S P O S E D                    ***");
                System.err.println("***********************************************************************************************");
                System.err.println("Created at:");
                entry.getValue().printStackTrace(System.err);
                entry.getKey().dispose();
            }
        }
    }


    @Override
    
    public ModifiableRootModel getModifiableModel() {
        return getModifiableModel(new RootConfigurationAccessor());
    }

    
    public ModifiableRootModel getModifiableModel(final RootConfigurationAccessor accessor) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        final RootModelImpl model = new RootModelImpl(myRootModel, this, true, accessor, myFilePointerManager, myProjectRootManager) {
            @Override
            public void dispose() {
                super.dispose();
                if (Disposer.isDebugMode()) {
                    myModelCreations.remove(this);
                }

                for (OrderEntry entry : ModuleRootManagerImpl.this.getOrderEntries()) {
                    assert !((RootModelComponentBase)entry).isDisposed();
                }
            }
        };
        if (Disposer.isDebugMode()) {
            myModelCreations.put(model, new Throwable());
        }
        return model;
    }

    void makeRootsChange( Runnable runnable) {
        ProjectRootManagerEx projectRootManagerEx = (ProjectRootManagerEx)ProjectRootManager.getInstance(myModule.getProject());
        // IMPORTANT: should be the first listener!
        projectRootManagerEx.makeRootsChange(runnable, false, isModuleAdded);
    }

    public RootModelImpl getRootModel() {
        return myRootModel;
    }

    
    @Override
    public ContentEntry[] getContentEntries() {
        return myRootModel.getContentEntries();
    }

    @Override
    
    public OrderEntry[] getOrderEntries() {
        return myRootModel.getOrderEntries();
    }

    @Override
    public Sdk getSdk() {
        return myRootModel.getSdk();
    }

    @Override
    public boolean isSdkInherited() {
        return myRootModel.isSdkInherited();
    }

    void commitModel(RootModelImpl rootModel) {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        LOG.assertTrue(rootModel.myModuleRootManager == this);

        final Project project = myModule.getProject();
        final ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
        ModifiableModelCommitter.multiCommit(new ModifiableRootModel[]{rootModel}, moduleModel);
    }

    static void doCommit(RootModelImpl rootModel) {
        rootModel.docommit();
        rootModel.dispose();
    }


    @Override
    
    public Module[] getDependencies() {
        return myRootModel.getModuleDependencies();
    }

    
    @Override
    public Module[] getDependencies(boolean includeTests) {
        return myRootModel.getModuleDependencies(includeTests);
    }

    
    @Override
    public Module[] getModuleDependencies() {
        return myRootModel.getModuleDependencies();
    }

    
    @Override
    public Module[] getModuleDependencies(boolean includeTests) {
        return myRootModel.getModuleDependencies(includeTests);
    }

    @Override
    public boolean isDependsOn(Module module) {
        return myRootModel.isDependsOn(module);
    }

    @Override
    
    public String[] getDependencyModuleNames() {
        return myRootModel.getDependencyModuleNames();
    }

    @Override
    public <T> T getModuleExtension(final Class<T> klass) {
        return myRootModel.getModuleExtension(klass);
    }

    @Override
    public <R> R processOrder(RootPolicy<R> policy, R initialValue) {
        LOG.assertTrue(!myIsDisposed);
        return myRootModel.processOrder(policy, initialValue);
    }

    
    @Override
    public OrderEnumerator orderEntries() {
        return new ModuleOrderEnumerator(myRootModel, myOrderRootsCache);
    }

    public static OrderRootsEnumerator getCachingEnumeratorForType(OrderRootType type, Module module) {
        return getEnumeratorForType(type, module).usingCache();
    }

    
    private static OrderRootsEnumerator getEnumeratorForType(OrderRootType type, Module module) {
        OrderEnumerator base = OrderEnumerator.orderEntries(module);
        if (type == OrderRootType.CLASSES) {
            return base.exportedOnly().withoutModuleSourceEntries().recursively().classes();
        }
        if (type == OrderRootType.SOURCES) {
            return base.exportedOnly().recursively().sources();
        }
        return base.roots(type);
    }

    @Override
    
    public VirtualFile[] getContentRoots() {
        LOG.assertTrue(!myIsDisposed);
        return myRootModel.getContentRoots();
    }

    @Override
    
    public String[] getContentRootUrls() {
        LOG.assertTrue(!myIsDisposed);
        return myRootModel.getContentRootUrls();
    }

    @Override
    
    public String[] getExcludeRootUrls() {
        LOG.assertTrue(!myIsDisposed);
        return myRootModel.getExcludeRootUrls();
    }

    @Override
    
    public VirtualFile[] getExcludeRoots() {
        LOG.assertTrue(!myIsDisposed);
        return myRootModel.getExcludeRoots();
    }

    @Override
    
    public String[] getSourceRootUrls() {
        return getSourceRootUrls(true);
    }

    
    @Override
    public String[] getSourceRootUrls(boolean includingTests) {
        LOG.assertTrue(!myIsDisposed);
        return myRootModel.getSourceRootUrls(includingTests);
    }

    @Override
    
    public VirtualFile[] getSourceRoots() {
        return getSourceRoots(true);
    }

    @Override
    
    public VirtualFile[] getSourceRoots(final boolean includingTests) {
        LOG.assertTrue(!myIsDisposed);
        return myRootModel.getSourceRoots(includingTests);
    }

    
    @Override
    public List<VirtualFile> getSourceRoots( JpsModuleSourceRootType<?> rootType) {
        return myRootModel.getSourceRoots(rootType);
    }

    
    @Override
    public List<VirtualFile> getSourceRoots( Set<? extends JpsModuleSourceRootType<?>> rootTypes) {
        return myRootModel.getSourceRoots(rootTypes);
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void moduleAdded() {
        isModuleAdded = true;
    }


    public void dropCaches() {
        myOrderRootsCache.clearCache();
    }

    public ModuleRootManagerState getState() {
        return new ModuleRootManagerState(myRootModel);
    }

    public void loadState(ModuleRootManagerState object) {
        loadState(object, myLoaded || isModuleAdded);
        myLoaded = true;
    }

    protected void loadState(ModuleRootManagerState object, boolean throwEvent) {
        try {
            final RootModelImpl newModel = new RootModelImpl(object.getRootModelElement(), this, myProjectRootManager, myFilePointerManager, throwEvent);

            if (throwEvent) {
                makeRootsChange(new Runnable() {
                    @Override
                    public void run() {
                        doCommit(newModel);
                    }
                });
            }
            else {
                myRootModel.dispose();
                myRootModel = newModel;
            }

            assert !myRootModel.isOrderEntryDisposed();
        }
        catch (InvalidDataException e) {
            LOG.error(e);
        }
    }

    public static class ModuleRootManagerState implements JDOMExternalizable {
        private RootModelImpl myRootModel;
        private Element myRootModelElement;

        public ModuleRootManagerState() {
        }

        public ModuleRootManagerState(RootModelImpl rootModel) {
            myRootModel = rootModel;
        }

        @Override
        public void readExternal(Element element) {
            myRootModelElement = element;
        }

        @Override
        public void writeExternal(Element element) throws WriteExternalException {
            myRootModel.writeExternal(element);
        }

        public Element getRootModelElement() {
            return myRootModelElement;
        }
    }
}
