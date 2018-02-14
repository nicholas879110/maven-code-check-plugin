/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.openapi.roots.ui.configuration;

import com.gome.maven.ide.projectView.impl.ModuleGroup;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.LibraryOrderEntry;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.roots.impl.libraries.LibraryEx;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.roots.libraries.PersistentLibraryKind;

/**
 * @author yole
 */
public class ProjectSettingsService {
    public static ProjectSettingsService getInstance(Project project) {
        return ServiceManager.getService(project, ProjectSettingsService.class);
    }

    public void openProjectSettings() {
    }

    public void openGlobalLibraries() {
    }

    public void openLibrary( Library library) {
    }

    public void openModuleSettings(final Module module) {
    }

    public boolean canOpenModuleSettings() {
        return false;
    }

    public void openModuleLibrarySettings(final Module module) {
    }

    public boolean canOpenModuleLibrarySettings() {
        return false;
    }

    public void openContentEntriesSettings(final Module module) {
    }

    public boolean canOpenContentEntriesSettings() {
        return false;
    }

    public void openModuleDependenciesSettings( Module module,  OrderEntry orderEntry) {
    }

    public boolean canOpenModuleDependenciesSettings() {
        return false;
    }

    public void openLibraryOrSdkSettings( final OrderEntry orderEntry) {
        Configurable additionalSettingsConfigurable = getLibrarySettingsConfigurable(orderEntry);
        if (additionalSettingsConfigurable != null) {
            ShowSettingsUtil.getInstance().showSettingsDialog(orderEntry.getOwnerModule().getProject(),
                    additionalSettingsConfigurable.getDisplayName());
        }
    }

    public boolean canOpenLibraryOrSdkSettings(final OrderEntry orderEntry) {
        return getLibrarySettingsConfigurable(orderEntry) != null;
    }

    
    private static Configurable getLibrarySettingsConfigurable(OrderEntry orderEntry) {
        if (!(orderEntry instanceof LibraryOrderEntry)) return null;
        LibraryOrderEntry libOrderEntry = (LibraryOrderEntry)orderEntry;
        Library lib = libOrderEntry.getLibrary();
        if (lib instanceof LibraryEx) {
            Project project = libOrderEntry.getOwnerModule().getProject();
            PersistentLibraryKind<?> libKind = ((LibraryEx)lib).getKind();
            if (libKind != null) {
                return LibrarySettingsProvider.getAdditionalSettingsConfigurable(project, libKind);
            }
        }
        return null;
    }

    public boolean processModulesMoved(final Module[] modules,  final ModuleGroup targetGroup) {
        return false;
    }

    public void showModuleConfigurationDialog( String moduleToSelect,  String editorNameToSelect) {
    }

    public Sdk chooseAndSetSdk() {
        return null;
    }
}
