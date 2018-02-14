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
package com.gome.maven.compiler.impl.javaCompiler.javac;

import com.gome.maven.compiler.impl.javaCompiler.BackendCompiler;
import com.gome.maven.openapi.compiler.CompilerBundle;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.project.Project;
import org.jetbrains.jps.model.java.compiler.JavaCompilers;

import java.util.Collections;
import java.util.Set;

public class JavacCompiler implements BackendCompiler {
    private final Project myProject;

    public JavacCompiler(Project project) {
        myProject = project;
    }

    
    
    public String getId() { // used for externalization
        return JavaCompilers.JAVAC_ID;
    }

    
    public String getPresentableName() {
        return CompilerBundle.message("compiler.javac.name");
    }

    
    public Configurable createConfigurable() {
        return new JavacConfigurable(JavacConfiguration.getOptions(myProject, JavacConfiguration.class));
    }

    
    public Set<FileType> getCompilableFileTypes() {
        return Collections.<FileType>singleton(StdFileTypes.JAVA);
    }
}
