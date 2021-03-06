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
package com.gome.maven.util.containers;

import com.gome.maven.util.Function;

public class JBTreeTraverser<T> extends FilteredTraverserBase<T, JBTreeTraverser<T>> {

    public JBTreeTraverser(Function<T, ? extends Iterable<? extends T>> treeStructure) {
        super(null, treeStructure);
    }

    protected JBTreeTraverser(Meta<T> meta, Function<T, ? extends Iterable<? extends T>> treeStructure) {
        super(meta, treeStructure);
    }

    @Override
    protected JBTreeTraverser<T> newInstance(Meta<T> meta) {
        return new JBTreeTraverser<T>(meta, tree);
    }
}