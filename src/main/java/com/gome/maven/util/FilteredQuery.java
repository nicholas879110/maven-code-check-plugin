/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.util;

import com.gome.maven.concurrency.AsyncFuture;
import com.gome.maven.openapi.util.Condition;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author max
 */
public class FilteredQuery<T> implements Query<T> {
    private final Query<T> myOriginal;
    private final Condition<T> myFilter;

    public FilteredQuery(final Query<T> original, Condition<T> filter) {
        myOriginal = original;
        myFilter = filter;
    }

    @Override
    public T findFirst() {
        final CommonProcessors.FindFirstProcessor<T> processor = new CommonProcessors.FindFirstProcessor<T>();
        forEach(processor);
        return processor.getFoundValue();
    }

    @Override
    public boolean forEach( final Processor<T> consumer) {
        myOriginal.forEach(new MyProcessor(consumer));
        return true;
    }

    
    @Override
    public AsyncFuture<Boolean> forEachAsync( Processor<T> consumer) {
        return myOriginal.forEachAsync(new MyProcessor(consumer));
    }

    @Override
    
    public Collection<T> findAll() {
        CommonProcessors.CollectProcessor<T> processor = new CommonProcessors.CollectProcessor<T>();
        forEach(processor);
        return processor.getResults();
    }

    
    @Override
    public T[] toArray( final T[] a) {
        return findAll().toArray(a);
    }

    @Override
    public Iterator<T> iterator() {
        return findAll().iterator();
    }

    private class MyProcessor implements Processor<T> {
        private final Processor<T> myConsumer;

        public MyProcessor(Processor<T> consumer) {
            myConsumer = consumer;
        }

        @Override
        public boolean process(final T t) {
            if (!myFilter.value(t)) return true;
            if (!myConsumer.process(t)) return false;

            return true;
        }
    }
}
