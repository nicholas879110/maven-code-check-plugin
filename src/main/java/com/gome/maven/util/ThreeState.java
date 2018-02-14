package com.gome.maven.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:58
 * @opyright(c) gome inc Gome Co.,LTD
 */
public enum ThreeState {
    YES, NO, UNSURE;

    public static ThreeState fromBoolean(boolean value) {
        return value ? YES : NO;
    }

    public boolean toBoolean() {
        if (this == UNSURE) {
            throw new IllegalStateException("Must be or YES, or NO");
        }
        return this == YES;
    }
}
