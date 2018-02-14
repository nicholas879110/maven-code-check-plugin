package com.gome.maven.openapi.diff.impl.incrementalMerge;

import com.gome.maven.openapi.diff.impl.highlighting.FragmentSide;
import com.gome.maven.openapi.util.TextRange;

public class MergeNoConflict extends TwoSideChange<NoConflictChange> {
    MergeNoConflict( TextRange baseRange,
                     TextRange leftRange,
                     TextRange rightRange,
                     MergeList mergeList) {
        super(baseRange, mergeList, new ChangeHighlighterHolder());
        myLeftChange = new NoConflictChange(this, FragmentSide.SIDE1, baseRange, leftRange, mergeList.getLeftChangeList());
        myRightChange = new NoConflictChange(this, FragmentSide.SIDE2, baseRange, rightRange, mergeList.getRightChangeList());
    }
}
