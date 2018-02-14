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
package com.gome.maven.openapi.editor;

import com.gome.maven.openapi.editor.markup.EffectType;
import com.gome.maven.openapi.editor.markup.TextAttributes;
//import org.gome.maven.lang.annotations.JdkConstants;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class LineExtensionInfo {
     private final String myText;
     private final Color myColor;
     private final EffectType myEffectType;
     private final Color myEffectColor;
    /*@JdkConstants.FontStyle*/ private final int myFontType;

    public LineExtensionInfo( String text,
                              Color color,
                              EffectType effectType,
                              Color effectColor,
                             /*@JdkConstants.FontStyle*/ int fontType) {
        myText = text;
        myColor = color;
        myEffectType = effectType;
        myEffectColor = effectColor;
        myFontType = fontType;
    }
    public LineExtensionInfo( String text,  TextAttributes attr) {
        myText = text;
        myColor = attr.getForegroundColor();
        myEffectType = attr.getEffectType();
        myEffectColor = attr.getEffectColor();
        myFontType = attr.getFontType();
    }

    
    public String getText() {
        return myText;
    }

    
    public Color getColor() {
        return myColor;
    }

    
    public EffectType getEffectType() {
        return myEffectType;
    }

    
    public Color getEffectColor() {
        return myEffectColor;
    }

//    @JdkConstants.FontStyle
    public int getFontType() {
        return myFontType;
    }
}
