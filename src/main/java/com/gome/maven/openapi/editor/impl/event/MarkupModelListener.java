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
package com.gome.maven.openapi.editor.impl.event;

import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;

import java.util.EventListener;

public interface MarkupModelListener extends EventListener {
    MarkupModelListener[] EMPTY_ARRAY = new MarkupModelListener[0];

    void afterAdded( RangeHighlighterEx highlighter);

    void beforeRemoved( RangeHighlighterEx highlighter);

    void attributesChanged( RangeHighlighterEx highlighter, boolean renderersChanged);

    abstract class Adapter implements MarkupModelListener {
        @Override
        public void afterAdded( RangeHighlighterEx highlighter) {
        }

        @Override
        public void beforeRemoved( RangeHighlighterEx highlighter) {
        }

        @Override
        public void attributesChanged( RangeHighlighterEx highlighter, boolean renderersChanged) {
        }
    }
}
