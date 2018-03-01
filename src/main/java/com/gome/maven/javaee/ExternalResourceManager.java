/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.javaee;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.SimpleModificationTracker;
import com.gome.maven.psi.PsiFile;

public abstract class ExternalResourceManager extends SimpleModificationTracker {
    public static ExternalResourceManager getInstance() {
        return ServiceManager.getService(ExternalResourceManager.class);
    }

    public abstract void addResource(  String url,  String location);

    public abstract void addResource(  String url,   String version,  String location);

    public abstract void removeResource( String url);

    public abstract void removeResource( String url,  String version);

    /**
     * @see #getResourceLocation(String, Project)
     */
    @Deprecated
    public abstract String getResourceLocation(  String url);

    public abstract String getResourceLocation(  String url,  String version);

    public abstract String getResourceLocation(  String url,  Project project);

    
    public abstract PsiFile getResourceLocation(  String url,  PsiFile baseFile,  String version);

    public abstract String[] getResourceUrls( FileType fileType, boolean includeStandard);

    public abstract String[] getResourceUrls( FileType fileType,   String version, boolean includeStandard);
}
