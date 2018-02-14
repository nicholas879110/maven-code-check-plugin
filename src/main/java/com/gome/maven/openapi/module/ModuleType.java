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
package com.gome.maven.openapi.module;

import com.gome.maven.ide.util.frameworkSupport.FrameworkRole;
import com.gome.maven.ide.util.projectWizard.ModuleBuilder;
import com.gome.maven.ide.util.projectWizard.ModuleWizardStep;
import com.gome.maven.ide.util.projectWizard.SettingsStep;
import com.gome.maven.ide.util.projectWizard.WizardContext;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;

public abstract class ModuleType<T extends ModuleBuilder> {
    public static final ModuleType EMPTY;

    
    private final String myId;
    private final FrameworkRole myFrameworkRole;

    protected ModuleType(  String id) {
        myId = id;
        myFrameworkRole = new FrameworkRole(id);
    }

    
    public abstract T createModuleBuilder();

    
    public abstract String getName();
    
    public abstract String getDescription();
    public abstract Icon getBigIcon();

    public Icon getIcon() {
        return getNodeIcon(false);
    }

    public abstract Icon getNodeIcon(@Deprecated boolean isOpened);

    
    public ModuleWizardStep[] createWizardSteps( WizardContext wizardContext,  T moduleBuilder,  ModulesProvider modulesProvider) {
        return ModuleWizardStep.EMPTY_ARRAY;
    }

    
    public ModuleWizardStep modifySettingsStep( SettingsStep settingsStep,  ModuleBuilder moduleBuilder) {
        return null;
    }

    
    public ModuleWizardStep modifyProjectTypeStep( SettingsStep settingsStep,  ModuleBuilder moduleBuilder) {
        return null;
    }

    
    public final String getId() {
        return myId;
    }

    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleType)) return false;

        final ModuleType moduleType = (ModuleType)o;

        return myId.equals(moduleType.myId);
    }

    public final int hashCode() {
        return myId.hashCode();
    }

    public String toString() {
        return getName();
    }

    static {
        EMPTY = instantiate("com.intellij.openapi.module.EmptyModuleType");
    }

    
    private static ModuleType instantiate(String className) {
        try {
            return (ModuleType)Class.forName(className).newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean isValidSdk( Module module,  final Sdk projectSdk) {
        return true;
    }

    public static ModuleType get( Module module) {
        final ModuleTypeManager instance = ModuleTypeManager.getInstance();
        if (instance == null) {
            return EMPTY;
        }
        return instance.findByID(module.getOptionValue(Module.ELEMENT_TYPE));
    }

    
    public FrameworkRole getDefaultAcceptableRole() {
        return myFrameworkRole;
    }

    public boolean isSupportedRootType(JpsModuleSourceRootType type) {
        return true;
    }
}
