package com.gome.maven.openapi.diff.impl.incrementalMerge;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.impl.highlighting.FragmentSide;
import com.gome.maven.openapi.util.TextRange;

public class NoConflictChange extends TwoSideChange.SideChange<MergeNoConflict> {
    private static final Logger LOG = Logger.getInstance(NoConflictChange.class);

    private boolean myApplied;

    public NoConflictChange( MergeNoConflict twoSideChange,
                             FragmentSide mergeSide,
                             TextRange baseRange,
                             TextRange versionRange,
                             ChangeList changeList) {
        super(twoSideChange, changeList, ChangeType.fromRanges(baseRange, versionRange), mergeSide, versionRange);
    }

    @Override
    public void onApplied() {
        markApplied();

        NoConflictChange otherChange = myTwoSideChange.getOtherChange(this);
        LOG.assertTrue(otherChange != null, String.format("Other change is null. This change: %s Merge conflict: %s", this, myTwoSideChange));
        otherChange.markApplied();
    }

    @Override
    protected void markApplied() {
        if (!myApplied) {
            myApplied = true;
            super.markApplied();
        }
    }
}
