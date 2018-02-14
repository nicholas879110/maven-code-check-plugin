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
import com.gome.maven.openapi.editor.impl.event.MarkupModelListener;
import com.gome.maven.openapi.editor.markup.HighlighterTargetArea;
import com.gome.maven.openapi.editor.markup.MarkupModel;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Processor;

/**
 * @author max
 */
public interface MarkupModelEx extends MarkupModel {
    void dispose();

    
    RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes);

    void fireAttributesChanged( RangeHighlighterEx segmentHighlighter, boolean renderersChanged);

    void fireAfterAdded( RangeHighlighterEx segmentHighlighter);

    void fireBeforeRemoved( RangeHighlighterEx segmentHighlighter);

    boolean containsHighlighter( RangeHighlighter highlighter);

    void addRangeHighlighter( RangeHighlighterEx marker,
                             int start,
                             int end,
                             boolean greedyToLeft,
                             boolean greedyToRight,
                             int layer);

    void addMarkupModelListener( Disposable parentDisposable,  MarkupModelListener listener);

    void setRangeHighlighterAttributes( RangeHighlighter highlighter,  TextAttributes textAttributes);

    boolean processRangeHighlightersOverlappingWith(int start, int end,  Processor<? super RangeHighlighterEx> processor);
    boolean processRangeHighlightersOutside(int start, int end,  Processor<? super RangeHighlighterEx> processor);

    
    DisposableIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset);

    // optimization: creates highlighter and fires only one event: highlighterCreated
    
    RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                              int endOffset,
                                                              int layer,
                                                              TextAttributes textAttributes,
                                                               HighlighterTargetArea targetArea,
                                                              boolean isPersistent,
                                                              Consumer<RangeHighlighterEx> changeAttributesAction);

    // runs change attributes action and fires highlighterChanged event if there were changes
    void changeAttributesInBatch( RangeHighlighterEx highlighter,  Consumer<RangeHighlighterEx> changeAttributesAction);
}
