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
package com.gome.maven.openapi.editor.ex;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.FoldRegion;
import com.gome.maven.openapi.editor.FoldingGroup;
import com.gome.maven.openapi.editor.FoldingModel;
import com.gome.maven.openapi.editor.markup.TextAttributes;

import java.awt.*;

/**
 * @author max
 */
public interface FoldingModelEx extends FoldingModel {
    void setFoldingEnabled(boolean isEnabled);
    boolean isFoldingEnabled();

    FoldRegion getFoldingPlaceholderAt(Point p);

    boolean intersectsRegion(int startOffset, int endOffset);

    /**
     * @deprecated Use an equivalent method {@link FoldingModel#getCollapsedRegionAtOffset(int)} instead. To be removed in IDEA 16.
     */
    FoldRegion fetchOutermost(int offset);

    /**
     * Returns an index in an array returned by {@link #fetchTopLevel()} method, for the last folding region lying entirely before given
     * offset (region can touch given offset at its right edge).
     */
    int getLastCollapsedRegionBefore(int offset);

    TextAttributes getPlaceholderAttributes();

    FoldRegion[] fetchTopLevel();

    
    FoldRegion createFoldRegion(int startOffset, int endOffset,  String placeholder,  FoldingGroup group,
                                boolean neverExpands);

    void addListener( FoldingListener listener,  Disposable parentDisposable);

    void clearFoldRegions();

    void rebuild();
}
