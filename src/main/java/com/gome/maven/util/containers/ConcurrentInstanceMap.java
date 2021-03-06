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

package com.gome.maven.util.containers;


/**
 * @author peter
 */
public class ConcurrentInstanceMap<T> extends ConcurrentFactoryMap<Class<? extends T>,T>{
    @Override

    protected T create(final Class<? extends T> key) {
        try {
            return key.newInstance();
        }
        catch (InstantiationException e) {
            throw new RuntimeException("Couldn't instantiate " + key, e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't instantiate " + key, e);
        }
    }
}