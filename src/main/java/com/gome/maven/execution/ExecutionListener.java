/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import com.gome.maven.execution.process.ProcessHandler;
import com.gome.maven.execution.runners.ExecutionEnvironment;

import java.util.EventListener;

/**
 * @author nik
 */
public interface ExecutionListener extends EventListener {

    void processStartScheduled(String executorId, ExecutionEnvironment env);

    void processStarting(String executorId,  ExecutionEnvironment env);

    void processNotStarted(String executorId,  ExecutionEnvironment env);

    void processStarted(String executorId,  ExecutionEnvironment env,  ProcessHandler handler);

    void processTerminating( RunProfile runProfile,  ProcessHandler handler);

    void processTerminated( RunProfile runProfile,  ProcessHandler handler);
}
