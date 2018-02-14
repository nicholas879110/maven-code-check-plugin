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
package com.gome.maven.openapi.options.ex;

import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurableGroup;
import com.gome.maven.openapi.options.OptionsBundle;
import com.gome.maven.openapi.project.Project;

/**
 * @author max
 */
public class ProjectConfigurablesGroup extends ConfigurablesGroupBase implements ConfigurableGroup {
    private final Project myProject;

    public ProjectConfigurablesGroup( Project project) {
        super(project, Configurable.PROJECT_CONFIGURABLE, true);
        myProject = project;
    }

    @Override
    public String getDisplayName() {
        if (isDefault()) return OptionsBundle.message("template.project.settings.display.name");
        return OptionsBundle.message("project.settings.display.name", myProject.getName());
    }

    private boolean isDefault() {
        return myProject.isDefault();
    }

    @Override
    public ConfigurableFilter getConfigurableFilter() {
        return new ConfigurableFilter() {
            @Override
            public boolean isIncluded(final Configurable configurable) {
                return !isDefault() || !ConfigurableWrapper.isNonDefaultProject(configurable);
            }
        };
    }

    public int hashCode() {
        return 0;
    }

    public boolean equals(Object object) {
        return object instanceof ProjectConfigurablesGroup && ((ProjectConfigurablesGroup)object).myProject == myProject;
    }
}
