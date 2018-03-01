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
package com.gome.maven.diff.tools.util;

import com.gome.maven.util.containers.SLRUMap;
import com.gome.maven.util.containers.SoftValueHashMap;

public class SoftHardCacheMap<K, V> {
     private final SLRUMap<K, V> mySLRUMap;
     private final SoftValueHashMap<K, V> mySoftLinkMap;

    public SoftHardCacheMap(final int protectedQueueSize, final int probationalQueueSize) {
        mySLRUMap = new SLRUMap<K, V>(protectedQueueSize, probationalQueueSize);
        mySoftLinkMap = new SoftValueHashMap<K, V>();
    }

    
    public V get( K key) {
        V val = mySLRUMap.get(key);
        if (val != null) return val;

        val = mySoftLinkMap.get(key);
        if (val != null) mySLRUMap.put(key, val);

        return val;
    }

    public void put( K key,  V value) {
        mySLRUMap.put(key, value);
        mySoftLinkMap.put(key, value);
    }

    public boolean remove( K key) {
        boolean remove1 = mySLRUMap.remove(key);
        boolean remove2 = mySoftLinkMap.remove(key) != null;
        return remove1 || remove2;
    }

    public void clear() {
        mySLRUMap.clear();
        mySoftLinkMap.clear();
    }
}