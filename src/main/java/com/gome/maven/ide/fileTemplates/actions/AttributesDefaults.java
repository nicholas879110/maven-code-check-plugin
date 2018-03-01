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

package com.gome.maven.ide.fileTemplates.actions;

import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.util.containers.HashMap;

import java.util.Map;
import java.util.Properties;

/**
 * @author Roman Chernyatchik
 */
public class AttributesDefaults {
    private final String myDefaultName;
    private final TextRange myDefaultRange;
    private final Map<String, Pair<String, TextRange>> myNamesToValueAndRangeMap = new HashMap<String, Pair<String, TextRange>>();
    private Properties myDefaultProperties = null;
    private boolean myFixedName;

    public AttributesDefaults(  final String defaultName,
                               final TextRange defaultRange) {
        myDefaultName = defaultName;
        myDefaultRange = defaultRange;
    }

    public AttributesDefaults(  final String defaultName) {
        this(defaultName, null);
    }

    public AttributesDefaults() {
        this(null, null);
    }

    
    public String getDefaultFileName() {
        return myDefaultName;
    }
    
    public TextRange getDefaultFileNameSelection() {
        return myDefaultRange;
    }

    public void add(  final String attributeKey,
                      final String value,
                     final TextRange selectionRange) {
        myNamesToValueAndRangeMap.put(attributeKey, Pair.create(value, selectionRange));
    }

    public void add(  final String attributeKey,
                      final String value) {
        add(attributeKey, value, null);
    }

    public void addPredefined( String key,  String value) {
        if (myDefaultProperties == null) {
            myDefaultProperties = new Properties();
        }
        myDefaultProperties.setProperty(key, value);
    }

    public Properties getDefaultProperties() {
        return myDefaultProperties;
    }

    
    public TextRange getRangeFor(  final String attributeKey) {
        final Pair<String, TextRange> valueAndRange = myNamesToValueAndRangeMap.get(attributeKey);
        return valueAndRange == null ? null : valueAndRange.second;
    }

    
    public String getDefaultValueFor(  final String attributeKey) {
        final Pair<String, TextRange> valueAndRange = myNamesToValueAndRangeMap.get(attributeKey);
        return valueAndRange == null ? null : valueAndRange.first;
    }

    public boolean isFixedName() {
        return myFixedName;
    }

    public AttributesDefaults withFixedName(boolean fixedName) {
        myFixedName = fixedName;
        return this;
    }
}
