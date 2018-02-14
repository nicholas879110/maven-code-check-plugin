package com.gome.maven.util.containers;

import com.gome.maven.util.IncorrectOperationException;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:19
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class SingletonIterator<T> extends SingletonIteratorBase<T> {
    private final T myElement;

    public SingletonIterator(T element) {
        myElement = element;
    }

    @Override
    protected void checkCoModification() {
    }

    @Override
    protected T getElement() {
        return myElement;
    }

    @Override
    public void remove() {
        throw new IncorrectOperationException();
    }
}
