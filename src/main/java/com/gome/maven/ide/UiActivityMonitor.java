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
package com.gome.maven.ide;

import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.BusyObject;

public abstract class UiActivityMonitor {
    
    public abstract BusyObject getBusy( Project project,  UiActivity ... toWatch);

    
    public abstract BusyObject getBusy( UiActivity ... toWatch);

    public abstract void addActivity( Project project,  UiActivity activity);

    public abstract void addActivity( Project project,  UiActivity activity,  ModalityState effectiveModalityState);

    public abstract void addActivity( UiActivity activity);

    public abstract void addActivity( UiActivity activity,  ModalityState effectiveModalityState);

    public abstract void removeActivity( Project project,  UiActivity activity);

    public abstract void removeActivity( UiActivity activity);

    public abstract void clear();

    public abstract void setActive(boolean active);

    public static UiActivityMonitor getInstance() {
        return ServiceManager.getService(UiActivityMonitor.class);
    }

}
