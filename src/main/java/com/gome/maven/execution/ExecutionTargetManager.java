/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.gome.maven.execution.runners.ExecutionEnvironment;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.messages.Topic;

import java.util.Collections;
import java.util.List;

public abstract class ExecutionTargetManager {
    public static final Topic<ExecutionTargetListener> TOPIC = Topic.create("ExecutionTarget topic", ExecutionTargetListener.class);

    
    public static ExecutionTargetManager getInstance( Project project) {
        return ServiceManager.getService(project, ExecutionTargetManager.class);
    }

    
    public static ExecutionTarget getActiveTarget( Project project) {
        return getInstance(project).getActiveTarget();
    }

    public static void setActiveTarget( Project project,  ExecutionTarget target) {
        getInstance(project).setActiveTarget(target);
    }

    
    public static List<ExecutionTarget> getTargetsFor( Project project,  RunnerAndConfigurationSettings settings) {
        return getInstance(project).getTargetsFor(settings);
    }

    
    public static List<ExecutionTarget> getTargetsToChooseFor( Project project,  RunnerAndConfigurationSettings settings) {
        List<ExecutionTarget> result = getInstance(project).getTargetsFor(settings);
        if (result.size() == 1 && DefaultExecutionTarget.INSTANCE.equals(result.get(0))) return Collections.emptyList();
        return result;
    }

    public static boolean canRun( RunnerAndConfigurationSettings settings,  ExecutionTarget target) {
        return settings != null && target != null && settings.canRunOn(target) && target.canRun(settings);
    }

    public static boolean canRun( ExecutionEnvironment environment) {
        return canRun(environment.getRunnerAndConfigurationSettings(), environment.getExecutionTarget());
    }

    public static void update( Project project) {
        getInstance(project).update();
    }

    
    public abstract ExecutionTarget getActiveTarget();

    public abstract void setActiveTarget( ExecutionTarget target);

    
    public abstract List<ExecutionTarget> getTargetsFor( RunnerAndConfigurationSettings settings);

    public abstract void update();
}
