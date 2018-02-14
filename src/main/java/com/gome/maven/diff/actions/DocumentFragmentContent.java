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
package com.gome.maven.diff.actions;

import com.gome.maven.diff.contents.DocumentContent;
import com.gome.maven.openapi.diff.FragmentContent;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.LineSeparator;

import java.nio.charset.Charset;

/**
 * Represents sub text of other content. Original content should provide not null document.
 */
public class DocumentFragmentContent implements DocumentContent {
    // TODO: reuse DocumentWindow ?

    public static final Key<Document> ORIGINAL_DOCUMENT = FragmentContent.ORIGINAL_DOCUMENT; // TODO: replace with own one ?

     private final DocumentContent myOriginal;

     private final MyDocumentsSynchronizer mySynchonizer;

    private int myAssignments = 0;

    public DocumentFragmentContent( Project project,  DocumentContent original,  TextRange range) {
        myOriginal = original;

        RangeMarker rangeMarker = myOriginal.getDocument().createRangeMarker(range.getStartOffset(), range.getEndOffset(), true);
        rangeMarker.setGreedyToLeft(true);
        rangeMarker.setGreedyToRight(true);

        mySynchonizer = new MyDocumentsSynchronizer(project, rangeMarker);
    }

    
    @Override
    public Document getDocument() {
        return mySynchonizer.getDocument2();
    }

    
    @Override
    public VirtualFile getHighlightFile() {
        return myOriginal.getHighlightFile();
    }

    
    @Override
    public OpenFileDescriptor getOpenFileDescriptor(int offset) {
        return myOriginal.getOpenFileDescriptor(offset + mySynchonizer.getStartOffset());
    }

    
    @Override
    public LineSeparator getLineSeparator() {
        return null;
    }

    
    @Override
    public Charset getCharset() {
        return null;
    }

    
    @Override
    public FileType getContentType() {
        return myOriginal.getContentType();
    }

    
    @Override
    public OpenFileDescriptor getOpenFileDescriptor() {
        return getOpenFileDescriptor(0);
    }

    @Override
    public void onAssigned(boolean isAssigned) {
        if (isAssigned) {
            if (myAssignments == 0) mySynchonizer.startListen();
            myAssignments++;
        }
        else {
            myAssignments--;
            if (myAssignments == 0) mySynchonizer.stopListen();
        }
        assert myAssignments >= 0;
    }

    private static class MyDocumentsSynchronizer extends DocumentsSynchronizer {
         private final RangeMarker myRangeMarker;

        public MyDocumentsSynchronizer( Project project,  RangeMarker originalRange) {
            super(project, getOriginal(originalRange), getCopy(originalRange));
            myRangeMarker = originalRange;
        }

        public int getStartOffset() {
            return myRangeMarker.getStartOffset();
        }

        public int getEndOffset() {
            return myRangeMarker.getEndOffset();
        }

        @Override
        protected void onDocumentChanged1( DocumentEvent event) {
            if (!myRangeMarker.isValid()) {
                myDocument2.setReadOnly(false);
                replaceString(myDocument2, 0, myDocument2.getTextLength(), "Invalid selection range");
                myDocument2.setReadOnly(true);
                return;
            }
            CharSequence newText = myDocument1.getCharsSequence().subSequence(myRangeMarker.getStartOffset(), myRangeMarker.getEndOffset());
            replaceString(myDocument2, 0, myDocument2.getTextLength(), newText);
        }

        @Override
        protected void onDocumentChanged2( DocumentEvent event) {
            if (!myRangeMarker.isValid()) {
                return;
            }
            if (!myDocument1.isWritable()) return;

            CharSequence newText = event.getNewFragment();
            int originalOffset = event.getOffset() + myRangeMarker.getStartOffset();
            int originalEnd = originalOffset + event.getOldLength();
            replaceString(myDocument1, originalOffset, originalEnd, newText);
        }

        @Override
        public void startListen() {
            if (myRangeMarker.isValid()) {
                myDocument2.setReadOnly(false);
                CharSequence nexText = myDocument1.getCharsSequence().subSequence(myRangeMarker.getStartOffset(), myRangeMarker.getEndOffset());
                replaceString(myDocument2, 0, myDocument2.getTextLength(), nexText);
                myDocument2.setReadOnly(!myDocument1.isWritable());
            }
            else {
                myDocument2.setReadOnly(false);
                replaceString(myDocument2, 0, myDocument2.getTextLength(), "Invalid selection range");
                myDocument2.setReadOnly(true);
            }
            super.startListen();
        }
    }

    
    protected static Document getOriginal( RangeMarker rangeMarker) {
        return rangeMarker.getDocument();
    }

    
    protected static Document getCopy( RangeMarker rangeMarker) {
        final Document originalDocument = rangeMarker.getDocument();

        Document result = EditorFactory.getInstance().createDocument("");
        result.putUserData(ORIGINAL_DOCUMENT, originalDocument);
        return result;
    }
}
