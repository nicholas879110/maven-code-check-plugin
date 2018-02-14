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
package com.gome.maven.openapi.vfs.newvfs.impl;

import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.keyFMap.KeyFMap;
import com.gome.maven.util.keyFMap.OneElementFMap;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentMap;

/**
 * @author peter
 */
class UserDataInterner {
    private static final ConcurrentMap<OneElementFMap, OneElementFMap> ourCache = ContainerUtil.createConcurrentWeakMap();

    static KeyFMap internUserData( KeyFMap map) {
        if (map instanceof OneElementFMap && shouldIntern((OneElementFMap)map)) {
            return ConcurrencyUtil.cacheOrGet(ourCache, (OneElementFMap)map, (OneElementFMap)map);
        }
        return map;
    }

    private static boolean shouldIntern(OneElementFMap map) {
        Object value = map.getValue();
        return value instanceof Enum || value instanceof Boolean || value instanceof Charset;
    }
}
