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
package com.gome.maven.compiler;

import com.gome.maven.openapi.compiler.CompileTask;
import com.gome.maven.openapi.extensions.AbstractExtensionPointBean;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.AtomicNotNullLazyValue;
import com.gome.maven.util.xmlb.annotations.Attribute;

/**
 * @author nik
 */
public class CompileTaskBean extends AbstractExtensionPointBean {
    public static final ExtensionPointName<CompileTaskBean> EP_NAME = ExtensionPointName.create("com.gome.maven.compiler.task");
    public enum CompileTaskExecutionPhase { BEFORE, AFTER }

    private final Project myProject;

    public CompileTaskBean(Project project) {
        myProject = project;
    }

    @Attribute("execute")
    public CompileTaskExecutionPhase myExecutionPhase = CompileTaskExecutionPhase.BEFORE;

    @Attribute("implementation")
    public String myImplementation;

    private final AtomicNotNullLazyValue<CompileTask> myInstanceHolder = new AtomicNotNullLazyValue<CompileTask>() {

        @Override
        protected CompileTask compute() {
            try {
                return instantiate(myImplementation, myProject.getPicoContainer());
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    };

    public CompileTask getTaskInstance() {
        return myInstanceHolder.getValue();
    }
}
