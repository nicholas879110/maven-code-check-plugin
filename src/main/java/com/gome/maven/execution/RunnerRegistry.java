/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.execution.configurations.RunProfile;
import com.gome.maven.execution.runners.ProgramRunner;
import com.gome.maven.openapi.components.ServiceManager;

public abstract class RunnerRegistry {
    public static RunnerRegistry getInstance() {
        return ServiceManager.getService(RunnerRegistry.class);
    }

    @SuppressWarnings("unused")
    @Deprecated
    public abstract boolean hasRunner( String executorId,  RunProfile settings);

    
    public abstract ProgramRunner getRunner( String executorId,  RunProfile settings);

    @SuppressWarnings("unused")
    @Deprecated
    public abstract ProgramRunner[] getRegisteredRunners();

    
    public abstract ProgramRunner findRunnerById(String id);
}
