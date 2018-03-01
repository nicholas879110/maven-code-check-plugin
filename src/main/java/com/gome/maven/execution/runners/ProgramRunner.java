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
package com.gome.maven.execution.runners;

import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.ExecutionResult;
import com.gome.maven.execution.Executor;
import com.gome.maven.execution.configurations.*;
import com.gome.maven.execution.ui.RunContentDescriptor;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.options.SettingsEditor;

/**
 * A ProgramRunner is responsible for the execution workflow of certain types of run configurations with a certain executor. For example,
 * one ProgramRunner can be responsible for debugging all Java-based run configurations (applications, JUnit tests, etc.); the run
 * configuration takes care of building a command line and the program runner takes care of how exactly it needs to be executed.
 *
 * A newly created program runner should be registered in a corresponding plugin.xml:
 *
 * &lt;extensions defaultExtensionNs="com.gome.maven"&gt;
 *   &lt;programRunner implementation="RunnerClassFQN"/&gt;
 * &lt;/extensions&gt;
 *
 * @param <Settings>
 * @see GenericProgramRunner
 */
public interface ProgramRunner<Settings extends RunnerSettings> {
    ExtensionPointName<ProgramRunner> PROGRAM_RUNNER_EP = ExtensionPointName.create("com.gome.maven.programRunner");

    interface Callback {
        void processStarted(RunContentDescriptor descriptor);
    }

    /**
     * Returns the unique ID of this runner. This ID is used to store settings and must not change between plugin versions.
     *
     * @return the program runner ID.
     */
     
    String getRunnerId();

    /**
     * Checks if the program runner is capable of running the specified configuration with the specified executor.
     *
     * @param executorId ID of the {@link Executor} with which the user is trying to run the configuration.
     * @param profile the configuration being run.
     * @return true if the runner can handle it, false otherwise.
     */
    boolean canRun( final String executorId,  final RunProfile profile);

    /**
     * Creates a block of per-configuration settings used by this program runner.
     *
     * @param settingsProvider source of assorted information about the configuration being edited.
     * @return the per-runner settings, or null if this runner doesn't use any per-runner settings.
     */
    
    Settings createConfigurationData(ConfigurationInfoProvider settingsProvider);

    void checkConfiguration(RunnerSettings settings,  ConfigurationPerRunnerSettings configurationPerRunnerSettings)
            throws RuntimeConfigurationException;

    void onProcessStarted(RunnerSettings settings, ExecutionResult executionResult);

    
    SettingsEditor<Settings> getSettingsEditor(Executor executor, RunConfiguration configuration);

    void execute( ExecutionEnvironment environment) throws ExecutionException;
    void execute( ExecutionEnvironment environment,  Callback callback) throws ExecutionException;
}