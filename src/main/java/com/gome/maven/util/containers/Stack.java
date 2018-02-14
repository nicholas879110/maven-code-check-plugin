package com.gome.maven.util.containers;

import java.util.*;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:56
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class Stack<T> extends ArrayList<T> {
    public Stack() { }

    public Stack(int initialCapacity) {
        super(initialCapacity);
    }

    public Stack( Collection<T> init) {
        super(init);
    }

    public Stack( T... items) {
        for (T item : items) {
            push(item);
        }
    }

    public void push(T t) {
        add(t);
    }

    public T peek() {
        final int size = size();
        if (size == 0) throw new EmptyStackException();
        return get(size - 1);
    }

    public T pop() {
        final int size = size();
        if (size == 0) throw new EmptyStackException();
        return remove(size - 1);
    }


    public T tryPop() {
        return isEmpty() ? null : pop();
    }

    public boolean empty() {
        return isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RandomAccess && o instanceof List) {
            List other = (List)o;
            if (size() != other.size()) {
                return false;
            }

            for (int i = 0; i < other.size(); i++) {
                Object o1 = other.get(i);
                Object o2 = get(i);
                if (!(o1 == null ? o2 == null : o1.equals(o2))) {
                    return false;
                }
            }

            return true;
        }

        return super.equals(o);
    }
}
