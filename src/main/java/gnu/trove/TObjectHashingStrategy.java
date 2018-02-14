package gnu.trove;

import java.io.Serializable;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:52
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface TObjectHashingStrategy<T> extends Serializable, Equality<T> {

    /**
     * Computes a hash code for the specified object.  Implementors
     * can use the object's own <tt>hashCode</tt> method, the Java
     * runtime's <tt>identityHashCode</tt>, or a custom scheme.
     *
     * @param object for which the hashcode is to be computed
     * @return the hashCode
     */
    int computeHashCode(T object);

    /**
     * Compares o1 and o2 for equality.  Strategy implementors may use
     * the objects' own equals() methods, compare object references,
     * or implement some custom scheme.
     *
     * @param o1 an <code>Object</code> value
     * @param o2 an <code>Object</code> value
     * @return true if the objects are equal according to this strategy.
     */
    @Override
    boolean equals(T o1, T o2);

    TObjectHashingStrategy IDENTITY = new TObjectIdentityHashingStrategy();
    TObjectHashingStrategy CANONICAL = new TObjectCanonicalHashingStrategy();
} // TObjectHashingStrategy
