/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.openapi.editor.impl.softwrap.mapping;

import com.gome.maven.openapi.editor.FoldRegion;

/**
 * @author Denis Zhdanov
 * @since 11/23/11 7:04 PM
 */
public abstract class SoftWrapAwareDocumentParsingListenerAdapter implements SoftWrapAwareDocumentParsingListener {
    @Override
    public void onVisualLineStart( EditorPosition position) {
    }

    @Override
    public void onVisualLineEnd( EditorPosition position) {
    }

    @Override
    public void onCollapsedFoldRegion( FoldRegion foldRegion, int collapsedFoldingWidthInColumns, int visualLine) {
    }

    @Override
    public void onTabulation( EditorPosition position, int widthInColumns) {
    }

    @Override
    public void beforeSoftWrapLineFeed( EditorPosition position) {
    }

    @Override
    public void afterSoftWrapLineFeed( EditorPosition position) {
    }

    @Override
    public void revertToOffset(int offset, int visualLine) {
    }

    @Override
    public void onCacheUpdateStart( IncrementalCacheUpdateEvent event) {
    }

    @Override
    public void onRecalculationEnd( IncrementalCacheUpdateEvent event) {
    }

    @Override
    public void recalculationEnds() {
    }

    @Override
    public void reset() {
    }
}
