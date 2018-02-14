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
package com.gome.maven.openapi.options;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.SystemInfo;

import java.awt.*;

public abstract class ShowSettingsUtil {
    public static ShowSettingsUtil getInstance() {
        return ServiceManager.getService(ShowSettingsUtil.class);
    }

    public abstract void showSettingsDialog(Project project, ConfigurableGroup... group);

    public abstract void showSettingsDialog( Project project, Class toSelect);

    public abstract void showSettingsDialog( Project project,  String nameToSelect);

    public abstract void showSettingsDialog( final Project project, final Configurable toSelect);

    public abstract boolean editConfigurable(Project project, Configurable configurable);

    public abstract boolean editConfigurable( Project project, Configurable configurable,  Runnable advancedInitialization);

    public abstract boolean editConfigurable( Component parent,  Configurable configurable);

    public abstract boolean editConfigurable(Component parent, Configurable configurable,  Runnable advancedInitialization);

    public abstract boolean editConfigurable(Project project,  String dimensionServiceKey, Configurable configurable);

    public abstract boolean editConfigurable(Project project,  String dimensionServiceKey, Configurable configurable, boolean showApplyButton);

    public abstract boolean editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable);

    /**
     * @deprecated create a new instance of configurable instead
     * to remove in IDEA 15
     */
    public abstract <T extends Configurable> T findProjectConfigurable(Project project, Class<T> confClass);

    /**
     * @deprecated create a new instance of configurable instead
     * to remove in IDEA 15
     */
    public abstract <T extends Configurable> T findApplicationConfigurable(Class<T> confClass);

    public static String getSettingsMenuName() {
        return SystemInfo.isMac ? "Preferences" : "Settings";
    }
}