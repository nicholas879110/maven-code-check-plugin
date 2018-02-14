/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.util;


import com.gome.maven.openapi.diagnostic.Logger;

import java.io.Serializable;

public class TextRange implements Segment, Serializable {
    private static final Logger LOG = Logger.getInstance(TextRange.class);
    private static final long serialVersionUID = -670091356599757430L;
    public static final TextRange EMPTY_RANGE = new TextRange(0,0);
    private final int myStartOffset;
    private final int myEndOffset;

    public TextRange(int startOffset, int endOffset) {
        this(startOffset, endOffset, true);
    }

    /**
     * @param checkForProperTextRange <code>true</code> if offsets should be checked by {@link #assertProperRange(int, int, Object)}
     * @see com.gome.maven.openapi.util.UnfairTextRange
     */
    protected TextRange(int startOffset, int endOffset, boolean checkForProperTextRange) {
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        if (checkForProperTextRange) {
            assertProperRange(this);
        }
    }

    @Override
    public final int getStartOffset() {
        return myStartOffset;
    }

    @Override
    public final int getEndOffset() {
        return myEndOffset;
    }

    public final int getLength() {
        return myEndOffset - myStartOffset;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TextRange)) return false;
        TextRange range = (TextRange)obj;
        return myStartOffset == range.myStartOffset && myEndOffset == range.myEndOffset;
    }

    @Override
    public int hashCode() {
        return myStartOffset + myEndOffset;
    }

    public boolean contains( TextRange range) {
        return contains((Segment)range);
    }
    public boolean contains( Segment range) {
        return containsRange(range.getStartOffset(), range.getEndOffset());
    }

    public boolean containsRange(int startOffset, int endOffset) {
        return getStartOffset() <= startOffset && getEndOffset() >= endOffset;
    }

    public static boolean containsRange( Segment outer,  Segment inner) {
        return outer.getStartOffset() <= inner.getStartOffset() && inner.getEndOffset() <= outer.getEndOffset();
    }

    public boolean containsOffset(int offset) {
        return myStartOffset <= offset && offset <= myEndOffset;
    }

    @Override
    public String toString() {
        return "(" + myStartOffset + "," + myEndOffset + ")";
    }

    public boolean contains(int offset) {
        return myStartOffset <= offset && offset < myEndOffset;
    }

    
    public String substring( String str) {
        try {
            return str.substring(myStartOffset, myEndOffset);
        }
        catch (StringIndexOutOfBoundsException e) {
            throw new StringIndexOutOfBoundsException("Can't extract " + this + " range from " + str);
        }
    }

    
    public CharSequence subSequence( CharSequence str) {
        try {
            return str.subSequence(myStartOffset, myEndOffset);
        }
        catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Can't extract " + this + " range from " + str);
        }
    }

    
    public TextRange cutOut( TextRange subRange) {
        assert subRange.getStartOffset() <= getLength() : subRange + "; this="+this;
        assert subRange.getEndOffset() <= getLength() : subRange + "; this="+this;
        return new TextRange(myStartOffset + subRange.getStartOffset(), Math.min(myEndOffset, myStartOffset + subRange.getEndOffset()));
    }

    
    public TextRange shiftRight(int delta) {
        if (delta == 0) return this;
        return new TextRange(myStartOffset + delta, myEndOffset + delta);
    }

    
    public TextRange grown(int lengthDelta) {
        return from(myStartOffset, getLength() + lengthDelta);
    }

    
    public static TextRange from(int offset, int length) {
        return create(offset, offset + length);
    }

    
    public static TextRange create(int startOffset, int endOffset) {
        return new TextRange(startOffset, endOffset);
    }
    
    public static TextRange create( Segment segment) {
        return create(segment.getStartOffset(), segment.getEndOffset());
    }

    public static boolean areSegmentsEqual( Segment segment1,  Segment segment2) {
        return segment1.getStartOffset() == segment2.getStartOffset()
                && segment1.getEndOffset() == segment2.getEndOffset();
    }

    
    public String replace( String original,  String replacement) {
        try {
            String beginning = original.substring(0, getStartOffset());
            String ending = original.substring(getEndOffset(), original.length());
            return beginning + replacement + ending;
        }
        catch (StringIndexOutOfBoundsException e) {
            throw new StringIndexOutOfBoundsException("Can't replace " + this + " range from '" + original + "' with '" + replacement + "'");
        }
    }

    public boolean intersects( TextRange textRange) {
        return intersects((Segment)textRange);
    }
    public boolean intersects( Segment textRange) {
        return intersects(textRange.getStartOffset(), textRange.getEndOffset());
    }
    public boolean intersects(int startOffset, int endOffset) {
        return Math.max(myStartOffset, startOffset) <= Math.min(myEndOffset, endOffset);
    }
    public boolean intersectsStrict( TextRange textRange) {
        return intersectsStrict(textRange.getStartOffset(), textRange.getEndOffset());
    }
    public boolean intersectsStrict(int startOffset, int endOffset) {
        return Math.max(myStartOffset, startOffset) < Math.min(myEndOffset, endOffset);
    }

    public TextRange intersection( TextRange range) {
        if (!intersects(range)) return null;
        return new TextRange(Math.max(myStartOffset, range.getStartOffset()), Math.min(myEndOffset, range.getEndOffset()));
    }

    public boolean isEmpty() {
        return myStartOffset >= myEndOffset;
    }

    
    public TextRange union( TextRange textRange) {
        return new TextRange(Math.min(myStartOffset, textRange.getStartOffset()), Math.max(myEndOffset, textRange.getEndOffset()));
    }

    public boolean equalsToRange(int startOffset, int endOffset) {
        return startOffset == myStartOffset && endOffset == myEndOffset;
    }

    public static TextRange allOf(String s) {
        return new TextRange(0, s.length());
    }

    public static void assertProperRange( Segment range) throws AssertionError {
        assertProperRange(range, "");
    }

    public static void assertProperRange( Segment range, Object message) throws AssertionError {
        assertProperRange(range.getStartOffset(), range.getEndOffset(), message);
    }

    public static void assertProperRange(int startOffset, int endOffset, Object message) {
        if (startOffset > endOffset) {
            LOG.error("Invalid range specified: (" + startOffset + "," + endOffset + "); " + message);
        }
        if (startOffset < 0) {
            LOG.error("Negative start offset: (" + startOffset + "," + endOffset + "); " + message);
        }
    }
}
