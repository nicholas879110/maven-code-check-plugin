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
package com.gome.maven.xdebugger.settings;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.options.Configurable;

import java.util.Collection;
import java.util.Collections;

public abstract class DebuggerConfigurableProvider {
    public static final ExtensionPointName<DebuggerConfigurableProvider> EXTENSION_POINT = ExtensionPointName.create("com.intellij.xdebugger.configurableProvider");

    
    public Collection<? extends Configurable> getConfigurables( DebuggerSettingsCategory category) {
        return Collections.emptyList();
    }

    /**
     * General settings of category were applied
     */
    public void generalApplied( DebuggerSettingsCategory category) {
    }

    public boolean isTargetedToProduct( Configurable configurable) {
        return false;
    }
}