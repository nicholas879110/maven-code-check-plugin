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
package com.gome.maven.openapi.roots;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.pom.java.LanguageLevel;

/**
 * @author Dmitry Avdeev
 */
public abstract class LanguageLevelProjectExtension {

    public static LanguageLevelProjectExtension getInstance(Project project) {
        return ServiceManager.getService(project, LanguageLevelProjectExtension.class);
    }

    
    public abstract LanguageLevel getLanguageLevel();

    public abstract void setLanguageLevel( LanguageLevel languageLevel);

    private Boolean myDefault;

    /**
     * Auto-detect language level from project JDK maximum possible level.
     * @return null if the property is not set yet (e.g. after migration).
     */
    
    public Boolean getDefault() {
        return myDefault;
    }

    public void setDefault( Boolean value) {
        myDefault = value;
    }

    public boolean isDefault() {
        return myDefault != null && myDefault;
    }

    public abstract void languageLevelsChanged();

    /**
     * Project reloading is not needed on language level changes
     * @deprecated to remove in IDEA 15
     */
    public abstract void reloadProjectOnLanguageLevelChange( LanguageLevel languageLevel, boolean forceReload);
}
