/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.facet.ui;

import com.gome.maven.facet.Facet;
import com.gome.maven.ide.util.projectWizard.ModuleBuilder;
import com.gome.maven.ide.util.projectWizard.WizardContext;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModifiableRootModel;
import com.gome.maven.openapi.roots.ModuleRootModel;
import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.roots.ui.configuration.FacetsProvider;
import com.gome.maven.openapi.roots.ui.configuration.ModulesProvider;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * @author nik
 */
public interface FacetEditorContext extends UserDataHolder {

    
    Project getProject();

    
    Library findLibrary( String name);

    @Deprecated
    
    ModuleBuilder getModuleBuilder();

    boolean isNewFacet();

    
    Facet getFacet();

    
    Module getModule();

    
    Facet getParentFacet();

    
    FacetsProvider getFacetsProvider();

    
    ModulesProvider getModulesProvider();

    
    ModifiableRootModel getModifiableRootModel();

    
    ModuleRootModel getRootModel();

    Library[] getLibraries();

    @Deprecated
    
    WizardContext getWizardContext();

    Library createProjectLibrary( String name, final VirtualFile[] roots, final VirtualFile[] sources);

    VirtualFile[] getLibraryFiles(Library library, OrderRootType rootType);

    
    String getFacetName();
}
