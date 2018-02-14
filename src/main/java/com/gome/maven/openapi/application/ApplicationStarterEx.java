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
package com.gome.maven.openapi.application;


/**
 * Implementers of the interface declared via {@link com.gome.maven.ExtensionPoints#APPLICATION_STARTER}
 * may be capable of processing an external command line within a running IntelliJ Platform instance.
 *
 * @author yole
 */
public abstract class ApplicationStarterEx implements ApplicationStarter {
    public abstract boolean isHeadless();

    public boolean canProcessExternalCommandLine() {
        return false;
    }

    @Deprecated
    public void processExternalCommandLine(String[] args) { }

    public void processExternalCommandLine(String[] args, String currentDirectory) {
        processExternalCommandLine(args);
    }
}
