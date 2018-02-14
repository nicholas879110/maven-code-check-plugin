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
import com.gome.maven.openapi.components.StateStorage;
import com.gome.maven.openapi.components.StateStorage.SaveSession;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.module.impl.ModuleImpl;
import com.gome.maven.openapi.project.impl.ProjectImpl;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.List;
import java.util.Set;

public class ProjectWithModulesStoreImpl extends ProjectStoreImpl {
    public ProjectWithModulesStoreImpl( ProjectImpl project,  PathMacroManager pathMacroManager) {
        super(project, pathMacroManager);
    }

    @Override
    protected boolean reinitComponent( String componentName,  Set<StateStorage> changedStorages) {
        if (super.reinitComponent(componentName, changedStorages)) {
            return true;
        }

        for (Module module : getPersistentModules()) {
            // we have to reinit all modules for component because we don't know affected module
            ((ModuleImpl)module).getStateStore().reinitComponent(componentName, changedStorages);
        }
        return true;
    }

    @Override
    public TrackingPathMacroSubstitutor[] getSubstitutors() {
        List<TrackingPathMacroSubstitutor> result = new SmartList<TrackingPathMacroSubstitutor>();
        ContainerUtil.addIfNotNull(result, getStateStorageManager().getMacroSubstitutor());

        for (Module module : getPersistentModules()) {
            ContainerUtil.addIfNotNull(result, ((ModuleImpl)module).getStateStore().getStateStorageManager().getMacroSubstitutor());
        }

        return result.toArray(new TrackingPathMacroSubstitutor[result.size()]);
    }

    @Override
    public boolean isReloadPossible( Set<String> componentNames) {
        if (!super.isReloadPossible(componentNames)) {
            return false;
        }

        for (Module module : getPersistentModules()) {
            if (!((ModuleImpl)module).getStateStore().isReloadPossible(componentNames)) {
                return false;
            }
        }

        return true;
    }

    
    protected Module[] getPersistentModules() {
        ModuleManager moduleManager = ModuleManager.getInstance(myProject);
        return moduleManager == null ? Module.EMPTY_ARRAY : moduleManager.getModules();
    }

    @Override
    protected void beforeSave( List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
        super.beforeSave(readonlyFiles);

        for (Module module : getPersistentModules()) {
            ((ModuleImpl)module).getStateStore().save(readonlyFiles);
        }
    }
}
