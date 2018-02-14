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
package com.gome.maven.platform;

import com.gome.maven.ide.util.projectWizard.AbstractModuleBuilder;
import com.gome.maven.openapi.ui.ValidationInfo;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 *         Date: 9/28/12
 */
public interface ProjectTemplate {

    ProjectTemplate[] EMPTY_ARRAY = new ProjectTemplate[0];

    
    String getName();

    
    String getDescription();

    Icon getIcon();

    
    AbstractModuleBuilder createModuleBuilder();

    /**
     * @return null if ok, error message otherwise
     */
    
    ValidationInfo validateSettings();
}
