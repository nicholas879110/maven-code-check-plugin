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
package com.gome.maven.compiler.options;

import com.gome.maven.openapi.compiler.CompileContext;
import com.gome.maven.openapi.compiler.Compiler;
import com.gome.maven.openapi.compiler.options.ExcludedEntriesConfiguration;
import com.gome.maven.openapi.compiler.options.ExcludesConfiguration;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.xmlb.XmlSerializerUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
@State(
        name = "ValidationConfiguration",
        storages = {
                @Storage(file = StoragePathMacros.WORKSPACE_FILE),
                @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/validation.xml", scheme = StorageScheme.DIRECTORY_BASED)
        }
)
public class ValidationConfiguration implements PersistentStateComponent<ValidationConfiguration> {

    public boolean VALIDATE_ON_BUILD = false;
    public Map<String, Boolean> VALIDATORS = new HashMap<String, Boolean>();

    public static boolean shouldValidate(Compiler validator, CompileContext context) {
        ValidationConfiguration configuration = getInstance(context.getProject());
        return (configuration.VALIDATE_ON_BUILD) && configuration.isSelected(validator);
    }

    public boolean isSelected(Compiler validator) {
        return isSelected(validator.getDescription());
    }

    public boolean isSelected(String validatorDescription) {
        final Boolean selected = VALIDATORS.get(validatorDescription);
        return selected == null || selected.booleanValue();
    }

    public void setSelected(Compiler validator, boolean selected) {
        setSelected(validator.getDescription(), selected);
    }

    public void setSelected(String validatorDescription, boolean selected) {
        VALIDATORS.put(validatorDescription, selected);
    }

    public static ValidationConfiguration getInstance(Project project) {
        return ServiceManager.getService(project, ValidationConfiguration.class);
    }

    public static ExcludesConfiguration getExcludedEntriesConfiguration(Project project) {
        return ServiceManager.getService(project, ExcludedFromValidationConfiguration.class);
    }

    public ValidationConfiguration getState() {
        return this;
    }

    public void loadState(final ValidationConfiguration state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @State(
            name = "ExcludeFromValidation",
            storages = {
                    @Storage(file = StoragePathMacros.PROJECT_FILE),
                    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/excludeFromValidation.xml", scheme = StorageScheme.DIRECTORY_BASED)
            }
    )
    public static class ExcludedFromValidationConfiguration extends ExcludedEntriesConfiguration {}
}
