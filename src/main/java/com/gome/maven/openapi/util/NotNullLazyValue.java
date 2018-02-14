package com.gome.maven.openapi.util;

/**
 * @author zhangliewei
 * @date 2017/12/29 18:04
 * @opyright(c) gome inc Gome Co.,LTD
 */
public abstract class NotNullLazyValue<T> {
    private static final RecursionGuard ourGuard = RecursionManager.createGuard("NotNullLazyValue");
    private T myValue;

    
    protected abstract T compute();

   
    public T getValue() {
        T result = myValue;
        if (result == null) {
            RecursionGuard.StackStamp stamp = ourGuard.markStack();
            result = compute();
            if (stamp.mayCacheNow()) {
                myValue = result;
            }
        }
        return result;
    }

   
    public static <T> NotNullLazyValue<T> createConstantValue( final T value) {
        return new NotNullLazyValue<T>() {
           
            @Override
            protected T compute() {
                return value;
            }
        };
    }
}
