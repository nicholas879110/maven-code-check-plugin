package com.gome.maven.util.containers;

import java.util.Collection;
import java.util.List;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:23
 * @opyright(c) gome inc Gome Co.,LTD
 */

public interface ConcurrentList<E> extends List<E> {
    boolean addIfAbsent(E e);
    int addAllAbsent( Collection<? extends E> c);
}
