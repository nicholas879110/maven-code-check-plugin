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
package com.gome.maven.psi.codeStyle;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.extensions.ExtensionPointName;

/**
 * @author peter
 */
public abstract class CodeStyleSettingsProvider {
    public static final ExtensionPointName<CodeStyleSettingsProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.gome.maven.codeStyleSettingsProvider");


    
    public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return null;
    }

    
    public abstract Configurable createSettingsPage(CodeStyleSettings settings, final CodeStyleSettings originalSettings);

    /**
     * Returns the name of the configurable page without creating a Configurable instance.
     *
     * @return the display name of the configurable page.
     * @since 9.0
     */
    
    public String getConfigurableDisplayName() {
        Language lang = getLanguage();
        return lang == null ? null : lang.getDisplayName();
    }

    public boolean hasSettingsPage() {
        return true;
    }

    public DisplayPriority getPriority() {
        return DisplayPriority.LANGUAGE_SETTINGS;
    }

    /**
     * Specifies a language this provider applies to. If the language is not null, its display name will
     * be used as a configurable name by default if <code>getConfigurableDisplayName()</code> is not
     * overridden.
     *
     * @return null by default.
     */
    
    public Language getLanguage() {
        return null;
    }

}
