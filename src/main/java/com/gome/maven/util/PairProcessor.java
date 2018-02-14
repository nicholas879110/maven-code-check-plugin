package com.gome.maven.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:21
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface PairProcessor<S, T> {
    PairProcessor TRUE = new PairProcessor() {
        @Override
        public boolean process(Object o, Object o1) {
            return true;
        }
    };
    PairProcessor FALSE = new PairProcessor() {
        @Override
        public boolean process(Object o, Object o1) {
            return false;
        }
    };
    boolean process(S s, T t);
}
