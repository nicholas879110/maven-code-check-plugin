/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ex.MarkupModelEx;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.util.Getter;

/**
 * User: cdr
 */
public class RangeHighlighterTree extends RangeMarkerTree<RangeHighlighterEx> {
    private final MarkupModelEx myMarkupModel;

    public RangeHighlighterTree( Document document,  MarkupModelEx markupModel) {
        super(document);
        myMarkupModel = markupModel;
    }

    @Override
    protected int compareEqualStartIntervals( IntervalNode<RangeHighlighterEx> i1,  IntervalNode<RangeHighlighterEx> i2) {
        RHNode o1 = (RHNode)i1;
        RHNode o2 = (RHNode)i2;
        int d = o2.myLayer - o1.myLayer;
        if (d != 0) {
            return d;
        }
        return super.compareEqualStartIntervals(i1, i2);
    }

    
    @Override
    protected RHNode createNewNode( RangeHighlighterEx key, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {
        return new RHNode(this, key, start, end, greedyToLeft, greedyToRight,layer);
    }

    static class RHNode extends RMNode<RangeHighlighterEx> {
        final int myLayer;

        public RHNode( RangeHighlighterTree rangeMarkerTree,
                       final RangeHighlighterEx key,
                      int start,
                      int end,
                      boolean greedyToLeft,
                      boolean greedyToRight,
                      int layer) {
            super(rangeMarkerTree, key, start, end, greedyToLeft, greedyToRight);
            myLayer = layer;
        }

        //range highlighters are strongly referenced
        @Override
        protected Getter<RangeHighlighterEx> createGetter( RangeHighlighterEx interval) {
            return (Getter<RangeHighlighterEx>)interval;
        }
    }

    @Override
    void reportInvalidation(RangeHighlighterEx markerEx, Object reason) {
        super.reportInvalidation(markerEx, reason);
        myMarkupModel.fireBeforeRemoved(markerEx);
    }
}
