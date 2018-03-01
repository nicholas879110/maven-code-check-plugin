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
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.UserDataHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 9/15/11
 * Time: 1:28 PM
 */
public class CommitContext implements UserDataHolder {
    private final Map<Key, Object> myMap;

    public CommitContext() {
        myMap = new HashMap<Key, Object>();
    }

    @Override
    public <T> T getUserData( Key<T> key) {
        return (T)myMap.get(key);
    }

    @Override
    public <T> void putUserData( Key<T> key,  T value) {
        myMap.put(key, value);
    }
}
