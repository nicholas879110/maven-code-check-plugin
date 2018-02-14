package gnu.trove;

/**
 * @author zhangliewei
 * @date 2017/12/29 16:19
 * @opyright(c) gome inc Gome Co.,LTD
 */
class CanonicalEquality<T> implements Equality<T> {
    CanonicalEquality() {
    }

    public boolean equals(T o1, T o2) {
        return o1 != null ? o1.equals(o2) : o2 == null;
    }
}
