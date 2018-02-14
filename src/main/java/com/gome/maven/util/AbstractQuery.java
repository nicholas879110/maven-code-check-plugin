/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.gome.maven.concurrency.AsyncUtil;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author peter
 */
public abstract class AbstractQuery<Result> implements Query<Result> {
    private boolean myIsProcessing = false;

    @Override
    
    public Collection<Result> findAll() {
        assertNotProcessing();
        final CommonProcessors.CollectProcessor<Result> processor = new CommonProcessors.CollectProcessor<Result>();
        forEach(processor);
        return processor.getResults();
    }

    @Override
    public Iterator<Result> iterator() {
        assertNotProcessing();
        return new UnmodifiableIterator<Result>(findAll().iterator());
    }

    @Override
    
    public Result findFirst() {
        assertNotProcessing();
        final CommonProcessors.FindFirstProcessor<Result> processor = new CommonProcessors.FindFirstProcessor<Result>();
        forEach(processor);
        return processor.getFoundValue();
    }

    private void assertNotProcessing() {
        assert !myIsProcessing : "Operation is not allowed while query is being processed";
    }

    
    @Override
    public Result[] toArray( Result[] a) {
        assertNotProcessing();

        final Collection<Result> all = findAll();
        return all.toArray(a);
    }

    @Override
    public boolean forEach( Processor<Result> consumer) {
        assertNotProcessing();

        myIsProcessing = true;
        try {
            return processResults(consumer);
        }
        finally {
            myIsProcessing = false;
        }
    }

    
    @Override
    public AsyncFuture<Boolean> forEachAsync( Processor<Result> consumer) {
        return AsyncUtil.wrapBoolean(forEach(consumer));
    }

    protected abstract boolean processResults( Processor<Result> consumer);

    
    protected AsyncFuture<Boolean> processResultsAsync( Processor<Result> consumer) {
        return AsyncUtil.wrapBoolean(processResults(consumer));
    }
}
