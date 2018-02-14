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
package com.gome.maven.ide.browsers;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

import java.io.File;
import java.net.URI;

public abstract class BrowserLauncher {
    public static BrowserLauncher getInstance() {
        return ServiceManager.getService(BrowserLauncher.class);
    }

    public abstract void open( String url);

    public abstract void browse( URI uri);

    public abstract void browse( File file);

    public abstract void browse( String url,  WebBrowser browser);

    public abstract void browse( String url,  WebBrowser browser,  Project project);

    public abstract boolean browseUsingPath( String url,
                                             String browserPath,
                                             WebBrowser browser,
                                             Project project,
                                             String[] additionalParameters);
}