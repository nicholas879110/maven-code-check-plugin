package com.gome.maven.util;

import com.gome.maven.openapi.util.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around map, allowing to put and get values by keys in a type-safe way. Used in various extension implementations
 * to manage temporary state. For example:
 * <ul>
 * <li/>When creating ElementPattern, processing context may be used to cache some intermediate data
 * to be shared between pattern parts.
 * <li/> Some extensions (e.g, PsiReferenceProvider, CompletionContributors) use per-pattern registration. That allows to use
 * ElementPattern#save to put matched objects into processing contexts and then retrieve those objects inside extension implementation
 * after the matching is complete.
 * </ul>
 *
 * Simple processing context can contain a shared processing context inside, which should be used when iterating over several patterns
 * or extensions, possibly from different plugins. They may still wish to reuse some cached information that a previous extension has already calculated.
 * <p/>
 * In this case, a separate ProcessingContext object is created for each of those extensions, but the same {@link com.gome.maven.util.SharedProcessingContext}
 * is passed to their constructors. To reuse shared context, extensions are required to work with {@link #getSharedContext()} result.
 * <p/>
 * Not thread-safe.
 *
 * @see #get(com.gome.maven.openapi.util.Key)
 * @see #put(com.gome.maven.openapi.util.Key, Object)
 * @see #ProcessingContext(SharedProcessingContext)
 * @author peter
 */
public class ProcessingContext {
    private Map<Object, Object> myMap;
    private SharedProcessingContext mySharedContext;

    public ProcessingContext() {
    }

    public ProcessingContext(final SharedProcessingContext sharedContext) {
        mySharedContext = sharedContext;
    }

    
    public SharedProcessingContext getSharedContext() {
        if (mySharedContext == null) {
            return mySharedContext = new SharedProcessingContext();
        }
        return mySharedContext;
    }

    @SuppressWarnings({"ConstantConditions"})
    public Object get(  final Object key) {
        return myMap == null? null : myMap.get(key);
    }

    public void put(  final Object key,  final Object value) {
        checkMapInitialized();
        myMap.put(key, value);
    }

    public <T> void put(Key<T> key, T value) {
        checkMapInitialized();
        myMap.put(key, value);
    }

    @SuppressWarnings({"ConstantConditions"})
    public <T> T get(Key<T> key) {
        return myMap == null ? null : (T)myMap.get(key);
    }

    private void checkMapInitialized() {
        if (myMap == null) myMap = new HashMap<Object, Object>(1);
    }

}
