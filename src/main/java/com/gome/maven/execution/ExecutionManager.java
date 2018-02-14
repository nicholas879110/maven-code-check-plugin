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

import com.gome.maven.execution.configurations.RunProfileState;
import com.gome.maven.execution.process.ProcessHandler;
import com.gome.maven.execution.runners.ExecutionEnvironment;
import com.gome.maven.execution.runners.ProgramRunner;
import com.gome.maven.execution.ui.RunContentDescriptor;
import com.gome.maven.execution.ui.RunContentManager;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.messages.Topic;

/**
 * Manages the execution of run configurations and the relationship between running processes and Run/Debug toolwindow tabs.
 */
public abstract class ExecutionManager {
    public static final Topic<ExecutionListener> EXECUTION_TOPIC =
            Topic.create("configuration executed", ExecutionListener.class, Topic.BroadcastDirection.TO_PARENT);

    public static ExecutionManager getInstance( Project project) {
        return ServiceManager.getService(project, ExecutionManager.class);
    }

    /**
     * Returns the manager of running process tabs in Run and Debug toolwindows.
     *
     * @return the run content manager instance.
     */
    
    public abstract RunContentManager getContentManager();

    /**
     * Executes the before launch tasks for a run configuration.
     *
     * @param startRunnable    the runnable to actually start the process for the run configuration.
     * @param environment              the execution environment describing the process to be started.
     * @param state            the ready-to-start process
     * @param onCancelRunnable the callback to call if one of the before launch tasks cancels the process execution.
     */
    public abstract void compileAndRun( Runnable startRunnable,
                                        ExecutionEnvironment environment,
                                        RunProfileState state,
                                        Runnable onCancelRunnable);

    /**
     * Returns the list of processes managed by all open run and debug tabs.
     *
     * @return the list of processes.
     */
    
    public abstract ProcessHandler[] getRunningProcesses();

    /**
     * Prepares the run or debug tab for running the specified process and calls a callback to start it.
     *
     * @param starter the callback to start the process execution.
     * @param state   the ready-to-start process
     * @param environment     the execution environment describing the process to be started.
     */
    public abstract void startRunProfile( RunProfileStarter starter,
                                          RunProfileState state,
                                          ExecutionEnvironment environment);

    public abstract void restartRunProfile( Project project,
                                            Executor executor,
                                            ExecutionTarget target,
                                            RunnerAndConfigurationSettings configuration,
                                            ProcessHandler processHandler);

    /**
     * currentDescriptor is null for toolbar/popup action and not null for actions in run/debug toolwindows
     * @deprecated use {@link #restartRunProfile(com.gome.maven.execution.runners.ExecutionEnvironment)}
     * to remove in IDEA 15
     */
    public abstract void restartRunProfile( Project project,
                                            Executor executor,
                                            ExecutionTarget target,
                                            RunnerAndConfigurationSettings configuration,
                                            RunContentDescriptor currentDescriptor);

    /**
     * currentDescriptor is null for toolbar/popup action and not null for actions in run/debug toolwindows
     * @deprecated use {@link #restartRunProfile(com.gome.maven.execution.runners.ExecutionEnvironment)}
     * to remove in IDEA 15
     */
    public abstract void restartRunProfile( ProgramRunner runner,
                                            ExecutionEnvironment environment,
                                            RunContentDescriptor currentDescriptor);

    public abstract void restartRunProfile( ExecutionEnvironment environment);
}
