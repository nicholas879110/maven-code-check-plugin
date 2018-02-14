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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Apr 19, 2002
 * Time: 2:26:19 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.openapi.editor.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ex.DisposableIterator;
import com.gome.maven.openapi.editor.ex.DocumentEx;
import com.gome.maven.openapi.editor.ex.MarkupModelEx;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.editor.impl.event.MarkupModelListener;
import com.gome.maven.openapi.editor.markup.HighlighterTargetArea;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.util.CommonProcessors;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.DocumentUtil;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;

public class MarkupModelImpl extends UserDataHolderBase implements MarkupModelEx {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.editor.impl.MarkupModelImpl");
    private final DocumentEx myDocument;

    private RangeHighlighter[] myCachedHighlighters;
    private final List<MarkupModelListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private final RangeHighlighterTree myHighlighterTree;          // this tree holds regular highlighters with target = HighlighterTargetArea.EXACT_RANGE
    private final RangeHighlighterTree myHighlighterTreeForLines;  // this tree holds line range highlighters with target = HighlighterTargetArea.LINES_IN_RANGE

    MarkupModelImpl( DocumentEx document) {
        myDocument = document;
        myHighlighterTree = new RangeHighlighterTree(document, this);
        myHighlighterTreeForLines = new RangeHighlighterTree(document, this);
    }

    @Override
    public void dispose() {
        myHighlighterTree.dispose();
        myHighlighterTreeForLines.dispose();
    }

    @Override
    
    public RangeHighlighter addLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        if (isNotValidLine(lineNumber)) {
            throw new IndexOutOfBoundsException("lineNumber:" + lineNumber + ". Must be in [0, " + (getDocument().getLineCount() - 1) + "]");
        }

        int offset = DocumentUtil.getFirstNonSpaceCharOffset(getDocument(), lineNumber);
        return addRangeHighlighter(offset, offset, layer, textAttributes, HighlighterTargetArea.LINES_IN_RANGE);
    }

    @Override
    
    public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
        if (isNotValidLine(lineNumber)) {
            return null;
        }

        int offset = DocumentUtil.getFirstNonSpaceCharOffset(getDocument(), lineNumber);
        return addRangeHighlighter(PersistentRangeHighlighterImpl.create(this, offset, layer, HighlighterTargetArea.LINES_IN_RANGE, textAttributes, false), null);
    }

    private boolean isNotValidLine(int lineNumber) {
        return lineNumber >= getDocument().getLineCount() || lineNumber < 0;
    }

    // NB: Can return invalid highlighters
    @Override
    
    public RangeHighlighter[] getAllHighlighters() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (myCachedHighlighters == null) {
            int size = myHighlighterTree.size() + myHighlighterTreeForLines.size();
            if (size == 0) return RangeHighlighter.EMPTY_ARRAY;
            List<RangeHighlighterEx> list = new ArrayList<RangeHighlighterEx>(size);
            CommonProcessors.CollectProcessor<RangeHighlighterEx> collectProcessor = new CommonProcessors.CollectProcessor<RangeHighlighterEx>(list);
            myHighlighterTree.process(collectProcessor);
            myHighlighterTreeForLines.process(collectProcessor);
            myCachedHighlighters = list.toArray(new RangeHighlighter[list.size()]);
        }
        return myCachedHighlighters;
    }

    
    @Override
    public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                                     int endOffset,
                                                                     int layer,
                                                                     TextAttributes textAttributes,
                                                                      HighlighterTargetArea targetArea,
                                                                     boolean isPersistent,
                                                                      Consumer<RangeHighlighterEx> changeAttributesAction) {
        return addRangeHighlighter(isPersistent
                ? PersistentRangeHighlighterImpl.create(this, startOffset, layer, targetArea, textAttributes, true)
                : new RangeHighlighterImpl(this, startOffset, endOffset, layer, targetArea, textAttributes, false,
                false), changeAttributesAction);
    }

    
    private RangeHighlighterEx addRangeHighlighter( RangeHighlighterImpl highlighter,
                                                    Consumer<RangeHighlighterEx> changeAttributesAction) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        myCachedHighlighters = null;
        if (changeAttributesAction != null) {
            highlighter.changeAttributesNoEvents(changeAttributesAction);
        }
        fireAfterAdded(highlighter);
        return highlighter;
    }

    @Override
    public void changeAttributesInBatch( RangeHighlighterEx highlighter,
                                         Consumer<RangeHighlighterEx> changeAttributesAction) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        RangeHighlighterImpl.ChangeResult changed = ((RangeHighlighterImpl)highlighter).changeAttributesNoEvents(changeAttributesAction);
        if (changed != RangeHighlighterImpl.ChangeResult.NOT_CHANGED) {
            fireAttributesChanged(highlighter, changed == RangeHighlighterImpl.ChangeResult.RENDERERS_CHANGED);
        }
    }

    @Override
    public void addRangeHighlighter( RangeHighlighterEx marker,
                                    int start,
                                    int end,
                                    boolean greedyToLeft,
                                    boolean greedyToRight,
                                    int layer) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        treeFor(marker).addInterval(marker, start, end, greedyToLeft, greedyToRight, layer);
    }

    private RangeHighlighterTree treeFor(RangeHighlighter marker) {
        return marker.getTargetArea() == HighlighterTargetArea.EXACT_RANGE ? myHighlighterTree : myHighlighterTreeForLines;
    }

    @Override
    
    public RangeHighlighter addRangeHighlighter(int startOffset,
                                                int endOffset,
                                                int layer,
                                                TextAttributes textAttributes,
                                                 HighlighterTargetArea targetArea) {
        return addRangeHighlighterAndChangeAttributes(startOffset, endOffset, layer, textAttributes, targetArea, false, null);
    }

    @Override
    public void removeHighlighter( RangeHighlighter segmentHighlighter) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        myCachedHighlighters = null;
        if (!segmentHighlighter.isValid()) return;

        boolean removed = treeFor(segmentHighlighter).removeInterval((RangeHighlighterEx)segmentHighlighter);
        LOG.assertTrue(removed);
    }

    @Override
    public void removeAllHighlighters() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        for (RangeHighlighter highlighter : getAllHighlighters()) {
            highlighter.dispose();
        }
        myCachedHighlighters = null;
        myHighlighterTree.clear();
        myHighlighterTreeForLines.clear();
    }

    @Override
    
    public Document getDocument() {
        return myDocument;
    }

    @Override
    public void addMarkupModelListener( Disposable parentDisposable,  final MarkupModelListener listener) {
        myListeners.add(listener);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removeMarkupModelListener(listener);
            }
        });
    }

    void removeMarkupModelListener( MarkupModelListener listener) {
        boolean success = myListeners.remove(listener);
        LOG.assertTrue(success);
    }

    @Override
    public void setRangeHighlighterAttributes( final RangeHighlighter highlighter,  final TextAttributes textAttributes) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        ((RangeHighlighterEx)highlighter).setTextAttributes(textAttributes);
    }

    @Override
    public void fireAttributesChanged( RangeHighlighterEx segmentHighlighter, boolean renderersChanged) {
        for (MarkupModelListener listener : myListeners) {
            listener.attributesChanged(segmentHighlighter, renderersChanged);
        }
    }

    @Override
    public void fireAfterAdded( RangeHighlighterEx segmentHighlighter) {
        for (MarkupModelListener listener : myListeners) {
            listener.afterAdded(segmentHighlighter);
        }
    }

    @Override
    public void fireBeforeRemoved( RangeHighlighterEx segmentHighlighter) {
        for (MarkupModelListener listener : myListeners) {
            listener.beforeRemoved(segmentHighlighter);
        }
    }

    @Override
    public boolean containsHighlighter( final RangeHighlighter highlighter) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        Processor<RangeHighlighterEx> equalId = new Processor<RangeHighlighterEx>() {
            @Override
            public boolean process(RangeHighlighterEx h) {
                return h.getId() != ((RangeHighlighterEx)highlighter).getId();
            }
        };
        return !treeFor(highlighter).processOverlappingWith(highlighter.getStartOffset(), highlighter.getEndOffset(), equalId);
    }

    @Override
    public boolean processRangeHighlightersOverlappingWith(int start, int end,  Processor<? super RangeHighlighterEx> processor) {
        DisposableIterator<RangeHighlighterEx> iterator = overlappingIterator(start, end);
        try {
            while (iterator.hasNext()) {
                if (!processor.process(iterator.next())) {
                    return false;
                }
            }
            return true;
        }
        finally {
            iterator.dispose();
        }
    }

    @Override
    public boolean processRangeHighlightersOutside(int start, int end,  Processor<? super RangeHighlighterEx> processor) {
        return myHighlighterTree.processOverlappingWithOutside(start, end, processor)
                && myHighlighterTreeForLines.processOverlappingWithOutside(start, end, processor);
    }

    @Override
    
    public IntervalTreeImpl.PeekableIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
        startOffset = Math.max(0,startOffset);
        endOffset = Math.max(startOffset, endOffset);
        return IntervalTreeImpl
                .mergingOverlappingIterator(myHighlighterTree, new TextRangeInterval(startOffset, endOffset), myHighlighterTreeForLines,
                        roundToLineBoundaries(startOffset, endOffset), RangeHighlighterEx.BY_AFFECTED_START_OFFSET);
    }

    
    private TextRangeInterval roundToLineBoundaries(int startOffset, int endOffset) {
        Document document = getDocument();
        int textLength = document.getTextLength();
        int lineStartOffset = startOffset <= 0 ? 0 : startOffset > textLength ? textLength : document.getLineStartOffset(document.getLineNumber(startOffset));
        int lineEndOffset = endOffset <= 0 ? 0 : endOffset >= textLength ? textLength : document.getLineEndOffset(document.getLineNumber(endOffset));
        return new TextRangeInterval(lineStartOffset, lineEndOffset);
    }
}
