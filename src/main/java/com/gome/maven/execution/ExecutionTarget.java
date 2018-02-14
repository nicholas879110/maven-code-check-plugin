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


import javax.swing.*;

/**
 * Execution targets allow running same run configuration on various targets such as devices, simulators etc.<br>
 * IDE can suggest possible targets for every configuration depending on its type or settings.<br>
 * <br>
 * When a run configuration is executed on a specific target, it becomes associated with this target and all the following actions (e.g. rerun, rerun failed tests)
 * are be performed on this target, even if another target is selected in the UI.<br> 
 * <br>
 * Example:<br> 
 *   AppCode suggests available iOS Devices and iOS Simulators for iOS run configuration,<br> 
 *   while only showing OS X 32-bit/64-bit targets for OS X configurations.<br>
 * <br>   
 * RunConfiguration can decide, if it can be run on a given target<br>
 * (see {@link com.gome.maven.execution.RunnerAndConfigurationSettings#canRunOn(com.gome.maven.execution.ExecutionTarget)} and {@link com.gome.maven.execution.configurations.TargetAwareRunProfile#canRunOn(com.gome.maven.execution.ExecutionTarget)})<br>
 * <br>   
 * Targets are collected from {@link com.gome.maven.execution.ExecutionTargetProvider}
 */
public abstract class ExecutionTarget {
    /**
     * Id is used to save selected target between restarts
     */
    
    public abstract String getId();

    
    public abstract String getDisplayName();

    
    public abstract Icon getIcon();

    /**
     * Implementation-specific logic should decide whether to suggest this target for the given configuration. 
     */
    public abstract boolean canRun( RunnerAndConfigurationSettings configuration);

    @Override
    public boolean equals(Object obj) {
        return obj == this || (getClass().isInstance(obj) && getId().equals(((ExecutionTarget)obj).getId()));
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }
}
