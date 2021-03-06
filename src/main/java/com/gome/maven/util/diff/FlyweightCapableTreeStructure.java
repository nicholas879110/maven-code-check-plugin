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

package com.gome.maven.util.diff;

import com.gome.maven.openapi.util.Ref;

/**
 * @author max
 */
public interface FlyweightCapableTreeStructure<T> {
    
    T getRoot();

    
    T getParent( T node);

    
    T prepareForGetChildren( T node);

    int getChildren( T parent,  Ref<T[]> into);

    void disposeChildren(T[] nodes, int count);

    
    CharSequence toString( T node);

    int getStartOffset( T node);
    int getEndOffset( T node);
}
