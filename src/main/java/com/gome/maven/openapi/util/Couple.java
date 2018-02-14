package com.gome.maven.openapi.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 11:41
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class Couple<T> extends Pair<T, T> {
    @SuppressWarnings("unchecked")
    private static final Couple EMPTY_COUPLE = new Couple(null, null);

    public Couple(T first, T second) {
        super(first, second);
    }

    public static <T> Couple<T> of(T first, T second) {
        return new Couple<T>(first, second);
    }

    @SuppressWarnings("unchecked")
    public static <T> Couple<T> getEmpty() {
        return EMPTY_COUPLE;
    }
}
