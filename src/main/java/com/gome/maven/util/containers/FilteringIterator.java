package com.gome.maven.util.containers;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Conditions;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author zhangliewei
 * @date 2018/1/2 11:47
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class FilteringIterator<Dom, E extends Dom> implements Iterator<E> {
    private final Iterator<Dom> myBaseIterator;
    private final Condition<? super Dom> myFilter;
    private boolean myNextObtained = false;
    private boolean myCurrentIsValid = false;
    private Dom myCurrent;
    private Boolean myCurrentPassedFilter = null;
    public static final Condition NOT_NULL = new Condition() {
        @Override
        public boolean value(Object t) {
            return t != null;
        }
    };

    public FilteringIterator( Iterator<Dom> baseIterator, Condition<? super Dom> filter) {
        myBaseIterator = baseIterator;
        myFilter = filter;
    }

    private void obtainNext() {
        if (myNextObtained) return;
        boolean hasNext = myBaseIterator.hasNext();
        setCurrent(hasNext ? myBaseIterator.next() : null);

        myCurrentIsValid = hasNext;
        myNextObtained = true;
    }

    @Override
    public boolean hasNext() {
        obtainNext();
        if (!myCurrentIsValid) return false;
        boolean value = isCurrentPassesFilter();
        while (!value && myBaseIterator.hasNext()) {
            Dom next = myBaseIterator.next();
            setCurrent(next);
            value = isCurrentPassesFilter();
        }
        return value;
    }

    private void setCurrent(Dom next) {
        myCurrent = next;
        myCurrentPassedFilter = null;
    }

    private boolean isCurrentPassesFilter() {
        if (myCurrentPassedFilter != null) return myCurrentPassedFilter.booleanValue();
        boolean passed = myFilter.value(myCurrent);
        myCurrentPassedFilter = Boolean.valueOf(passed);
        return passed;
    }

    @Override
    public E next() {
        if (!hasNext()) throw new NoSuchElementException();
        E result = (E)myCurrent;
        myNextObtained = false;
        return result;
    }

    /**
     * Works after call {@link #next} until call {@link #hasNext}
     * @throws IllegalStateException if {@link #hasNext} called
     */
    @Override
    public void remove() {
        if (myNextObtained) throw new IllegalStateException();
        myBaseIterator.remove();
    }

    public static <T> Iterator<T> skipNulls(Iterator<T> iterator) {
        return create(iterator, NOT_NULL);
    }

    public static <Dom, T extends Dom> Iterator<T> create(Iterator<Dom> iterator, Condition<? super Dom> condition) {
        return new FilteringIterator<Dom, T>(iterator, condition);
    }

    public static <T> Condition<T> alwaysTrueCondition(Class<T> aClass) {
        return Conditions.alwaysTrue();
    }

    public static <T> InstanceOf<T> instanceOf(final Class<T> aClass) {
        return new InstanceOf<T>(aClass);
    }

    public static <T> Iterator<T> createInstanceOf(Iterator<?> iterator, Class<T> aClass) {
        return create((Iterator<T>)iterator, instanceOf(aClass));
    }

    public static class InstanceOf<T> implements Condition<Object> {
        private final Class<T> myInstancesClass;

        public InstanceOf(Class<T> instancesClass) {
            myInstancesClass = instancesClass;
        }

        @Override
        public boolean value(Object object) {
            return myInstancesClass.isInstance(object);
        }
    }
}
