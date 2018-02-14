package com.gome.maven.util.containers;

import java.util.Map;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:47
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class HashMap<K, V> extends java.util.HashMap<K, V> {
    public HashMap() { }

    public HashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public HashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public <K1 extends K, V1 extends V> HashMap(Map<? extends K1, ? extends V1> map) {
        super(map);
    }

    @Override
    public void clear() {
        if (size() == 0) return; // optimization
        super.clear();
    }
}
