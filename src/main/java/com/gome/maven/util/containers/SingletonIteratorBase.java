package com.gome.maven.util.containers;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author zhangliewei
 * @date 2017/12/29 17:35
 * @opyright(c) gome inc Gome Co.,LTD
 */

public abstract class SingletonIteratorBase<T> implements Iterator<T> {
    private boolean myVisited;

    @Override
    public final boolean hasNext() {
        return !myVisited;
    }

    @Override
    public final T next() {
        if (myVisited) {
            throw new NoSuchElementException();
        }
        myVisited = true;
        checkCoModification();
        return getElement();
    }

    protected abstract void checkCoModification();

    protected abstract T getElement();
}
