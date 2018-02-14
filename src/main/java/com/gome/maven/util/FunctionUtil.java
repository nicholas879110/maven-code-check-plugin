/*
 * Copyright 2000-2012 JetBrains s.r.o.
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


/**
 * @author nik
 */
public class FunctionUtil {
    private FunctionUtil() { }

    
    public static <T> Function<T, T> id() {
        @SuppressWarnings("unchecked") Function<T, T> id = Function.ID;
        return id;
    }

    
    public static <A, B> NullableFunction<A, B> nullConstant() {
        @SuppressWarnings("unchecked") NullableFunction<A, B> function = NullableFunction.NULL;
        return function;
    }

    
    public static <T> Function<T, String> string() {
        @SuppressWarnings("unchecked") Function<T, String> function = Function.TO_STRING;
        return function;
    }

    
    public static <A, B> Function<A, B> constant(final B b) {
        return new Function<A, B>() {
            @Override
            public B fun(A a) {
                return b;
            }
        };
    }

    
    public static <A, B, C> NotNullFunction<A, C> composition( final NotNullFunction<B, C> f,  final NotNullFunction<A, B> g) {
        return new NotNullFunction<A, C>() {
            @Override
            
            public C fun(A a) {
                return f.fun(g.fun(a));
            }
        };
    }
}
