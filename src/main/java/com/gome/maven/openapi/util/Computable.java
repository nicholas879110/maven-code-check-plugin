package com.gome.maven.openapi.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 9:37
 * @opyright(c) gome inc Gome Co.,LTD
 */
public interface Computable <T> {

    T compute();

    class PredefinedValueComputable<T> implements Computable<T> {

        private final T myValue;

        public PredefinedValueComputable( T value) {
            myValue = value;
        }

        @Override
        public T compute() {
            return myValue;
        }
    }

    abstract class NotNullCachedComputable<T> implements Computable<T> {
        private T myValue;

        
        protected abstract T internalCompute();

        
        @Override
        public final T compute() {
            if (myValue == null) {
                myValue = internalCompute();
            }
            return myValue;
        }
    }

    abstract class NullableCachedComputable<T> implements Computable<T> {
        private static final Object NULL_VALUE = new Object();
        private Object myValue;

        
        protected abstract T internalCompute();

        
        @Override
        public final T compute() {
            if (myValue == null) {
                final T value = internalCompute();
                myValue = value != null ? value : NULL_VALUE;
            }
            return myValue != NULL_VALUE ? (T)myValue : null;
        }
    }
}
