package com.gome.maven.openapi.util;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:20
 * @opyright(c) gome inc Gome Co.,LTD
 */
public abstract class AtomicNotNullLazyValue<T> extends NotNullLazyValue<T> {
    private static final RecursionGuard ourGuard = RecursionManager.createGuard("AtomicNotNullLazyValue");
    private volatile T myValue;

    @Override
    public final T getValue() {
        T value = myValue;
        if (value != null) {
            return value;
        }
        //noinspection SynchronizeOnThis
        synchronized (this) {
            value = myValue;
            if (value == null) {
                RecursionGuard.StackStamp stamp = ourGuard.markStack();
                value = compute();
                if (stamp.mayCacheNow()) {
                    myValue = value;
                }
            }
        }
        return value;
    }
}
