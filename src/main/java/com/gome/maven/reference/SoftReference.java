package com.gome.maven.reference;

import com.gome.maven.openapi.util.Getter;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:01
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class SoftReference<T> extends java.lang.ref.SoftReference<T> implements Getter<T> {
    //private final T myReferent;

    public SoftReference(final T referent) {
        super(referent);
        //myReferent = referent;
    }

    public SoftReference(final T referent, final ReferenceQueue<? super T> q) {
        super(referent, q);
        //myReferent = referent;
    }

    //@Override
    //public T get() {
    //  return myReferent;
    //}

   
    public static <T> T dereference( Reference<T> ref) {
        return ref == null ? null : ref.get();
    }
   
    public static <T> T deref( Getter<T> ref) {
        return ref == null ? null : ref.get();
    }
}
