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

/*
 * User: anna
 * Date: 10-Jul-2007
 */
package com.gome.maven.ide.util.projectWizard;

import com.gome.maven.openapi.module.ModifiableModuleModel;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.projectRoots.SdkTypeId;
import com.gome.maven.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.gome.maven.openapi.roots.ui.configuration.ModulesProvider;

import java.util.List;

public abstract class ProjectBuilder {

    public boolean isUpdate() {
        return false;
    }

    
    public abstract List<Module> commit(final  Project project,  final ModifiableModuleModel model, final ModulesProvider modulesProvider);

    public List<Module> commit( Project project) {
        return commit(project, null, DefaultModulesProvider.createForProject(project));
    }

    public boolean validate(Project current, Project dest) {
        return true;
    }

    public void cleanup() {}

    public boolean isOpenProjectSettingsAfter() {
        return false;
    }

    /**
     * Deprecated. Use {@link #isSuitableSdkType(SdkTypeId)} instead.
     *
     * Used for automatically assigning an SDK to the project when it gets created.
     * If no SDK is specified in the template project and there is no specific SDK chooser step,
     * the SDK which is set for the project is the highest version SDK for which
     * <code>isSuitableSdk</code> returns true.
     *
     * @param sdk the candidate SDK
     * @return true if the SDK can be used for this project type, false otherwise
     */
    @Deprecated
    public boolean isSuitableSdk(Sdk sdk) {
        return isSuitableSdkType(sdk.getSdkType());
    }

    public boolean isSuitableSdkType(SdkTypeId sdkType) {
        return true;
    }

    
    public Project createProject(String name, String path) {
        return ProjectManager.getInstance().createProject(name, path);
    }
}