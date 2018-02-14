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
package com.gome.maven.ide.macro;

import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.util.PlatformUtils;

/**
 * @author yole
 */
public class ModuleSdkPathMacro extends Macro {
    @Override
    public String getName() {
        return "ModuleSdkPath";
    }

    @Override
    public String getDescription() {
        return PlatformUtils.isPyCharm()
                ? "Project interpreter path"
                : "Module SDK path";
    }


    @Override
    public String expand(DataContext dataContext) throws ExecutionCancelledException {
        final Module module = LangDataKeys.MODULE.getData(dataContext);
        if (module == null) {
            return null;
        }
        return JdkPathMacro.sdkPath(ModuleRootManager.getInstance(module).getSdk());
    }
}
