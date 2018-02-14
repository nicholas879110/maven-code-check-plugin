package com.gome.maven.util.containers;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author zhangliewei
 * @date 2018/1/2 11:51
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class SequenceIterator<T> implements Iterator<T> {
    private final Iterator<T>[] myIterators;
    private int myCurrentIndex;

    public SequenceIterator( Iterator<T>... iterators){
        myIterators = new Iterator[iterators.length];
        System.arraycopy(iterators, 0, myIterators, 0, iterators.length);
    }
    public SequenceIterator( Collection<Iterator<T>> iterators) {
        this(iterators.toArray(new Iterator[iterators.size()]));
    }

    @Override
    public boolean hasNext(){
        for (int index = myCurrentIndex; index < myIterators.length; index++) {
            Iterator iterator = myIterators[index];
            if (iterator != null && iterator.hasNext()) {
                myCurrentIndex = index;
                return true;
            }
        }
        return false;
    }

    @Override
    public T next(){
        if(hasNext()) {
            return myIterators[myCurrentIndex].next();
        }
        throw new NoSuchElementException("Iterator has no more elements");
    }

    @Override
    public void remove(){
        if(myCurrentIndex >= myIterators.length){
            throw new IllegalStateException();
        }
        myIterators[myCurrentIndex].remove();
    }

    public static <T> SequenceIterator<T> create(Iterator<T> first, Iterator<T> second) {
        return new SequenceIterator<T>(new Iterator[]{first, second});
    }

    public static <T> SequenceIterator<T> create(Iterator<T> first, Iterator<T> second, Iterator<T> third) {
        return new SequenceIterator<T>(new Iterator[]{first, second, third});
    }
}
