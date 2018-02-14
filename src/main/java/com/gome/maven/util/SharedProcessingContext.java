package com.gome.maven.util;

import com.gome.maven.openapi.util.Key;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashMap;

import java.util.Map;

/**
 * @author peter
 */
public class SharedProcessingContext {
    private final Map<Object, Object> myMap = ContainerUtil.newConcurrentMap();

    public Object get(  final String key) {
        return myMap.get(key);
    }

    public void put(  final String key,  final Object value) {
        myMap.put(key, value);
    }

    public <T> void put(Key<T> key, T value) {
        myMap.put(key, value);
    }

    public <T> T get(Key<T> key) {
        return (T)myMap.get(key);
    }

    
    public <T> T get( Key<T> key, Object element) {
        Map map = (Map)myMap.get(key);
        if (map == null) {
            return null;
        }
        else {
            return (T) map.get(element);
        }
    }

    public <T> void put( Key<T> key, Object element, T value) {
        Map map = (Map)myMap.get(key);
        if (map == null) {
            map = new THashMap();
            myMap.put(key, map);
        }
        map.put(element, value);
    }
}