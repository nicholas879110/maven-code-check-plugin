/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.module;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.roots.LanguageLevelModuleExtension;
import com.gome.maven.openapi.roots.LanguageLevelModuleExtensionImpl;
import com.gome.maven.openapi.roots.LanguageLevelProjectExtension;
import com.gome.maven.pom.java.LanguageLevel;

public class EffectiveLanguageLevelUtil {
    
    public static LanguageLevel getEffectiveLanguageLevel( final Module module) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        LanguageLevelModuleExtension moduleLevel = LanguageLevelModuleExtensionImpl.getInstance(module);
        LanguageLevel level = moduleLevel == null ? null : moduleLevel.getLanguageLevel();
        if (level != null) return level;
        return LanguageLevelProjectExtension.getInstance(module.getProject()).getLanguageLevel();
    }
}
