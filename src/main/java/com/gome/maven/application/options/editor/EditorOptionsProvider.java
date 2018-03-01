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

/*
 * User: anna
 * Date: 14-Feb-2008
 */
package com.gome.maven.application.options.editor;

import com.gome.maven.openapi.options.SearchableConfigurable;

/**
 * Register implementation of this interface in the plugin.xml to provide sub-section of Editor section in the Settings dialog:
 * <p/>
 * &lt;extensions defaultExtensionNs="com.gome.maven"&gt;<br>
 * &nbsp;&nbsp;&lt;editorOptionsProvider instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 */
public interface EditorOptionsProvider extends SearchableConfigurable {
}