package com.gome.maven.util.containers;

import java.util.Collection;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:55
 * @opyright(c) gome inc Gome Co.,LTD
 */
class LinkedHashSet<E> extends java.util.LinkedHashSet<E> {
    public LinkedHashSet() { }

    public LinkedHashSet(Collection<? extends E> collection) {
        super(collection);
    }

    public LinkedHashSet(int i, float v) {
        super(i, v);
    }

    public LinkedHashSet(int i) {
        super(i);
    }

    public void clear() {
        if (size() == 0) return; // optimization
        super.clear();
    }
}