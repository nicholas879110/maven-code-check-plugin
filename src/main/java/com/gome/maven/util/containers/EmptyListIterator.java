package com.gome.maven.util.containers;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:26
 * @opyright(c) gome inc Gome Co.,LTD
 * */
public class EmptyListIterator<E> extends EmptyIterator<E> implements ListIterator<E> {
private static final EmptyListIterator<Object> INSTANCE = new EmptyListIterator<Object>();

public static <E> EmptyListIterator<E> getInstance() {
//noinspection unchecked
return (EmptyListIterator<E>)INSTANCE;
}

public boolean hasPrevious() {
return false;
}

public E previous() {
throw new NoSuchElementException();
}

public int nextIndex() {
return 0;
}

public int previousIndex() {
return -1;
}

public void set(E e) {
throw new IllegalStateException();
}

public void add(E e) {
throw new UnsupportedOperationException();
}
}
