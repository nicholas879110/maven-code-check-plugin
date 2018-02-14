package com.gome.maven.util.containers;

import com.gome.maven.util.ArrayUtilRt;
import com.gome.maven.util.Function;
import com.gome.maven.openapi.util.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:37
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class ContainerUtilRt {
    private static final int ARRAY_COPY_THRESHOLD = 20;

    
   
    public static <K, V> java.util.HashMap<K, V> newHashMap() {
        return new HashMap<K, V>();
    }

    
   
    public static <K, V> java.util.HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
        return new HashMap<K, V>(map);
    }

    
   
    public static <K, V> Map<K, V> newHashMap( List<K> keys,  List<V> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException(keys + " should have same length as " + values);
        }

        Map<K, V> map = newHashMap(keys.size());
        for (int i = 0; i < keys.size(); ++i) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    
   
    public static <K, V> Map<K, V> newHashMap(Pair<K, ? extends V> first, Pair<K, ? extends V>... entries) {
        Map<K, V> map = newHashMap(entries.length + 1);
        map.put(first.getFirst(), first.getSecond());
        for (Pair<K, ? extends V> entry : entries) {
            map.put(entry.getFirst(), entry.getSecond());
        }
        return map;
    }

    
   
    public static <K, V> Map<K, V> newHashMap(int initialCapacity) {
        return new HashMap<K, V>(initialCapacity);
    }

    
   
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<K, V>();
    }

    
   
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap( Map<K, V> map) {
        return new TreeMap<K, V>(map);
    }

    
   
    public static <K, V> java.util.LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<K, V>();
    }

    
   
    public static <K, V> java.util.LinkedHashMap<K, V> newLinkedHashMap(int capacity) {
        return new LinkedHashMap<K, V>(capacity);
    }

    
   
    public static <K, V> java.util.LinkedHashMap<K, V> newLinkedHashMap(Map<K, V> map) {
        return new LinkedHashMap<K, V>(map);
    }

    
   
    public static <K, V> java.util.LinkedHashMap<K, V> newLinkedHashMap(Pair<K, V> first, Pair<K, V>[] entries) {
        java.util.LinkedHashMap<K, V> map = newLinkedHashMap();
        map.put(first.getFirst(), first.getSecond());
        for (Pair<K, V> entry : entries) {
            map.put(entry.getFirst(), entry.getSecond());
        }
        return map;
    }

    
   
    public static <T> LinkedList<T> newLinkedList() {
        return new LinkedList<T>();
    }

    
   
    public static <T> LinkedList<T> newLinkedList( T... elements) {
        final LinkedList<T> list = newLinkedList();
        Collections.addAll(list, elements);
        return list;
    }

    
   
    public static <T> LinkedList<T> newLinkedList( Iterable<? extends T> elements) {
        return copy(ContainerUtilRt.<T>newLinkedList(), elements);
    }

    
   
    public static <T> ArrayList<T> newArrayList() {
        return new ArrayList<T>();
    }

    
   
    public static <T> ArrayList<T> newArrayList( T... elements) {
        ArrayList<T> list = newArrayListWithCapacity(elements.length);
        Collections.addAll(list, elements);
        return list;
    }

    
   
    public static <T> ArrayList<T> newArrayList( Iterable<? extends T> elements) {
        if (elements instanceof Collection) {
            @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
            return new ArrayList<T>(collection);
        }
        return copy(ContainerUtilRt.<T>newArrayList(), elements);
    }

    /** @deprecated Use {@link #newArrayListWithCapacity(int)} (to remove in IDEA 15) */
   
    public static <T> ArrayList<T> newArrayListWithExpectedSize(int size) {
        return newArrayListWithCapacity(size);
    }

    
   
    public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
        return new ArrayList<T>(size);
    }

    
    private static <T, C extends Collection<T>> C copy( C collection,  Iterable<? extends T> elements) {
        for (T element : elements) {
            collection.add(element);
        }
        return collection;
    }

    
   
    public static <T> java.util.HashSet<T> newHashSet() {
        return new HashSet<T>();
    }

    
   
    public static <T> java.util.HashSet<T> newHashSet(int initialCapacity) {
        return new HashSet<T>(initialCapacity);
    }

    
   
    public static <T> java.util.HashSet<T> newHashSet(T... elements) {
        return new HashSet<T>(Arrays.asList(elements));
    }

    
   
    public static <T> java.util.HashSet<T> newHashSet(Iterable<? extends T> elements) {
        if (elements instanceof Collection) {
            @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
            return new HashSet<T>(collection);
        }
        return newHashSet(elements.iterator());
    }

    
   
    public static <T> java.util.HashSet<T> newHashSet(Iterator<? extends T> iterator) {
        java.util.HashSet<T> set = newHashSet();
        while (iterator.hasNext()) set.add(iterator.next());
        return set;
    }

    
   
    public static <T> java.util.LinkedHashSet<T> newLinkedHashSet() {
        return new LinkedHashSet<T>();
    }

    
   
    public static <T> java.util.LinkedHashSet<T> newLinkedHashSet(T... elements) {
        return newLinkedHashSet(Arrays.asList(elements));
    }

    
   
    public static <T> java.util.LinkedHashSet<T> newLinkedHashSet(Iterable<? extends T> elements) {
        if (elements instanceof Collection) {
            @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
            return new LinkedHashSet<T>(collection);
        }
        return copy(ContainerUtilRt.<T>newLinkedHashSet(), elements);
    }

    
   
    public static <T> TreeSet<T> newTreeSet() {
        return new TreeSet<T>();
    }

    
   
    public static <T> TreeSet<T> newTreeSet( T... elements) {
        TreeSet<T> set = newTreeSet();
        Collections.addAll(set, elements);
        return set;
    }

    
   
    public static <T> TreeSet<T> newTreeSet( Iterable<? extends T> elements) {
        return copy(ContainerUtilRt.<T>newTreeSet(), elements);
    }

    
   
    public static <T> TreeSet<T> newTreeSet( Comparator<? super T> comparator) {
        return new TreeSet<T>(comparator);
    }

    
   
    public static <T> Stack<T> newStack() {
        return new Stack<T>();
    }

    
   
    public static <T> Stack<T> newStack( Collection<T> elements) {
        return new Stack<T>(elements);
    }

    
   
    public static <T> Stack<T> newStack( T... initial) {
        return new Stack<T>(Arrays.asList(initial));
    }

    /**
     * A variant of {@link java.util.Collections#emptyList()},
     * except that {@link #toArray()} here does not create garbage <code>new Object[0]</code> constantly.
     */
    private static class EmptyList<T> extends AbstractList<T> implements RandomAccess, Serializable {
        private static final long serialVersionUID = 1L;

        private static final EmptyList INSTANCE = new EmptyList();

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean contains(Object obj) {
            return false;
        }

        @Override
        public T get(int index) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        
        @Override
        public Object[] toArray() {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }

        
        @Override
        public <E> E[] toArray( E[] a) {
            if (a.length != 0) {
                a[0] = null;
            }
            return a;
        }

        
        @Override
        public Iterator<T> iterator() {
            return EmptyIterator.getInstance();
        }
    }

    
   
    public static <T> List<T> emptyList() {
        //noinspection unchecked
        return (List<T>)EmptyList.INSTANCE;
    }

    
   
    public static <T> CopyOnWriteArrayList<T> createEmptyCOWList() {
        // does not create garbage new Object[0]
        return new CopyOnWriteArrayList<T>(ContainerUtilRt.<T>emptyList());
    }

    public static <T> void addIfNotNull( T element,  Collection<T> result) {
        if (element != null) {
            result.add(element);
        }
    }

    public static <T> void addIfNotNull( Collection<T> result,  T element) {
        if (element != null) {
            result.add(element);
        }
    }

    /**
     * @return read-only list consisting of the elements from array converted by mapper
     */
    
   
    public static <T, V> List<V> map2List( T[] array,  Function<T, V> mapper) {
        return map2List(Arrays.asList(array), mapper);
    }

    /**
     * @return read-only list consisting of the elements from collection converted by mapper
     */
    
   
    public static <T, V> List<V> map2List( Collection<? extends T> collection,  Function<T, V> mapper) {
        if (collection.isEmpty()) return emptyList();
        List<V> list = new ArrayList<V>(collection.size());
        for (final T t : collection) {
            list.add(mapper.fun(t));
        }
        return list;
    }

    /**
     * @return read-only set consisting of the elements from collection converted by mapper
     */
    
   
    public static <T, V> Set<V> map2Set( T[] collection,  Function<T, V> mapper) {
        return map2Set(Arrays.asList(collection), mapper);
    }

    /**
     * @return read-only set consisting of the elements from collection converted by mapper
     */
    
   
    public static <T, V> Set<V> map2Set( Collection<? extends T> collection,  Function<T, V> mapper) {
        if (collection.isEmpty()) return Collections.emptySet();
        Set <V> set = new HashSet<V>(collection.size());
        for (final T t : collection) {
            set.add(mapper.fun(t));
        }
        return set;
    }

    
    public static <T> T[] toArray( List<T> collection,  T[] array) {
        final int length = array.length;
        if (length < ARRAY_COPY_THRESHOLD && array.length >= collection.size()) {
            for (int i = 0; i < collection.size(); i++) {
                array[i] = collection.get(i);
            }
            return array;
        }
        return collection.toArray(array);
    }

    /**
     * This is a replacement for {@link Collection#toArray(Object[])}. For small collections it is faster to stay at java level and refrain
     * from calling JNI {@link System#arraycopy(Object, int, Object, int, int)}
     */
    
    public static <T> T[] toArray( Collection<T> c,  T[] sample) {
        final int size = c.size();
        if (size == sample.length && size < ARRAY_COPY_THRESHOLD) {
            int i = 0;
            for (T t : c) {
                sample[i++] = t;
            }
            return sample;
        }

        return c.toArray(sample);
    }
}