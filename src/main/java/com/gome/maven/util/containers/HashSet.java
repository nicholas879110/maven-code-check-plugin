package com.gome.maven.util.containers;

import java.util.Collection;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:54
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class HashSet<E> extends java.util.HashSet<E> {
    public HashSet() { }

    public HashSet(Collection<? extends E> collection) {
        super(collection);
    }

    public HashSet(int i, float v) {
        super(i, v);
    }

    public HashSet(int i) {
        super(i);
    }

    public void clear() {
        if (size() == 0) return; // optimization
        super.clear();
    }
}
