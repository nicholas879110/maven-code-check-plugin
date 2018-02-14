package com.gome.maven.util.containers;

import com.gome.maven.reference.SoftReference;
import gnu.trove.TObjectHashingStrategy;

import java.lang.ref.ReferenceQueue;
import java.util.Map;

/**
 * @author zhangliewei
 * @date 2018/1/2 9:41
 * @opyright(c) gome inc Gome Co.,LTD
 */
public final class SoftHashMap<K,V> extends RefHashMap<K,V> {
    public SoftHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public SoftHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public SoftHashMap() {
        super();
    }

    public SoftHashMap( Map<K, V> t) {
        super(t);
    }

    public SoftHashMap( TObjectHashingStrategy<K> hashingStrategy) {
        super(hashingStrategy);
    }

    
    @Override
    protected <T> Key<T> createKey( T k,  ReferenceQueue<? super T> q) {
        return new SoftKey<T>(k, q);
    }

    private static class SoftKey<T> extends SoftReference<T> implements Key<T> {
        private final int myHash;  /* Hash code of key, stored here since the key may be tossed by the GC */

        private SoftKey( T k,  ReferenceQueue<? super T> q) {
            super(k, q);
            myHash = k.hashCode();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            if (myHash != o.hashCode()) return false;
            Object t = get();
            Object u = ((Key)o).get();
            if (t == null || u == null) return false;
            if (t == u) return true;
            return t.equals(u);
        }

        public int hashCode() {
            return myHash;
        }


        @Override
        public String toString() {
            return "SoftHashMap.SoftKey(" + get() + ")";
        }
    }
}
