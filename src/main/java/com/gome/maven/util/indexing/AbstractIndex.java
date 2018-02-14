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

package com.gome.maven.util.indexing;

import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.util.Processor;

/**
 * @author Eugene Zhuravlev
 *         Date: Dec 24, 2007
 */
public interface AbstractIndex<Key, Value> {
    
    ValueContainer<Value> getData( Key key) throws StorageException;

    boolean processAllKeys( Processor<Key> processor,  GlobalSearchScope scope,  IdFilter idFilter) throws StorageException;
}
