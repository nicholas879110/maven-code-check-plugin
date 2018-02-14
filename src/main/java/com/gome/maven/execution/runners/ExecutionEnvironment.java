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
package com.gome.maven.execution.runners;

import com.gome.maven.execution.*;
import com.gome.maven.execution.configurations.ConfigurationPerRunnerSettings;
import com.gome.maven.execution.configurations.RunProfile;
import com.gome.maven.execution.configurations.RunProfileState;
import com.gome.maven.execution.configurations.RunnerSettings;
import com.gome.maven.execution.ui.RunContentDescriptor;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.UserDataHolderBase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.gome.maven.openapi.actionSystem.LangDataKeys.*;

public class ExecutionEnvironment extends UserDataHolderBase implements Disposable {
    private static final AtomicLong myIdHolder = new AtomicLong(1L);

     private final Project myProject;

     private RunProfile myRunProfile;
     private final Executor myExecutor;
     private ExecutionTarget myTarget;

     private RunnerSettings myRunnerSettings;
     private ConfigurationPerRunnerSettings myConfigurationSettings;
     private final RunnerAndConfigurationSettings myRunnerAndConfigurationSettings;
     private RunContentDescriptor myContentToReuse;
    private final ProgramRunner<?> myRunner;
    private long myExecutionId = 0;
     private DataContext myDataContext;

    
    public ExecutionEnvironment() {
        myProject = null;
        myContentToReuse = null;
        myRunnerAndConfigurationSettings = null;
        myExecutor = null;
        myRunner = null;
    }

    public ExecutionEnvironment( Executor executor,
                                 ProgramRunner runner,
                                 RunnerAndConfigurationSettings configuration,
                                 Project project) {
        this(configuration.getConfiguration(),
                executor,
                DefaultExecutionTarget.INSTANCE,
                project,
                configuration.getRunnerSettings(runner),
                configuration.getConfigurationSettings(runner),
                null,
                null,
                runner);
    }

    /**
     * @deprecated, use {@link com.gome.maven.execution.runners.ExecutionEnvironmentBuilder} instead
     * to remove in IDEA 14
     */
    
    public ExecutionEnvironment( Executor executor,
                                 final ProgramRunner runner,
                                 final ExecutionTarget target,
                                 final RunnerAndConfigurationSettings configuration,
                                 Project project) {
        this(configuration.getConfiguration(),
                executor,
                target,
                project,
                configuration.getRunnerSettings(runner),
                configuration.getConfigurationSettings(runner),
                null,
                configuration,
                runner);
    }

    /**
     * @deprecated, use {@link com.gome.maven.execution.runners.ExecutionEnvironmentBuilder} instead
     * to remove in IDEA 15
     */
    public ExecutionEnvironment( RunProfile runProfile,
                                 Executor executor,
                                 Project project,
                                 RunnerSettings runnerSettings) {
        //noinspection ConstantConditions
        this(runProfile, executor, DefaultExecutionTarget.INSTANCE, project, runnerSettings, null, null, null, RunnerRegistry.getInstance().getRunner(executor.getId(), runProfile));
    }

    ExecutionEnvironment( RunProfile runProfile,
                          Executor executor,
                          ExecutionTarget target,
                          Project project,
                          RunnerSettings runnerSettings,
                          ConfigurationPerRunnerSettings configurationSettings,
                          RunContentDescriptor contentToReuse,
                          RunnerAndConfigurationSettings settings,
                          ProgramRunner<?> runner) {
        myExecutor = executor;
        myTarget = target;
        myRunProfile = runProfile;
        myRunnerSettings = runnerSettings;
        myConfigurationSettings = configurationSettings;
        myProject = project;
        setContentToReuse(contentToReuse);
        myRunnerAndConfigurationSettings = settings;

        myRunner = runner;
    }

    @Override
    public void dispose() {
        myContentToReuse = null;
    }

    
    public Project getProject() {
        return myProject;
    }

    
    public ExecutionTarget getExecutionTarget() {
        return myTarget;
    }

    
    public RunProfile getRunProfile() {
        return myRunProfile;
    }

    
    public RunnerAndConfigurationSettings getRunnerAndConfigurationSettings() {
        return myRunnerAndConfigurationSettings;
    }

    
    public RunContentDescriptor getContentToReuse() {
        return myContentToReuse;
    }

    public void setContentToReuse( RunContentDescriptor contentToReuse) {
        myContentToReuse = contentToReuse;

        if (contentToReuse != null) {
            Disposer.register(contentToReuse, this);
        }
    }

    
    @Deprecated
    /**
     * Use {@link #getRunner()} instead
     * to remove in IDEA 15
     */
    public String getRunnerId() {
        return myRunner.getRunnerId();
    }

    
    public ProgramRunner<?> getRunner() {
        return myRunner;
    }

    
    public RunnerSettings getRunnerSettings() {
        return myRunnerSettings;
    }

    
    public ConfigurationPerRunnerSettings getConfigurationSettings() {
        return myConfigurationSettings;
    }

    
    public RunProfileState getState() throws ExecutionException {
        return myRunProfile.getState(myExecutor, this);
    }

    public long assignNewExecutionId() {
        myExecutionId = myIdHolder.incrementAndGet();
        return myExecutionId;
    }

    public void setExecutionId(long executionId) {
        myExecutionId = executionId;
    }

    public long getExecutionId() {
        return myExecutionId;
    }

    
    public Executor getExecutor() {
        return myExecutor;
    }

    @Override
    public String toString() {
        if (myRunnerAndConfigurationSettings != null) {
            return myRunnerAndConfigurationSettings.getName();
        }
        else if (myRunProfile != null) {
            return myRunProfile.getName();
        }
        else if (myContentToReuse != null) {
            return myContentToReuse.getDisplayName();
        }
        return super.toString();
    }

    void setDataContext( DataContext dataContext) {
        myDataContext = CachingDataContext.cacheIfNeed(dataContext);
    }

    
    public DataContext getDataContext() {
        return myDataContext;
    }

    private static class CachingDataContext implements DataContext {
        private static final DataKey[] keys = {PROJECT, PROJECT_FILE_DIRECTORY, EDITOR, VIRTUAL_FILE, MODULE, PSI_FILE};
        private final Map<String, Object> values = new HashMap<String, Object>();

        
        static CachingDataContext cacheIfNeed( DataContext context) {
            if (context instanceof CachingDataContext)
                return (CachingDataContext)context;
            return new CachingDataContext(context);
        }

        private CachingDataContext(DataContext context) {
            for (DataKey key : keys) {
                values.put(key.getName(), key.getData(context));
            }
        }

        @Override
        public Object getData( String dataId) {
            return values.get(dataId);
        }
    }
}
