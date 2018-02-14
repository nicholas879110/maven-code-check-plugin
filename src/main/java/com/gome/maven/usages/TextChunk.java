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
package com.gome.maven.usages;

import com.gome.maven.openapi.editor.markup.AttributesFlyweight;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.usages.impl.rules.UsageType;

public class TextChunk {
    public static final TextChunk[] EMPTY_ARRAY = new TextChunk[0];

    private final AttributesFlyweight myAttributes;
    private final String myText;
    private final UsageType myType;

    public TextChunk( TextAttributes attributes,  String text) {
        this(attributes, text, null);
    }

    public TextChunk( TextAttributes attributes,  String text,  UsageType type) {
        myAttributes = attributes.getFlyweight();
        myText = text;
        myType = type;
    }

    
    public TextAttributes getAttributes() {
        return TextAttributes.fromFlyweight(myAttributes);
    }

    
    public String getText() {
        return myText;
    }

    public String toString() {
        return getText();
    }

    public  UsageType getType() {
        return myType;
    }

    
    public SimpleTextAttributes getSimpleAttributesIgnoreBackground() {
        SimpleTextAttributes simples = SimpleTextAttributes.fromTextAttributes(getAttributes());
        simples = new SimpleTextAttributes(null, simples.getFgColor(), simples.getWaveColor(), simples.getStyle());
        return simples;
    }
}
