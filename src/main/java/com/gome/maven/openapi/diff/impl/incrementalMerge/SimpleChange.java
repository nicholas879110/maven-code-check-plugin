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
package com.gome.maven.openapi.diff.impl.incrementalMerge;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.impl.highlighting.FragmentSide;
import com.gome.maven.openapi.util.TextRange;

class SimpleChange extends Change implements DiffRangeMarker.RangeInvalidListener{
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.diff.impl.incrementalMerge.Change");
    private ChangeType myType;
    private final SimpleChangeSide[] mySides;
    private final ChangeList myChangeList;

    SimpleChange( ChangeType type,  TextRange range1,  TextRange range2,  ChangeList changeList) {
        mySides = new SimpleChangeSide[]{ createSide(changeList, range1, FragmentSide.SIDE1),
                createSide(changeList, range2, FragmentSide.SIDE2)};
        myType = type;
        myChangeList = changeList;
    }

    
    private SimpleChangeSide createSide( ChangeList changeList,  TextRange range1,  FragmentSide side) {
        return new SimpleChangeSide(side, new DiffRangeMarker(changeList.getDocument(side), range1, this));
    }

    /**
     * Changes the given Side of a Change to a new text range.
     * @param sideToChange Side to be changed.
     * @param newRange     New change range.
     */
    @Override
    protected void changeSide( ChangeSide sideToChange,  DiffRangeMarker newRange) {
        for (int i = 0; i < mySides.length; i++) {
            SimpleChangeSide side = mySides[i];
            if (side.equals(sideToChange)) {
                mySides[i] = new SimpleChangeSide(sideToChange, newRange);
                break;
            }
        }
    }

    @Override
    protected void removeFromList() {
        myChangeList.remove(this);
    }

    @Override
    
    public ChangeSide getChangeSide( FragmentSide side) {
        return mySides[side.getIndex()];
    }

    @Override
    public ChangeType getType() {
        return myType;
    }

    @Override
    public ChangeList getChangeList() {
        return myChangeList;
    }

    @Override
    public void onApplied() {
        myType = ChangeType.deriveApplied(myType);
        for (SimpleChangeSide side : mySides) {
            ChangeHighlighterHolder highlighterHolder = side.getHighlighterHolder();
            highlighterHolder.setActions(new AnAction[0]);
            highlighterHolder.updateHighlighter(side, myType);
        }
        myChangeList.apply(this);
    }

    @Override
    public void onRemovedFromList() {
        for (int i = 0; i < mySides.length; i++) {
            SimpleChangeSide side = mySides[i];
            side.getRange().removeListener(this);
            side.getHighlighterHolder().removeHighlighters();
            mySides[i] = null;
        }
    }

    @Override
    public boolean isValid() {
        LOG.assertTrue((mySides[0] == null) == (mySides[1] == null));
        return mySides[0] != null;
    }

    @Override
    public void onRangeInvalidated() {
        myChangeList.remove(this);
    }

    static Change fromRanges( TextRange baseRange,  TextRange versionRange,  ChangeList changeList) {
        ChangeType type = ChangeType.fromRanges(baseRange, versionRange);
        return new SimpleChange(type, baseRange, versionRange, changeList);
    }
}
