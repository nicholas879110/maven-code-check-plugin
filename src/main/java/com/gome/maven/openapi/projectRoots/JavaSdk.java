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
package com.gome.maven.openapi.projectRoots;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.projectRoots.impl.SdkVersionUtil;

import java.io.File;

public abstract class JavaSdk extends SdkType implements JavaSdkType {
    public JavaSdk(  String name) {
        super(name);
    }

    public static JavaSdk getInstance() {
        return ApplicationManager.getApplication().getComponent(JavaSdk.class);
    }

    public final Sdk createJdk( String jdkName,  String jreHome) {
        return createJdk(jdkName, jreHome, true);
    }

    /**
     * @deprecated use {@link #isOfVersionOrHigher(Sdk, JavaSdkVersion)} instead
     */
    public abstract int compareTo( String versionString,  String versionNumber);

    public abstract Sdk createJdk( String jdkName,  String home, boolean isJre);

    
    public abstract JavaSdkVersion getVersion( Sdk sdk);

    
    public abstract JavaSdkVersion getVersion( String versionString);

    public abstract boolean isOfVersionOrHigher( Sdk sdk,  JavaSdkVersion version);

    public static boolean checkForJdk(File file) {
        return JdkUtil.checkForJdk(file);
    }

    public static boolean checkForJre(String file) {
        return JdkUtil.checkForJre(file);
    }

    
    public static String getJdkVersion(final String sdkHome) {
        return SdkVersionUtil.detectJdkVersion(sdkHome);
    }
}
