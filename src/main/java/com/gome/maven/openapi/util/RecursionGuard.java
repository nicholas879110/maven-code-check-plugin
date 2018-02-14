package com.gome.maven.openapi.util;

import java.util.List;

/**
 * @author zhangliewei
 * @date 2018/1/2 9:36
 * @opyright(c) gome inc Gome Co.,LTD
 */
public abstract class RecursionGuard {

    /**
     * See {@link #doPreventingRecursion(Object, boolean, Computable)} with memoization disabled
     */
    @SuppressWarnings("JavaDoc")
    @Deprecated
   
    public <T> T doPreventingRecursion( Object key,  Computable<T> computation) {
        return doPreventingRecursion(key, false, computation);
    }

    /**
     * @param key an id of the computation. Is stored internally to ensure that a recursive calls with the same key won't lead to endless recursion.
     * @param memoize whether the result of the computation may me cached thread-locally until the last currently active doPreventingRecursion call
     *                completes. May be used to speedup things when recursion re-entrance happens: otherwise nothing would be cached at all and
     *                in some cases exponential performance may be observed.
     * @param computation a piece of code to compute.
     * @return the result of the computation or null if we're entering a computation with this key on this thread recursively,
     */
   
    public abstract <T> T doPreventingRecursion( Object key, boolean memoize,  Computable<T> computation);

    /**
     * Used in pair with {@link com.gome.maven.openapi.util.RecursionGuard.StackStamp#mayCacheNow()} to ensure that cached are only the reliable values,
     * not depending on anything incomplete due to recursive prevention policies.
     * A typical usage is this:
     * <code>
     *  RecursionGuard.StackStamp stamp = RecursionManager.createGuard("id").markStack();
     *
     *   Result result = doComputation();
     *
     *   if (stamp.mayCacheNow()) {
     *     cache(result);
     *   }
     *   return result;
     * </code>

     * @return an object representing the current stack state, managed by {@link RecursionManager}
     */
    
    public abstract StackStamp markStack();

    /**
     * @return the current thread-local stack of keys passed to {@link #doPreventingRecursion(Object, Computable)}
     */
    
    public abstract List<Object> currentStack();

    /**
     * Makes {@link com.gome.maven.openapi.util.RecursionGuard.StackStamp#mayCacheNow()} return false for all stamps created since a computation with
     * key <code>since</code> began.
     *
     * Used to prevent caching of results that are non-reliable NOT due to recursion prevention: for example, too deep recursion
     * ({@link #currentStack()} may help in determining the recursion depth)
     *
     * Also disables thread-local memoization (see the second parameter of {@link #doPreventingRecursion(Object, boolean, Computable)}.
     *
     * @param since the id of a computation whose result is safe to cache whilst for more nested ones it's not.
     */
    public abstract void prohibitResultCaching(Object since);

    public interface StackStamp {

        /**
         * @return whether a computation that started at the moment of this {@link StackStamp} instance creation does not depend on any re-entrant recursive
         * results. When such non-reliable results exist in the thread's call stack, returns false, otherwise true.
         * If you use this with {@link RecursionGuard#doPreventingRecursion(Object, Computable)}, then the
         * {@link com.gome.maven.openapi.util.RecursionGuard#markStack()}+{@link #mayCacheNow()} should be outside of recursion prevention call. Otherwise
         * even the outer recursive computation result won't be cached.
         *
         */
        boolean mayCacheNow();
    }
}
