package gnu.trove;

/**
 * @author zhangliewei
 * @date 2017/12/29 16:19
 * @opyright(c) gome inc Gome Co.,LTD
 */
class IdentityEquality<T> implements Equality<T> {
    IdentityEquality() {
    }

    public boolean equals(T o1, T o2) {
        return o1 == o2;
    }
}
