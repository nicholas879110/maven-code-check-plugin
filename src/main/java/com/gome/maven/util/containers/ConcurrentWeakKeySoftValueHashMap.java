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

/*
 * Created by IntelliJ IDEA.
 * User: Alexey
 * Date: 18.12.2006
 * Time: 20:18:31
 */
package com.gome.maven.util.containers;


import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Getter;
import gnu.trove.TObjectHashingStrategy;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import com.gome.maven.reference.SoftReference;

import static com.gome.maven.reference.SoftReference.deref;


/**
 * Concurrent map with weak keys and soft values.
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * @deprecated Use {@link ContainerUtil#createConcurrentWeakKeySoftValueMap(int, float, int, gnu.trove.TObjectHashingStrategy)} instead
 */
public class ConcurrentWeakKeySoftValueHashMap<K, V> implements ConcurrentMap<K, V> {
    private final ConcurrentMap<KeyReference<K,V>, ValueReference<K,V>> myMap;
    final ReferenceQueue<K> myKeyQueue = new ReferenceQueue<K>();
    final ReferenceQueue<V> myValueQueue = new ReferenceQueue<V>();
     final TObjectHashingStrategy<K> myHashingStrategy;

    public ConcurrentWeakKeySoftValueHashMap(int initialCapacity,
                                             float loadFactor,
                                             int concurrencyLevel,
                                              final TObjectHashingStrategy<K> hashingStrategy) {
        myHashingStrategy = hashingStrategy;
        myMap = ContainerUtil.newConcurrentMap(initialCapacity, loadFactor, concurrencyLevel, ContainerUtil.<KeyReference<K,V>>canonicalStrategy());
    }

    public interface KeyReference<K, V> extends Getter<K> {
        @Override
        K get();

        
        ValueReference<K,V> getValueReference(); // no strong references

        // MUST work even with gced references for the code in processQueue to work
        @Override
        boolean equals(Object o);

        @Override
        int hashCode();
    }

    public interface ValueReference<K, V> extends Getter<V> {
        
        KeyReference<K,V> getKeyReference(); // no strong references

        @Override
        V get();
    }

    static class WeakKey<K, V> extends WeakReference<K> implements KeyReference<K, V> {
        private final int myHash; // Hash code of the key, stored here since the key may be tossed by the GC
        private final TObjectHashingStrategy<K> myStrategy;
         private final ValueReference<K, V> myValueReference;

        WeakKey( K k,
                 ValueReference<K, V> valueReference,
                 TObjectHashingStrategy<K> strategy,
                 ReferenceQueue<K> queue) {
            super(k, queue);
            myValueReference = valueReference;
            myHash = strategy.computeHashCode(k);
            myStrategy = strategy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof KeyReference)) return false;
            K t = get();
            K other = ((KeyReference<K,V>)o).get();
            if (t == null || other == null) return false;
            if (t == other) return true;
            return myHash == o.hashCode() && myStrategy.equals(t, other);
        }
        @Override
        public int hashCode() {
            return myHash;
        }

        
        @Override
        public ValueReference<K, V> getValueReference() {
            return myValueReference;
        }
    }

    private static class SoftValue<K, V> extends SoftReference<V> implements ValueReference<K,V> {
         private volatile KeyReference<K, V> myKeyReference; // can't make it final because of circular dependency of KeyReference to ValueReference
        private final int myHash; // Hashcode of key, stored here since the key may be tossed by the GC
        private SoftValue( V value,  ReferenceQueue<V> queue) {
            super(value, queue);
            myHash = value.hashCode();
        }

        // MUST work with gced references too for the code in processQueue to work
        @Override
        public final boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null) return false;

            ValueReference that = (ValueReference)o;

            return myHash == that.hashCode() && Comparing.equal(get(), that.get());
        }

        @Override
        public final int hashCode() {
            return myHash;
        }

        
        @Override
        public KeyReference<K, V> getKeyReference() {
            return myKeyReference;
        }
    }

    
    protected KeyReference<K,V> createKeyReference( K k,  final V v) {
        final ValueReference<K, V> valueReference = createValueReference(v, myValueQueue);
        WeakKey<K, V> keyReference = new WeakKey<K, V>(k, valueReference, myHashingStrategy, myKeyQueue);
        if (valueReference instanceof SoftValue) {
            ((SoftValue)valueReference).myKeyReference = keyReference;
        }
        return keyReference;
    }

    
    protected ValueReference<K, V> createValueReference( V value,  ReferenceQueue<V> queue) {
        return new SoftValue<K, V>(value, queue);
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
    public void clear() {
        processQueues();
        myMap.clear();
    }

    /////////////////////////////
    private static class HardKey<K,V> implements KeyReference<K,V> {
        private K myKey;
        private int myHash;

        private void set( K key, int hash) {
            myKey = key;
            myHash = hash;
        }

        private void clear() {
            myKey = null;
        }

        @Override
        public K get() {
            return myKey;
        }

        @Override
        public boolean equals(Object o) {
            return o.equals(this); // see WeakKey.equals()
        }

        @Override
        public int hashCode() {
            return myHash;
        }

        
        @Override
        public ValueReference<K, V> getValueReference() {
            throw new UnsupportedOperationException();
        }
    }

    private static final ThreadLocal<HardKey> HARD_KEY = new ThreadLocal<HardKey>() {
        @Override
        protected HardKey initialValue() {
            return new HardKey();
        }
    };

    
    private HardKey<K,V> createHardKey(Object o) {
        @SuppressWarnings("unchecked") K key = (K)o;
        @SuppressWarnings("unchecked") HardKey<K,V> hardKey = HARD_KEY.get();
        hardKey.set(key, myHashingStrategy.computeHashCode(key));
        return hardKey;
    }

    ///////////////////////////////////

    @Override
    public V get( Object key) {
        HardKey<K,V> hardKey = createHardKey(key);
        ValueReference<K, V> valueReference = myMap.get(hardKey);
        V v = deref(valueReference);
        hardKey.clear();
        return v;
    }

    @Override
    public boolean containsKey(Object key) {
        HardKey<K,V> hardKey = createHardKey(key);
        boolean result = myMap.containsKey(hardKey);
        hardKey.clear();
        return result;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        processQueues();
        HardKey<K,V> hardKey = createHardKey(key);
        ValueReference<K, V> valueReference = myMap.remove(hardKey);
        V v = deref(valueReference);
        hardKey.clear();
        return v;
    }

    @Override
    public void putAll( Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V put(K key, V value) {
        processQueues();

        KeyReference<K, V> keyReference = createKeyReference(key, value);
        ValueReference<K,V> valueReference = keyReference.getValueReference();
        ValueReference<K, V> prevValReference = myMap.put(keyReference, valueReference);

        return deref(prevValReference);
    }

    boolean processQueues() {
        boolean removed = false;
        KeyReference<K,V> keyReference;
        while ((keyReference = (KeyReference<K, V>)myKeyQueue.poll()) != null) {
            ValueReference<K, V> valueReference = keyReference.getValueReference();
            removed |= myMap.remove(keyReference, valueReference);
        }

        ValueReference<K,V> valueReference;
        while ((valueReference = (ValueReference<K, V>)myValueQueue.poll()) != null) {
            keyReference = valueReference.getKeyReference();
            removed |= myMap.remove(keyReference, valueReference);
        }

        return removed;
    }

    
    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove( Object key,  Object value) {
        processQueues();

        HardKey<K, V> hardKey = createHardKey(key);
        ValueReference<K, V> valueReference = myMap.get(hardKey);
        V v = deref(valueReference);

        boolean result = value.equals(v) && myMap.remove(hardKey, valueReference);
        hardKey.clear();
        return result;
    }

    @Override
    public V putIfAbsent( K key,  V value) {
        processQueues();
        KeyReference<K, V> keyReference = createKeyReference(key, value);
        ValueReference<K, V> valueReference = keyReference.getValueReference();

        ValueReference<K, V> result = myMap.putIfAbsent(keyReference, valueReference);
        return deref(result);
    }

    @Override
    public boolean replace( K key,  V oldValue,  V newValue) {
        processQueues();
        KeyReference<K, V> oldKeyReference = createKeyReference(key, oldValue);
        ValueReference<K, V> oldValueReference = oldKeyReference.getValueReference();
        KeyReference<K, V> newKeyReference = createKeyReference(key, newValue);
        ValueReference<K, V> newValueReference = newKeyReference.getValueReference();

        return myMap.replace(oldKeyReference, oldValueReference, newValueReference);
    }

    @Override
    public V replace( K key,  V value) {
        processQueues();
        KeyReference<K, V> keyReference = createKeyReference(key, value);
        ValueReference<K, V> valueReference = keyReference.getValueReference();
        ValueReference<K, V> result = myMap.replace(keyReference, valueReference);
        return deref(result);
    }
}