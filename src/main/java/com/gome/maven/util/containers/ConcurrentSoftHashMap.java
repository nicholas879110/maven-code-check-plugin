package com.gome.maven.util.containers;

import gnu.trove.TObjectHashingStrategy;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:28
 * @opyright(c) gome inc Gome Co.,LTD
 */
final class ConcurrentSoftHashMap<K, V> extends ConcurrentRefHashMap<K, V> {
    private static class SoftKey<K, V> extends SoftReference<K> implements KeyReference<K, V> {
        private final int myHash; // Hashcode of key, stored here since the key may be tossed by the GC
        private final TObjectHashingStrategy<K> myStrategy;
        private final V value;

        private SoftKey( K k, final int hash,  TObjectHashingStrategy<K> strategy, V v,  ReferenceQueue<K> q) {
            super(k, q);
            myStrategy = strategy;
            value = v;
            myHash = hash;
        }

        
        @Override
        public V getValue() {
            return value;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof KeyReference)) return false;
            K t = get();
            K u = ((KeyReference<K,V>)o).get();
            if (t == u) return true;
            if (t == null || u == null) return false;
            return myStrategy.equals(t, u);
        }

        public int hashCode() {
            return myHash;
        }
    }

    @Override
    protected KeyReference<K, V> createKeyReference( K key,  V value,  TObjectHashingStrategy<K> hashingStrategy) {
        return new SoftKey<K, V>(key, hashingStrategy.computeHashCode(key), hashingStrategy, value, myReferenceQueue);
    }

    public ConcurrentSoftHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ConcurrentSoftHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ConcurrentSoftHashMap() {
    }

    public ConcurrentSoftHashMap(int initialCapacity,
                                 float loadFactor,
                                 int concurrencyLevel,
                                  TObjectHashingStrategy<K> hashingStrategy) {
        super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
    }

    public ConcurrentSoftHashMap(Map<? extends K, ? extends V> t) {
        super(t);
    }

    public ConcurrentSoftHashMap( TObjectHashingStrategy<K> hashingStrategy) {
        super(hashingStrategy);
    }
}