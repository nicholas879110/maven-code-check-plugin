package com.gome.maven.util.containers;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:57
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class EmptyIterator<T> implements Iterator<T> {
    private static final EmptyIterator INSTANCE = new EmptyIterator();
    public static <T> EmptyIterator<T> getInstance() {
        //noinspection unchecked
        return INSTANCE;
    }
    public boolean hasNext() {
        return false;
    }

    public T next() {
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new IllegalStateException();
    }
}
