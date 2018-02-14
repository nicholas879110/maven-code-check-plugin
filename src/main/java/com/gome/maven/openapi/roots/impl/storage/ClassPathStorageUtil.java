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
package com.gome.maven.openapi.roots.impl.storage;

import com.gome.maven.openapi.module.Module;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;

public class ClassPathStorageUtil {
     public static final String DEFAULT_STORAGE = "default";

    public static boolean isDefaultStorage( Module module) {
        return getStorageType(module).equals(DEFAULT_STORAGE);
    }

    
    public static String getStorageType( Module module) {
        String id = module.getOptionValue(JpsProjectLoader.CLASSPATH_ATTRIBUTE);
        return id == null ? DEFAULT_STORAGE : id;
    }
}