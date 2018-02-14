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
package com.gome.maven.util;


import java.awt.*;

public class FontUtil {
    
    public static String rightArrow( Font font) {
        return canDisplay(font, '\u2192', "->");
    }

    
    public static String upArrow( Font font,  String defaultValue) {
        return canDisplay(font, '\u2191', defaultValue);
    }

    
    public static String canDisplay( Font font, char value,  String defaultValue) {
        return font.canDisplay(value) ? String.valueOf(value) : defaultValue;
    }
}
