/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * @author max
 */
package com.gome.maven.psi.util;

import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.Function;

public class PsiCacheKey<T, H extends PsiElement> extends Key<SoftReference<Pair<Long, T>>> {
    private final Function<H, T> myFunction;
    /**
     * One of {@link com.gome.maven.psi.util.PsiModificationTracker} constants that marks when to flush cache
     */
    
    private final Key<?> myModifyCause;

    private PsiCacheKey(  String name,  Function<H, T> function,  Key<?> modifyCause) {
        super(name);
        myFunction = function;
        myModifyCause = modifyCause;
    }

    public final T getValue( H h) {
        T result = getCachedValueOrNull(h);
        if (result != null) {
            return result;
        }

        result = myFunction.fun(h);
        final long count = getModificationCount(h.getManager().getModificationTracker());
        h.putUserData(this, new SoftReference<Pair<Long, T>>(new Pair<Long, T>(count, result)));
        return result;
    }

    
    public final T getCachedValueOrNull( H h) {
        SoftReference<Pair<Long, T>> ref = h.getUserData(this);
        Pair<Long, T> data = SoftReference.dereference(ref);
        if (data == null || data.getFirst() != getModificationCount(h.getManager().getModificationTracker())) {
            return null;
        }

        return data.getSecond();
    }


    /**
     * Gets modification count from tracker based on {@link #myModifyCause}
     *
     * @param tracker track to get modification count from
     * @return modification count
     * @throws AssertionError if {@link #myModifyCause} is junk
     */
    private long getModificationCount( PsiModificationTracker tracker) {
        if (myModifyCause.equals(PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT)) {
            return tracker.getJavaStructureModificationCount();
        }
        if (myModifyCause.equals(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)) {
            return tracker.getOutOfCodeBlockModificationCount();
        }
        if (myModifyCause.equals(PsiModificationTracker.MODIFICATION_COUNT)) {
            return tracker.getModificationCount();
        }
        throw new AssertionError("No modification tracker found for key " + myModifyCause);
    }

    /**
     * Creates cache key value
     *
     * @param name        key name
     * @param function    function to reproduce new value when old value is stale
     * @param modifyCause one one {@link com.gome.maven.psi.util.PsiModificationTracker}'s constants that marks when to flush cache
     * @param <T>         value type
     * @param <H>         key type
     * @return instance
     */
    public static <T, H extends PsiElement> PsiCacheKey<T, H> create(  String name,
                                                                      Function<H, T> function,
                                                                      Key<?> modifyCause) {
        return new PsiCacheKey<T, H>(name, function, modifyCause);
    }

    /**
     * Creates cache key value using {@link com.gome.maven.psi.util.PsiModificationTracker#JAVA_STRUCTURE_MODIFICATION_COUNT} as
     * modification count to flush cache
     *
     * @param name     key name
     * @param function function to reproduce new value when old value is stale
     * @param <T>      value type
     * @param <H>      key type
     * @return instance
     */
    public static <T, H extends PsiElement> PsiCacheKey<T, H> create(  String name,  Function<H, T> function) {
        return create(name, function, PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT);
    }
}
