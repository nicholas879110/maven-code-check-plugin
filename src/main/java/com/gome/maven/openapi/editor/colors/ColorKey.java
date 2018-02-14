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

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.ui.Gray;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public final class ColorKey implements Comparable<ColorKey> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.editor.colors.ColorKey");
    private static final Color NULL_COLOR = Gray._0;

    private final String myExternalName;
    private Color myDefaultColor = NULL_COLOR;
    private static final Map<String, ColorKey> ourRegistry = new HashMap<String, ColorKey>();

    private ColorKey( String externalName) {
        myExternalName = externalName;
        if (ourRegistry.containsKey(myExternalName)) {
            LOG.error("Key " + myExternalName + " already registered.");
        }
        else {
            ourRegistry.put(myExternalName, this);
        }
    }

    
    public static ColorKey find( String externalName) {
        ColorKey key = ourRegistry.get(externalName);
        return key == null ? new ColorKey(externalName) : key;
    }

    public String toString() {
        return myExternalName;
    }

    
    public String getExternalName() {
        return myExternalName;
    }

    @Override
    public int compareTo( ColorKey key) {
        return myExternalName.compareTo(key.myExternalName);
    }

    public Color getDefaultColor() {
        if (myDefaultColor == NULL_COLOR) {
            myDefaultColor = null;
      /*
      EditorColorsManager manager = EditorColorsManager.getInstance();
      if (manager != null) { // Can be null in test mode
        myDefaultColor = manager.getGlobalScheme().getColor(this);
      }
      */
        }

        return myDefaultColor;
    }

    
    public static ColorKey createColorKey(  String externalName) {
        return find(externalName);
    }

    
    public static ColorKey createColorKey(  String externalName, Color defaultColor) {
        ColorKey key = createColorKey(externalName);

        if (key.getDefaultColor() == null) {
            key.myDefaultColor = defaultColor;
        }
        return key;
    }
}