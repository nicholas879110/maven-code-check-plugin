package com.gome.maven.util.containers;

import java.util.Map;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:53
 * @opyright(c) gome inc Gome Co.,LTD
 */
class LinkedHashMap<K, V> extends java.util.LinkedHashMap<K, V> {
    public LinkedHashMap() { }

    public LinkedHashMap(int i, float v) {
        super(i, v);
    }

    public LinkedHashMap(int i) {
        super(i);
    }

    public <K1 extends K, V1 extends V> LinkedHashMap(Map<K1, V1> map) {
        super(map);
    }

    public void clear() {
        if (size() == 0) return; // optimization
        super.clear();
    }
}
