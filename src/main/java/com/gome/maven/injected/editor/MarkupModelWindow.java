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

package com.gome.maven.injected.editor;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ex.DisposableIterator;
import com.gome.maven.openapi.editor.ex.MarkupModelEx;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.editor.impl.event.MarkupModelListener;
import com.gome.maven.openapi.editor.markup.HighlighterTargetArea;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.util.ProperTextRange;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Processor;

/**
 * @author cdr
 */
public class MarkupModelWindow extends UserDataHolderBase implements MarkupModelEx {
    private final DocumentWindow myDocument;
    private final MarkupModelEx myHostModel;

    public MarkupModelWindow(MarkupModelEx editorMarkupModel, final DocumentWindow document) {
        myDocument = document;
        myHostModel = editorMarkupModel;
    }

    @Override
    
    public Document getDocument() {
        return myDocument;
    }

    @Override
    
    public RangeHighlighter addRangeHighlighter(final int startOffset,
                                                final int endOffset,
                                                final int layer,
                                                final TextAttributes textAttributes,
                                                 final HighlighterTargetArea targetArea) {
        TextRange hostRange = myDocument.injectedToHost(new ProperTextRange(startOffset, endOffset));
        return myHostModel.addRangeHighlighter(hostRange.getStartOffset(), hostRange.getEndOffset(), layer, textAttributes, targetArea);
    }

    
    @Override
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                                     int endOffset,
                                                                     int layer,
                                                                     TextAttributes textAttributes,
                                                                      HighlighterTargetArea targetArea,
                                                                     boolean isPersistent,
                                                                     Consumer<RangeHighlighterEx> changeAttributesAction) {
        TextRange hostRange = myDocument.injectedToHost(new ProperTextRange(startOffset, endOffset));
        return myHostModel.addRangeHighlighterAndChangeAttributes(hostRange.getStartOffset(), hostRange.getEndOffset(), layer, textAttributes,
                targetArea, isPersistent, changeAttributesAction);
    }

    @Override
    public void changeAttributesInBatch( RangeHighlighterEx highlighter,
                                         Consumer<RangeHighlighterEx> changeAttributesAction) {
        myHostModel.changeAttributesInBatch(highlighter, changeAttributesAction);
    }

    @Override
    
    public RangeHighlighter addLineHighlighter(final int line, final int layer, final TextAttributes textAttributes) {
        int hostLine = myDocument.injectedToHostLine(line);
        return myHostModel.addLineHighlighter(hostLine, layer, textAttributes);
    }

    @Override
    public void removeHighlighter( final RangeHighlighter rangeHighlighter) {
        myHostModel.removeHighlighter(rangeHighlighter);
    }

    @Override
    public void removeAllHighlighters() {
        myHostModel.removeAllHighlighters();
    }

    @Override
    
    public RangeHighlighter[] getAllHighlighters() {
        return myHostModel.getAllHighlighters();
    }

    @Override
    public void dispose() {
        myHostModel.dispose();
    }

    @Override
    public RangeHighlighterEx addPersistentLineHighlighter(final int line, final int layer, final TextAttributes textAttributes) {
        int hostLine = myDocument.injectedToHostLine(line);
        return myHostModel.addPersistentLineHighlighter(hostLine, layer, textAttributes);
    }


    @Override
    public boolean containsHighlighter( final RangeHighlighter highlighter) {
        return myHostModel.containsHighlighter(highlighter);
    }

    @Override
    public void addMarkupModelListener( Disposable parentDisposable,  MarkupModelListener listener) {
        myHostModel.addMarkupModelListener(parentDisposable, listener);
    }

    @Override
    public void setRangeHighlighterAttributes( final RangeHighlighter highlighter,  final TextAttributes textAttributes) {
        myHostModel.setRangeHighlighterAttributes(highlighter, textAttributes);
    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end,  Processor<? super RangeHighlighterEx> processor) {
        //todo
        return false;
    }

    @Override
    public boolean processRangeHighlightersOutside(int start, int end,  Processor<? super RangeHighlighterEx> processor) {
        //todo
        return false;
    }

    
    @Override
    public DisposableIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
        // todo convert
        return myHostModel.overlappingIterator(startOffset, endOffset);
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
