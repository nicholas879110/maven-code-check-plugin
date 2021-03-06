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
package com.gome.maven.util.xml;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.HashSet;

import java.util.Set;

public abstract class ModuleContextProvider {
    public static final ExtensionPointName<ModuleContextProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.moduleContextProvider");

    
    public abstract Module[] getContextModules( PsiFile context);

    public static Module[] getModules( PsiFile context) {
        if (context == null) return Module.EMPTY_ARRAY;

        final Set<Module> modules = new HashSet<Module>();
        for (ModuleContextProvider moduleContextProvider : Extensions.getExtensions(EP_NAME)) {
            ContainerUtil.addAllNotNull(modules, moduleContextProvider.getContextModules(context));
        }
        Module module = ModuleUtilCore.findModuleForPsiElement(context);
        if (module != null) modules.add(module);

        return modules.toArray(new Module[modules.size()]);
    }
}
