package com.gome.maven.util.containers;


import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.*;
import com.gome.maven.util.*;
import com.gome.maven.openapi.util.text.StringUtil;
import gnu.trove.*;


import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhangliewei
 * @date 2017/12/29 17:26
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class ContainerUtil extends ContainerUtilRt {
    private static final int INSERTION_SORT_THRESHOLD = 10;
    private static final int DEFAULT_CONCURRENCY_LEVEL = Math.min(16, Runtime.getRuntime().availableProcessors());

    
    
    public static <T> T[] ar( T... elements) {
        return elements;
    }

    
    
    public static <K, V> java.util.HashMap<K, V> newHashMap() {
        return ContainerUtilRt.newHashMap();
    }

    
    
    public static <K, V> java.util.HashMap<K, V> newHashMap( Map<? extends K, ? extends V> map) {
        return ContainerUtilRt.newHashMap(map);
    }

    
    
    public static <K, V> Map<K, V> newHashMap(Pair<K, ? extends V> first, Pair<K, ? extends V>... entries) {
        return ContainerUtilRt.newHashMap(first, entries);
    }

    
    
    public static <K, V> Map<K, V> newHashMap( List<K> keys,  List<V> values) {
        return ContainerUtilRt.newHashMap(keys, values);
    }

    
    
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
        return ContainerUtilRt.newTreeMap();
    }

    
    
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap( Map<K, V> map) {
        return ContainerUtilRt.newTreeMap(map);
    }

    
    
    public static <K, V> java.util.LinkedHashMap<K, V> newLinkedHashMap() {
        return ContainerUtilRt.newLinkedHashMap();
    }

    
    
    public static <K, V> java.util.LinkedHashMap<K, V> newLinkedHashMap(int capacity) {
        return ContainerUtilRt.newLinkedHashMap(capacity);
    }

    
    
    public static <K, V> java.util.LinkedHashMap<K, V> newLinkedHashMap( Map<K, V> map) {
        return ContainerUtilRt.newLinkedHashMap(map);
    }

    
    
    public static <K, V> java.util.LinkedHashMap<K, V> newLinkedHashMap( Pair<K, V> first,  Pair<K, V>... entries) {
        return ContainerUtilRt.newLinkedHashMap(first, entries);
    }

    
    
    public static <K, V> THashMap<K, V> newTroveMap() {
        return new THashMap<K, V>();
    }

    
    
    public static <K, V> THashMap<K, V> newTroveMap( TObjectHashingStrategy<K> strategy) {
        return new THashMap<K, V>(strategy);
    }

    
    
    public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap( Class<K> keyType) {
        return new EnumMap<K, V>(keyType);
    }

    @SuppressWarnings("unchecked")
    public static <T> TObjectHashingStrategy<T> canonicalStrategy() {
        return TObjectHashingStrategy.CANONICAL;
    }

    @SuppressWarnings("unchecked")
    public static <T> TObjectHashingStrategy<T> identityStrategy() {
        return TObjectHashingStrategy.IDENTITY;
    }

    
    
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
        return new IdentityHashMap<K, V>();
    }

    
    
    public static <T> LinkedList<T> newLinkedList() {
        return ContainerUtilRt.newLinkedList();
    }

    
    
    public static <T> LinkedList<T> newLinkedList( T... elements) {
        return ContainerUtilRt.newLinkedList(elements);
    }

    
    
    public static <T> LinkedList<T> newLinkedList( Iterable<? extends T> elements) {
        return ContainerUtilRt.newLinkedList(elements);
    }

    
    
    public static <T> ArrayList<T> newArrayList() {
        return ContainerUtilRt.newArrayList();
    }

    
    
    public static <E> ArrayList<E> newArrayList( E... array) {
        return ContainerUtilRt.newArrayList(array);
    }

    
    
    public static <E> ArrayList<E> newArrayList( Iterable<? extends E> iterable) {
        return ContainerUtilRt.newArrayList(iterable);
    }

    /** @deprecated Use {@link #newArrayListWithCapacity(int)} (to remove in IDEA 15) */
    @SuppressWarnings("deprecation")
    
    public static <T> ArrayList<T> newArrayListWithExpectedSize(int size) {
        return ContainerUtilRt.newArrayListWithCapacity(size);
    }

    
    
    public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
        return ContainerUtilRt.newArrayListWithCapacity(size);
    }

    
    
    public static <T> List<T> newArrayList( final T[] elements, final int start, final int end) {
        if (start < 0 || start > end || end > elements.length) {
            throw new IllegalArgumentException("start:" + start + " end:" + end + " length:" + elements.length);
        }

        return new AbstractList<T>() {
            private final int size = end - start;

            @Override
            public T get(final int index) {
                if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index:" + index + " size:" + size);
                return elements[start + index];
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    
    
    public static <T> List<T> newUnmodifiableList(List<? extends T> originalList) {
        int size = originalList.size();
        if (size == 0) {
            return emptyList();
        }
        else if (size == 1) {
            return Collections.singletonList(originalList.get(0));
        }
        else {
            return Collections.unmodifiableList(newArrayList(originalList));
        }
    }


    
    
    public static <T> List<T> newSmartList() {
        return new SmartList<T>();
    }

    
    
    public static <T> List<T> newSmartList(T element) {
        return new SmartList<T>(element);
    }

    
    
    public static <T> List<T> newSmartList( T... elements) {
        return new SmartList<T>(elements);
    }

    
    
    public static <T> java.util.HashSet<T> newHashSet() {
        return ContainerUtilRt.newHashSet();
    }

    
    
    public static <T> java.util.HashSet<T> newHashSet(int initialCapacity) {
        return ContainerUtilRt.newHashSet(initialCapacity);
    }

    
    
    public static <T> java.util.HashSet<T> newHashSet( T... elements) {
        return ContainerUtilRt.newHashSet(elements);
    }

    
    
    public static <T> java.util.HashSet<T> newHashSet( Iterable<? extends T> iterable) {
        return ContainerUtilRt.newHashSet(iterable);
    }

    
    public static <T> java.util.HashSet<T> newHashSet( Iterator<? extends T> iterator) {
        return ContainerUtilRt.newHashSet(iterator);
    }

    
    
    public static <T> Set<T> newHashOrEmptySet( Iterable<? extends T> iterable) {
        boolean empty = iterable == null || iterable instanceof Collection && ((Collection)iterable).isEmpty();
        return empty ? Collections.<T>emptySet() : ContainerUtilRt.newHashSet(iterable);
    }

    
    
    public static <T> java.util.LinkedHashSet<T> newLinkedHashSet() {
        return ContainerUtilRt.newLinkedHashSet();
    }

    
    
    public static <T> java.util.LinkedHashSet<T> newLinkedHashSet( Iterable<? extends T> elements) {
        return ContainerUtilRt.newLinkedHashSet(elements);
    }

    
    
    public static <T> java.util.LinkedHashSet<T> newLinkedHashSet( T... elements) {
        return ContainerUtilRt.newLinkedHashSet(elements);
    }

    
    
    public static <T> THashSet<T> newTroveSet() {
        return new THashSet<T>();
    }

    
    
    public static <T> THashSet<T> newTroveSet( TObjectHashingStrategy<T> strategy) {
        return new THashSet<T>(strategy);
    }

    
    
    public static <T> THashSet<T> newTroveSet( T... elements) {
        return newTroveSet(Arrays.asList(elements));
    }

    
    
    public static <T> THashSet<T> newTroveSet( TObjectHashingStrategy<T> strategy,  T... elements) {
        return new THashSet<T>(Arrays.asList(elements), strategy);
    }

    
    
    public static <T> THashSet<T> newTroveSet( TObjectHashingStrategy<T> strategy,  Collection<T> elements) {
        return new THashSet<T>(elements, strategy);
    }

    
    
    public static <T> THashSet<T> newTroveSet( Collection<T> elements) {
        return new THashSet<T>(elements);
    }

    
    
    public static <K> THashSet<K> newIdentityTroveSet() {
        return new THashSet<K>(ContainerUtil.<K>identityStrategy());
    }

    
    
    public static <K> THashSet<K> newIdentityTroveSet(int initialCapacity) {
        return new THashSet<K>(initialCapacity, ContainerUtil.<K>identityStrategy());
    }
    
    
    public static <K> THashSet<K> newIdentityTroveSet( Collection<K> collection) {
        return new THashSet<K>(collection, ContainerUtil.<K>identityStrategy());
    }

    
    
    public static <K,V> THashMap<K,V> newIdentityTroveMap() {
        return new THashMap<K,V>(ContainerUtil.<K>identityStrategy());
    }

    
    
    public static <T> TreeSet<T> newTreeSet() {
        return ContainerUtilRt.newTreeSet();
    }

    
    
    public static <T> TreeSet<T> newTreeSet( Iterable<? extends T> elements) {
        return ContainerUtilRt.newTreeSet(elements);
    }

    
    
    public static <T> TreeSet<T> newTreeSet( T... elements) {
        return ContainerUtilRt.newTreeSet(elements);
    }

    
    
    public static <T> TreeSet<T> newTreeSet( Comparator<? super T> comparator) {
        return ContainerUtilRt.newTreeSet(comparator);
    }

    
    
    public static <T> Set<T> newConcurrentSet() {
        return new ConcurrentHashSet<T>();
    }

    
    
    public static <T> Set<T> newConcurrentSet( TObjectHashingStrategy<T> hashStrategy) {
        return new ConcurrentHashSet<T>(hashStrategy);
    }

    
    
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return CHM_FACTORY.createMap();
    }

    
    public static <K, V> ConcurrentMap<K,V> newConcurrentMap( TObjectHashingStrategy<K> hashStrategy) {
        return CHM_FACTORY.createMap(hashStrategy);
    }

    
    public static <K, V> ConcurrentMap<K,V> newConcurrentMap(int initialCapacity) {
        return CHM_FACTORY.createMap(initialCapacity);
    }

    
    public static <K, V> ConcurrentMap<K,V> newConcurrentMap(int initialCapacity, float loadFactor, int concurrencyLevel,  TObjectHashingStrategy<K> hashStrategy) {
        return CHM_FACTORY.createMap(initialCapacity, loadFactor, concurrencyLevel, hashStrategy);
    }

    
    public static <K, V> ConcurrentMap<K,V> newConcurrentMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        return CHM_FACTORY.createMap(initialCapacity, loadFactor, concurrencyLevel);
    }

    
    
    public static <E> List<E> reverse( final List<E> elements) {
        if (elements.isEmpty()) {
            return ContainerUtilRt.emptyList();
        }

        return new AbstractList<E>() {
            @Override
            public E get(int index) {
                return elements.get(elements.size() - 1 - index);
            }

            @Override
            public int size() {
                return elements.size();
            }
        };
    }

    
    
    public static <K, V> Map<K, V> union( Map<? extends K, ? extends V> map,  Map<? extends K, ? extends V> map2) {
        Map<K, V> result = new THashMap<K, V>(map.size() + map2.size());
        result.putAll(map);
        result.putAll(map2);
        return result;
    }

    
    
    public static <T> Set<T> union( Set<T> set,  Set<T> set2) {
        Set<T> result = new THashSet<T>(set.size() + set2.size());
        result.addAll(set);
        result.addAll(set2);
        return result;
    }

    
    
    public static <E> Set<E> immutableSet( E ... elements) {
        return Collections.unmodifiableSet(new THashSet<E>(Arrays.asList(elements)));
    }

    
    
    public static <E> ImmutableList<E> immutableList( E ... array) {
        return new ImmutableListBackedByArray<E>(array);
    }

    
    
    public static <E> ImmutableList<E> immutableList( List<E> list) {
        return new ImmutableListBackedByList<E>(list);
    }

    
    
    public static <K, V> ImmutableMapBuilder<K, V> immutableMapBuilder() {
        return new ImmutableMapBuilder<K, V>();
    }

    public static class ImmutableMapBuilder<K, V> {
        private final Map<K, V> myMap = new THashMap<K, V>();

        public ImmutableMapBuilder<K, V> put(K key, V value) {
            myMap.put(key, value);
            return this;
        }

        
        public Map<K, V> build() {
            return Collections.unmodifiableMap(myMap);
        }
    }

    private static class ImmutableListBackedByList<E> extends ImmutableList<E> {
        private final List<E> myStore;

        private ImmutableListBackedByList( List<E> list) {
            myStore = list;
        }

        @Override
        public E get(int index) {
            return myStore.get(index);
        }

        @Override
        public int size() {
            return myStore.size();
        }
    }

    private static class ImmutableListBackedByArray<E> extends ImmutableList<E> {
        private final E[] myStore;

        private ImmutableListBackedByArray( E[] array) {
            myStore = array;
        }

        @Override
        public E get(int index) {
            return myStore[index];
        }

        @Override
        public int size() {
            return myStore.length;
        }
    }

    
    
    public static <K, V> Map<K, V> intersection( Map<K, V> map1,  Map<K, V> map2) {
        final Map<K, V> res = newHashMap();
        final Set<K> keys = newHashSet();
        keys.addAll(map1.keySet());
        keys.addAll(map2.keySet());
        for (K k : keys) {
            V v1 = map1.get(k);
            V v2 = map2.get(k);
            if (v1 == v2 || v1 != null && v1.equals(v2)) {
                res.put(k, v1);
            }
        }
        return res;
    }

    
    
    public static <K, V> Map<K,Couple<V>> diff(Map<K, V> map1, Map<K, V> map2) {
        final Map<K, Couple<V>> res = newHashMap();
        final Set<K> keys = newHashSet();
        keys.addAll(map1.keySet());
        keys.addAll(map2.keySet());
        for (K k : keys) {
            V v1 = map1.get(k);
            V v2 = map2.get(k);
            if (!(v1 == v2 || v1 != null && v1.equals(v2))) {
                res.put(k, Couple.of(v1, v2));
            }
        }
        return res;
    }

    public static <T> boolean processSortedListsInOrder( List<T> list1,
                                                         List<T> list2,
                                                         Comparator<? super T> comparator,
                                                        boolean mergeEqualItems,
                                                         Processor<T> processor) {
        int index1 = 0;
        int index2 = 0;
        while (index1 < list1.size() || index2 < list2.size()) {
            T e;
            if (index1 >= list1.size()) {
                e = list2.get(index2++);
            }
            else if (index2 >= list2.size()) {
                e = list1.get(index1++);
            }
            else {
                T element1 = list1.get(index1);
                T element2 = list2.get(index2);
                int c = comparator.compare(element1, element2);
                if (c <= 0) {
                    e = element1;
                    index1++;
                }
                else {
                    e = element2;
                    index2++;
                }
                if (c == 0 && !mergeEqualItems) {
                    if (!processor.process(e)) return false;
                    index2++;
                    e = element2;
                }
            }
            if (!processor.process(e)) return false;
        }

        return true;
    }

    
    
    public static <T> List<T> mergeSortedLists( List<T> list1,
                                                List<T> list2,
                                                Comparator<? super T> comparator,
                                               boolean mergeEqualItems) {
        final List<T> result = new ArrayList<T>(list1.size() + list2.size());
        processSortedListsInOrder(list1, list2, comparator, mergeEqualItems, new Processor<T>() {
            @Override
            public boolean process(T t) {
                result.add(t);
                return true;
            }
        });
        return result;
    }

    
    
    public static <T> List<T> mergeSortedArrays( T[] list1,  T[] list2,  Comparator<? super T> comparator, boolean mergeEqualItems,  Processor<? super T> filter) {
        int index1 = 0;
        int index2 = 0;
        List<T> result = new ArrayList<T>(list1.length + list2.length);

        while (index1 < list1.length || index2 < list2.length) {
            if (index1 >= list1.length) {
                T t = list2[index2++];
                if (filter != null && !filter.process(t)) continue;
                result.add(t);
            }
            else if (index2 >= list2.length) {
                T t = list1[index1++];
                if (filter != null && !filter.process(t)) continue;
                result.add(t);
            }
            else {
                T element1 = list1[index1];
                if (filter != null && !filter.process(element1)) {
                    index1++;
                    continue;
                }
                T element2 = list2[index2];
                if (filter != null && !filter.process(element2)) {
                    index2++;
                    continue;
                }
                int c = comparator.compare(element1, element2);
                if (c < 0) {
                    result.add(element1);
                    index1++;
                }
                else if (c > 0) {
                    result.add(element2);
                    index2++;
                }
                else {
                    result.add(element1);
                    if (!mergeEqualItems) {
                        result.add(element2);
                    }
                    index1++;
                    index2++;
                }
            }
        }

        return result;
    }

    
    
    public static <T> List<T> subList( List<T> list, int from) {
        return list.subList(from, list.size());
    }

    public static <T> void addAll( Collection<T> collection,  Iterable<? extends T> appendix) {
        addAll(collection, appendix.iterator());
    }

    public static <T> void addAll( Collection<T> collection,  Iterator<? extends T> iterator) {
        while (iterator.hasNext()) {
            T o = iterator.next();
            collection.add(o);
        }
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    public static <T> void addAllNotNull( Collection<T> collection,  Iterable<? extends T> elements) {
        addAllNotNull(collection, elements.iterator());
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    public static <T> void addAllNotNull( Collection<T> collection,  Iterator<? extends T> elements) {
        while (elements.hasNext()) {
            T o = elements.next();
            if (o != null) {
                collection.add(o);
            }
        }
    }

    
    public static <T> List<T> collect( Iterator<T> iterator) {
        if (!iterator.hasNext()) return emptyList();
        List<T> list = new ArrayList<T>();
        addAll(list, iterator);
        return list;
    }

    
    public static <T> Set<T> collectSet( Iterator<T> iterator) {
        if (!iterator.hasNext()) return Collections.emptySet();
        Set<T> hashSet = newHashSet();
        addAll(hashSet, iterator);
        return hashSet;
    }

    
    public static <K, V> Map<K, V> newMapFromKeys( Iterator<K> keys,  Convertor<K, V> valueConvertor) {
        Map<K, V> map = newHashMap();
        while (keys.hasNext()) {
            K key = keys.next();
            map.put(key, valueConvertor.convert(key));
        }
        return map;
    }

    
    public static <K, V> Map<K, V> newMapFromValues( Iterator<V> values,  Convertor<V, K> keyConvertor) {
        Map<K, V> map = newHashMap();
        while (values.hasNext()) {
            V value = values.next();
            map.put(keyConvertor.convert(value), value);
        }
        return map;
    }

    
    public static <K, V> Map<K, Set<V>> classify( Iterator<V> iterator,  Convertor<V, K> keyConvertor) {
        Map<K, Set<V>> hashMap = new LinkedHashMap<K, Set<V>>();
        while (iterator.hasNext()) {
            V value = iterator.next();
            final K key = keyConvertor.convert(value);
            Set<V> set = hashMap.get(key);
            if (set == null) {
                hashMap.put(key, set = new java.util.LinkedHashSet<V>()); // ordered set!!
            }
            set.add(value);
        }
        return hashMap;
    }

    
    
    public static <T> Iterator<T> emptyIterator() {
        return EmptyIterator.getInstance();
    }

    
    
    public static <T> Iterable<T> emptyIterable() {
        return EmptyIterable.getInstance();
    }

    
    
    public static <T> T find( T[] array,  Condition<T> condition) {
        for (T element : array) {
            if (condition.value(element)) return element;
        }
        return null;
    }

    public static <T> boolean process( Iterable<? extends T> iterable,  Processor<T> processor) {
        for (final T t : iterable) {
            if (!processor.process(t)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean process( List<? extends T> list,  Processor<T> processor) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, size = list.size(); i < size; i++) {
            T t = list.get(i);
            if (!processor.process(t)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean process( T[] iterable,  Processor<? super T> processor) {
        for (final T t : iterable) {
            if (!processor.process(t)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean process( Iterator<T> iterator,  Processor<? super T> processor) {
        while (iterator.hasNext()) {
            if (!processor.process(iterator.next())) {
                return false;
            }
        }
        return true;
    }

    
    
    public static <T, V extends T> V find( Iterable<V> iterable,  Condition<T> condition) {
        return find(iterable.iterator(), condition);
    }

    
    
    public static <T> T find( Iterable<? extends T> iterable,  final T equalTo) {
        return find(iterable, new Condition<T>() {
            @Override
            public boolean value(final T object) {
                return equalTo == object || equalTo.equals(object);
            }
        });
    }

    
    public static <T, V extends T> V find( Iterator<V> iterator,  Condition<T> condition) {
        while (iterator.hasNext()) {
            V value = iterator.next();
            if (condition.value(value)) return value;
        }
        return null;
    }

    
    
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2Map( T[] collection,  Function<T, Pair<KEY, VALUE>> mapper) {
        return map2Map(Arrays.asList(collection), mapper);
    }

    
    
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2Map( Collection<? extends T> collection,
                                                           Function<T, Pair<KEY, VALUE>> mapper) {
        final Map<KEY, VALUE> set = new THashMap<KEY, VALUE>(collection.size());
        for (T t : collection) {
            Pair<KEY, VALUE> pair = mapper.fun(t);
            set.put(pair.first, pair.second);
        }
        return set;
    }

    
    
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2MapNotNull( T[] collection,
                                                                  Function<T, Pair<KEY, VALUE>> mapper) {
        return map2MapNotNull(Arrays.asList(collection), mapper);
    }

    
    
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2MapNotNull( Collection<? extends T> collection,
                                                                  Function<T, Pair<KEY, VALUE>> mapper) {
        final Map<KEY, VALUE> set = new THashMap<KEY, VALUE>(collection.size());
        for (T t : collection) {
            Pair<KEY, VALUE> pair = mapper.fun(t);
            if (pair != null) {
                set.put(pair.first, pair.second);
            }
        }
        return set;
    }

    
    
    public static <KEY, VALUE> Map<KEY, VALUE> map2Map( Collection<Pair<KEY, VALUE>> collection) {
        final Map<KEY, VALUE> result = new THashMap<KEY, VALUE>(collection.size());
        for (Pair<KEY, VALUE> pair : collection) {
            result.put(pair.first, pair.second);
        }
        return result;
    }

    
    
    public static <T> Object[] map2Array( T[] array,  Function<T, Object> mapper) {
        return map2Array(array, Object.class, mapper);
    }

    
    
    public static <T> Object[] map2Array( Collection<T> array,  Function<T, Object> mapper) {
        return map2Array(array, Object.class, mapper);
    }

    
    
    public static <T, V> V[] map2Array( T[] array,  Class<? super V> aClass,  Function<T, V> mapper) {
        return map2Array(Arrays.asList(array), aClass, mapper);
    }

    
    
    public static <T, V> V[] map2Array( Collection<? extends T> collection,  Class<? super V> aClass,  Function<T, V> mapper) {
        final List<V> list = map2List(collection, mapper);
        @SuppressWarnings("unchecked") V[] array = (V[]) Array.newInstance(aClass, list.size());
        return list.toArray(array);
    }

    
    
    public static <T, V> V[] map2Array( Collection<? extends T> collection,  V[] to,  Function<T, V> mapper) {
        return map2List(collection, mapper).toArray(to);
    }

    
    
    public static <T> List<T> filter( T[] collection,  Condition<? super T> condition) {
        return findAll(collection, condition);
    }

    
    
    public static int[] filter( int[] collection,  TIntProcedure condition) {
        TIntArrayList result = new TIntArrayList();
        for (int t : collection) {
            if (condition.execute(t)) {
                result.add(t);
            }
        }
        return result.isEmpty() ? ArrayUtil.EMPTY_INT_ARRAY : result.toNativeArray();
    }

    
    
    public static <T> List<T> filter( Condition<? super T> condition,  T... collection) {
        return findAll(collection, condition);
    }

    
    
    public static <T> List<T> findAll( T[] collection,  Condition<? super T> condition) {
        final List<T> result = new SmartList<T>();
        for (T t : collection) {
            if (condition.value(t)) {
                result.add(t);
            }
        }
        return result;
    }

    
    
    public static <T> List<T> filter( Collection<? extends T> collection,  Condition<? super T> condition) {
        return findAll(collection, condition);
    }

    
    
    public static <K, V> Map<K, V> filter( Map<K, ? extends V> map,  Condition<? super K> keyFilter) {
        Map<K, V> result = newHashMap();
        for (Map.Entry<K, ? extends V> entry : map.entrySet()) {
            if (keyFilter.value(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    
    
    public static <T> List<T> findAll( Collection<? extends T> collection,  Condition<? super T> condition) {
        if (collection.isEmpty()) return emptyList();
        final List<T> result = new SmartList<T>();
        for (final T t : collection) {
            if (condition.value(t)) {
                result.add(t);
            }
        }
        return result;
    }

    
    
    public static <T> List<T> skipNulls( Collection<? extends T> collection) {
        return findAll(collection, Condition.NOT_NULL);
    }

    
    
    public static <T, V> List<V> findAll( T[] collection,  Class<V> instanceOf) {
        return findAll(Arrays.asList(collection), instanceOf);
    }

    
    
    public static <T, V> V[] findAllAsArray( T[] collection,  Class<V> instanceOf) {
        List<V> list = findAll(Arrays.asList(collection), instanceOf);
        @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(instanceOf, list.size());
        return list.toArray(array);
    }

    
    
    public static <T, V> V[] findAllAsArray( Collection<? extends T> collection,  Class<V> instanceOf) {
        List<V> list = findAll(collection, instanceOf);
        @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(instanceOf, list.size());
        return list.toArray(array);
    }

    
    
    public static <T> T[] findAllAsArray( T[] collection,  Condition<? super T> instanceOf) {
        List<T> list = findAll(collection, instanceOf);
        @SuppressWarnings("unchecked") T[] array = (T[])Array.newInstance(collection.getClass().getComponentType(), list.size());
        return list.toArray(array);
    }

    
    
    public static <T, V> List<V> findAll( Collection<? extends T> collection,  Class<V> instanceOf) {
        final List<V> result = new SmartList<V>();
        for (final T t : collection) {
            if (instanceOf.isInstance(t)) {
                @SuppressWarnings("unchecked") V v = (V)t;
                result.add(v);
            }
        }
        return result;
    }

    public static <T> void removeDuplicates( Collection<T> collection) {
        Set<T> collected = newHashSet();
        for (Iterator<T> iterator = collection.iterator(); iterator.hasNext();) {
            T t = iterator.next();
            if (!collected.contains(t)) {
                collected.add(t);
            }
            else {
                iterator.remove();
            }
        }
    }

    
    
    public static Map<String, String> stringMap( final String... keyValues) {
        final Map<String, String> result = newHashMap();
        for (int i = 0; i < keyValues.length - 1; i+=2) {
            result.put(keyValues[i], keyValues[i+1]);
        }

        return result;
    }

    
    
    public static <T> Iterator<T> iterate( T[] arrays) {
        return Arrays.asList(arrays).iterator();
    }

    
    
    public static <T> Iterator<T> iterate( final Enumeration<T> enumeration) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }

            @Override
            public T next() {
                return enumeration.nextElement();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    
    
    public static <T> Iterable<T> iterate( T[] arrays,  Condition<? super T> condition) {
        return iterate(Arrays.asList(arrays), condition);
    }

    
    
    public static <T> Iterable<T> iterate( final Collection<? extends T> collection,  final Condition<? super T> condition) {
        if (collection.isEmpty()) return emptyIterable();
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private Iterator<? extends T> impl = collection.iterator();
                    private T next = findNext();

                    @Override
                    public boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public T next() {
                        T result = next;
                        next = findNext();
                        return result;
                    }

                    
                    private T findNext() {
                        while (impl.hasNext()) {
                            T each = impl.next();
                            if (condition.value(each)) {
                                return each;
                            }
                        }
                        return null;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    
    
    public static <T> Iterable<T> iterateBackward( final List<? extends T> list) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private ListIterator<? extends T> it = list.listIterator(list.size());

                    @Override
                    public boolean hasNext() {
                        return it.hasPrevious();
                    }

                    @Override
                    public T next() {
                        return it.previous();
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
    }

    public static <E> void swapElements( List<E> list, int index1, int index2) {
        E e1 = list.get(index1);
        E e2 = list.get(index2);
        list.set(index1, e2);
        list.set(index2, e1);
    }

    
    public static <T> List<T> collect( Iterator<?> iterator,  FilteringIterator.InstanceOf<T> instanceOf) {
        @SuppressWarnings("unchecked") List<T> list = collect(FilteringIterator.create((Iterator<T>)iterator, instanceOf));
        return list;
    }

    public static <T> void addAll( Collection<T> collection,  Enumeration<? extends T> enumeration) {
        while (enumeration.hasMoreElements()) {
            T element = enumeration.nextElement();
            collection.add(element);
        }
    }

    
    public static <T, A extends T, C extends Collection<T>> C addAll( C collection,  A... elements) {
        //noinspection ManualArrayToCollectionCopy
        for (T element : elements) {
            collection.add(element);
        }
        return collection;
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    
    public static <T, A extends T, C extends Collection<T>> C addAllNotNull( C collection,  A... elements) {
        for (T element : elements) {
            if (element != null) {
                collection.add(element);
            }
        }
        return collection;
    }

    public static <T> boolean removeAll( Collection<T> collection,  T... elements) {
        boolean modified = false;
        for (T element : elements) {
            modified |= collection.remove(element);
        }
        return modified;
    }

    // returns true if the collection was modified
    public static <T> boolean retainAll( Collection<T> collection,  Condition<? super T> condition) {
        boolean modified = false;

        for (Iterator<T> iterator = collection.iterator(); iterator.hasNext(); ) {
            T next = iterator.next();
            if (!condition.value(next)) {
                iterator.remove();
                modified = true;
            }
        }

        return modified;
    }

    
    public static <T, U extends T> U findInstance( Iterable<T> iterable,  Class<U> aClass) {
        return findInstance(iterable.iterator(), aClass);
    }

    public static <T, U extends T> U findInstance( Iterator<T> iterator,  Class<U> aClass) {
        //noinspection unchecked
        U u = (U)find(iterator, FilteringIterator.instanceOf(aClass));
        return u;
    }

    
    
    public static <T, U extends T> U findInstance( T[] array,  Class<U> aClass) {
        return findInstance(Arrays.asList(array), aClass);
    }

    
    
    public static <T, V> List<T> concat( V[] array,  Function<V, Collection<? extends T>> fun) {
        return concat(Arrays.asList(array), fun);
    }

    /**
     * @return read-only list consisting of the elements from the collections stored in list added together
     */
    
    
    public static <T> List<T> concat( Iterable<? extends Collection<T>> list) {
        List<T> result = new ArrayList<T>();
        for (final Collection<T> ts : list) {
            result.addAll(ts);
        }
        return result.isEmpty() ? Collections.<T>emptyList() : result;
    }

    /**
     * @deprecated Use {@link #append(java.util.List, java.lang.Object[])} or {@link #prepend(java.util.List, java.lang.Object[])} instead
     * @param appendTail specify whether additional values should be appended in front or after the list
     * @return read-only list consisting of the elements from specified list with some additional values
     */
    @Deprecated
    
    
    public static <T> List<T> concat(boolean appendTail,  List<? extends T> list,  T... values) {
        return appendTail ? concat(list, list(values)) : concat(list(values), list);
    }

    
    
    public static <T> List<T> append( List<? extends T> list,  T... values) {
        return concat(list, list(values));
    }

    /**
     * prepend values in front of the list
     * @return read-only list consisting of values and the elements from specified list
     */
    
    
    public static <T> List<T> prepend( List<? extends T> list,  T... values) {
        return concat(list(values), list);
    }

    /**
     * @return read-only list consisting of the two lists added together
     */
    
    
    public static <T> List<T> concat( final List<? extends T> list1,  final List<? extends T> list2) {
        if (list1.isEmpty() && list2.isEmpty()) {
            return Collections.emptyList();
        }
        if (list1.isEmpty()) {
            //noinspection unchecked
            return (List<T>)list2;
        }
        if (list2.isEmpty()) {
            //noinspection unchecked
            return (List<T>)list1;
        }

        final int size1 = list1.size();
        final int size = size1 + list2.size();

        return new AbstractList<T>() {
            @Override
            public T get(int index) {
                if (index < size1) {
                    return list1.get(index);
                }

                return list2.get(index - size1);
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    
    
    public static <T> Iterable<T> concat( final Iterable<? extends T>... iterables) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                Iterator[] iterators = new Iterator[iterables.length];
                for (int i = 0; i < iterables.length; i++) {
                    Iterable<? extends T> iterable = iterables[i];
                    iterators[i] = iterable.iterator();
                }
                @SuppressWarnings("unchecked") Iterator<T> i = concatIterators(iterators);
                return i;
            }
        };
    }

    
    
    public static <T> Iterator<T> concatIterators( Iterator<T>... iterators) {
        return new SequenceIterator<T>(iterators);
    }

    
    
    public static <T> Iterator<T> concatIterators( Collection<Iterator<T>> iterators) {
        return new SequenceIterator<T>(iterators);
    }

    
    
    public static <T> Iterable<T> concat( final T[]... iterables) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                Iterator[] iterators = new Iterator[iterables.length];
                for (int i = 0, iterablesLength = iterables.length; i < iterablesLength; i++) {
                    T[] iterable = iterables[i];
                    iterators[i] = Arrays.asList(iterable).iterator();
                }
                @SuppressWarnings("unchecked") Iterator<T> i = concatIterators(iterators);
                return i;
            }
        };
    }

    /**
     * @return read-only list consisting of the lists added together
     */
    
    
    public static <T> List<T> concat( final List<? extends T>... lists) {
        int size = 0;
        for (List<? extends T> each : lists) {
            size += each.size();
        }
        if (size == 0) return emptyList();
        final int finalSize = size;
        return new AbstractList<T>() {
            @Override
            public T get(final int index) {
                if (index >= 0 && index < finalSize) {
                    int from = 0;
                    for (List<? extends T> each : lists) {
                        if (from <= index && index < from + each.size()) return each.get(index - from);
                        from += each.size();
                    }
                }
                throw new IndexOutOfBoundsException("index: " + index + "size: " + size());
            }

            @Override
            public int size() {
                return finalSize;
            }
        };
    }

    /**
     * @return read-only list consisting of the lists added together
     */
    
    
    public static <T> List<T> concat( final List<List<? extends T>> lists) {
        @SuppressWarnings("unchecked") List<? extends T>[] array = lists.toArray(new List[lists.size()]);
        return concat(array);
    }

    /**
     * @return read-only list consisting of the lists (made by listGenerator) added together
     */
    
    
    public static <T, V> List<T> concat( Iterable<? extends V> list,  Function<V, Collection<? extends T>> listGenerator) {
        List<T> result = new ArrayList<T>();
        for (final V v : list) {
            result.addAll(listGenerator.fun(v));
        }
        return result.isEmpty() ? ContainerUtil.<T>emptyList() : result;
    }

    
    public static <T> boolean intersects( Collection<? extends T> collection1,  Collection<? extends T> collection2) {
        if (collection1.size() <= collection2.size()) {
            for (T t : collection1) {
                if (collection2.contains(t)) {
                    return true;
                }
            }
        }
        else {
            for (T t : collection2) {
                if (collection1.contains(t)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return read-only collection consisting of elements from both collections
     */
    
    
    public static <T> Collection<T> intersection( Collection<? extends T> collection1,  Collection<? extends T> collection2) {
        List<T> result = new ArrayList<T>();
        for (T t : collection1) {
            if (collection2.contains(t)) {
                result.add(t);
            }
        }
        return result.isEmpty() ? ContainerUtil.<T>emptyList() : result;
    }

    
    
    public static <T> T getFirstItem( Collection<T> items) {
        return getFirstItem(items, null);
    }

    
    
    public static <T> T getFirstItem( List<T> items) {
        return items == null || items.isEmpty() ? null : items.get(0);
    }

    
    public static <T> T getFirstItem( final Collection<T> items,  final T defaultResult) {
        return items == null || items.isEmpty() ? defaultResult : items.iterator().next();
    }

    /**
     * The main difference from <code>subList</code> is that <code>getFirstItems</code> does not
     * throw any exceptions, even if maxItems is greater than size of the list
     *
     * @param items list
     * @param maxItems size of the result will be equal or less than <code>maxItems</code>
     * @param <T> type of list
     * @return new list with no more than <code>maxItems</code> first elements
     */
    
    
    public static <T> List<T> getFirstItems( final List<T> items, int maxItems) {
        return items.subList(0, Math.min(maxItems, items.size()));
    }

    
    
    public static <T> T iterateAndGetLastItem( Iterable<T> items) {
        Iterator<T> itr = items.iterator();
        T res = null;
        while (itr.hasNext()) {
            res = itr.next();
        }

        return res;
    }

    
    
    public static <T, L extends List<T>> T getLastItem( L list,  T def) {
        return isEmpty(list) ? def : list.get(list.size() - 1);
    }

    
    
    public static <T, L extends List<T>> T getLastItem( L list) {
        return getLastItem(list, null);
    }

    /**
     * @return read-only collection consisting of elements from the 'from' collection which are absent from the 'what' collection
     */
    
    
    public static <T> Collection<T> subtract( Collection<T> from,  Collection<T> what) {
        final Set<T> set = newHashSet(from);
        set.removeAll(what);
        return set.isEmpty() ? ContainerUtil.<T>emptyList() : set;
    }

    
    
    public static <T> T[] toArray( Collection<T> c,  ArrayFactory<T> factory) {
        return c != null ? c.toArray(factory.create(c.size())) : factory.create(0);
    }

    
    
    public static <T> T[] toArray( Collection<? extends T> c1,  Collection<? extends T> c2,  ArrayFactory<T> factory) {
        return ArrayUtil.mergeCollections(c1, c2, factory);
    }

    
    
    public static <T> T[] mergeCollectionsToArray( Collection<? extends T> c1,  Collection<? extends T> c2,  ArrayFactory<T> factory) {
        return ArrayUtil.mergeCollections(c1, c2, factory);
    }

    public static <T extends Comparable<T>> void sort( List<T> list) {
        int size = list.size();

        if (size < 2) return;
        if (size == 2) {
            T t0 = list.get(0);
            T t1 = list.get(1);

            if (t0.compareTo(t1) > 0) {
                list.set(0, t1);
                list.set(1, t0);
            }
        }
        else if (size < INSERTION_SORT_THRESHOLD) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < i; j++) {
                    T ti = list.get(i);
                    T tj = list.get(j);

                    if (ti.compareTo(tj) < 0) {
                        list.set(i, tj);
                        list.set(j, ti);
                    }
                }
            }
        }
        else {
            Collections.sort(list);
        }
    }

    public static <T> void sort( List<T> list,  Comparator<? super T> comparator) {
        int size = list.size();

        if (size < 2) return;
        if (size == 2) {
            T t0 = list.get(0);
            T t1 = list.get(1);

            if (comparator.compare(t0, t1) > 0) {
                list.set(0, t1);
                list.set(1, t0);
            }
        }
        else if (size < INSERTION_SORT_THRESHOLD) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < i; j++) {
                    T ti = list.get(i);
                    T tj = list.get(j);

                    if (comparator.compare(ti, tj) < 0) {
                        list.set(i, tj);
                        list.set(j, ti);
                    }
                }
            }
        }
        else {
            Collections.sort(list, comparator);
        }
    }

    public static <T extends Comparable<T>> void sort( T[] a) {
        int size = a.length;

        if (size < 2) return;
        if (size == 2) {
            T t0 = a[0];
            T t1 = a[1];

            if (t0.compareTo(t1) > 0) {
                a[0] = t1;
                a[1] = t0;
            }
        }
        else if (size < INSERTION_SORT_THRESHOLD) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < i; j++) {
                    T ti = a[i];
                    T tj = a[j];

                    if (ti.compareTo(tj) < 0) {
                        a[i] = tj;
                        a[j] = ti;
                    }
                }
            }
        }
        else {
            Arrays.sort(a);
        }
    }

    
    
    public static <T> List<T> sorted( Collection<T> list,  Comparator<T> comparator) {
        return sorted((Iterable<T>)list, comparator);
    }

    
    
    public static <T> List<T> sorted( Iterable<T> list,  Comparator<T> comparator) {
        List<T> sorted = newArrayList(list);
        sort(sorted, comparator);
        return sorted;
    }

    
    
    public static <T extends Comparable<T>> List<T> sorted( Collection<T> list) {
        return sorted(list, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        });
    }

    public static <T> void sort( T[] a,  Comparator<T> comparator) {
        int size = a.length;

        if (size < 2) return;
        if (size == 2) {
            T t0 = a[0];
            T t1 = a[1];

            if (comparator.compare(t0, t1) > 0) {
                a[0] = t1;
                a[1] = t0;
            }
        }
        else if (size < INSERTION_SORT_THRESHOLD) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < i; j++) {
                    T ti = a[i];
                    T tj = a[j];

                    if (comparator.compare(ti, tj) < 0) {
                        a[i] = tj;
                        a[j] = ti;
                    }
                }
            }
        }
        else {
            Arrays.sort(a, comparator);
        }
    }

    /**
     * @return read-only list consisting of the elements from the iterable converted by mapping
     */
    
    
    public static <T,V> List<V> map( Iterable<? extends T> iterable,  Function<T, V> mapping) {
        List<V> result = new ArrayList<V>();
        for (T t : iterable) {
            result.add(mapping.fun(t));
        }
        return result.isEmpty() ? ContainerUtil.<V>emptyList() : result;
    }

    /**
     * @return read-only list consisting of the elements from the iterable converted by mapping
     */
    
    
    public static <T,V> List<V> map( Collection<? extends T> iterable,  Function<T, V> mapping) {
        if (iterable.isEmpty()) return emptyList();
        List<V> result = new ArrayList<V>(iterable.size());
        for (T t : iterable) {
            result.add(mapping.fun(t));
        }
        return result;
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
     */
    
    
    public static <T, V> List<V> mapNotNull( T[] array,  Function<T, V> mapping) {
        return mapNotNull(Arrays.asList(array), mapping);
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
     */
    
    
    public static <T, V> V[] mapNotNull( T[] array,  Function<T, V> mapping,  V[] emptyArray) {
        List<V> result = new ArrayList<V>(array.length);
        for (T t : array) {
            V v = mapping.fun(t);
            if (v != null) {
                result.add(v);
            }
        }
        if (result.isEmpty()) {
            assert emptyArray.length == 0 : "You must pass an empty array";
            return emptyArray;
        }
        return result.toArray(emptyArray);
    }

    /**
     * @return read-only list consisting of the elements from the iterable converted by mapping with nulls filtered out
     */
    
    
    public static <T, V> List<V> mapNotNull( Iterable<? extends T> iterable,  Function<T, V> mapping) {
        List<V> result = new ArrayList<V>();
        for (T t : iterable) {
            final V o = mapping.fun(t);
            if (o != null) {
                result.add(o);
            }
        }
        return result.isEmpty() ? ContainerUtil.<V>emptyList() : result;
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
     */
    
    
    public static <T, V> List<V> mapNotNull( Collection<? extends T> iterable,  Function<T, V> mapping) {
        if (iterable.isEmpty()) {
            return emptyList();
        }

        List<V> result = new ArrayList<V>(iterable.size());
        for (T t : iterable) {
            final V o = mapping.fun(t);
            if (o != null) {
                result.add(o);
            }
        }
        return result.isEmpty() ? ContainerUtil.<V>emptyList() : result;
    }

    /**
     * @return read-only list consisting of the elements with nulls filtered out
     */
    
    
    public static <T> List<T> packNullables( T... elements) {
        List<T> list = new ArrayList<T>();
        for (T element : elements) {
            addIfNotNull(element, list);
        }
        return list.isEmpty() ? ContainerUtil.<T>emptyList() : list;
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping
     */
    
    
    public static <T, V> List<V> map( T[] array,  Function<T, V> mapping) {
        List<V> result = new ArrayList<V>(array.length);
        for (T t : array) {
            result.add(mapping.fun(t));
        }
        return result.isEmpty() ? ContainerUtil.<V>emptyList() : result;
    }

    
    
    public static <T, V> V[] map( T[] arr,  Function<T, V> mapping,  V[] emptyArray) {
        if (arr.length==0) {
            assert emptyArray.length == 0 : "You must pass an empty array";
            return emptyArray;
        }

        List<V> result = new ArrayList<V>(arr.length);
        for (T t : arr) {
            result.add(mapping.fun(t));
        }
        return result.toArray(emptyArray);
    }

    
    
    public static <T> Set<T> set( T ... items) {
        return newHashSet(items);
    }

    public static <K, V> void putIfNotNull(final K key,  V value,  final Map<K, V> result) {
        if (value != null) {
            result.put(key, value);
        }
    }

    public static <T> void add(final T element,  final Collection<T> result,  final Disposable parentDisposable) {
        if (result.add(element)) {
            Disposer.register(parentDisposable, new Disposable() {
                @Override
                public void dispose() {
                    result.remove(element);
                }
            });
        }
    }

    
    
    public static <T> List<T> createMaybeSingletonList( T element) {
        return element == null ? ContainerUtil.<T>emptyList() : Collections.singletonList(element);
    }

    
    
    public static <T> Set<T> createMaybeSingletonSet( T element) {
        return element == null ? Collections.<T>emptySet() : Collections.singleton(element);
    }

    
    public static <T, V> V getOrCreate( Map<T, V> result, final T key,  V defaultValue) {
        V value = result.get(key);
        if (value == null) {
            result.put(key, value = defaultValue);
        }
        return value;
    }

    public static <T, V> V getOrCreate( Map<T, V> result, final T key,  Factory<V> factory) {
        V value = result.get(key);
        if (value == null) {
            result.put(key, value = factory.create());
        }
        return value;
    }

    
    
    public static <T, V> V getOrElse( Map<T, V> result, final T key,  V defValue) {
        V value = result.get(key);
        return value == null ? defValue : value;
    }

    
    public static <T> boolean and( T[] iterable,  Condition<T> condition) {
        return and(Arrays.asList(iterable), condition);
    }

    
    public static <T> boolean and( Iterable<T> iterable,  Condition<T> condition) {
        for (final T t : iterable) {
            if (!condition.value(t)) return false;
        }
        return true;
    }

    
    public static <T> boolean exists( T[] iterable,  Condition<T> condition) {
        return or(Arrays.asList(iterable), condition);
    }

    
    public static <T> boolean exists( Iterable<T> iterable,  Condition<T> condition) {
        return or(iterable, condition);
    }

    
    public static <T> boolean or( T[] iterable,  Condition<T> condition) {
        return or(Arrays.asList(iterable), condition);
    }

    
    public static <T> boolean or( Iterable<T> iterable,  Condition<T> condition) {
        for (final T t : iterable) {
            if (condition.value(t)) return true;
        }
        return false;
    }

    
    
    public static <T> List<T> unfold( T t,  NullableFunction<T, T> next) {
        if (t == null) return emptyList();

        List<T> list = new ArrayList<T>();
        while (t != null) {
            list.add(t);
            t = next.fun(t);
        }
        return list;
    }

    
    
    public static <T> List<T> dropTail( List<T> items) {
        return items.subList(0, items.size() - 1);
    }

    
    
    public static <T> List<T> list( T... items) {
        return Arrays.asList(items);
    }

    // Generalized Quick Sort. Does neither array.clone() nor list.toArray()

    public static <T> void quickSort( List<T> list,  Comparator<? super T> comparator) {
        quickSort(list, comparator, 0, list.size());
    }

    private static <T> void quickSort( List<T> x,  Comparator<? super T> comparator, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && comparator.compare(x.get(j), x.get(j - 1)) < 0; j--) {
                    swapElements(x, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, comparator, l, l + s, l + 2 * s);
                m = med3(x, comparator, m - s, m, m + s);
                n = med3(x, comparator, n - 2 * s, n - s, n);
            }
            m = med3(x, comparator, l, m, n); // Mid-size, med of 3
        }
        T v = x.get(m);

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off;
        int b = a;
        int c = off + len - 1;
        int d = c;
        while (true) {
            while (b <= c && comparator.compare(x.get(b), v) <= 0) {
                if (comparator.compare(x.get(b), v) == 0) {
                    swapElements(x, a++, b);
                }
                b++;
            }
            while (c >= b && comparator.compare(v, x.get(c)) <= 0) {
                if (comparator.compare(x.get(c), v) == 0) {
                    swapElements(x, c, d--);
                }
                c--;
            }
            if (b > c) break;
            swapElements(x, b++, c--);
        }

        // Swap partition elements back to middle
        int n = off + len;
        int s = Math.min(a - off, b - a);
        vecswap(x, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) quickSort(x, comparator, off, s);
        if ((s = d - c) > 1) quickSort(x, comparator, n - s, s);
    }

    /*
     * Returns the index of the median of the three indexed longs.
     */
    private static <T> int med3( List<T> x, Comparator<? super T> comparator, int a, int b, int c) {
        return comparator.compare(x.get(a), x.get(b)) < 0 ? comparator.compare(x.get(b), x.get(c)) < 0
                ? b
                : comparator.compare(x.get(a), x.get(c)) < 0 ? c : a
                : comparator.compare(x.get(c), x.get(b)) < 0
                ? b
                : comparator.compare(x.get(c), x.get(a)) < 0 ? c : a;
    }

    /*
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static <T> void vecswap(List<T> x, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swapElements(x, a, b);
        }
    }

    /**
     * Merge sorted points, which are sorted by x and with equal x by y.
     * Result is put to x1 y1.
     */
    public static void mergeSortedArrays( TIntArrayList x1,
                                          TIntArrayList y1,
                                          TIntArrayList x2,
                                          TIntArrayList y2) {
        TIntArrayList newX = new TIntArrayList();
        TIntArrayList newY = new TIntArrayList();

        int i = 0;
        int j = 0;

        while (i < x1.size() && j < x2.size()) {
            if (x1.get(i) < x2.get(j) || x1.get(i) == x2.get(j) && y1.get(i) < y2.get(j)) {
                newX.add(x1.get(i));
                newY.add(y1.get(i));
                i++;
            }
            else if (x1.get(i) > x2.get(j) || x1.get(i) == x2.get(j) && y1.get(i) > y2.get(j)) {
                newX.add(x2.get(j));
                newY.add(y2.get(j));
                j++;
            }
            else { //equals
                newX.add(x1.get(i));
                newY.add(y1.get(i));
                i++;
                j++;
            }
        }

        while (i < x1.size()) {
            newX.add(x1.get(i));
            newY.add(y1.get(i));
            i++;
        }

        while (j < x2.size()) {
            newX.add(x2.get(j));
            newY.add(y2.get(j));
            j++;
        }

        x1.clear();
        y1.clear();
        x1.add(newX.toNativeArray());
        y1.add(newY.toNativeArray());
    }


    /**
     * @return read-only set consisting of the only element o
     */
    
    
    public static <T> Set<T> singleton(final T o,  final TObjectHashingStrategy<T> strategy) {
        return new SingletonSet<T>(o, strategy);
    }

    /**
     * @return read-only list consisting of the elements from all of the collections
     */
    
    
    public static <E> List<E> flatten( Collection<E>[] collections) {
        return flatten(Arrays.asList(collections));
    }

    /**
     * @return read-only list consisting of the elements from all of the collections
     */
    
    
    public static <E> List<E> flatten( Iterable<? extends Collection<E>> collections) {
        List<E> result = new ArrayList<E>();
        for (Collection<E> list : collections) {
            result.addAll(list);
        }

        return result.isEmpty() ? ContainerUtil.<E>emptyList() : result;
    }

    /**
     * @return read-only list consisting of the elements from all of the collections
     */
    
    
    public static <E> List<E> flattenIterables( Iterable<? extends Iterable<E>> collections) {
        List<E> result = new ArrayList<E>();
        for (Iterable<E> list : collections) {
            for (E e : list) {
                result.add(e);
            }
        }
        return result.isEmpty() ? ContainerUtil.<E>emptyList() : result;
    }

    
    
    public static <K,V> V[] convert( K[] from,  V[] to,  Function<K,V> fun) {
        if (to.length < from.length) {
            @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(to.getClass().getComponentType(), from.length);
            to = array;
        }
        for (int i = 0; i < from.length; i++) {
            to[i] = fun.fun(from[i]);
        }
        return to;
    }

    
    public static <T> boolean containsIdentity( Iterable<T> list, T element) {
        for (T t : list) {
            if (t == element) {
                return true;
            }
        }
        return false;
    }

    
    public static <T> int indexOfIdentity( List<T> list, T element) {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            if (list.get(i) == element) {
                return i;
            }
        }
        return -1;
    }

    
    public static <T> boolean equalsIdentity( List<T> list1,  List<T> list2) {
        int listSize = list1.size();
        if (list2.size() != listSize) {
            return false;
        }

        for (int i = 0; i < listSize; i++) {
            if (list1.get(i) != list2.get(i)) {
                return false;
            }
        }
        return true;
    }

    
    public static <T> int indexOf( List<T> list,  Condition<T> condition) {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            T t = list.get(i);
            if (condition.value(t)) {
                return i;
            }
        }
        return -1;
    }

    
    public static <T> int lastIndexOf( List<T> list,  Condition<T> condition) {
        for (int i = list.size() - 1; i >= 0; i--) {
            T t = list.get(i);
            if (condition.value(t)) {
                return i;
            }
        }
        return -1;
    }

    
    
    public static <T, U extends T> U findLastInstance( List<T> list,  final Class<U> clazz) {
        int i = lastIndexOf(list, new Condition<T>() {
            @Override
            public boolean value(T t) {
                return clazz.isInstance(t);
            }
        });
        //noinspection unchecked
        return i < 0 ? null : (U)list.get(i);
    }

    
    public static <T> int indexOf( List<T> list,  final T object) {
        return indexOf(list, new Condition<T>() {
            @Override
            public boolean value(T t) {
                return t.equals(object);
            }
        });
    }

    
    
    public static <A,B> Map<B,A> reverseMap( Map<A,B> map) {
        final Map<B,A> result = newHashMap();
        for (Map.Entry<A, B> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    
    public static <T> boolean processRecursively(final T root,  PairProcessor<T, List<T>> processor) {
        final LinkedList<T> list = new LinkedList<T>();
        list.add(root);
        while (!list.isEmpty()) {
            final T o = list.removeFirst();
            if (!processor.process(o, list)) return false;
        }
        return true;
    }

    
    public static <T> List<T> trimToSize( List<T> list) {
        if (list == null) return null;
        if (list.isEmpty()) return emptyList();

        if (list instanceof ArrayList) {
            ((ArrayList)list).trimToSize();
        }

        return list;
    }

    
    
    public static <T> Stack<T> newStack() {
        return ContainerUtilRt.newStack();
    }

    
    
    public static <T> Stack<T> newStack( Collection<T> initial) {
        return ContainerUtilRt.newStack(initial);
    }

    
    
    public static <T> Stack<T> newStack( T... initial) {
        return ContainerUtilRt.newStack(initial);
    }

    
    
    public static <T> List<T> emptyList() {
        return ContainerUtilRt.emptyList();
    }

    
    
    public static <T> CopyOnWriteArrayList<T> createEmptyCOWList() {
        return ContainerUtilRt.createEmptyCOWList();
    }

    /**
     * Creates List which is thread-safe to modify and iterate.
     * It differs from the java.util.concurrent.CopyOnWriteArrayList in the following:
     * - faster modification in the uncontended case
     * - less memory
     * - slower modification in highly contented case (which is the kind of situation you shouldn't use COWAL anyway)
     *
     * N.B. Avoid using <code>list.toArray(new T[list.size()])</code> on this list because it is inherently racey and
     * therefore can return array with null elements at the end.
     */
    
    
    public static <T> List<T> createLockFreeCopyOnWriteList() {
        return createConcurrentList();
    }

    
    
    public static <T> List<T> createLockFreeCopyOnWriteList( Collection<? extends T> c) {
        return new LockFreeCopyOnWriteArrayList<T>(c);
    }

    
    
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectMap() {
        //noinspection deprecation
        return new ConcurrentIntObjectHashMap<V>();
    }

    
    
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectSoftValueMap() {
        //noinspection deprecation
        return new ConcurrentSoftValueIntObjectHashMap<V>();
    }

    
    
    public static <V> ConcurrentLongObjectMap<V> createConcurrentLongObjectMap() {
        //noinspection deprecation
        return new ConcurrentLongObjectHashMap<V>();
    }

    
    
    public static <K,V> ConcurrentMap<K,V> createConcurrentWeakValueMap() {
        //noinspection deprecation
        return new ConcurrentWeakValueHashMap<K, V>();
    }

    
    
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectWeakValueMap() {
        //noinspection deprecation
        return new ConcurrentWeakValueIntObjectHashMap<V>();
    }

    
    
    public static <K,V> ConcurrentMap<K,V> createConcurrentWeakKeySoftValueMap(int initialCapacity,
                                                                               float loadFactor,
                                                                               int concurrencyLevel,
                                                                                final TObjectHashingStrategy<K> hashingStrategy) {
        //noinspection deprecation
        return new ConcurrentWeakKeySoftValueHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
    }

    
    
    public static <K,V> ConcurrentMap<K,V> createConcurrentWeakKeyWeakValueMap() {
        //noinspection deprecation
        return new ConcurrentWeakKeyWeakValueHashMap<K, V>(100, 0.75f, Runtime.getRuntime().availableProcessors(), ContainerUtil.<K>canonicalStrategy());
    }

    
    
    public static <K,V> ConcurrentMap<K,V> createConcurrentSoftValueMap() {
        //noinspection deprecation
        return new ConcurrentSoftValueHashMap<K, V>();
    }

    
    
    public static <K,V> ConcurrentMap<K,V> createConcurrentSoftMap() {
        //noinspection deprecation
        return new ConcurrentSoftHashMap<K, V>();
    }

    
    
    public static <K,V> ConcurrentMap<K,V> createConcurrentWeakMap() {
        //noinspection deprecation
        return new ConcurrentWeakHashMap<K, V>();
    }
    
    
    public static <K,V> ConcurrentMap<K,V> createConcurrentWeakMap(int initialCapacity,
                                                                   float loadFactor,
                                                                   int concurrencyLevel,
                                                                    TObjectHashingStrategy<K> hashingStrategy) {
        //noinspection deprecation
        return new ConcurrentWeakHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
    }

    /**
     * @see {@link #createLockFreeCopyOnWriteList()}
     */
    
    
    public static <T> ConcurrentList<T> createConcurrentList() {
        return new LockFreeCopyOnWriteArrayList<T>();
    }

    public static <T> void addIfNotNull( T element,  Collection<T> result) {
        ContainerUtilRt.addIfNotNull(element, result);
    }

    public static <T> void addIfNotNull( Collection<T> result,  T element) {
        ContainerUtilRt.addIfNotNull(result, element);
    }

    
    
    public static <T, V> List<V> map2List( T[] array,  Function<T, V> mapper) {
        return ContainerUtilRt.map2List(array, mapper);
    }

    
    
    public static <T, V> List<V> map2List( Collection<? extends T> collection,  Function<T, V> mapper) {
        return ContainerUtilRt.map2List(collection, mapper);
    }

    
    
    public static <T, V> Set<V> map2Set( T[] collection,  Function<T, V> mapper) {
        return ContainerUtilRt.map2Set(collection, mapper);
    }

    
    
    public static <T, V> Set<V> map2Set( Collection<? extends T> collection,  Function<T, V> mapper) {
        return ContainerUtilRt.map2Set(collection, mapper);
    }

    
    
    public static <T, V> Set<V> map2LinkedSet( Collection<? extends T> collection,  Function<T, V> mapper) {
        if (collection.isEmpty()) return Collections.emptySet();
        Set <V> set = new LinkedHashSet<V>(collection.size());
        for (final T t : collection) {
            set.add(mapper.fun(t));
        }
        return set;
    }

    
    
    public static <T, V> Set<V> map2SetNotNull( Collection<? extends T> collection,  Function<T, V> mapper) {
        if (collection.isEmpty()) return Collections.emptySet();
        Set <V> set = new HashSet<V>(collection.size());
        for (T t : collection) {
            V value = mapper.fun(t);
            if (value != null) {
                set.add(value);
            }
        }
        return set.isEmpty() ? Collections.<V>emptySet() : set;
    }

    
    
    public static <T> T[] toArray( List<T> collection,  T[] array) {
        return ContainerUtilRt.toArray(collection, array);
    }

    
    
    public static <T> T[] toArray( Collection<T> c,  T[] sample) {
        return ContainerUtilRt.toArray(c, sample);
    }

    
    public static <T> T[] copyAndClear( Collection<T> collection,  ArrayFactory<T> factory, boolean clear) {
        int size = collection.size();
        T[] a = factory.create(size);
        if (size > 0) {
            a = collection.toArray(a);
            if (clear) collection.clear();
        }
        return a;
    }

    
    
    public static <T> Collection<T> toCollection( Iterable<T> iterable) {
        return iterable instanceof Collection ? (Collection<T>)iterable : newArrayList(iterable);
    }

    
    public static <T> List<T> toList( Enumeration<T> enumeration) {
        if (!enumeration.hasMoreElements()) {
            return Collections.emptyList();
        }

        List<T> result = new SmartList<T>();
        while (enumeration.hasMoreElements()) {
            result.add(enumeration.nextElement());
        }
        return result;
    }


    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    
    
    public static <T> List<T> notNullize( List<T> list) {
        return list == null ? ContainerUtilRt.<T>emptyList() : list;
    }

    
    
    public static <T> Set<T> notNullize( Set<T> set) {
        //noinspection unchecked
        return set == null ? Collections.<T>emptySet() : set;
    }

    
    
    public static <T, C extends Collection<T>> C nullize( C collection) {
        return isEmpty(collection) ? null : collection;
    }

    private interface ConcurrentMapFactory {
         <T, V> ConcurrentMap<T, V> createMap();
         <T, V> ConcurrentMap<T, V> createMap(int initialCapacity);
         <T, V> ConcurrentMap<T, V> createMap( TObjectHashingStrategy<T> hashStrategy);
         <T, V> ConcurrentMap<T, V> createMap(int initialCapacity, float loadFactor, int concurrencyLevel);
         <T, V> ConcurrentMap<T, V> createMap(int initialCapacity, float loadFactor, int concurrencyLevel,  TObjectHashingStrategy<T> hashStrategy);
    }

    private static final ConcurrentMapFactory V8_MAP_FACTORY = new ConcurrentMapFactory() {
        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap() {
            return new ConcurrentHashMap<T,V>();
        }

        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap(int initialCapacity) {
            return new ConcurrentHashMap<T,V>(initialCapacity);
        }

        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap( TObjectHashingStrategy<T> hashStrategy) {
            return new ConcurrentHashMap<T,V>(hashStrategy);
        }

        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
            return new ConcurrentHashMap<T,V>(initialCapacity, loadFactor, concurrencyLevel);
        }

        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap(int initialCapacity, float loadFactor, int concurrencyLevel,  TObjectHashingStrategy<T> hashingStrategy) {
            return new ConcurrentHashMap<T,V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
        }
    };

    private static final ConcurrentMapFactory PLATFORM_MAP_FACTORY = new ConcurrentMapFactory() {
        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap() {
            return createMap(16, 0.75f, DEFAULT_CONCURRENCY_LEVEL);
        }

        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap(int initialCapacity) {
            return new java.util.concurrent.ConcurrentHashMap<T,V>(initialCapacity);
        }

        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap( TObjectHashingStrategy<T> hashingStrategy) {
            if (hashingStrategy != canonicalStrategy()) {
                throw new UnsupportedOperationException("Custom hashStrategy is not supported in java.util.concurrent.ConcurrentHashMap");
            }
            // ignoring strategy parameter, because it is not supported by this implementation
            return createMap();
        }

        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
            return new java.util.concurrent.ConcurrentHashMap<T,V>(initialCapacity, loadFactor, concurrencyLevel);
        }

        @Override
        
        public <T, V> ConcurrentMap<T, V> createMap(int initialCapacity, float loadFactor, int concurrencyLevel,  TObjectHashingStrategy<T> hashingStrategy) {
            if (hashingStrategy != canonicalStrategy()) {
                throw new UnsupportedOperationException("Custom hashStrategy is not supported in java.util.concurrent.ConcurrentHashMap");
            }
            // ignoring strategy parameter, because it is not supported by this implementation
            return createMap(initialCapacity, loadFactor, concurrencyLevel);
        }
    };

    private static final ConcurrentMapFactory CHM_FACTORY = SystemInfo.isOracleJvm || SystemInfo.isSunJvm || SystemInfo.isAppleJvm || isAtLeastJava7() ? V8_MAP_FACTORY : PLATFORM_MAP_FACTORY;

    private static boolean isAtLeastJava7() {
        // IBM JDK provides correct version in java.version property, but not in java.runtime.version property
        return StringUtil.compareVersionNumbers(SystemInfo.JAVA_VERSION, "1.7") >= 0;
    }

    
    public static <T extends Comparable<T>> int compareLexicographically( List<T> o1,  List<T> o2) {
        for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
            int result = Comparing.compare(o1.get(i), o2.get(i));
            if (result != 0) {
                return result;
            }
        }
        return o1.size() < o2.size() ? -1 : o1.size() == o2.size() ? 0 : 1;
    }

    
    public static <T> int compareLexicographically( List<T> o1,  List<T> o2,  Comparator<T> comparator) {
        for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
            int result = comparator.compare(o1.get(i), o2.get(i));
            if (result != 0) {
                return result;
            }
        }
        return o1.size() < o2.size() ? -1 : o1.size() == o2.size() ? 0 : 1;
    }

    /**
     * Returns a String representation of the given map, by listing all key-value pairs contained in the map.
     */
    
    
    public static String toString( Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<?, ?> entry = iterator.next();
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
