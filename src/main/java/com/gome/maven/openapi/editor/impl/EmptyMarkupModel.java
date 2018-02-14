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
package com.gome.maven.openapi.editor.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ex.DisposableIterator;
import com.gome.maven.openapi.editor.ex.MarkupModelEx;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.editor.impl.event.MarkupModelListener;
import com.gome.maven.openapi.editor.markup.HighlighterTargetArea;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Processor;

/**
 * This is mock implementation to be used in null-object pattern where necessary.
 * @author max
 */
public class EmptyMarkupModel implements MarkupModelEx {
    private final Document myDocument;

    public EmptyMarkupModel(final Document document) {
        myDocument = document;
    }

    @Override
    
    public Document getDocument() {
        return myDocument;
    }

    @Override
    
    public RangeHighlighter addRangeHighlighter(int startOffset,
                                                int endOffset,
                                                int layer,
                                                 TextAttributes textAttributes,
                                                 HighlighterTargetArea targetArea) {
        throw new ProcessCanceledException();
    }

    
    @Override
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                                     int endOffset,
                                                                     int layer,
                                                                     TextAttributes textAttributes,
                                                                      HighlighterTargetArea targetArea,
                                                                     boolean isPersistent,
                                                                     Consumer<RangeHighlighterEx> changeAttributesAction) {
        throw new ProcessCanceledException();
    }

    @Override
    public void changeAttributesInBatch( RangeHighlighterEx highlighter,
                                         Consumer<RangeHighlighterEx> changeAttributesAction) {
    }

    @Override
    
    public RangeHighlighter addLineHighlighter(int line, int layer,  TextAttributes textAttributes) {
        throw new ProcessCanceledException();
    }

    @Override
    public void removeHighlighter( RangeHighlighter rangeHighlighter) {
    }

    @Override
    public void removeAllHighlighters() {
    }

    @Override
    
    public RangeHighlighter[] getAllHighlighters() {
        return RangeHighlighter.EMPTY_ARRAY;
    }

    @Override
    public <T> T getUserData( Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData( Key<T> key, T value) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        return null;
    }

    @Override
    public boolean containsHighlighter( RangeHighlighter highlighter) {
        return false;
    }

    @Override
    public void addMarkupModelListener( Disposable parentDisposable,  MarkupModelListener listener) {
    }

    @Override
    public void setRangeHighlighterAttributes( final RangeHighlighter highlighter,  final TextAttributes textAttributes) {

    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end,  Processor<? super RangeHighlighterEx> processor) {
        return false;
    }

    @Override
    public boolean processRangeHighlightersOutside(int start, int end,  Processor<? super RangeHighlighterEx> processor) {
        return false;
    }

    
    @Override
    public DisposableIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
        return IntervalTreeImpl.PeekableIterator.EMPTY;
    }

    @Override
    public void fireAttributesChanged( RangeHighlighterEx segmentHighlighter, boolean renderersChanged) {

    }

    @Override
    public void fireAfterAdded( RangeHighlighterEx segmentHighlighter) {

    }

    @Override
    public void fireBeforeRemoved( RangeHighlighterEx segmentHighlighter) {

    }

    @Override
    public void addRangeHighlighter( RangeHighlighterEx marker, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {

    }
}
