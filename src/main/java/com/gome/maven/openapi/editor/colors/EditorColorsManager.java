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
package com.gome.maven.openapi.editor.colors;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;

public abstract class EditorColorsManager {
     public static final String DEFAULT_SCHEME_NAME = "Default";

    public static EditorColorsManager getInstance() {
        return ServiceManager.getService(EditorColorsManager.class);
    }

    public abstract void addColorsScheme( EditorColorsScheme scheme);

    public abstract void removeAllSchemes();

    
    public abstract EditorColorsScheme[] getAllSchemes();

    public abstract void setGlobalScheme(EditorColorsScheme scheme);

    
    public abstract EditorColorsScheme getGlobalScheme();

    public abstract EditorColorsScheme getScheme( String schemeName);

    public abstract boolean isDefaultScheme(EditorColorsScheme scheme);

    /**
     * @deprecated use {@link #addEditorColorsListener(EditorColorsListener, Disposable)} instead
     */
    public abstract void addEditorColorsListener( EditorColorsListener listener);
    /**
     * @deprecated use {@link #addEditorColorsListener(EditorColorsListener, Disposable)} instead
     */
    public abstract void removeEditorColorsListener( EditorColorsListener listener);
    public abstract void addEditorColorsListener( EditorColorsListener listener,  Disposable disposable);

    public abstract boolean isUseOnlyMonospacedFonts();
    public abstract void setUseOnlyMonospacedFonts(boolean b);
}
