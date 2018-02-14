package com.gome.maven.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 11:42
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface Processor<T> {
    Processor TRUE = new Processor() {
        public boolean process(Object o) {
            return true;
        }
    };

    boolean process(T var1);
}
