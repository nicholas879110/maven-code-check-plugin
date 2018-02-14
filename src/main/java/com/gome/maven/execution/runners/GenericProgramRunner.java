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
import com.gome.maven.execution.ExecutionManager;
import com.gome.maven.execution.RunProfileStarter;
import com.gome.maven.execution.configurations.RunProfileState;
import com.gome.maven.execution.configurations.RunnerSettings;
import com.gome.maven.execution.ui.RunContentDescriptor;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.project.Project;

public abstract class GenericProgramRunner<Settings extends RunnerSettings> extends BaseProgramRunner<Settings> {
    @Deprecated
    public static final DataKey<RunContentDescriptor> CONTENT_TO_REUSE_DATA_KEY = DataKey.create("contentToReuse");
    @SuppressWarnings({"UnusedDeclaration", "deprecation"}) @Deprecated 
    public static final String CONTENT_TO_REUSE = CONTENT_TO_REUSE_DATA_KEY.getName();

    @Override
    protected void execute( ExecutionEnvironment environment,  final Callback callback,  RunProfileState state)
            throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(new RunProfileStarter() {
            @Override
            public RunContentDescriptor execute( RunProfileState state,  ExecutionEnvironment environment) throws ExecutionException {
                return postProcess(environment, doExecute(state, environment), callback);
            }
        }, state, environment);
    }

    
    protected RunContentDescriptor doExecute( RunProfileState state,  ExecutionEnvironment environment) throws ExecutionException {
        return doExecute(environment.getProject(), state, environment.getContentToReuse(), environment);
    }

    @Deprecated
    
    /**
     * @deprecated to remove in IDEA 16
     */
    protected RunContentDescriptor doExecute( Project project,
                                              RunProfileState state,
                                              RunContentDescriptor contentToReuse,
                                              ExecutionEnvironment environment) throws ExecutionException {
        throw new AbstractMethodError();
    }
}
