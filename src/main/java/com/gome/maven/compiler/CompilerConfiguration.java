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

package com.gome.maven.compiler;

import com.gome.maven.openapi.compiler.options.ExcludesConfiguration;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;

public abstract class CompilerConfiguration {
    // need this flag for profiling purposes. In production code is always set to 'true'
    public static final boolean MAKE_ENABLED = true;

    
    public abstract String getProjectBytecodeTarget();

    
    public abstract String getBytecodeTargetLevel(Module module);

    public abstract void setBytecodeTargetLevel(Module module, String level);

    
    public abstract AnnotationProcessingConfiguration getAnnotationProcessingConfiguration(Module module);

    /**
     * @return true if exists at least one enabled annotation processing profile
     */
    public abstract boolean isAnnotationProcessorsEnabled();

    public static CompilerConfiguration getInstance(Project project) {
        return project.getComponent(CompilerConfiguration.class);
    }

    public abstract boolean isExcludedFromCompilation(VirtualFile virtualFile);

    public abstract boolean isResourceFile(VirtualFile virtualFile);

    public abstract boolean isResourceFile(String path);

    public abstract boolean isCompilableResourceFile(Project project, VirtualFile file);

    public abstract void addResourceFilePattern(String namePattern) throws MalformedPatternException;

    public abstract boolean isAddNotNullAssertions();

    public abstract void setAddNotNullAssertions(boolean enabled);

    public abstract ExcludesConfiguration getExcludedEntriesConfiguration();
}