/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.util;

import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.SingletonIterator;
import gnu.trove.TObjectHashingStrategy;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Read-only set consisting of the only element
 */
public class SingletonSet<E> implements Set<E> {
    private final E theElement;
     private final TObjectHashingStrategy<E> strategy;

    public SingletonSet(E e) {
        this(e, ContainerUtil.<E>canonicalStrategy());
    }

    public SingletonSet(E e,  final TObjectHashingStrategy<E> strategy) {
        theElement = e;
        this.strategy = strategy;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean contains(Object elem) {
        return strategy.equals(theElement, (E)elem);
    }

    
    @Override
    public Iterator<E> iterator() {
        return new SingletonIterator<E>(theElement);
    }

    
    @Override
    public Object[] toArray() {
        return new Object[]{theElement};
    }

    
    @Override
    public <T> T[] toArray( T[] a) {
        if (a.length == 0) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), 1);
        }
        a[0] = (T)theElement;
        if (a.length > 1) {
            a[1] = null;
        }
        return a;
    }

    @Override
    public boolean add(E t) {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean containsAll( Collection<?> c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll( Collection<? extends E> c) {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean retainAll( Collection<?> c) {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean removeAll( Collection<?> c) {
        throw new IncorrectOperationException();
    }

    @Override
    public void clear() {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
