package com.gome.maven.util;

/**
 * @author zhangliewei
 * @date 2017/12/29 15:50
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface NullableFunction<Param, Result> extends Function<Param, Result> {
    Result fun(final Param param);

    /**
     * @see FunctionUtil#nullConstant()
     */
    NullableFunction NULL = new NullableFunction() {
        public Object fun(final Object o) {
            return null;
        }
    };
}
