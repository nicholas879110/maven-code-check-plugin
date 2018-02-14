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
package com.gome.maven.ide;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WelcomeWizardUtil {
    private static volatile String ourDefaultLAF;
    private static volatile String ourWizardLAF;
    private static volatile String ourWizardMacKeymap;
    private static volatile String ourWizardEditorScheme;
    private static volatile Boolean ourAutoScrollToSource;
    private static volatile Set<String> ourFeaturedPluginsToInstall = new HashSet<String>();

    public static void setDefaultLAF(String laf) {
        ourDefaultLAF = laf;
    }

    public static String getDefaultLAF() {
        return ourDefaultLAF;
    }

    public static void setWizardLAF(String laf) {
        ourWizardLAF = laf;
    }

    public static String getWizardLAF() {
        return ourWizardLAF;
    }

    public static void setWizardKeymap( String keymap) {
        ourWizardMacKeymap = keymap;
    }

    
    public static String getWizardMacKeymap() {
        return ourWizardMacKeymap;
    }

    public static void setWizardEditorScheme( String wizardEditorScheme) {
        ourWizardEditorScheme = wizardEditorScheme;
    }

    
    public static String getWizardEditorScheme() {
        return ourWizardEditorScheme;
    }

    
    public static Boolean getAutoScrollToSource() {
        return ourAutoScrollToSource;
    }

    public static void setAutoScrollToSource( Boolean autoScrollToSource) {
        ourAutoScrollToSource = autoScrollToSource;
    }

    public static Set<String> getFeaturedPluginsToInstall() {
        return Collections.unmodifiableSet(ourFeaturedPluginsToInstall);
    }

    public static void setFeaturedPluginsToInstall(Set<String> pluginsToInstall) {
        ourFeaturedPluginsToInstall.clear();
        ourFeaturedPluginsToInstall.addAll(pluginsToInstall);
    }
}
