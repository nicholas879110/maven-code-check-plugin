/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.ide.util.projectWizard;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.roots.ui.configuration.ModulesProvider;

import javax.swing.*;

/**
 * @author yole
 */
public abstract class AbstractModuleBuilder extends ProjectBuilder {
    public abstract Icon getNodeIcon();

    
    public abstract String getBuilderId();

    public abstract ModuleWizardStep[] createWizardSteps( WizardContext wizardContext,  ModulesProvider modulesProvider);

    
    public ModuleWizardStep modifySettingsStep( SettingsStep settingsStep) {
        return null;
    }

    
    public ModuleWizardStep modifyProjectTypeStep( SettingsStep step) { return null; }

    /**
     * Custom UI to be shown on the first wizard page
     */
    
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return null;
    }

    public abstract void setName(String name);

    public abstract void setModuleFilePath( String path);

    public abstract void setContentEntryPath(String moduleRootPath);

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AbstractModuleBuilder && getBuilderId() != null && getBuilderId().equals(((AbstractModuleBuilder)obj).getBuilderId());
    }
}
