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
package com.gome.maven.openapi.extensions;


/**
 * Extensions should implement this interface when it is important to find out what particular plugin has provided this extension.
 * @author akireyev
 */
public interface PluginAware {
    /**
     * Called by extensions framework when extension is loaded from plugin.xml descriptor.
     * @param pluginDescriptor descriptor of the plugin that provided this particular extension.
     */
    void setPluginDescriptor(PluginDescriptor pluginDescriptor);
}
