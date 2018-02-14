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
package com.gome.maven.ide.ui.search;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.extensions.ExtensionPointName;

/**
 * @author ignatov
 */
public abstract class ActionFromOptionDescriptorProvider {
    public static final ExtensionPointName<ActionFromOptionDescriptorProvider> EP = new ExtensionPointName<ActionFromOptionDescriptorProvider>("com.gome.maven.actionFromOptionDescriptorProvider");

    public abstract AnAction provide( OptionDescription description);
}
