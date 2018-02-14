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
package com.gome.maven.execution;

import com.gome.maven.execution.configurations.ConfigurationFactory;
import com.gome.maven.execution.configurations.ConfigurationType;
import com.gome.maven.execution.configurations.RunConfiguration;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class RunManagerEx extends RunManager {
    public static RunManagerEx getInstanceEx(final Project project) {
        return (RunManagerEx)project.getComponent(RunManager.class);
    }

    //public abstract boolean isTemporary( RunnerAndConfigurationSettings configuration);

    /**
     * @deprecated use {@link #setSelectedConfiguration(RunnerAndConfigurationSettings)} instead
     */
    @Deprecated
    public void setActiveConfiguration( RunnerAndConfigurationSettings configuration) {
        setSelectedConfiguration(configuration);
    }

    public abstract void setTemporaryConfiguration( RunnerAndConfigurationSettings tempConfiguration);

    public abstract RunManagerConfig getConfig();

    /**
     * @deprecated use {@link RunManager#createRunConfiguration(String, com.gome.maven.execution.configurations.ConfigurationFactory)} instead
     * @param name
     * @param type
     * @return
     */
    
    public abstract RunnerAndConfigurationSettings createConfiguration(String name, ConfigurationFactory type);

    public abstract void addConfiguration(RunnerAndConfigurationSettings settings,
                                          boolean isShared,
                                          List<BeforeRunTask> tasks,
                                          boolean addTemplateTasksIfAbsent);

    public abstract boolean isConfigurationShared(RunnerAndConfigurationSettings settings);

    
    public abstract List<BeforeRunTask> getBeforeRunTasks(RunConfiguration settings);

    public abstract void setBeforeRunTasks(RunConfiguration runConfiguration, List<BeforeRunTask> tasks, boolean addEnabledTemplateTasksIfAbsent);

    
    public abstract <T extends BeforeRunTask> List<T> getBeforeRunTasks(RunConfiguration settings, Key<T> taskProviderID);

    
    public abstract <T extends BeforeRunTask> List<T> getBeforeRunTasks(Key<T> taskProviderID);

    public abstract RunnerAndConfigurationSettings findConfigurationByName( final String name);

    public abstract Icon getConfigurationIcon( RunnerAndConfigurationSettings settings);

    
    public abstract Collection<RunnerAndConfigurationSettings> getSortedConfigurations();

    public abstract void removeConfiguration( RunnerAndConfigurationSettings settings);

    public abstract void addRunManagerListener(RunManagerListener listener);
    public abstract void removeRunManagerListener(RunManagerListener listener);

    
    public abstract Map<String, List<RunnerAndConfigurationSettings>> getStructure( ConfigurationType type);

    public static void disableTasks(Project project, RunConfiguration settings, Key<? extends BeforeRunTask>... keys) {
        for (Key<? extends BeforeRunTask> key : keys) {
            List<? extends BeforeRunTask> tasks = getInstanceEx(project).getBeforeRunTasks(settings, key);
            for (BeforeRunTask task : tasks) {
                task.setEnabled(false);
            }
        }
    }

    public static int getTasksCount(Project project, RunConfiguration settings, Key<? extends BeforeRunTask>... keys) {
        int result = 0;
        for (Key<? extends BeforeRunTask> key : keys) {
            result += getInstanceEx(project).getBeforeRunTasks(settings, key).size();
        }
        return result;
    }
}