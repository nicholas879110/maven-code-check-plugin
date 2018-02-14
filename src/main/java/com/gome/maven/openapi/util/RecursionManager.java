package com.gome.maven.openapi.util;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.util.containers.SoftHashMap;
import com.gome.maven.util.containers.SoftKeySoftValueHashMap;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.*;

/**
 * @author zhangliewei
 * @date 2018/1/2 9:38
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class RecursionManager {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.util.RecursionManager");
    private static final Object NULL = new Object();
    private static final ThreadLocal<CalculationStack> ourStack = new ThreadLocal<CalculationStack>() {
        @Override
        protected CalculationStack initialValue() {
            return new CalculationStack();
        }
    };
    private static boolean ourAssertOnPrevention;

    /**
     * @see RecursionGuard#doPreventingRecursion(Object, boolean, Computable)
     */
    @SuppressWarnings("JavaDoc")
    
    public static <T> T doPreventingRecursion( Object key, boolean memoize, Computable<T> computation) {
        return createGuard(computation.getClass().getName()).doPreventingRecursion(key, memoize, computation);
    }

    /**
     * @param id just some string to separate different recursion prevention policies from each other
     * @return a helper object which allow you to perform reentrancy-safe computations and check whether caching will be safe.
     */
    public static RecursionGuard createGuard( final String id) {
        return new RecursionGuard() {
            @Override
            public <T> T doPreventingRecursion( Object key, boolean memoize,  Computable<T> computation) {
                MyKey realKey = new MyKey(id, key, true);
                final CalculationStack stack = ourStack.get();

                if (stack.checkReentrancy(realKey)) {
                    if (ourAssertOnPrevention) {
                        throw new AssertionError("Endless recursion prevention occurred");
                    }
                    return null;
                }

                if (memoize) {
                    Object o = stack.getMemoizedValue(realKey);
                    if (o != null) {
                        SoftKeySoftValueHashMap<MyKey, Object> map = stack.intermediateCache.get(realKey);
                        if (map != null) {
                            for (MyKey noCacheUntil : map.keySet()) {
                                stack.prohibitResultCaching(noCacheUntil);
                            }
                        }

                        //noinspection unchecked
                        return o == NULL ? null : (T)o;
                    }
                }

                realKey = new MyKey(id, key, false);

                final int sizeBefore = stack.progressMap.size();
                stack.beforeComputation(realKey);
                final int sizeAfter = stack.progressMap.size();
                int startStamp = stack.memoizationStamp;

                try {
                    T result = computation.compute();

                    if (memoize) {
                        stack.maybeMemoize(realKey, result == null ? NULL : result, startStamp);
                    }

                    return result;
                }
                finally {
                    try {
                        stack.afterComputation(realKey, sizeBefore, sizeAfter);
                    }
                    catch (Throwable e) {
                        //noinspection ThrowFromFinallyBlock
                        throw new RuntimeException("Throwable in afterComputation", e);
                    }

                    stack.checkDepth("4");
                }
            }

            
            @Override
            public StackStamp markStack() {
                final int stamp = ourStack.get().reentrancyCount;
                return new StackStamp() {
                    @Override
                    public boolean mayCacheNow() {
                        return stamp == ourStack.get().reentrancyCount;
                    }
                };
            }

            
            @Override
            public List<Object> currentStack() {
                ArrayList<Object> result = new ArrayList<Object>();
                LinkedHashMap<MyKey, Integer> map = ourStack.get().progressMap;
                for (MyKey pair : map.keySet()) {
                    if (pair.guardId.equals(id)) {
                        result.add(pair.userObject);
                    }
                }
                return result;
            }

            @Override
            public void prohibitResultCaching(Object since) {
                MyKey realKey = new MyKey(id, since, false);
                final CalculationStack stack = ourStack.get();
                stack.enableMemoization(realKey, stack.prohibitResultCaching(realKey));
                stack.memoizationStamp++;
            }

        };
    }

    private static class MyKey {
        final String guardId;
        final Object userObject;
        private final int myHashCode;
        private final boolean myCallEquals;

        public MyKey(String guardId,  Object userObject, boolean mayCallEquals) {
            this.guardId = guardId;
            this.userObject = userObject;
            // remember user object hashCode to ensure our internal maps consistency
            myHashCode = guardId.hashCode() * 31 + userObject.hashCode();
            myCallEquals = mayCallEquals;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MyKey && guardId.equals(((MyKey)obj).guardId))) return false;
            if (userObject == ((MyKey)obj).userObject) {
                return true;
            }
            if (myCallEquals || ((MyKey)obj).myCallEquals) {
                return userObject.equals(((MyKey)obj).userObject);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return myHashCode;
        }
    }

    private static class CalculationStack {
        private int reentrancyCount;
        private int memoizationStamp;
        private int depth;
        private final LinkedHashMap<MyKey, Integer> progressMap = new LinkedHashMap<MyKey, Integer>();
        private final Set<MyKey> toMemoize = new THashSet<MyKey>();
        private final THashMap<MyKey, MyKey> key2ReentrancyDuringItsCalculation = new THashMap<MyKey, MyKey>();
        private final SoftHashMap<MyKey, SoftKeySoftValueHashMap<MyKey, Object>> intermediateCache = new SoftHashMap<MyKey, SoftKeySoftValueHashMap<MyKey, Object>>();
        private int enters = 0;
        private int exits = 0;

        boolean checkReentrancy(MyKey realKey) {
            if (progressMap.containsKey(realKey)) {
                enableMemoization(realKey, prohibitResultCaching(realKey));

                return true;
            }
            return false;
        }

        
        Object getMemoizedValue(MyKey realKey) {
            SoftKeySoftValueHashMap<MyKey, Object> map = intermediateCache.get(realKey);
            if (map == null) return null;

            if (depth == 0) {
                throw new AssertionError("Memoized values with empty stack");
            }

            for (MyKey key : map.keySet()) {
                final Object result = map.get(key);
                if (result != null) {
                    return result;
                }
            }

            return null;
        }

        final void beforeComputation(MyKey realKey) {
            enters++;

            if (progressMap.isEmpty()) {
                assert reentrancyCount == 0 : "Non-zero stamp with empty stack: " + reentrancyCount;
            }

            checkDepth("1");

            int sizeBefore = progressMap.size();
            progressMap.put(realKey, reentrancyCount);
            depth++;

            checkDepth("2");

            int sizeAfter = progressMap.size();
            if (sizeAfter != sizeBefore + 1) {
                LOG.error("Key doesn't lead to the map size increase: " + sizeBefore + " " + sizeAfter + " " + realKey.userObject);
            }
        }

        final void maybeMemoize(MyKey realKey,  Object result, int startStamp) {
            if (memoizationStamp == startStamp && toMemoize.contains(realKey)) {
                SoftKeySoftValueHashMap<MyKey, Object> map = intermediateCache.get(realKey);
                if (map == null) {
                    intermediateCache.put(realKey, map = new SoftKeySoftValueHashMap<MyKey, Object>());
                }
                final MyKey reentered = key2ReentrancyDuringItsCalculation.get(realKey);
                assert reentered != null;
                map.put(reentered, result);
            }
        }

        final void afterComputation(MyKey realKey, int sizeBefore, int sizeAfter) {
            exits++;
            if (sizeAfter != progressMap.size()) {
                LOG.error("Map size changed: " + progressMap.size() + " " + sizeAfter + " " + realKey.userObject);
            }

            if (depth != progressMap.size()) {
                LOG.error("Inconsistent depth after computation; depth=" + depth + "; map=" + progressMap);
            }

            Integer value = progressMap.remove(realKey);
            depth--;
            toMemoize.remove(realKey);
            key2ReentrancyDuringItsCalculation.remove(realKey);

            if (depth == 0) {
                intermediateCache.clear();
                if (!key2ReentrancyDuringItsCalculation.isEmpty()) {
                    LOG.error("non-empty key2ReentrancyDuringItsCalculation: " + new HashMap<MyKey, MyKey>(key2ReentrancyDuringItsCalculation));
                }
                if (!toMemoize.isEmpty()) {
                    LOG.error("non-empty toMemoize: " + new HashSet<MyKey>(toMemoize));
                }
            }

            if (sizeBefore != progressMap.size()) {
                LOG.error("Map size doesn't decrease: " + progressMap.size() + " " + sizeBefore + " " + realKey.userObject);
            }

            reentrancyCount = value;
            checkZero();

        }

        private void enableMemoization(MyKey realKey, Set<MyKey> loop) {
            toMemoize.addAll(loop);
            List<MyKey> stack = new ArrayList<MyKey>(progressMap.keySet());

            for (MyKey key : loop) {
                final MyKey existing = key2ReentrancyDuringItsCalculation.get(key);
                if (existing == null || stack.indexOf(realKey) >= stack.indexOf(key)) {
                    key2ReentrancyDuringItsCalculation.put(key, realKey);
                }
            }
        }

        private Set<MyKey> prohibitResultCaching(MyKey realKey) {
            reentrancyCount++;

            if (!checkZero()) {
                throw new AssertionError("zero1");
            }

            Set<MyKey> loop = new THashSet<MyKey>();
            boolean inLoop = false;
            for (Map.Entry<MyKey, Integer> entry: progressMap.entrySet()) {
                if (inLoop) {
                    entry.setValue(reentrancyCount);
                    loop.add(entry.getKey());
                }
                else if (entry.getKey().equals(realKey)) {
                    inLoop = true;
                }
            }

            if (!checkZero()) {
                throw new AssertionError("zero2");
            }
            return loop;
        }

        private void checkDepth(String s) {
            int oldDepth = depth;
            if (oldDepth != progressMap.size()) {
                depth = progressMap.size();
                throw new AssertionError("_Inconsistent depth " + s + "; depth=" + oldDepth + "; enters=" + enters + "; exits=" + exits + "; map=" + progressMap);
            }
        }

        private boolean checkZero() {
            if (!progressMap.isEmpty() && !new Integer(0).equals(progressMap.get(progressMap.keySet().iterator().next()))) {
                LOG.error("Prisoner Zero has escaped: " + progressMap + "; value=" + progressMap.get(progressMap.keySet().iterator().next()));
                return false;
            }
            return true;
        }

    }

    public static void assertOnRecursionPrevention( Disposable parentDisposable) {
        ourAssertOnPrevention = true;
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                ourAssertOnPrevention = false;
            }
        });
    }

}
