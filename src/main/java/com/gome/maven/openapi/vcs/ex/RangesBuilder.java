/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.ex;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.diff.Diff;
import com.gome.maven.util.diff.FilesTooBigForDiffException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * author: lesya
 */

public class RangesBuilder {
    private static final Logger LOG = Logger.getInstance(RangesBuilder.class);

     private final List<Range> myRanges;

    public RangesBuilder( Document current,
                          Document vcs) throws FilesTooBigForDiffException {
        this(current, vcs, LineStatusTracker.Mode.DEFAULT);
    }

    public RangesBuilder( Document current,
                          Document vcs,
                          LineStatusTracker.Mode mode)
            throws FilesTooBigForDiffException {
        this(new DocumentWrapper(current).getLines(), new DocumentWrapper(vcs).getLines(), 0, 0, mode);
    }

    public RangesBuilder( List<String> current,
                          List<String> vcs,
                         int shift,
                         int vcsShift,
                          LineStatusTracker.Mode mode)
            throws FilesTooBigForDiffException {
        myRanges = new LinkedList<Range>();

        switch (mode) {
            case DEFAULT:
                processDefault(current, vcs, shift, vcsShift);
                break;
            case SMART:
                processSmart(current, vcs, shift, vcsShift);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    
    public List<Range> getRanges() {
        return myRanges;
    }

    private void processDefault( List<String> current,
                                 List<String> vcs,
                                int shift,
                                int vcsShift) throws FilesTooBigForDiffException {
        Diff.Change ch = Diff.buildChanges(ArrayUtil.toStringArray(vcs), ArrayUtil.toStringArray(current));

        while (ch != null) {
            Range range = createOn(ch, shift, vcsShift);
            myRanges.add(range);
            ch = ch.link;
        }
    }

    private void processSmart( List<String> current,
                               List<String> vcs,
                              int shift,
                              int vcsShift) throws FilesTooBigForDiffException {
        Diff.Change ch = Diff.buildChanges(ArrayUtil.toStringArray(vcs), ArrayUtil.toStringArray(current));

        while (ch != null) {
            Range range = createOnSmart(ch, shift, vcsShift, current, vcs);
            myRanges.add(range);
            ch = ch.link;
        }
    }

    private static Range createOn( Diff.Change change, int shift, int vcsShift) {
        byte type = getChangeType(change);

        int offset1 = shift + change.line1;
        int offset2 = offset1 + change.inserted;

        int uOffset1 = vcsShift + change.line0;
        int uOffset2 = uOffset1 + change.deleted;

        return new Range(offset1, offset2, uOffset1, uOffset2, type);
    }

    private static Range createOnSmart( Diff.Change change,
                                       int shift,
                                       int vcsShift,
                                        List<String> current,
                                        List<String> vcs) throws FilesTooBigForDiffException {
        byte type = getChangeType(change);

        int offset1 = shift + change.line1;
        int offset2 = offset1 + change.inserted;

        int uOffset1 = vcsShift + change.line0;
        int uOffset2 = uOffset1 + change.deleted;

        if (type != Range.MODIFIED) {
            return new Range(offset1, offset2, uOffset1, uOffset2, type, Collections.singletonList(new Range.InnerRange(offset1, offset2, type)));
        }

        LineWrapper[] lines1 = new LineWrapper[change.deleted];
        LineWrapper[] lines2 = new LineWrapper[change.inserted];
        for (int i = 0; i < change.deleted; i++) {
            lines1[i] = new LineWrapper(vcs.get(i + change.line0));
        }
        for (int i = 0; i < change.inserted; i++) {
            lines2[i] = new LineWrapper(current.get(i + change.line1));
        }

        Diff.Change ch = Diff.buildChanges(lines1, lines2);

        List<Range.InnerRange> inner = new ArrayList<Range.InnerRange>();

        int last0 = 0;
        int last1 = 0;
        while (ch != null) {
            if (ch.line0 != last0 && ch.line1 != last1) {
                byte innerType = Range.EQUAL;
                int innerStart = shift + change.line1 + last1;
                int innerEnd = shift + change.line1 + ch.line1;
                inner.add(new Range.InnerRange(innerStart, innerEnd, innerType));
            }

            byte innerType = getChangeType(ch);
            int innerStart = shift + change.line1 + ch.line1;
            int innerEnd = innerStart + ch.inserted;
            inner.add(new Range.InnerRange(innerStart, innerEnd, innerType));

            last0 = ch.line0 + ch.deleted;
            last1 = ch.line1 + ch.inserted;

            ch = ch.link;
        }
        if (change.deleted != last0 && change.inserted != last1) {
            byte innerType = Range.EQUAL;
            int innerStart = shift + change.line1 + last1;
            int innerEnd = shift + change.line1 + change.inserted;
            inner.add(new Range.InnerRange(innerStart, innerEnd, innerType));
        }

        return new Range(offset1, offset2, uOffset1, uOffset2, type, inner);
    }

    private static byte getChangeType( Diff.Change change) {
        if ((change.deleted > 0) && (change.inserted > 0)) return Range.MODIFIED;
        if ((change.deleted > 0)) return Range.DELETED;
        if ((change.inserted > 0)) return Range.INSERTED;
        LOG.error("Unknown change type");
        return Range.EQUAL;
    }

    private static class LineWrapper {
         private final String myLine;
        private final int myHash;

        public LineWrapper( String line) {
            myLine = line;
            myHash = StringUtil.stringHashCodeIgnoreWhitespaces(line);
        }

        
        public String getLine() {
            return myLine;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LineWrapper wrapper = (LineWrapper)o;

            if (myHash != wrapper.myHash) return false;

            return StringUtil.equalsIgnoreWhitespaces(myLine, wrapper.myLine);
        }

        @Override
        public int hashCode() {
            return myHash;
        }
    }
}
