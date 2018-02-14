package com.gome.maven.util.containers;

import gnu.trove.TObjectHashingStrategy;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author zhangliewei
 * @date 2018/1/2 11:39
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class ConcurrentHashSet<K> implements Set<K> {
    private final ConcurrentMap<K, Boolean> map;

    public ConcurrentHashSet(int initialCapacity) {
        map = ContainerUtil.newConcurrentMap(initialCapacity);
    }
    public ConcurrentHashSet() {
        map = ContainerUtil.newConcurrentMap();
    }
    public ConcurrentHashSet( TObjectHashingStrategy<K> hashingStrategy) {
        map = ContainerUtil.newConcurrentMap(hashingStrategy);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    
    @Override
    public Iterator<K> iterator() {
        return map.keySet().iterator();
    }

    
    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    
    @Override
    public <T> T[] toArray( T[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public boolean add(K o) {
        return map.putIfAbsent(o, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.keySet().remove(o);
    }

    @Override
    public boolean containsAll( Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll( Collection<? extends K> c) {
        boolean ret = false;
        for (K o : c) {
            ret |= add(o);
        }

        return ret;
    }

    @Override
    public boolean retainAll( Collection<?> c) {
        return map.keySet().retainAll(c);
    }

    @Override
    public boolean removeAll( Collection<?> c) {
        return map.keySet().removeAll(c);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public String toString() {
        return map.keySet().toString();
    }
}
