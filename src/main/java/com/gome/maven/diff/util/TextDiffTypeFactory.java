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
package com.gome.maven.diff.util;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.markup.TextAttributes;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TextDiffTypeFactory {
    private static final TextDiffTypeFactory ourInstance = new TextDiffTypeFactory();
    private final List<TextDiffType> myTypes = new ArrayList<TextDiffType>();

    private TextDiffTypeFactory() {
    }

    
    public synchronized TextDiffType createTextDiffType(  TextAttributesKey key,
                                                         String name) {
        TextDiffType type = new TextDiffTypeImpl(key, name);
        myTypes.add(type);
        return type;
    }

    public synchronized TextDiffType[] getAllDiffTypes() {
        return myTypes.toArray(new TextDiffType[myTypes.size()]);
    }

    public static TextDiffTypeFactory getInstance() {
        return ourInstance;
    }

    private static class TextDiffTypeImpl implements TextDiffType {
         private final TextAttributesKey myKey;
         private final String myName;

        @SuppressWarnings("SpellCheckingInspection")
        public TextDiffTypeImpl( TextAttributesKey key,  String name) {
            myKey = key;
            myName = name;
        }

        
        @Override
        public String getName() {
            return myName;
        }

        
        public TextAttributes getAttributes( Editor editor) {
            if (editor == null) {
                return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(myKey);
            }
            else {
                return editor.getColorsScheme().getAttributes(myKey);
            }
        }

        
        @Override
        public Color getColor( Editor editor) {
            return getAttributes(editor).getBackgroundColor();
        }

        
        @Override
        public Color getIgnoredColor( Editor editor) {
            TextAttributes attributes = getAttributes(editor);
            Color color = attributes.getForegroundColor();
            if (color != null) return color;

            if (editor instanceof EditorEx) {
                Color fg = attributes.getBackgroundColor();
                Color bg = ((EditorEx)editor).getBackgroundColor();
                return getMiddleColor(fg, bg);
            }
            else {
                Color fg = attributes.getBackgroundColor();
                Color bg = EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground();
                return getMiddleColor(fg, bg);
            }
        }

        
        @Override
        public Color getMarkerColor( Editor editor) {
            return getAttributes(editor).getErrorStripeColor();
        }
    }

    private static final double MIDDLE_COLOR_FACTOR = 0.6;

    
    private static Color getMiddleColor( Color fg,  Color bg) {
        int red = avg(fg.getRed(), bg.getRed(), MIDDLE_COLOR_FACTOR);
        int green = avg(fg.getGreen(), bg.getGreen(), MIDDLE_COLOR_FACTOR);
        int blue = avg(fg.getBlue(), bg.getBlue(), MIDDLE_COLOR_FACTOR);
        //noinspection UseJBColor
        return new Color(red, green, blue);
    }

    private static int avg(int fg, int bg, double factor) {
        return (int)(fg + Math.round(factor * (bg - fg)));
    }
}
