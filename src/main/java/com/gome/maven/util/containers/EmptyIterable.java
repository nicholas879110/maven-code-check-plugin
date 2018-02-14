package com.gome.maven.util.containers;

import java.util.Iterator;

/**
 * @author zhangliewei
 * @date 2018/1/2 11:45
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class EmptyIterable<T> implements Iterable<T> {
    private static final EmptyIterable INSTANCE = new EmptyIterable();

    public EmptyIterable() {
    }

    public static <T> EmptyIterable<T> getInstance() {
        return INSTANCE;
    }

    public Iterator<T> iterator() {
        return EmptyIterator.getInstance();
    }
}
