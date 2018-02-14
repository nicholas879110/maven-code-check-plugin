/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.concurrency;

import com.gome.maven.openapi.util.Getter;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Function;

class DonePromise<T> extends Promise<T> implements Getter<T> {
    private final T result;

    public DonePromise(T result) {
        this.result = result;
    }

    
    @Override
    public Promise<T> done( Consumer<T> done) {
        if (!AsyncPromise.isObsolete(done)) {
            done.consume(result);
        }
        return this;
    }

    
    @Override
    public Promise<T> processed( AsyncPromise<T> fulfilled) {
        fulfilled.setResult(result);
        return this;
    }

    @Override
    public Promise<T> processed( Consumer<T> processed) {
        done(processed);
        return this;
    }

    
    @Override
    public Promise<T> rejected( Consumer<Throwable> rejected) {
        return this;
    }

    
    @Override
    public <SUB_RESULT> Promise<SUB_RESULT> then( Function<T, SUB_RESULT> done) {
        if (done instanceof Obsolescent && ((Obsolescent)done).isObsolete()) {
            return Promise.reject("obsolete");
        }
        else {
            return Promise.resolve(done.fun(result));
        }
    }

    
    @Override
    public <SUB_RESULT> Promise<SUB_RESULT> then( AsyncFunction<T, SUB_RESULT> done) {
        return done.fun(result);
    }

    
    @Override
    public State getState() {
        return State.FULFILLED;
    }

    @Override
    public T get() {
        return result;
    }

    @Override
    void notify( AsyncPromise<T> child) {
        child.setResult(result);
    }
}