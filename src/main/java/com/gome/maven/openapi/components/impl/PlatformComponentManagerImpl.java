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
package com.gome.maven.openapi.components.impl;

import com.gome.maven.ide.plugins.PluginManager;
import com.gome.maven.openapi.components.ComponentConfig;
import com.gome.maven.openapi.components.ComponentManager;

public abstract class PlatformComponentManagerImpl extends ComponentManagerImpl {
    private boolean myHandlingInitComponentError;

    protected PlatformComponentManagerImpl(ComponentManager parent) {
        super(parent);
    }

    protected PlatformComponentManagerImpl(ComponentManager parent,  String name) {
        super(parent, name);
    }

    @Override
    protected void handleInitComponentError(Throwable t, String componentClassName, ComponentConfig config) {
        if (!myHandlingInitComponentError) {
            myHandlingInitComponentError = true;
            try {
                PluginManager.handleComponentError(t, componentClassName, config);
            }
            finally {
                myHandlingInitComponentError = false;
            }
        }
    }
}
