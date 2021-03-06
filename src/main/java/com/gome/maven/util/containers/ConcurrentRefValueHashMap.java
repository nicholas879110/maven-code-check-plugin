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

package com.gome.maven.util.containers;

import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.TObjectHashingStrategy;

import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class for concurrent strong key:K -> (soft/weak) value:V map
 * Null keys are NOT allowed
 * Null values are NOT allowed
 */
abstract class ConcurrentRefValueHashMap<K, V> implements ConcurrentMap<K, V> {
    private final ConcurrentMap<K, ValueReference<K, V>> myMap;
    protected final ReferenceQueue<V> myQueue = new ReferenceQueue<V>();

    public ConcurrentRefValueHashMap( Map<K, V> map) {
        this();
        putAll(map);
    }

    public ConcurrentRefValueHashMap() {
        myMap = ContainerUtil.newConcurrentMap();
    }

    public ConcurrentRefValueHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        myMap = ContainerUtil.newConcurrentMap(initialCapacity, loadFactor, concurrencyLevel);
    }

    public ConcurrentRefValueHashMap(int initialCapacity,
                                     float loadFactor,
                                     int concurrencyLevel,
                                      TObjectHashingStrategy<K> hashingStrategy) {
        myMap = ContainerUtil.newConcurrentMap(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
    }

    protected interface ValueReference<K, V> {
        
        K getKey();

        V get();
    }

    // returns true if some refs were tossed
    boolean processQueue() {
        boolean processed = false;

        while (true) {
            ValueReference<K, V> ref = (ValueReference<K, V>)myQueue.poll();
            if (ref == null) break;
            myMap.remove(ref.getKey(), ref);
            processed = true;
        }
        return processed;
    }

    @Override
    public V get( Object key) {
        ValueReference<K, V> ref = myMap.get(key);
        if (ref == null) return null;
        return ref.get();
    }

    @Override
    public V put( K key,  V value) {
        processQueue();
        ValueReference<K, V> oldRef = myMap.put(key, createValueReference(key, value));
        return oldRef != null ? oldRef.get() : null;
    }

    protected abstract ValueReference<K, V> createValueReference( K key,  V value);

    @Override
    public V putIfAbsent( K key,  V value) {
        ValueReference<K, V> newRef = createValueReference(key, value);
        while (true) {
            processQueue();
            ValueReference<K, V> oldRef = myMap.putIfAbsent(key, newRef);
            if (oldRef == null) return null;
            final V oldVal = oldRef.get();
            if (oldVal == null) {
                if (myMap.replace(key, oldRef, newRef)) return null;
            }
            else {
                return oldVal;
            }
        }
    }

    @Override
    public boolean remove( final Object key,  Object value) {
        processQueue();
        return myMap.remove(key, createValueReference((K)key, (V)value));
    }

    @Override
    public boolean replace( final K key,  final V oldValue,  final V newValue) {
        processQueue();
        return myMap.replace(key, createValueReference(key, oldValue), createValueReference(key, newValue));
    }

    @Override
    public V replace( final K key,  final V value) {
        processQueue();
        ValueReference<K, V> ref = myMap.replace(key, createValueReference(key, value));
        return ref == null ? null : ref.get();
    }

    @Override
    public V remove(Object key) {
        processQueue();
        ValueReference<K, V> ref = myMap.remove(key);
        return ref == null ? null : ref.get();
    }

    @Override
    public void putAll( Map<? extends K, ? extends V> t) {
        processQueue();
        for (K k : t.keySet()) {
            V v = t.get(k);
            if (v != null) {
                put(k, v);
            }
        }
    }

    @Override
    public void clear() {
        myMap.clear();
        processQueue();
    }

    @Override
    public int size() {
        return myMap.size();
    }

    @Override
    public boolean isEmpty() {
        return myMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public Set<K> keySet() {
        return myMap.keySet();
    }

    
    @Override
    public Collection<V> values() {
        List<V> result = new ArrayList<V>();
        final Collection<ValueReference<K, V>> refs = myMap.values();
        for (ValueReference<K, V> ref : refs) {
            final V value = ref.get();
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    
    @Override
    public Set<Entry<K, V>> entrySet() {
        final Set<K> keys = keySet();
        Set<Entry<K, V>> entries = new HashSet<Entry<K, V>>();

        for (final K key : keys) {
            final V value = get(key);
            if (value != null) {
                entries.add(new Entry<K, V>() {
                    @Override
                    public K getKey() {
                        return key;
                    }

                    @Override
                    public V getValue() {
                        return value;
                    }

                    @Override
                    public V setValue(V value) {
                        throw new UnsupportedOperationException("setValue is not implemented");
                    }
                });
            }
        }

        return entries;
    }

    @Override
    public String toString() {
        String s = "map size:" + size() + " [";
        for (K k : myMap.keySet()) {
            Object v = get(k);
            s += "'" + k + "': '" + v + "', ";
        }
        s += "] ";
        return s;
    }

    int underlyingMapSize() {
        return myMap.size();
    }
}
