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

import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.Executor;
import com.gome.maven.execution.runners.ExecutionEnvironment;

import javax.swing.*;

/**
 * Base interface for things that can be executed (run configurations explicitly managed by user, or custom run profile implementations
 * created from code).
 *
 * @see RunConfiguration
 * @see ConfigurationFactory#createTemplateConfiguration(com.gome.maven.openapi.project.Project)
 */
public interface RunProfile {
    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    
    RunProfileState getState( Executor executor,  ExecutionEnvironment environment) throws ExecutionException;

    /**
     * Returns the name of the run configuration.
     *
     * @return the name of the run configuration.
     */
    String getName();

    /**
     * Returns the icon for the run configuration. This icon is displayed in the tab showing the results of executing the run profile,
     * and for persistent run configurations is also used in the run configuration management UI.
     *
     * @return the icon for the run configuration, or null if the default executor icon should be used.
     */
    
    Icon getIcon();
}