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

package com.gome.maven.openapi.projectRoots.ex;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ComparatorUtil;
import com.gome.maven.util.containers.Convertor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.gome.maven.util.containers.ContainerUtil.map;
import static com.gome.maven.util.containers.ContainerUtil.skipNulls;

/**
 * @author Eugene Zhuravlev
 *         Date: Apr 14, 2004
 */
public class PathUtilEx {

    private static final Function<Module, Sdk> MODULE_JDK = new Function<Module, Sdk>() {
        @Override
        public Sdk fun(Module module) {
            return ModuleRootManager.getInstance(module).getSdk();
        }
    };
    private static final Convertor<Sdk, String> JDK_VERSION = new Convertor<Sdk, String>() {
        @Override
        public String convert(Sdk jdk) {
            return StringUtil.notNullize(jdk.getVersionString());
        }
    };


    public static Sdk getAnyJdk(Project project) {
        return chooseJdk(project, Arrays.asList(ModuleManager.getInstance(project).getModules()));
    }


    public static Sdk chooseJdk(Project project, Collection<Module> modules) {
        Sdk projectJdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (projectJdk != null) {
            return projectJdk;
        }
        return chooseJdk(modules);
    }


    public static Sdk chooseJdk(Collection<Module> modules) {
        List<Sdk> jdks = skipNulls(map(skipNulls(modules), MODULE_JDK));
        if (jdks.isEmpty()) {
            return null;
        }
        Collections.sort(jdks, ComparatorUtil.compareBy(JDK_VERSION, String.CASE_INSENSITIVE_ORDER));
        return jdks.get(jdks.size() - 1);
    }
}
