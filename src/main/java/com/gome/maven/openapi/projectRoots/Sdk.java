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
package com.gome.maven.openapi.projectRoots;

import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.roots.RootProvider;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * @author Eugene Zhuravlev
 *         Date: Sep 23, 2004
 *
 * @see ProjectJdkTable
 * @see ProjectRootManager#getProjectSdk()
 */
public interface Sdk extends UserDataHolder {
    
    SdkTypeId getSdkType();

    
    String getName();

    
    String getVersionString();

    
    String getHomePath();

    
    VirtualFile getHomeDirectory();

    
    RootProvider getRootProvider();

    
    SdkModificator getSdkModificator();

    
    SdkAdditionalData getSdkAdditionalData();

    
    Object clone() throws CloneNotSupportedException;
}