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

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.util.containers.Convertor;

/**
 * @author peter
 */
public class ObjectUtils {
    private ObjectUtils() {
    }

    public static final Object NULL = new Object();

    
    public static <T> T assertNotNull( final T t) {
        return _assertNotNull(t);
    }

    
    private static <T> T _assertNotNull( T t) {
        return t;
    }

    public static <T> T chooseNotNull( T t1,  T t2) {
        return t1 == null? t2 : t1;
    }

    
    public static <T> T notNull( T value,  T defaultValue) {
        return value != null ? value : defaultValue;
    }

    
    public static <T> T tryCast( Object obj,  Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        return null;
    }

    
    public static <T, S> S doIfCast( Object obj,  Class<T> clazz, final Convertor<T, S> convertor) {
        if (clazz.isInstance(obj)) {
            //noinspection unchecked
            return convertor.convert((T)obj);
        }
        return null;
    }

    
    public static <T> T nullizeByCondition( final T obj,  final Condition<T> condition) {
        if (condition.value(obj)) {
            return null;
        }
        return obj;
    }
}
