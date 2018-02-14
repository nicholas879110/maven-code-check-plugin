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

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.components.ServiceManager;

public abstract class RecentProjectsManager {
    public static RecentProjectsManager getInstance() {
        return ServiceManager.getService(RecentProjectsManager.class);
    }

    
    public abstract String getLastProjectCreationLocation();

    public abstract void setLastProjectCreationLocation( String lastProjectLocation);

    public abstract void clearNameCache();

    public abstract void updateLastProjectPath();

    public abstract String getLastProjectPath();

    public abstract void removePath( String path);

    /**
     * @param addClearListItem whether the "Clear List" action should be added to the end of the list.
     */
    public abstract AnAction[] getRecentProjectsActions(boolean addClearListItem);

    public boolean hasPath(String path) {
        return false;
    }
}