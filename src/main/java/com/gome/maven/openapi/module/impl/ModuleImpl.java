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
package com.gome.maven.openapi.module.impl;

import com.gome.maven.ide.highlighter.ModuleFileType;
import com.gome.maven.ide.plugins.IdeaPluginDescriptor;
import com.gome.maven.ide.plugins.PluginManagerCore;
import com.gome.maven.openapi.application.impl.ApplicationInfoImpl;
import com.gome.maven.openapi.components.ExtensionAreas;
import com.gome.maven.openapi.components.impl.ModulePathMacroManager;
import com.gome.maven.openapi.components.impl.PlatformComponentManagerImpl;
import com.gome.maven.openapi.components.impl.stores.IComponentStore;
import com.gome.maven.openapi.components.impl.stores.ModuleStoreImpl;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.AreaInstance;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.ModifiableModuleModel;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleComponent;
import com.gome.maven.openapi.module.impl.scopes.ModuleScopeProviderImpl;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.impl.storage.ClasspathStorage;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.util.PathUtil;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author max
 */
public class ModuleImpl extends PlatformComponentManagerImpl implements ModuleEx {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.module.impl.ModuleImpl");

     private final Project myProject;
    private boolean isModuleAdded;

     private static final String OPTION_WORKSPACE = "workspace";

    public static final Object MODULE_RENAMING_REQUESTOR = new Object();

    private String myName;

    private String myModuleType;

    private final ModuleScopeProvider myModuleScopeProvider;

    public ModuleImpl( String filePath,  Project project) {
        super(project, "Module " + moduleNameByFileName(PathUtil.getFileName(filePath)));

        getPicoContainer().registerComponentInstance(Module.class, this);

        myProject = project;
        myModuleScopeProvider = new ModuleScopeProviderImpl(this);

        init(filePath);
    }

    @Override
    protected void bootstrapPicoContainer( String name) {
        Extensions.instantiateArea(ExtensionAreas.IDEA_MODULE, this, (AreaInstance)getParentComponentManager());
        super.bootstrapPicoContainer(name);
        getPicoContainer().registerComponentImplementation(IComponentStore.class, ModuleStoreImpl.class);
        getPicoContainer().registerComponentImplementation(ModulePathMacroManager.class);
    }

    
    public ModuleStoreImpl getStateStore() {
        return (ModuleStoreImpl)getPicoContainer().getComponentInstance(IComponentStore.class);
    }

    @Override
    public void initializeComponent( Object component, boolean service) {
        getStateStore().initComponent(component, service);
    }

    private void init(String filePath) {
        getStateStore().setModuleFilePath(filePath);
        myName = moduleNameByFileName(PathUtil.getFileName(filePath));

        MyVirtualFileListener myVirtualFileListener = new MyVirtualFileListener();
        VirtualFileManager.getInstance().addVirtualFileListener(myVirtualFileListener, this);
    }

    @Override
    public void loadModuleComponents() {
        final IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
        for (IdeaPluginDescriptor plugin : plugins) {
            if (PluginManagerCore.shouldSkipPlugin(plugin)) continue;
            loadComponentsConfiguration(plugin.getModuleComponents(), plugin, false);
        }
    }

    @Override
    protected boolean isComponentSuitable(Map<String, String> options) {
        if (!super.isComponentSuitable(options)) return false;
        if (options == null) return true;

        Set<String> optionNames = options.keySet();
        for (String optionName : optionNames) {
            if (Comparing.equal(OPTION_WORKSPACE, optionName)) continue;
            if (!parseOptionValue(options.get(optionName)).contains(getOptionValue(optionName))) return false;
        }

        return true;
    }

    private static List<String> parseOptionValue(String optionValue) {
        if (optionValue == null) return new ArrayList<String>(0);
        return Arrays.asList(optionValue.split(";"));
    }

    @Override
    
    public VirtualFile getModuleFile() {
        return getStateStore().getModuleFile();
    }

    @Override
    public void rename(String newName) {
        myName = newName;
        final VirtualFile file = getStateStore().getModuleFile();
        try {
            if (file != null) {
                ClasspathStorage.moduleRenamed(this, newName);
                file.rename(MODULE_RENAMING_REQUESTOR, newName + ModuleFileType.DOT_DEFAULT_EXTENSION);
                getStateStore().setModuleFilePath(VfsUtilCore.virtualToIoFile(file).getCanonicalPath());
                return;
            }

            // [dsl] we get here if either old file didn't exist or renaming failed
            final File oldFile = new File(getModuleFilePath());
            final File newFile = new File(oldFile.getParentFile(), newName + ModuleFileType.DOT_DEFAULT_EXTENSION);
            getStateStore().setModuleFilePath(newFile.getCanonicalPath());
        }
        catch (IOException e) {
            LOG.debug(e);
        }
    }

    @Override
    
    public String getModuleFilePath() {
        return getStateStore().getModuleFilePath();
    }

    @Override
    public synchronized void dispose() {
        isModuleAdded = false;
        disposeComponents();
        Extensions.disposeArea(this);
        super.dispose();
    }


    @Override
    public void projectOpened() {
        for (ModuleComponent component : getComponents(ModuleComponent.class)) {
            try {
                component.projectOpened();
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    @Override
    public void projectClosed() {
        List<ModuleComponent> components = new ArrayList<ModuleComponent>(Arrays.asList(getComponents(ModuleComponent.class)));
        Collections.reverse(components);

        for (ModuleComponent component : components) {
            try {
                component.projectClosed();
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    @Override
    
    public Project getProject() {
        return myProject;
    }

    @Override
    
    public String getName() {
        return myName;
    }

    @Override
    public boolean isLoaded() {
        return isModuleAdded;
    }

    @Override
    public void moduleAdded() {
        isModuleAdded = true;
        for (ModuleComponent component : getComponents(ModuleComponent.class)) {
            component.moduleAdded();
        }
    }

    @Override
    public void setOption( String optionName,  String optionValue) {
        if (ELEMENT_TYPE.equals(optionName)) {
            myModuleType = optionValue;
        }
        getStateStore().setOption(optionName, optionValue);
    }

    @Override
    public void clearOption( String optionName) {
        if (ELEMENT_TYPE.equals(optionName)) {
            myModuleType = null;
        }
        getStateStore().clearOption(optionName);
    }

    @Override
    public String getOptionValue( String optionName) {
        if (ELEMENT_TYPE.equals(optionName)) {
            if (myModuleType == null) {
                myModuleType = getStateStore().getOptionValue(optionName);
            }
            return myModuleType;
        }
        return getStateStore().getOptionValue(optionName);
    }

    
    @Override
    public GlobalSearchScope getModuleScope() {
        return myModuleScopeProvider.getModuleScope();
    }

    
    @Override
    public GlobalSearchScope getModuleScope(boolean includeTests) {
        return myModuleScopeProvider.getModuleScope(includeTests);
    }

    
    @Override
    public GlobalSearchScope getModuleWithLibrariesScope() {
        return myModuleScopeProvider.getModuleWithLibrariesScope();
    }

    
    @Override
    public GlobalSearchScope getModuleWithDependenciesScope() {
        return myModuleScopeProvider.getModuleWithDependenciesScope();
    }

    
    @Override
    public GlobalSearchScope getModuleContentScope() {
        return myModuleScopeProvider.getModuleContentScope();
    }

    
    @Override
    public GlobalSearchScope getModuleContentWithDependenciesScope() {
        return myModuleScopeProvider.getModuleContentWithDependenciesScope();
    }

    
    @Override
    public GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests) {
        return myModuleScopeProvider.getModuleWithDependenciesAndLibrariesScope(includeTests);
    }

    
    @Override
    public GlobalSearchScope getModuleWithDependentsScope() {
        return myModuleScopeProvider.getModuleWithDependentsScope();
    }

    
    @Override
    public GlobalSearchScope getModuleTestsWithDependentsScope() {
        return myModuleScopeProvider.getModuleTestsWithDependentsScope();
    }

    
    @Override
    public GlobalSearchScope getModuleRuntimeScope(boolean includeTests) {
        return myModuleScopeProvider.getModuleRuntimeScope(includeTests);
    }

    @Override
    public void clearScopesCache() {
        myModuleScopeProvider.clearCache();
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
        if (myName == null) return "Module (not initialized)";
        return "Module: '" + getName() + "'";
    }

    private static String moduleNameByFileName( String fileName) {
        return StringUtil.trimEnd(fileName, ModuleFileType.DOT_DEFAULT_EXTENSION);
    }

    
    @Override
    public <T> T[] getExtensions( final ExtensionPointName<T> extensionPointName) {
        return Extensions.getArea(this).getExtensionPoint(extensionPointName).getExtensions();
    }

    @Override
    protected boolean logSlowComponents() {
        return super.logSlowComponents() || ApplicationInfoImpl.getShadowInstance().isEAP();
    }

    private class MyVirtualFileListener extends VirtualFileAdapter {
        @Override
        public void propertyChanged( VirtualFilePropertyEvent event) {
            if (!isModuleAdded) return;
            final Object requestor = event.getRequestor();
            if (MODULE_RENAMING_REQUESTOR.equals(requestor)) return;
            if (!VirtualFile.PROP_NAME.equals(event.getPropertyName())) return;

            final VirtualFile parent = event.getParent();
            if (parent != null) {
                final String parentPath = parent.getPath();
                final String ancestorPath = parentPath + "/" + event.getOldValue();
                final String moduleFilePath = getModuleFilePath();
                if (VfsUtilCore.isAncestor(new File(ancestorPath), new File(moduleFilePath), true)) {
                    final String newValue = (String)event.getNewValue();
                    final String relativePath = FileUtil.getRelativePath(ancestorPath, moduleFilePath, '/');
                    final String newFilePath = parentPath + "/" + newValue + "/" + relativePath;
                    setModuleFilePath(moduleFilePath, newFilePath);
                }
            }

            final VirtualFile moduleFile = getModuleFile();
            if (moduleFile == null) return;
            if (moduleFile.equals(event.getFile())) {
                String oldName = myName;
                myName = moduleNameByFileName(moduleFile.getName());
                ModuleManagerImpl.getInstanceImpl(getProject()).fireModuleRenamedByVfsEvent(ModuleImpl.this, oldName);
            }
        }

        private void setModuleFilePath(String moduleFilePath, String newFilePath) {
            ClasspathStorage.modulePathChanged(ModuleImpl.this, newFilePath);

            final ModifiableModuleModel modifiableModel = ModuleManagerImpl.getInstanceImpl(getProject()).getModifiableModel();
            modifiableModel.setModuleFilePath(ModuleImpl.this, moduleFilePath, newFilePath);
            modifiableModel.commit();

            getStateStore().setModuleFilePath(newFilePath);
        }

        @Override
        public void fileMoved( VirtualFileMoveEvent event) {
            final VirtualFile oldParent = event.getOldParent();
            final VirtualFile newParent = event.getNewParent();
            final String dirName = event.getFileName();
            final String ancestorPath = oldParent.getPath() + "/" + dirName;
            final String moduleFilePath = getModuleFilePath();
            if (VfsUtilCore.isAncestor(new File(ancestorPath), new File(moduleFilePath), true)) {
                final String relativePath = FileUtil.getRelativePath(ancestorPath, moduleFilePath, '/');
                setModuleFilePath(moduleFilePath, newParent.getPath() + "/" + dirName + "/" + relativePath);
            }
        }
    }

    
    @Override
    protected MutablePicoContainer createPicoContainer() {
        return Extensions.getArea(this).getPicoContainer();
    }
}
