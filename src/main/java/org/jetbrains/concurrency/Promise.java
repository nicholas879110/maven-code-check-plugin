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

import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.AsyncResult;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Function;

import java.util.Collection;

public abstract class Promise<T> {
    public static final Promise<Void> DONE = new DonePromise<Void>(null);
    public static final Promise<Void> REJECTED = new RejectedPromise<Void>(createError("rejected"));

    
    public static RuntimeException createError( String error) {
        return new MessageError(error);
    }

    public enum State {
        PENDING, FULFILLED, REJECTED
    }

    
    public static <T> Promise<T> resolve(T result) {
        if (result == null) {
            //noinspection unchecked
            return (Promise<T>)DONE;
        }
        else {
            return new DonePromise<T>(result);
        }
    }

    
    public static <T> Promise<T> reject( String error) {
        return reject(createError(error));
    }

    
    public static <T> Promise<T> reject( Throwable error) {
        if (error == null) {
            //noinspection unchecked
            return (Promise<T>)REJECTED;
        }
        else {
            return new RejectedPromise<T>(error);
        }
    }

    
    public static Promise<Void> all( Collection<Promise<?>> promises) {
        return all(promises, null);
    }

    
    public static <T> Promise<T> all( Collection<Promise<?>> promises,  T totalResult) {
        if (promises.isEmpty()) {
            //noinspection unchecked
            return (Promise<T>)DONE;
        }

        final AsyncPromise<T> totalPromise = new AsyncPromise<T>();
        Consumer done = new CountDownConsumer<T>(promises.size(), totalPromise, totalResult);
        Consumer<Throwable> rejected = new Consumer<Throwable>() {
            @Override
            public void consume(Throwable error) {
                if (totalPromise.state == AsyncPromise.State.PENDING) {
                    totalPromise.setError(error);
                }
            }
        };

        for (Promise<?> promise : promises) {
            //noinspection unchecked
            promise.done(done);
            promise.rejected(rejected);
        }
        return totalPromise;
    }

    
    public static Promise<Void> wrapAsVoid( ActionCallback asyncResult) {
        final AsyncPromise<Void> promise = new AsyncPromise<Void>();
        asyncResult.doWhenDone(new Runnable() {
            @Override
            public void run() {
                promise.setResult(null);
            }
        }).doWhenRejected(new Consumer<String>() {
            @Override
            public void consume(String error) {
                promise.setError(createError(error));
            }
        });
        return promise;
    }

    
    public static <T> Promise<T> wrap( AsyncResult<T> asyncResult) {
        final AsyncPromise<T> promise = new AsyncPromise<T>();
        asyncResult.doWhenDone(new Consumer<T>() {
            @Override
            public void consume(T result) {
                promise.setResult(result);
            }
        }).doWhenRejected(new Consumer<String>() {
            @Override
            public void consume(String error) {
                promise.setError(createError(error));
            }
        });
        return promise;
    }

    
    public abstract Promise<T> done( Consumer<T> done);

    
    public abstract Promise<T> processed( AsyncPromise<T> fulfilled);

    
    public abstract Promise<T> rejected( Consumer<Throwable> rejected);

    public abstract Promise<T> processed( Consumer<T> processed);

    
    public abstract <SUB_RESULT> Promise<SUB_RESULT> then( Function<T, SUB_RESULT> done);

    
    public abstract <SUB_RESULT> Promise<SUB_RESULT> then( AsyncFunction<T, SUB_RESULT> done);

    
    public abstract State getState();

    @SuppressWarnings("ExceptionClassNameDoesntEndWithException")
    public static class MessageError extends RuntimeException {
        public MessageError( String error) {
            super(error);
        }

        
        @Override
        public final synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    abstract void notify( AsyncPromise<T> child);
}