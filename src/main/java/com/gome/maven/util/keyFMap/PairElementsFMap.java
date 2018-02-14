/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.util.keyFMap;

import com.gome.maven.openapi.util.Key;

class PairElementsFMap implements KeyFMap {
    private final Key key1;
    private final Key key2;
    private final Object value1;
    private final Object value2;

    PairElementsFMap( Key key1,  Object value1,  Key key2,  Object value2) {
        this.key1 = key1;
        this.value1 = value1;
        this.key2 = key2;
        this.value2 = value2;
        assert key1 != key2;
    }

    
    @Override
    public <V> KeyFMap plus( Key<V> key,  V value) {
        if (key == key1) return new PairElementsFMap(key, value, key2, value2);
        if (key == key2) return new PairElementsFMap(key, value, key1, value1);
        return new ArrayBackedFMap(new int[]{key1.hashCode(), key2.hashCode(), key.hashCode()}, new Object[]{value1, value2, value});
    }

    
    @Override
    public KeyFMap minus( Key<?> key) {
        if (key == key1) return new OneElementFMap<Object>(key2, value2);
        if (key == key2) return new OneElementFMap<Object>(key1, value1);
        return this;
    }

    @Override
    public <V> V get( Key<V> key) {
        //noinspection unchecked
        return key == key1 ? (V)value1 : key == key2 ? (V)value2 : null;
    }

    
    @Override
    public Key[] getKeys() {
        return new Key[] { key1, key2 };
    }

    @Override
    public String toString() {
        return "Pair: (" + key1 + " -> " + value1 + "; " + key2 + " -> " + value2 + ")";
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
