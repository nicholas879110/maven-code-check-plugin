package com.gome.maven.openapi.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:17
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface Factory<T> {
    Factory NULL_FACTORY = new Factory() {
        public Object create() {
            return null;
        }
    };

    T create();
}
