package com.gome.maven.util;

import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.util.text.CharArrayCharSequence;
import gnu.trove.Equality;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:35
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class ArrayUtil extends ArrayUtilRt {
    public static final short[] EMPTY_SHORT_ARRAY = ArrayUtilRt.EMPTY_SHORT_ARRAY;
    public static final char[] EMPTY_CHAR_ARRAY = ArrayUtilRt.EMPTY_CHAR_ARRAY;
    public static final byte[] EMPTY_BYTE_ARRAY = ArrayUtilRt.EMPTY_BYTE_ARRAY;
    public static final int[] EMPTY_INT_ARRAY = ArrayUtilRt.EMPTY_INT_ARRAY;
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = ArrayUtilRt.EMPTY_BOOLEAN_ARRAY;
    public static final Object[] EMPTY_OBJECT_ARRAY = ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    public static final String[] EMPTY_STRING_ARRAY = ArrayUtilRt.EMPTY_STRING_ARRAY;
    public static final Class[] EMPTY_CLASS_ARRAY = ArrayUtilRt.EMPTY_CLASS_ARRAY;
    public static final long[] EMPTY_LONG_ARRAY = ArrayUtilRt.EMPTY_LONG_ARRAY;
    public static final Collection[] EMPTY_COLLECTION_ARRAY = ArrayUtilRt.EMPTY_COLLECTION_ARRAY;
    public static final File[] EMPTY_FILE_ARRAY = ArrayUtilRt.EMPTY_FILE_ARRAY;
    public static final Runnable[] EMPTY_RUNNABLE_ARRAY = ArrayUtilRt.EMPTY_RUNNABLE_ARRAY;
    public static final CharSequence EMPTY_CHAR_SEQUENCE = new CharArrayCharSequence(EMPTY_CHAR_ARRAY);

    public static final ArrayFactory<String> STRING_ARRAY_FACTORY = new ArrayFactory<String>() {
        
        @Override
        public String[] create(int count) {
            return newStringArray(count);
        }
    };
    public static final ArrayFactory<Object> OBJECT_ARRAY_FACTORY = new ArrayFactory<Object>() {
        
        @Override
        public Object[] create(int count) {
            return newObjectArray(count);
        }
    };

    private ArrayUtil() { }

    
    
    public static byte[] realloc( byte[] array, final int newSize) {
        if (newSize == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        final int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        final byte[] result = new byte[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }
    
    
    public static boolean[] realloc( boolean[] array, final int newSize) {
        if (newSize == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        final int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        boolean[] result = new boolean[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    
    
    public static int[] realloc( int[] array, final int newSize) {
        if (newSize == 0) {
            return EMPTY_INT_ARRAY;
        }

        final int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        final int[] result = new int[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }
    
    
    public static <T> T[] realloc( T[] array, final int newSize,  ArrayFactory<T> factory) {
        final int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        T[] result = factory.create(newSize);
        if (newSize == 0) {
            return result;
        }

        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    
    
    public static int[] append( int[] array, int value) {
        array = realloc(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }

    
    
    public static <T> T[] insert( T[] array, int index, T value) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length + 1);
        System.arraycopy(array, 0, result, 0, index);
        result[index] = value;
        System.arraycopy(array, index, result, index + 1, array.length - index);
        return result;
    }

    
    
    public static int[] insert( int[] array, int index, int value) {
        int[] result = new int[array.length + 1];
        System.arraycopy(array, 0, result, 0, index);
        result[index] = value;
        System.arraycopy(array, index, result, index+1, array.length - index);
        return result;
    }

    
    
    public static byte[] append( byte[] array, byte value) {
        array = realloc(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }
    
    
    public static boolean[] append( boolean[] array, boolean value) {
        array = realloc(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }

    
    
    public static char[] realloc( char[] array, final int newSize) {
        if (newSize == 0) {
            return EMPTY_CHAR_ARRAY;
        }

        final int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        final char[] result = new char[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    
    
    public static <T> T[] toObjectArray( Collection<T> collection,  Class<T> aClass) {
        @SuppressWarnings("unchecked") T[] array = (T[])Array.newInstance(aClass, collection.size());
        return collection.toArray(array);
    }

    
    
    public static <T> T[] toObjectArray( Class<T> aClass,  Object... source) {
        @SuppressWarnings("unchecked") T[] array = (T[])Array.newInstance(aClass, source.length);
        System.arraycopy(source, 0, array, 0, array.length);
        return array;
    }

    
    
    public static Object[] toObjectArray( Collection<?> collection) {
        if (collection.isEmpty()) return EMPTY_OBJECT_ARRAY;
        //noinspection SSBasedInspection
        return collection.toArray(new Object[collection.size()]);
    }

    
    
    public static int[] toIntArray( Collection<Integer> list) {
        int[] ret = newIntArray(list.size());
        int i = 0;
        for (Integer e : list) {
            ret[i++] = e.intValue();
        }
        return ret;
    }

    
    
    public static <T> T[] mergeArrays( T[] a1,  T[] a2) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }

        final Class<?> class1 = a1.getClass().getComponentType();
        final Class<?> class2 = a2.getClass().getComponentType();
        final Class<?> aClass = class1.isAssignableFrom(class2) ? class1 : class2;

        @SuppressWarnings("unchecked") T[] result = (T[])Array.newInstance(aClass, a1.length + a2.length);
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    
    
    public static <T> T[] mergeCollections( Collection<? extends T> c1,  Collection<? extends T> c2,  ArrayFactory<T> factory) {
        T[] res = factory.create(c1.size() + c2.size());

        int i = 0;

        for (T t : c1) {
            res[i++] = t;
        }

        for (T t : c2) {
            res[i++] = t;
        }

        return res;
    }

    
    
    public static <T> T[] mergeArrays( T[] a1,  T[] a2,  ArrayFactory<T> factory) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }
        T[] result = factory.create(a1.length + a2.length);
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    
    
    public static String[] mergeArrays( String[] a1,  String... a2) {
        return mergeArrays(a1, a2, STRING_ARRAY_FACTORY);
    }

    
    
    public static int[] mergeArrays( int[] a1,  int[] a2) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }
        int[] result = new int[a1.length + a2.length];
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    
    
    public static byte[] mergeArrays( byte[] a1,  byte[] a2) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }
        byte[] result = new byte[a1.length + a2.length];
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    /**
     * Allocates new array of size <code>array.length + collection.size()</code> and copies elements of <code>array</code> and
     * <code>collection</code> to it.
     *
     * @param array      source array
     * @param collection source collection
     * @param factory    array factory used to create destination array of type <code>T</code>
     * @return destination array
     */
    
    
    public static <T> T[] mergeArrayAndCollection( T[] array,
                                                   Collection<T> collection,
                                                   final ArrayFactory<T> factory) {
        if (collection.isEmpty()) {
            return array;
        }

        final T[] array2;
        try {
            array2 = collection.toArray(factory.create(collection.size()));
        }
        catch (ArrayStoreException e) {
            throw new RuntimeException("Bad elements in collection: " + collection, e);
        }

        if (array.length == 0) {
            return array2;
        }

        final T[] result = factory.create(array.length + collection.size());
        System.arraycopy(array, 0, result, 0, array.length);
        System.arraycopy(array2, 0, result, array.length, array2.length);
        return result;
    }

    /**
     * Appends <code>element</code> to the <code>src</code> array. As you can
     * imagine the appended element will be the last one in the returned result.
     *
     * @param src     array to which the <code>element</code> should be appended.
     * @param element object to be appended to the end of <code>src</code> array.
     * @return new array
     */
    
    
    public static <T> T[] append( final T[] src, final T element) {
        return append(src, element, (Class<T>)src.getClass().getComponentType());
    }

    
    
    public static <T> T[] prepend(final T element,  final T[] array) {
        return prepend(element, array, (Class<T>)array.getClass().getComponentType());
    }

    
    
    public static <T> T[] prepend(T element,  T[] array,  Class<T> type) {
        int length = array.length;
        T[] result = (T[])Array.newInstance(type, length + 1);
        System.arraycopy(array, 0, result, 1, length);
        result[0] = element;
        return result;
    }

    
    
    public static byte[] prepend(byte element,  byte[] array) {
        int length = array.length;
        final byte[] result = new byte[length + 1];
        result[0] = element;
        System.arraycopy(array, 0, result, 1, length);
        return result;
    }

    
    public static <T> T[] append( final T[] src, final T element,  ArrayFactory<T> factory) {
        int length = src.length;
        T[] result = factory.create(length + 1);
        System.arraycopy(src, 0, result, 0, length);
        result[length] = element;
        return result;
    }

    
    
    public static <T> T[] append( T[] src, final T element,  Class<T> componentType) {
        int length = src.length;
        T[] result = (T[])Array.newInstance(componentType, length + 1);
        System.arraycopy(src, 0, result, 0, length);
        result[length] = element;
        return result;
    }

    /**
     * Removes element with index <code>idx</code> from array <code>src</code>.
     *
     * @param src array.
     * @param idx index of element to be removed.
     * @return modified array.
     */
    
    
    public static <T> T[] remove( final T[] src, int idx) {
        int length = src.length;
        if (idx < 0 || idx >= length) {
            throw new IllegalArgumentException("invalid index: " + idx);
        }
        T[] result = (T[])Array.newInstance(src.getClass().getComponentType(), length - 1);
        System.arraycopy(src, 0, result, 0, idx);
        System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
        return result;
    }

    
    
    public static <T> T[] remove( final T[] src, int idx,  ArrayFactory<T> factory) {
        int length = src.length;
        if (idx < 0 || idx >= length) {
            throw new IllegalArgumentException("invalid index: " + idx);
        }
        T[] result = factory.create(length - 1);
        System.arraycopy(src, 0, result, 0, idx);
        System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
        return result;
    }

    
    
    public static <T> T[] remove( final T[] src, T element) {
        final int idx = find(src, element);
        if (idx == -1) return src;

        return remove(src, idx);
    }

    
    
    public static <T> T[] remove( final T[] src, T element,  ArrayFactory<T> factory) {
        final int idx = find(src, element);
        if (idx == -1) return src;

        return remove(src, idx, factory);
    }

    
    
    public static int[] remove( final int[] src, int idx) {
        int length = src.length;
        if (idx < 0 || idx >= length) {
            throw new IllegalArgumentException("invalid index: " + idx);
        }
        int[] result = newIntArray(src.length - 1);
        System.arraycopy(src, 0, result, 0, idx);
        System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
        return result;
    }
    
    
    public static short[] remove( final short[] src, int idx) {
        int length = src.length;
        if (idx < 0 || idx >= length) {
            throw new IllegalArgumentException("invalid index: " + idx);
        }
        short[] result = src.length == 1 ? EMPTY_SHORT_ARRAY : new short[src.length - 1];
        System.arraycopy(src, 0, result, 0, idx);
        System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
        return result;
    }

    
    public static int find( int[] src, int obj) {
        return indexOf(src, obj);
    }

    
    public static <T> int find( final T[] src, final T obj) {
        return ArrayUtilRt.find(src, obj);
    }

    
    public static boolean startsWith( byte[] array,  byte[] prefix) {
        if (array == prefix) {
            return true;
        }
        int length = prefix.length;
        if (array.length < length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    
    public static <E> boolean startsWith( E[] array,  E[] subArray) {
        if (array == subArray) {
            return true;
        }
        int length = subArray.length;
        if (array.length < length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (!Comparing.equal(array[i], subArray[i])) {
                return false;
            }
        }

        return true;
    }

    
    public static boolean startsWith( byte[] array, int start,  byte[] subArray) {
        int length = subArray.length;
        if (array.length - start < length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (array[start + i] != subArray[i]) {
                return false;
            }
        }

        return true;
    }

    
    public static <T> boolean equals( T[] a1,  T[] a2,  Equality<? super T> comparator) {
        if (a1 == a2) {
            return true;
        }

        int length = a2.length;
        if (a1.length != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (!comparator.equals(a1[i], a2[i])) {
                return false;
            }
        }
        return true;
    }

    
    public static <T> boolean equals( T[] a1,  T[] a2,  Comparator<? super T> comparator) {
        if (a1 == a2) {
            return true;
        }
        int length = a2.length;
        if (a1.length != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (comparator.compare(a1[i], a2[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    
    
    public static <T> T[] reverseArray( T[] array) {
        T[] newArray = array.clone();
        for (int i = 0; i < array.length; i++) {
            newArray[array.length - i - 1] = array[i];
        }
        return newArray;
    }

    
    
    public static int[] reverseArray( int[] array) {
        int[] newArray = array.clone();
        for (int i = 0; i < array.length; i++) {
            newArray[array.length - i - 1] = array[i];
        }
        return newArray;
    }

    public static void reverse( char[] array) {
        for (int i = 0; i < array.length; i++) {
            swap(array, array.length - i - 1, i);
        }
    }

    
    public static int lexicographicCompare( String[] obj1,  String[] obj2) {
        for (int i = 0; i < Math.max(obj1.length, obj2.length); i++) {
            String o1 = i < obj1.length ? obj1[i] : null;
            String o2 = i < obj2.length ? obj2[i] : null;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            int res = o1.compareToIgnoreCase(o2);
            if (res != 0) return res;
        }
        return 0;
    }

    //must be Comparables
    
    public static <T> int lexicographicCompare( T[] obj1,  T[] obj2) {
        for (int i = 0; i < Math.max(obj1.length, obj2.length); i++) {
            T o1 = i < obj1.length ? obj1[i] : null;
            T o2 = i < obj2.length ? obj2[i] : null;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            int res = ((Comparable)o1).compareTo(o2);
            if (res != 0) return res;
        }
        return 0;
    }

    public static <T> void swap( T[] array, int i1, int i2) {
        final T t = array[i1];
        array[i1] = array[i2];
        array[i2] = t;
    }

    public static void swap( int[] array, int i1, int i2) {
        final int t = array[i1];
        array[i1] = array[i2];
        array[i2] = t;
    }

    public static void swap( boolean[] array, int i1, int i2) {
        final boolean t = array[i1];
        array[i1] = array[i2];
        array[i2] = t;
    }

    public static void swap( char[] array, int i1, int i2) {
        final char t = array[i1];
        array[i1] = array[i2];
        array[i2] = t;
    }

    public static <T> void rotateLeft( T[] array, int i1, int i2) {
        final T t = array[i1];
        System.arraycopy(array, i1 + 1, array, i1, i2 - i1);
        array[i2] = t;
    }

    public static <T> void rotateRight( T[] array, int i1, int i2) {
        final T t = array[i2];
        System.arraycopy(array, i1, array, i1 + 1, i2 - i1);
        array[i1] = t;
    }

    
    public static int indexOf( Object[] objects,  Object object) {
        return indexOf(objects, object, 0, objects.length);
    }

    
    public static int indexOf( Object[] objects, Object object, int start, int end) {
        if (object == null) {
            for (int i = start; i < end; i++) {
                if (objects[i] == null) return i;
            }
        }
        else {
            for (int i = start; i < end; i++) {
                if (object.equals(objects[i])) return i;
            }
        }
        return -1;
    }

    
    public static <T> int indexOf( List<T> objects, T object,  Equality<T> comparator) {
        for (int i = 0; i < objects.size(); i++) {
            if (comparator.equals(objects.get(i), object)) return i;
        }
        return -1;
    }

    
    public static <T> int indexOf( List<T> objects, T object,  Comparator<T> comparator) {
        for (int i = 0; i < objects.size(); i++) {
            if (comparator.compare(objects.get(i), object) == 0) return i;
        }
        return -1;
    }

    
    public static <T> int indexOf( T[] objects, T object,  Equality<T> comparator) {
        for (int i = 0; i < objects.length; i++) {
            if (comparator.equals(objects[i], object)) return i;
        }
        return -1;
    }

    
    public static int indexOf( int[] ints, int value) {
        for (int i = 0; i < ints.length; i++) {
            if (ints[i] == value) return i;
        }

        return -1;
    }
    
    public static int indexOf( short[] ints, short value) {
        for (int i = 0; i < ints.length; i++) {
            if (ints[i] == value) return i;
        }

        return -1;
    }

    
    public static <T> int lastIndexOf( final T[] src, final T obj) {
        for (int i = src.length - 1; i >= 0; i--) {
            final T o = src[i];
            if (o == null) {
                if (obj == null) {
                    return i;
                }
            }
            else {
                if (o.equals(obj)) {
                    return i;
                }
            }
        }
        return -1;
    }

    
    public static <T> int lastIndexOf( final T[] src, final T obj,  Equality<? super T> comparator) {
        for (int i = src.length - 1; i >= 0; i--) {
            final T o = src[i];
            if (comparator.equals(obj, o)) {
                return i;
            }
        }
        return -1;
    }

    
    public static <T> int lastIndexOf( List<T> src, final T obj,  Equality<? super T> comparator) {
        for (int i = src.size() - 1; i >= 0; i--) {
            final T o = src.get(i);
            if (comparator.equals(obj, o)) {
                return i;
            }
        }
        return -1;
    }

    
    public static boolean contains( final Object o,  Object... objects) {
        return indexOf(objects, o) >= 0;
    }

    
    public static boolean contains( final String s,  String... strings) {
        if (s == null) {
            for (String str : strings) {
                if (str == null) return true;
            }
        }
        else {
            for (String str : strings) {
                if (s.equals(str)) return true;
            }
        }

        return false;
    }

    
    
    public static int[] newIntArray(int count) {
        return count == 0 ? EMPTY_INT_ARRAY : new int[count];
    }

    
    
    public static long[] newLongArray(int count) {
        return count == 0 ? EMPTY_LONG_ARRAY : new long[count];
    }

    
    
    public static String[] newStringArray(int count) {
        return count == 0 ? EMPTY_STRING_ARRAY : new String[count];
    }

    
    
    public static Object[] newObjectArray(int count) {
        return count == 0 ? EMPTY_OBJECT_ARRAY : new Object[count];
    }

    
    
    public static <E> E[] ensureExactSize(int count,  E[] sample) {
        if (count == sample.length) return sample;
        @SuppressWarnings({"unchecked"}) final E[] array = (E[])Array.newInstance(sample.getClass().getComponentType(), count);
        return array;
    }

    
    
    public static <T> T getFirstElement( T[] array) {
        return array != null && array.length > 0 ? array[0] : null;
    }

    
    
    public static <T> T getLastElement( T[] array) {
        return array != null && array.length > 0 ? array[array.length - 1] : null;
    }

    
    
    public static String[] toStringArray( Collection<String> collection) {
        return ArrayUtilRt.toStringArray(collection);
    }

    public static <T> void copy( final Collection<? extends T> src,  final T[] dst, final int dstOffset) {
        int i = dstOffset;
        for (T t : src) {
            dst[i++] = t;
        }
    }

    
    public static <T> T[] stripTrailingNulls( T[] array) {
        return array.length != 0 && array[array.length-1] == null ? Arrays.copyOf(array, trailingNullsIndex(array)) : array;
    }

    private static <T> int trailingNullsIndex( T[] array) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] != null) {
                return i + 1;
            }
        }
        return 0;
    }
}
