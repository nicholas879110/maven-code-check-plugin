package gnu.trove;

/**
 * @author zhangliewei
 * @date 2017/12/29 16:18
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface Equality<T> {
    Equality CANONICAL = new CanonicalEquality();
    Equality IDENTITY = new IdentityEquality();
    boolean equals(T o1, T o2);
}
