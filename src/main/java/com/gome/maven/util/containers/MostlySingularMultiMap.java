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

/*
 * @author max
 */
package com.gome.maven.util.containers;

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.Processor;
import gnu.trove.THashMap;

import java.io.Serializable;
import java.util.*;

public class MostlySingularMultiMap<K, V> implements Serializable {
    private static final long serialVersionUID = 2784448345881807109L;

    protected final Map<K, Object> myMap;

    public MostlySingularMultiMap() {
        myMap = createMap();
    }

    
    protected Map<K, Object> createMap() {
        return new THashMap<K, Object>();
    }

    public void add( K key,  V value) {
        Object current = myMap.get(key);
        if (current == null) {
            myMap.put(key, value);
        }
        else if (current instanceof Object[]) {
            Object[] curArr = (Object[])current;
            Object[] newArr = ArrayUtil.append(curArr, value, ArrayUtil.OBJECT_ARRAY_FACTORY);
            myMap.put(key, newArr);
        }
        else {
            myMap.put(key, new Object[]{current, value});
        }
    }

    public boolean remove( K key,  V value) {
        Object current = myMap.get(key);
        if (current == null) {
            return false;
        }
        if (current instanceof Object[]) {
            Object[] curArr = (Object[])current;
            Object[] newArr = ArrayUtil.remove(curArr, value, ArrayUtil.OBJECT_ARRAY_FACTORY);
            myMap.put(key, newArr);
            return newArr.length == curArr.length-1;
        }

        if (value.equals(current)) {
            myMap.remove(key);
            return true;
        }

        return false;
    }

    public boolean removeAllValues( K key) {
        return myMap.remove(key) != null;
    }

    
    public Set<K> keySet() {
        return myMap.keySet();
    }

    public boolean isEmpty() {
        return myMap.isEmpty();
    }

    public boolean processForKey( K key,  Processor<? super V> p) {
        return processValue(p, myMap.get(key));
    }

    private boolean processValue( Processor<? super V> p, Object v) {
        if (v instanceof Object[]) {
            for (Object o : (Object[])v) {
                if (!p.process((V)o)) return false;
            }
        }
        else if (v != null) {
            return p.process((V)v);
        }

        return true;
    }

    public boolean processAllValues( Processor<? super V> p) {
        for (Object v : myMap.values()) {
            if (!processValue(p, v)) return false;
        }

        return true;
    }

    public int size() {
        return myMap.size();
    }

    public boolean containsKey( K key) {
        return myMap.containsKey(key);
    }

    public int valuesForKey( K key) {
        Object current = myMap.get(key);
        if (current == null) return 0;
        if (current instanceof Object[]) return ((Object[])current).length;
        return 1;
    }

    
    public Iterable<V> get( K name) {
        final Object value = myMap.get(name);
        return rawValueToCollection(value);
    }

    
    protected List<V> rawValueToCollection(Object value) {
        if (value == null) return Collections.emptyList();

        if (value instanceof Object[]) {
            return (List<V>)Arrays.asList((Object[])value);
        }

        return Collections.singletonList((V)value);
    }

    public void compact() {
        ((THashMap)myMap).compact();
    }

    @Override
    public String toString() {
        return "{" + StringUtil.join(myMap.entrySet(), new Function<Map.Entry<K, Object>, String>() {
            @Override
            public String fun(Map.Entry<K, Object> entry) {
                Object value = entry.getValue();
                String s = (value instanceof Object[] ? Arrays.asList((Object[])value) : Arrays.asList(value)).toString();
                return entry.getKey() + ": " + s;
            }
        }, "; ") + "}";
    }

    public void clear() {
        myMap.clear();
    }

    
    public static <K,V> MostlySingularMultiMap<K,V> emptyMap() {
        //noinspection unchecked
        return EMPTY;
    }

    
    public static <K, V> MostlySingularMultiMap<K, V> newMap() {
        return new MostlySingularMultiMap<K, V>();
    }
    private static final MostlySingularMultiMap EMPTY = new EmptyMap();

    private static class EmptyMap extends MostlySingularMultiMap {
        @Override
        public void add( Object key,  Object value) {
            throw new IncorrectOperationException();
        }

        @Override
        public boolean remove( Object key,  Object value) {
            throw new IncorrectOperationException();
        }

        @Override
        public boolean removeAllValues( Object key) {
            throw new IncorrectOperationException();
        }

        @Override
        public void clear() {
            throw new IncorrectOperationException();
        }

        
        @Override
        public Set keySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean processForKey( Object key,  Processor p) {
            return true;
        }

        @Override
        public boolean processAllValues( Processor p) {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int valuesForKey( Object key) {
            return 0;
        }

        
        @Override
        public Iterable get( Object name) {
            return ContainerUtil.emptyList();
        }
    }
}
