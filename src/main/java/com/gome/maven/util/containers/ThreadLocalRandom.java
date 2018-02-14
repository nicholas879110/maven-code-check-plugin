package com.gome.maven.util.containers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhangliewei
 * @date 2018/1/2 13:32
 * @opyright(c) gome inc Gome Co.,LTD
 */
class ThreadLocalRandom {
    public static int getProbe() {
        return tlr.get().threadLocalRandomProbe;
    }

    public static void localInit() {
        int p = probeGenerator.addAndGet(PROBE_INCREMENT);
        int probe = (p == 0) ? 1 : p; // skip 0
        long seed = mix64(seeder.getAndAdd(SEEDER_INCREMENT));
        Tlr t = tlr.get();
        t.threadLocalRandomProbe = probe;
        t.threadLocalRandomSeed = seed;
    }

    /**
     * Pseudo-randomly advances and records the given probe value for the
     * given thread.
     */
    static int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        tlr.get().threadLocalRandomProbe = probe;
        return probe;
    }

    static class Tlr {
        /**
         * The current seed for a ThreadLocalRandom
         */
        long threadLocalRandomSeed;

        /**
         * Probe hash value; nonzero if threadLocalRandomSeed initialized
         */
        int threadLocalRandomProbe;
    }

    private static final ThreadLocal<Tlr> tlr = new ThreadLocal<Tlr>() {
        @Override
        protected Tlr initialValue() {
            return new Tlr();
        }
    };
    private static final AtomicInteger probeGenerator = new AtomicInteger();
    /**
     * The increment for generating probe values
     */
    private static final int PROBE_INCREMENT = 0x9e3779b9;

    /**
     * The increment of seeder per new instance
     */
    private static final long SEEDER_INCREMENT = 0xbb67ae8584caa73bL;

    /**
     * The next seed for default constructors.
     */
    private static final AtomicLong seeder = new AtomicLong(/*initialSeed()*/);

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }
}
