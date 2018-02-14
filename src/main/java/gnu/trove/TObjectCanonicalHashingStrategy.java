package gnu.trove;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:57
 * @opyright(c) gome inc Gome Co.,LTD
 */
class TObjectCanonicalHashingStrategy<T> implements TObjectHashingStrategy<T> {
    @Override
    public int computeHashCode(T value) {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public boolean equals(T value, T value1) {
        return value != null ? value.equals(value1) : value1 == null;
    }
}
