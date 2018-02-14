package com.gome.maven.util.containers;

import com.gome.maven.reference.SoftReference;

import java.lang.ref.ReferenceQueue;
import java.util.*;

/**
 * @author zhangliewei
 * @date 2018/1/2 9:41
 * @opyright(c) gome inc Gome Co.,LTD
 */
public final class SoftKeySoftValueHashMap<K,V> implements Map<K,V> {
    private final SoftHashMap<K, ValueReference<K,V>> mySoftKeyMap = new SoftHashMap<K, ValueReference<K, V>>();
    private final ReferenceQueue<V> myQueue = new ReferenceQueue<V>();

    private static class ValueReference<K,V> extends SoftReference<V> {
        private final SoftHashMap.Key<K> key;

        private ValueReference(SoftHashMap.Key<K> key, V referent, ReferenceQueue<? super V> q) {
            super(referent, q);
            this.key = key;
        }
    }

    // returns true if some refs were tossed
    boolean processQueue() {
        boolean processed = mySoftKeyMap.processQueue();
        while(true) {
            ValueReference<K,V> ref = (ValueReference<K, V>)myQueue.poll();
            if (ref == null) break;
            SoftHashMap.Key<K> key = ref.key;
            mySoftKeyMap.removeKey(key);
            processed = true;
        }
        return processed;
    }

    @Override
    public V get(Object key) {
        ValueReference<K,V> ref = mySoftKeyMap.get(key);
        return SoftReference.dereference(ref);
    }

    @Override
    public V put(K key, V value) {
        processQueue();
        SoftHashMap.Key<K> softKey = mySoftKeyMap.createKey(key);
        ValueReference<K, V> reference = new ValueReference<K, V>(softKey, value, myQueue);
        ValueReference<K,V> oldRef = mySoftKeyMap.putKey(softKey, reference);
        return SoftReference.dereference(oldRef);
    }

    @Override
    public V remove(Object key) {
        processQueue();
        ValueReference<K,V> ref = mySoftKeyMap.remove(key);
        return SoftReference.dereference(ref);
    }

    @Override
    public void putAll( Map<? extends K, ? extends V> t) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void clear() {
        mySoftKeyMap.clear();
        processQueue();
    }

    @Override
    public int size() {
        return mySoftKeyMap.size(); //?
    }

    @Override
    public boolean isEmpty() {
        return mySoftKeyMap.isEmpty(); //?
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new RuntimeException("method not implemented");
    }

    
    @Override
    public Set<K> keySet() {
        return mySoftKeyMap.keySet();
    }

    
    @Override
    public Collection<V> values() {
        List<V> result = new ArrayList<V>();
        final Collection<ValueReference<K, V>> refs = mySoftKeyMap.values();
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
        throw new RuntimeException("method not implemented");
    }
}
