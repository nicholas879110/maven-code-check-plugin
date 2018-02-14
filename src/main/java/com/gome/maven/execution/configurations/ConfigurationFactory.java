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
package com.gome.maven.execution.configurations;

import com.gome.maven.execution.BeforeRunTask;
import com.gome.maven.execution.RunManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.util.IconUtil;

import javax.swing.*;

/**
 * Factory for run configuration instances.
 *
 * @see com.gome.maven.execution.configurations.ConfigurationType#getConfigurationFactories()
 * @author dyoma
 */
public abstract class ConfigurationFactory {
    public static final Icon ADD_ICON = IconUtil.getAddIcon();

    private final ConfigurationType myType;

    protected ConfigurationFactory( final ConfigurationType type) {
        myType = type;
    }

    /**
     * Creates a new run configuration with the specified name by cloning the specified template.
     *
     * @param name the name for the new run configuration.
     * @param template the template from which the run configuration is copied
     * @return the new run configuration.
     */
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        RunConfiguration newConfiguration = template.clone();
        newConfiguration.setName(name);
        return newConfiguration;
    }

    /**
     * Override this method and return {@code false} to hide the configuration from 'New' popup in 'Edit Configurations' dialog. It will be
     * still possible to create this configuration by clicking on '42 more items' in the 'New' popup.
     *
     * @return {@code true} if it makes sense to create configurations of this type in {@code project}
     */
    public boolean isApplicable( Project project) {
        return true;
    }

    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    public abstract RunConfiguration createTemplateConfiguration(Project project);

    public RunConfiguration createTemplateConfiguration(Project project, RunManager runManager) {
        return createTemplateConfiguration(project);
    }

    /**
     * Returns the name of the run configuration variant created by this factory.
     *
     * @return the name of the run configuration variant created by this factory
     */
    public String getName() {
        return myType.getDisplayName();
    }

    public Icon getAddIcon() {
        return ADD_ICON;
    }

    public Icon getIcon( final RunConfiguration configuration) {
        return getIcon();
    }

    public Icon getIcon() {
        return myType.getIcon();
    }

    
    public ConfigurationType getType() {
        return myType;
    }

    /**
     * In this method you can configure defaults for the task, which are preferable to be used for your particular configuration type
     * @param providerID
     * @param task
     */
    public void configureBeforeRunTaskDefaults(Key<? extends BeforeRunTask> providerID, BeforeRunTask task) {
    }

    public boolean isConfigurationSingletonByDefault() {
        return false;
    }

    public boolean canConfigurationBeSingleton() {
        return true; // Configuration may be marked as singleton by default
    }
}
