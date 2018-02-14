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

import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.ExecutionResult;
import com.gome.maven.execution.Executor;
import com.gome.maven.execution.RunManager;
import com.gome.maven.execution.configurations.*;
import com.gome.maven.execution.ui.RunContentDescriptor;
import com.gome.maven.openapi.options.SettingsEditor;

abstract class BaseProgramRunner<Settings extends RunnerSettings> implements ProgramRunner<Settings> {
    @Override
    
    public Settings createConfigurationData(ConfigurationInfoProvider settingsProvider) {
        return null;
    }

    @Override
    public void checkConfiguration(RunnerSettings settings, ConfigurationPerRunnerSettings configurationPerRunnerSettings)
            throws RuntimeConfigurationException {
    }

    @Override
    public void onProcessStarted(RunnerSettings settings, ExecutionResult executionResult) {
    }

    @Override
    
    public SettingsEditor<Settings> getSettingsEditor(Executor executor, RunConfiguration configuration) {
        return null;
    }

    @Override
    public void execute( ExecutionEnvironment environment) throws ExecutionException {
        execute(environment, null);
    }

    @Override
    public void execute( ExecutionEnvironment environment,  Callback callback) throws ExecutionException {
        RunProfileState state = environment.getState();
        if (state == null) {
            return;
        }

        RunManager.getInstance(environment.getProject()).refreshUsagesList(environment.getRunProfile());
        execute(environment, callback, state);
    }

    protected abstract void execute( ExecutionEnvironment environment,
                                     Callback callback,
                                     RunProfileState state) throws ExecutionException;

    
    static RunContentDescriptor postProcess( ExecutionEnvironment environment,  RunContentDescriptor descriptor,  Callback callback) {
        if (descriptor != null) {
            descriptor.setExecutionId(environment.getExecutionId());
        }
        if (callback != null) {
            callback.processStarted(descriptor);
        }
        return descriptor;
    }
}