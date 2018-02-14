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
package com.gome.maven.codeInsight.daemon;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Computable;
import gnu.trove.THashMap;

import java.util.Map;

public class HighlightDisplayKey {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.daemon.HighlightDisplayKey");

    private static final Map<String,HighlightDisplayKey> ourNameToKeyMap = new THashMap<String, HighlightDisplayKey>();
    private static final Map<String,HighlightDisplayKey> ourIdToKeyMap = new THashMap<String, HighlightDisplayKey>();
    private static final Map<HighlightDisplayKey, Computable<String>> ourKeyToDisplayNameMap = new THashMap<HighlightDisplayKey, Computable<String>>();
    private static final Map<HighlightDisplayKey, String> ourKeyToAlternativeIDMap = new THashMap<HighlightDisplayKey, String>();

    private final String myName;
    private final String myID;

    public static HighlightDisplayKey find(  final String name) {
        return ourNameToKeyMap.get(name);
    }

    
    public static HighlightDisplayKey findById(  final String id) {
        HighlightDisplayKey key = ourIdToKeyMap.get(id);
        if (key != null) return key;
        key = ourNameToKeyMap.get(id);
        if (key != null && key.getID().equals(id)) return key;
        return null;
    }

    
    public static HighlightDisplayKey register(  final String name) {
        if (find(name) != null) {
            LOG.info("Key with name \'" + name + "\' already registered");
            return null;
        }
        return new HighlightDisplayKey(name);
    }

    /**
     * @see #register(String, com.gome.maven.openapi.util.Computable)
     */
    
    public static HighlightDisplayKey register(  final String name,  final String displayName) {
        return register(name, displayName, name);
    }

    
    public static HighlightDisplayKey register(  final String name,  Computable<String> displayName) {
        return register(name, displayName, name);
    }


    /**
     * @see #register(String, com.gome.maven.openapi.util.Computable, String)
     */
    
    public static HighlightDisplayKey register(  final String name,
                                                final String displayName,
                                                 final String id) {
        return register(name, new Computable.PredefinedValueComputable<String>(displayName), id);
    }

    
    public static HighlightDisplayKey register(  final String name,
                                                final Computable<String> displayName,
                                                 final String id) {
        if (find(name) != null) {
            LOG.info("Key with name \'" + name + "\' already registered");
            return null;
        }
        HighlightDisplayKey highlightDisplayKey = new HighlightDisplayKey(name, id);
        ourKeyToDisplayNameMap.put(highlightDisplayKey, displayName);
        return highlightDisplayKey;
    }

    
    public static HighlightDisplayKey register(  final String name,
                                                final Computable<String> displayName,
                                                 final String id,
                                                 final String alternativeID) {
        final HighlightDisplayKey key = register(name, displayName, id);
        if (alternativeID != null) {
            ourKeyToAlternativeIDMap.put(key, alternativeID);
        }
        return key;
    }

    
    public static HighlightDisplayKey findOrRegister(  String name,  final String displayName) {
        return findOrRegister(name, displayName, null);
    }

    
    public static HighlightDisplayKey findOrRegister(  final String name,
                                                      final String displayName,
                                                       final String id) {
        HighlightDisplayKey key = find(name);
        if (key == null) {
            key = register(name, displayName, id != null ? id : name);
            assert key != null : name;
        }
        return key;
    }

    
    public static String getDisplayNameByKey( HighlightDisplayKey key) {
        if (key == null) {
            return null;
        }
        else {
            final Computable<String> computable = ourKeyToDisplayNameMap.get(key);
            return computable == null ? null : computable.compute();
        }
    }

    public static String getAlternativeID( HighlightDisplayKey key) {
        return ourKeyToAlternativeIDMap.get(key);
    }


    private HighlightDisplayKey(  final String name) {
        this(name, name);
    }

    public HighlightDisplayKey(  final String name,   final String ID) {
        myName = name;
        myID = ID;
        ourNameToKeyMap.put(myName, this);
        if (!Comparing.equal(ID, name)) {
            ourIdToKeyMap.put(ID, this);
        }
    }

    public String toString() {
        return myName;
    }

    
    public String getID(){
        return myID;
    }
}
