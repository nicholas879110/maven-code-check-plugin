package com.gome.maven.openapi.diff.impl.incrementalMerge;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.impl.highlighting.FragmentSide;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ex.DocumentEx;
import com.gome.maven.openapi.util.TextRange;

public abstract class TwoSideChange<T extends TwoSideChange.SideChange> extends ChangeSide implements DiffRangeMarker.RangeInvalidListener {
     protected final MergeList myMergeList;
     protected DiffRangeMarker myBaseRangeMarker;
    protected T myLeftChange;
    protected T myRightChange;
     protected final ChangeHighlighterHolder myCommonHighlighterHolder;

    protected TwoSideChange( TextRange baseRange,
                             MergeList mergeList,
                             ChangeHighlighterHolder highlighterHolder) {
        myBaseRangeMarker = new DiffRangeMarker((DocumentEx)mergeList.getBaseDocument(), baseRange, this);
        myMergeList = mergeList;
        myCommonHighlighterHolder = highlighterHolder;
    }

    
    public ChangeHighlighterHolder getHighlighterHolder() {
        return myCommonHighlighterHolder;
    }

    
    public DiffRangeMarker getRange() {
        return myBaseRangeMarker;
    }

    
    public Change getLeftChange() {
        return myLeftChange;
    }

    
    public Change getRightChange() {
        return myRightChange;
    }

    public void setRange( DiffRangeMarker range) {
        myBaseRangeMarker = range;
    }

    
    T getOtherChange( T change) {
        if (change == myLeftChange) {
            return myRightChange;
        }
        else if (change == myRightChange) {
            return myLeftChange;
        }
        else {
            throw new IllegalStateException("Unexpected change: " + change);
        }
    }

    public void removeOtherChange( T change) {
        if (change == myLeftChange) {
            myRightChange = null;
        }
        else if (change == myRightChange) {
            myLeftChange = null;
        }
        else {
            throw new IllegalStateException("Unexpected change: " + change);
        }
    }

    public void conflictRemoved() {
        removeHighlighters(myLeftChange);
        removeHighlighters(myRightChange);
        myCommonHighlighterHolder.removeHighlighters();
        myMergeList.removeChanges(myLeftChange, myRightChange);
        myBaseRangeMarker.removeListener(this);
    }

    private static <T extends SideChange> void removeHighlighters( T change) {
        if (change != null) {
            change.getOriginalSide().getHighlighterHolder().removeHighlighters();
        }
    }

    
    public Document getOriginalDocument(FragmentSide mergeSide) {
        return myMergeList.getChanges(mergeSide).getDocument(MergeList.BRANCH_SIDE);
    }

    public void onRangeInvalidated() {
        conflictRemoved();
    }

    
    public MergeList getMergeList() {
        return myMergeList;
    }

    protected static abstract class SideChange<V extends TwoSideChange> extends Change implements DiffRangeMarker.RangeInvalidListener {
        protected V myTwoSideChange;
         protected final ChangeList myChangeList;

        protected SimpleChangeSide myOriginalSide;
         protected ChangeType myType;

        protected SideChange( V twoSideChange,
                              ChangeList changeList,
                              ChangeType type,
                              FragmentSide mergeSide,
                              TextRange versionRange) {
            myTwoSideChange = twoSideChange;
            myChangeList = changeList;
            myOriginalSide =
                    new SimpleChangeSide(mergeSide, new DiffRangeMarker((DocumentEx)twoSideChange.getOriginalDocument(mergeSide), versionRange, this));
            myType = type;
        }

        
        public ChangeType getType() {
            return myType;
        }

        public SimpleChangeSide getOriginalSide() {
            return myOriginalSide;
        }

        protected void markApplied() {
            myType = ChangeType.deriveApplied(myType);
            myChangeList.apply(this);

            myOriginalSide.getHighlighterHolder().updateHighlighter(myOriginalSide, myType);
            myOriginalSide.getHighlighterHolder().setActions(new AnAction[0]);

            // display, what one side of the conflict was resolved to
            myTwoSideChange.getHighlighterHolder().updateHighlighter(myTwoSideChange, myType);
        }

        public ChangeList getChangeList() {
            return myTwoSideChange.getMergeList().getChanges(myOriginalSide.getFragmentSide());
        }

        @Override
        protected void changeSide(ChangeSide sideToChange, DiffRangeMarker newRange) {
            myTwoSideChange.setRange(newRange);
        }

        
        @Override
        public ChangeSide getChangeSide( FragmentSide side) {
            return isBranch(side) ? myOriginalSide : myTwoSideChange;
        }

        protected static boolean isBranch( FragmentSide side) {
            return MergeList.BRANCH_SIDE == side;
        }

        protected void removeFromList() {
            myTwoSideChange.conflictRemoved();
            myTwoSideChange = null;
        }

        public boolean isValid() {
            return myTwoSideChange != null;
        }

        public void onRemovedFromList() {
            myOriginalSide.getRange().removeListener(this);
            myTwoSideChange = null;
            myOriginalSide = null;
        }

        public void onRangeInvalidated() {
            removeFromList();
        }
    }
}
