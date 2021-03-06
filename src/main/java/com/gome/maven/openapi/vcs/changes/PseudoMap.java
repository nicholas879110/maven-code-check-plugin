/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.PairConsumer;

import java.util.HashMap;
import java.util.Map;

public class PseudoMap<Key, Value> implements PairConsumer<Key, Value>, NullableFunction<Key, Value> {
    private final Map<Key, Value> myMap;

    public PseudoMap() {
        myMap = createMap();
    }

    protected Map<Key, Value> createMap() {
        return new HashMap<Key, Value>();
    }

    public Value fun(Key key) {
        return myMap.get(key);
    }

    public void consume(Key key, Value value) {
        myMap.put(key, value);
    }
}
