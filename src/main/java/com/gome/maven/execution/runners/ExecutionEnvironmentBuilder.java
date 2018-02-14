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
import com.gome.maven.execution.configurations.RunConfiguration;
import com.gome.maven.execution.configurations.RunProfile;
import com.gome.maven.execution.configurations.RunnerSettings;
import com.gome.maven.execution.ui.RunContentDescriptor;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.project.Project;

/**
 * User: Vassiliy.Kudryashov
 */
public final class ExecutionEnvironmentBuilder {
     private RunProfile myRunProfile;
     private ExecutionTarget myTarget = DefaultExecutionTarget.INSTANCE;

     private final Project myProject;

     private RunnerSettings myRunnerSettings;
     private ConfigurationPerRunnerSettings myConfigurationSettings;
     private RunContentDescriptor myContentToReuse;
     private RunnerAndConfigurationSettings myRunnerAndConfigurationSettings;
     private String myRunnerId;
    private ProgramRunner<?> myRunner;
    private boolean myAssignNewId;
     private Executor myExecutor;
     private DataContext myDataContext;

    public ExecutionEnvironmentBuilder( Project project,  Executor executor) {
        myProject = project;
        myExecutor = executor;
    }

    
    public static ExecutionEnvironmentBuilder create( Project project,  Executor executor,  RunProfile runProfile) throws ExecutionException {
        ExecutionEnvironmentBuilder builder = createOrNull(project, executor, runProfile);
        if (builder == null) {
            throw new ExecutionException("Cannot find runner for " + runProfile.getName());
        }
        return builder;
    }

    
    public static ExecutionEnvironmentBuilder createOrNull( Project project,  Executor executor,  RunProfile runProfile) {
        ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), runProfile);
        if (runner == null) {
            return null;
        }
        return new ExecutionEnvironmentBuilder(project, executor).runner(runner).runProfile(runProfile);
    }

    
    public static ExecutionEnvironmentBuilder createOrNull( Executor executor,  RunnerAndConfigurationSettings settings) {
        ExecutionEnvironmentBuilder builder = createOrNull(settings.getConfiguration().getProject(), executor, settings.getConfiguration());
        return builder == null ? null : builder.runnerAndSettings(builder.myRunner, settings);
    }

    
    public static ExecutionEnvironmentBuilder create( Executor executor,  RunnerAndConfigurationSettings settings) throws ExecutionException {
        ExecutionEnvironmentBuilder builder = create(settings.getConfiguration().getProject(), executor, settings.getConfiguration());
        return builder.runnerAndSettings(builder.myRunner, settings);
    }

    
    public static ExecutionEnvironmentBuilder create( Executor executor,  RunConfiguration configuration) {
        return new ExecutionEnvironmentBuilder(configuration.getProject(), executor).runProfile(configuration);
    }

    
    Executor getExecutor() {
        return myExecutor;
    }

    /**
     * Creates an execution environment builder initialized with a copy of the specified environment.
     *
     * @param copySource the environment to copy from.
     */
    public ExecutionEnvironmentBuilder( ExecutionEnvironment copySource) {
        myTarget = copySource.getExecutionTarget();
        myProject = copySource.getProject();
        myRunnerAndConfigurationSettings = copySource.getRunnerAndConfigurationSettings();
        myRunProfile = copySource.getRunProfile();
        myRunnerSettings = copySource.getRunnerSettings();
        myConfigurationSettings = copySource.getConfigurationSettings();
        //noinspection deprecation
        myRunner = copySource.getRunner();
        myContentToReuse = copySource.getContentToReuse();
        myExecutor = copySource.getExecutor();
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setTarget( ExecutionTarget target) {
        return target(target);
    }

    public ExecutionEnvironmentBuilder target( ExecutionTarget target) {
        if (target != null) {
            myTarget = target;
        }
        return this;
    }

    public ExecutionEnvironmentBuilder activeTarget() {
        myTarget = ExecutionTargetManager.getActiveTarget(myProject);
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setRunnerAndSettings( ProgramRunner programRunner,
                                                             RunnerAndConfigurationSettings settings) {
        return runnerAndSettings(programRunner, settings);
    }

    public ExecutionEnvironmentBuilder runnerAndSettings( ProgramRunner runner,
                                                          RunnerAndConfigurationSettings settings) {
        myRunnerAndConfigurationSettings = settings;
        myRunProfile = settings.getConfiguration();
        myRunnerSettings = settings.getRunnerSettings(runner);
        myConfigurationSettings = settings.getConfigurationSettings(runner);
        myRunner = runner;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setRunnerSettings( RunnerSettings runnerSettings) {
        myRunnerSettings = runnerSettings;
        return this;
    }

    public ExecutionEnvironmentBuilder runnerSettings( RunnerSettings runnerSettings) {
        myRunnerSettings = runnerSettings;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setConfigurationSettings( ConfigurationPerRunnerSettings configurationSettings) {
        myConfigurationSettings = configurationSettings;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setContentToReuse( RunContentDescriptor contentToReuse) {
        contentToReuse(contentToReuse);
        return this;
    }

    public ExecutionEnvironmentBuilder contentToReuse( RunContentDescriptor contentToReuse) {
        myContentToReuse = contentToReuse;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setRunProfile( RunProfile runProfile) {
        return runProfile(runProfile);
    }

    public ExecutionEnvironmentBuilder runProfile( RunProfile runProfile) {
        myRunProfile = runProfile;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setRunnerId( String runnerId) {
        myRunnerId = runnerId;
        return this;
    }

    public ExecutionEnvironmentBuilder runner( ProgramRunner<?> runner) {
        myRunner = runner;
        return this;
    }

    public ExecutionEnvironmentBuilder assignNewId() {
        myAssignNewId = true;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setDataContext( DataContext dataContext) {
        return dataContext(dataContext);
    }

    public ExecutionEnvironmentBuilder dataContext( DataContext dataContext) {
        myDataContext = dataContext;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * to remove in IDEA 15
     */
    public ExecutionEnvironmentBuilder setExecutor( Executor executor) {
        return executor(executor);
    }

    public ExecutionEnvironmentBuilder executor( Executor executor) {
        myExecutor = executor;
        return this;
    }

    
    public ExecutionEnvironment build() {
        if (myRunner == null) {
            if (myRunnerId == null) {
                myRunner = RunnerRegistry.getInstance().getRunner(myExecutor.getId(), myRunProfile);
            }
            else {
                myRunner = RunnerRegistry.getInstance().findRunnerById(myRunnerId);
            }
        }

        if (myRunner == null) {
            throw new IllegalStateException("Runner must be specified");
        }

        ExecutionEnvironment environment = new ExecutionEnvironment(myRunProfile, myExecutor, myTarget, myProject, myRunnerSettings, myConfigurationSettings, myContentToReuse,
                myRunnerAndConfigurationSettings, myRunner);
        if (myAssignNewId) {
            environment.assignNewExecutionId();
        }
        if (myDataContext != null) {
            environment.setDataContext(myDataContext);
        }
        return environment;
    }

    public void buildAndExecute() throws ExecutionException {
        ExecutionEnvironment environment = build();
        myRunner.execute(environment);
    }
}
