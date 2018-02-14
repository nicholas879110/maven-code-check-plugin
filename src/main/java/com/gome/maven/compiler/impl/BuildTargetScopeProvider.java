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
package com.gome.maven.compiler.impl;

import com.gome.maven.openapi.compiler.CompileScope;
import com.gome.maven.openapi.compiler.CompilerFilter;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;

/**
 * Allows to control the list of build targets which are compiled when the Make action is invoked for a specific scope.
 *
 * @author nik
 */
public abstract class BuildTargetScopeProvider {
    public static final ExtensionPointName<BuildTargetScopeProvider> EP_NAME = ExtensionPointName.create("com.intellij.compiler.buildTargetScopeProvider");

    /**
     * @deprecated override {@link #getBuildTargetScopes(com.gome.maven.openapi.compiler.CompileScope, com.gome.maven.openapi.compiler.CompilerFilter, com.gome.maven.openapi.project.Project, boolean)} instead
     */
    
    public List<TargetTypeBuildScope> getBuildTargetScopes( CompileScope baseScope,  CompilerFilter filter,
                                                            Project project) {
        return Collections.emptyList();
    }


    
    public List<TargetTypeBuildScope> getBuildTargetScopes( CompileScope baseScope,  CompilerFilter filter,
                                                            Project project, boolean forceBuild) {
        return getBuildTargetScopes(baseScope, filter, project);
    }

}
