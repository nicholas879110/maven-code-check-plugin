/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.tools;

import com.google.common.collect.Lists;
import com.gome.maven.openapi.extensions.ExtensionPointName;

import java.util.List;

/**
 * @author traff
 */
public abstract class ToolsProvider {
    public static final ExtensionPointName<ToolsProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.toolsProvider");

    public abstract List<Tool> getTools();

    public static List<Tool> getAllTools() {
        List<Tool> result = Lists.newArrayList();
        for (ToolsProvider provider : EP_NAME.getExtensions()) {
            result.addAll(provider.getTools());
        }

        return result;
    }
}
