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
import com.gome.maven.util.ArrayUtil;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;

import static com.gome.maven.util.keyFMap.ArrayBackedFMap.getKeysByIndices;

class MapBackedFMap extends TIntObjectHashMap<Object> implements KeyFMap {
    private MapBackedFMap( MapBackedFMap oldMap, final int exclude) {
        super(oldMap.size());
        oldMap.forEachEntry(new TIntObjectProcedure<Object>() {
            @Override
            public boolean execute(int key, Object val) {
                if (key != exclude) put(key, val);
                assert key >= 0 : key;
                return true;
            }
        });
        assert size() > ArrayBackedFMap.ARRAY_THRESHOLD;
    }

    MapBackedFMap( int[] keys, int newKey,  Object[] values,  Object newValue) {
        super(keys.length + 1);
        for (int i = 0; i < keys.length; i++) {
            int key = keys[i];
            Object value = values[i];
            put(key, value);
            assert key >= 0 : key;
        }
        put(newKey, newValue);
        assert newKey >= 0 : newKey;
        assert size() > ArrayBackedFMap.ARRAY_THRESHOLD;
    }

    
    @Override
    public <V> KeyFMap plus( Key<V> key,  V value) {
        int keyCode = key.hashCode();
        assert keyCode >= 0 : key;
        @SuppressWarnings("unchecked")
        V oldValue = (V)get(keyCode);
        if (value == oldValue) return this;
        MapBackedFMap newMap = new MapBackedFMap(this, -1);
        newMap.put(keyCode, value);
        return newMap;
    }

    
    @Override
    public KeyFMap minus( Key<?> key) {
        int oldSize = size();
        int keyCode = key.hashCode();
        if (!containsKey(keyCode)) {
            return this;
        }
        if (oldSize == ArrayBackedFMap.ARRAY_THRESHOLD + 1) {
            int[] keys = keys();
            Object[] values = getValues();
            int i = ArrayUtil.indexOf(keys, keyCode);
            keys = ArrayUtil.remove(keys, i);
            values = ArrayUtil.remove(values, i);
            return new ArrayBackedFMap(keys, values);
        }
        return new MapBackedFMap(this, keyCode);
    }

    @Override
    public <V> V get( Key<V> key) {
        //noinspection unchecked
        return (V)get(key.hashCode());
    }

    
    @Override
    public Key[] getKeys() {
        return getKeysByIndices(keys());
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        forEachEntry(new TIntObjectProcedure<Object>() {
            @Override
            public boolean execute(int key, Object value) {
                s.append(s.length() == 0 ? "" : ", ").append(Key.getKeyByIndex(key)).append(" -> ").append(value);
                return true;
            }
        });
        return "[" + s.toString() + "]";
    }
}
