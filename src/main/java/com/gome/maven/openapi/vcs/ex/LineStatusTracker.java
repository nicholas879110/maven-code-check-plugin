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
package com.gome.maven.openapi.vcs.ex;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.undo.UndoConstants;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.impl.DocumentImpl;
import com.gome.maven.openapi.editor.impl.DocumentMarkupModel;
import com.gome.maven.openapi.editor.markup.*;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.changes.VcsDirtyScopeManager;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.EditorNotificationPanel;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.diff.FilesTooBigForDiffException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * @author irengrig
 *         author: lesya
 */
public class LineStatusTracker {
    public enum Mode {DEFAULT, SMART}

    public static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.ex.LineStatusTracker");
    private static final Key<CanNotCalculateDiffPanel> PANEL_KEY =
            new Key<CanNotCalculateDiffPanel>("LineStatusTracker.CanNotCalculateDiffPanel");

    private final Object myLock = new Object();
    private boolean myInitialized;

     private final Project myProject;
     private final Document myDocument;
     private final Document myVcsDocument;
     private final VirtualFile myVirtualFile;

     private final Application myApplication;
     private final FileEditorManager myFileEditorManager;
     private final VcsDirtyScopeManager myVcsDirtyScopeManager;

    private MyDocumentListener myDocumentListener;
     private RevisionPack myBaseRevisionNumber;

    private boolean mySuppressUpdate;
    private boolean myBulkUpdate;
    private boolean myAnathemaThrown;
    private boolean myReleased;

     private Mode myMode = Registry.is("diff.status.tracker.smart") ? Mode.SMART : Mode.DEFAULT;

     private List<Range> myRanges;

    private LineStatusTracker( final Document document,
                               final Document vcsDocument,
                               final Project project,
                               final VirtualFile virtualFile) {
        myDocument = document;
        myVcsDocument = vcsDocument;
        myProject = project;
        myVirtualFile = virtualFile;

        myApplication = ApplicationManager.getApplication();
        myFileEditorManager = FileEditorManager.getInstance(myProject);
        myVcsDirtyScopeManager = VcsDirtyScopeManager.getInstance(myProject);

        myRanges = new ArrayList<Range>();
    }

    public void initialize( final String vcsContent,  RevisionPack baseRevisionNumber) {
        myApplication.assertIsDispatchThread();

        synchronized (myLock) {
            try {
                if (myReleased) return;
                if (myBaseRevisionNumber != null && myBaseRevisionNumber.contains(baseRevisionNumber)) return;

                myBaseRevisionNumber = baseRevisionNumber;

                myVcsDocument.setReadOnly(false);
                myVcsDocument.setText(vcsContent);
                myVcsDocument.setReadOnly(true);
                reinstallRanges();

                if (myDocumentListener == null) {
                    myDocumentListener = new MyDocumentListener();
                    myDocument.addDocumentListener(myDocumentListener);
                }
            }
            finally {
                myInitialized = true;
            }
        }
    }

    private void reinstallRanges() {
        myApplication.assertIsDispatchThread();

        synchronized (myLock) {
            removeAnathema();
            removeHighlightersFromMarkupModel();
            try {
                myRanges = new RangesBuilder(myDocument, myVcsDocument, myMode).getRanges();
            }
            catch (FilesTooBigForDiffException e) {
                installAnathema();
                return;
            }
            for (final Range range : myRanges) {
                range.setHighlighter(createHighlighter(range));
            }
        }
    }

    private void installAnathema() {
        myAnathemaThrown = true;
        final FileEditor[] editors = myFileEditorManager.getAllEditors(myVirtualFile);
        for (FileEditor editor : editors) {
            CanNotCalculateDiffPanel panel = editor.getUserData(PANEL_KEY);
            if (panel == null) {
                final CanNotCalculateDiffPanel newPanel = new CanNotCalculateDiffPanel();
                editor.putUserData(PANEL_KEY, newPanel);
                myFileEditorManager.addTopComponent(editor, newPanel);
            }
        }
    }

    private void removeAnathema() {
        if (!myAnathemaThrown) return;
        myAnathemaThrown = false;
        final FileEditor[] editors = myFileEditorManager.getEditors(myVirtualFile);
        for (FileEditor editor : editors) {
            final CanNotCalculateDiffPanel panel = editor.getUserData(PANEL_KEY);
            if (panel != null) {
                myFileEditorManager.removeTopComponent(editor, panel);
                editor.putUserData(PANEL_KEY, null);
            }
        }
    }

    public void setMode( Mode mode) {
        synchronized (myLock) {
            myMode = mode;
            reinstallRanges();
        }
    }

    
    private RangeHighlighter createHighlighter( Range range) {
        myApplication.assertIsDispatchThread();

        LOG.assertTrue(!myReleased, "Already released");

        int first =
                range.getLine1() >= getLineCount(myDocument) ? myDocument.getTextLength() : myDocument.getLineStartOffset(range.getLine1());

        int second =
                range.getLine2() >= getLineCount(myDocument) ? myDocument.getTextLength() : myDocument.getLineStartOffset(range.getLine2());

        final TextAttributes attr = LineStatusTrackerDrawing.getAttributesFor(range);
        final RangeHighlighter highlighter = DocumentMarkupModel.forDocument(myDocument, myProject, true)
                .addRangeHighlighter(first, second, HighlighterLayer.FIRST - 1, attr, HighlighterTargetArea.LINES_IN_RANGE);

        highlighter.setThinErrorStripeMark(true);
        highlighter.setGreedyToLeft(true);
        highlighter.setGreedyToRight(true);
        highlighter.setLineMarkerRenderer(LineStatusTrackerDrawing.createRenderer(range, this));
        highlighter.setEditorFilter(MarkupEditorFilterFactory.createIsNotDiffFilter());

        final String tooltip;
        if (range.getLine1() == range.getLine2()) {
            if (range.getVcsLine1() + 1 == range.getVcsLine2()) {
                tooltip = VcsBundle.message("tooltip.text.line.before.deleted", range.getLine1() + 1);
            }
            else {
                tooltip = VcsBundle.message("tooltip.text.lines.before.deleted", range.getLine1() + 1, range.getVcsLine2() - range.getVcsLine1());
            }
        }
        else if (range.getLine1() + 1 == range.getLine2()) {
            tooltip = VcsBundle.message("tooltip.text.line.changed", range.getLine1() + 1);
        }
        else {
            tooltip = VcsBundle.message("tooltip.text.lines.changed", range.getLine1() + 1, range.getLine2());
        }

        highlighter.setErrorStripeTooltip(tooltip);
        return highlighter;
    }

    public void release() {
        synchronized (myLock) {
            myReleased = true;
            if (myDocumentListener != null) {
                myDocument.removeDocumentListener(myDocumentListener);
            }

            if (myApplication.isDispatchThread()) {
                removeAnathema();
                removeHighlightersFromMarkupModel();
            }
            else {
                invalidateRanges();
                myApplication.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        removeAnathema();
                        removeHighlightersFromMarkupModel();
                    }
                });
            }
        }
    }

    
    Project getProject() {
        return myProject;
    }

    
    public Document getDocument() {
        return myDocument;
    }

    
    public Document getVcsDocument() {
        return myVcsDocument;
    }

    
    public VirtualFile getVirtualFile() {
        return myVirtualFile;
    }

    
    public List<Range> getRanges() {
        synchronized (myLock) {
            return myRanges;
        }
    }

    public void startBulkUpdate() {
        synchronized (myLock) {
            if (myReleased) return;

            myBulkUpdate = true;
            removeAnathema();
            removeHighlightersFromMarkupModel();
        }
    }

    private void removeHighlightersFromMarkupModel() {
        myApplication.assertIsDispatchThread();

        synchronized (myLock) {
            for (Range range : myRanges) {
                if (range.getHighlighter() != null) {
                    range.getHighlighter().dispose();
                }
                range.invalidate();
            }
            myRanges.clear();
        }
    }

    private void invalidateRanges() {
        synchronized (myLock) {
            for (Range range : myRanges) {
                range.invalidate();
            }
        }
    }

    public void finishBulkUpdate() {
        synchronized (myLock) {
            if (myReleased) return;

            myBulkUpdate = false;
            reinstallRanges();
        }
    }

    private void markFileUnchanged() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FileDocumentManager.getInstance().saveDocument(myDocument);
                boolean stillEmpty;
                synchronized (myLock) {
                    stillEmpty = myRanges.isEmpty();
                }
                if (stillEmpty) {
                    // file was modified, and now it's not -> dirty local change
                    myVcsDirtyScopeManager.fileDirty(myVirtualFile);
                }
            }
        });
    }

    private class MyDocumentListener extends DocumentAdapter {
        // We have 3 document versions:
        // * VCS version
        // * before change
        // * after change

        private int myLine1;
        private int myBeforeChangedLines;
        private int myBeforeTotalLines;

        @Override
        public void beforeDocumentChange(DocumentEvent e) {
            myApplication.assertIsDispatchThread();

            synchronized (myLock) {
                if (myReleased) return;
                if (myBulkUpdate || mySuppressUpdate || myAnathemaThrown || !myInitialized) return;
                assert myDocument == e.getDocument();

                try {
                    myLine1 = myDocument.getLineNumber(e.getOffset());
                    if (e.getOldLength() == 0) {
                        myBeforeChangedLines = 1;
                    }
                    else {
                        int line1 = myLine1;
                        int line2 = myDocument.getLineNumber(e.getOffset() + e.getOldLength());
                        myBeforeChangedLines = line2 - line1 + 1;
                    }

                    myBeforeTotalLines = getLineCount(myDocument);
                }
                catch (ProcessCanceledException ignore) {
                }
            }
        }

        @Override
        public void documentChanged(final DocumentEvent e) {
            myApplication.assertIsDispatchThread();

            synchronized (myLock) {
                if (myReleased) return;
                if (myBulkUpdate || mySuppressUpdate || myAnathemaThrown || !myInitialized) return;
                assert myDocument == e.getDocument();

                int afterChangedLines;
                if (e.getNewLength() == 0) {
                    afterChangedLines = 1;
                }
                else {
                    int line1 = myLine1;
                    int line2 = myDocument.getLineNumber(e.getOffset() + e.getNewLength());
                    afterChangedLines = line2 - line1 + 1;
                }

                int linesShift = afterChangedLines - myBeforeChangedLines;

                int line1 = myLine1;
                int line2 = line1 + myBeforeChangedLines;

                int[] fixed = fixRanges(e, line1, line2);
                line1 = fixed[0];
                line2 = fixed[1];

                doUpdateRanges(line1, line2, linesShift, myBeforeTotalLines);
            }
        }
    }

    
    private int[] fixRanges( DocumentEvent e, int line1, int line2) {
        CharSequence document = myDocument.getCharsSequence();
        int offset = e.getOffset();

        if (e.getOldLength() == 0 && e.getNewLength() != 0) {
            if (StringUtil.endsWithChar(e.getNewFragment(), '\n') && isNewline(offset - 1, document)) {
                return new int[]{line1, line2 - 1};
            }
            if (StringUtil.startsWithChar(e.getNewFragment(), '\n') && isNewline(offset + e.getNewLength(), document)) {
                return new int[]{line1 + 1, line2};
            }
        }
        if (e.getOldLength() != 0 && e.getNewLength() == 0) {
            if (StringUtil.endsWithChar(e.getOldFragment(), '\n') && isNewline(offset - 1, document)) {
                return new int[]{line1, line2 - 1};
            }
            if (StringUtil.startsWithChar(e.getOldFragment(), '\n') && isNewline(offset + e.getNewLength(), document)) {
                return new int[]{line1 + 1, line2};
            }
        }

        return new int[]{line1, line2};
    }

    private static boolean isNewline(int offset,  CharSequence sequence) {
        if (offset < 0) return false;
        if (offset >= sequence.length()) return false;
        return sequence.charAt(offset) == '\n';
    }

    private void doUpdateRanges(int beforeChangedLine1,
                                int beforeChangedLine2,
                                int linesShift,
                                int beforeTotalLines) {
        List<Range> rangesBeforeChange = new ArrayList<Range>();
        List<Range> rangesAfterChange = new ArrayList<Range>();
        List<Range> changedRanges = new ArrayList<Range>();

        sortRanges(beforeChangedLine1, beforeChangedLine2, linesShift, rangesBeforeChange, changedRanges, rangesAfterChange);

        Range firstChangedRange = ContainerUtil.getFirstItem(changedRanges);
        Range lastChangedRange = ContainerUtil.getLastItem(changedRanges);

        if (firstChangedRange != null && firstChangedRange.getLine1() < beforeChangedLine1) {
            beforeChangedLine1 = firstChangedRange.getLine1();
        }
        if (lastChangedRange != null && lastChangedRange.getLine2() > beforeChangedLine2) {
            beforeChangedLine2 = lastChangedRange.getLine2();
        }

        doUpdateRanges(beforeChangedLine1, beforeChangedLine2, linesShift, beforeTotalLines,
                rangesBeforeChange, changedRanges, rangesAfterChange);
    }

    private void doUpdateRanges(int beforeChangedLine1,
                                int beforeChangedLine2,
                                int linesShift, // before -> after
                                int beforeTotalLines,
                                 List<Range> rangesBefore,
                                 List<Range> changedRanges,
                                 List<Range> rangesAfter) {
        try {
            int vcsTotalLines = getLineCount(myVcsDocument);

            Range lastRangeBefore = ContainerUtil.getLastItem(rangesBefore);
            Range firstRangeAfter = ContainerUtil.getFirstItem(rangesAfter);

            //noinspection UnnecessaryLocalVariable
            int afterChangedLine1 = beforeChangedLine1;
            int afterChangedLine2 = beforeChangedLine2 + linesShift;

            int vcsLine1 = getVcsLine1(lastRangeBefore, beforeChangedLine1);
            int vcsLine2 = getVcsLine2(firstRangeAfter, beforeChangedLine2, beforeTotalLines, vcsTotalLines);

            List<Range> newChangedRanges = getNewChangedRanges(afterChangedLine1, afterChangedLine2, vcsLine1, vcsLine2);

            shiftRanges(rangesAfter, linesShift);

            if (!changedRanges.equals(newChangedRanges)) {
                replaceRanges(changedRanges, newChangedRanges);

                myRanges = new ArrayList<Range>(rangesBefore.size() + newChangedRanges.size() + rangesAfter.size());

                myRanges.addAll(rangesBefore);
                myRanges.addAll(newChangedRanges);
                myRanges.addAll(rangesAfter);

                for (Range range : myRanges) {
                    if (!range.hasHighlighter()) range.setHighlighter(createHighlighter(range));
                }

                if (myRanges.isEmpty()) {
                    markFileUnchanged();
                }
            }
        }
        catch (ProcessCanceledException ignore) {
        }
        catch (FilesTooBigForDiffException e1) {
            installAnathema();
            removeHighlightersFromMarkupModel();
        }
    }

    private static int getVcsLine1( Range range, int line) {
        return range == null ? line : line + range.getVcsLine2() - range.getLine2();
    }

    private static int getVcsLine2( Range range, int line, int totalLinesBefore, int totalLinesAfter) {
        return range == null ? totalLinesAfter - totalLinesBefore + line : line + range.getVcsLine1() - range.getLine1();
    }

    private List<Range> getNewChangedRanges(int changedLine1, int changedLine2, int vcsLine1, int vcsLine2)
            throws FilesTooBigForDiffException {

        if (changedLine1 == changedLine2 && vcsLine1 == vcsLine2) {
            return Collections.emptyList();
        }
        if (changedLine1 == changedLine2) {
            return Collections.singletonList(new Range(changedLine1, changedLine2, vcsLine1, vcsLine2, Range.DELETED));
        }
        if (vcsLine1 == vcsLine2) {
            return Collections.singletonList(new Range(changedLine1, changedLine2, vcsLine1, vcsLine2, Range.INSERTED));
        }

        List<String> lines = new DocumentWrapper(myDocument).getLines(changedLine1, changedLine2 - 1);
        List<String> vcsLines = new DocumentWrapper(myVcsDocument).getLines(vcsLine1, vcsLine2 - 1);

        return new RangesBuilder(lines, vcsLines, changedLine1, vcsLine1, myMode).getRanges();
    }

    private void replaceRanges( List<Range> rangesInChange,  List<Range> newRangesInChange) {
        for (Range range : rangesInChange) {
            if (range.getHighlighter() != null) {
                range.getHighlighter().dispose();
            }
            range.setHighlighter(null);
            range.invalidate();
        }
        for (Range range : newRangesInChange) {
            range.setHighlighter(createHighlighter(range));
        }
    }

    private static void shiftRanges( List<Range> rangesAfterChange, int shift) {
        for (final Range range : rangesAfterChange) {
            range.shift(shift);
        }
    }

    private void sortRanges(int beforeChangedLine1,
                            int beforeChangedLine2,
                            int linesShift,
                             List<Range> rangesBeforeChange,
                             List<Range> changedRanges,
                             List<Range> rangesAfterChange) {
        if (!Registry.is("diff.status.tracker.skip.spaces")) {
            for (Range range : myRanges) {
                if (range.getLine2() < beforeChangedLine1) {
                    rangesBeforeChange.add(range);
                }
                else if (range.getLine1() > beforeChangedLine2) {
                    rangesAfterChange.add(range);
                }
                else {
                    changedRanges.add(range);
                }
            }
        }
        else {
            int lastBefore = -1;
            int firstAfter = myRanges.size();
            for (int i = 0; i < myRanges.size(); i++) {
                Range range = myRanges.get(i);

                if (range.getLine2() < beforeChangedLine1) {
                    lastBefore = i;
                }
                else if (range.getLine1() > beforeChangedLine2) {
                    firstAfter = i;
                    break;
                }
            }


            // Expand on ranges, that are separated from changes only by empty/whitespaces lines
            // This is needed to reduce amount of confusing cases, when changed blocks are matched wrong due to matched empty lines between them
            // TODO: try to simplify logic, it's too high change that current one is broken somehow
            CharSequence sequence = myDocument.getCharsSequence();
            int lineCount = getLineCount(myDocument);

            while (true) {
                if (lastBefore == -1) break;

                if (lastBefore < myRanges.size() - 1 && firstAfter - lastBefore > 1) {
                    Range firstChangedRange = myRanges.get(lastBefore + 1);
                    if (firstChangedRange.getLine1() < beforeChangedLine1) {
                        beforeChangedLine1 = firstChangedRange.getLine1();
                    }
                }

                if (beforeChangedLine1 < 0) break;
                if (beforeChangedLine1 >= lineCount) break;
                int offset1 = myDocument.getLineStartOffset(beforeChangedLine1) - 2;

                int deltaLines = 0;
                while (offset1 > 0) {
                    char c = sequence.charAt(offset1);
                    if (!StringUtil.isWhiteSpace(c)) break;
                    if (c == '\n') deltaLines++;
                    offset1--;
                }

                if (deltaLines == 0) break;
                beforeChangedLine1 -= deltaLines;

                if (myRanges.get(lastBefore).getLine2() < beforeChangedLine1) break;
                while (lastBefore != -1 && myRanges.get(lastBefore).getLine2() >= beforeChangedLine1) {
                    lastBefore--;
                }
            }

            while (true) {
                if (firstAfter == myRanges.size()) break;

                if (firstAfter > 0 && firstAfter - lastBefore > 1) {
                    Range lastChangedRange = myRanges.get(firstAfter - 1);
                    if (lastChangedRange.getLine2() > beforeChangedLine2) {
                        beforeChangedLine2 = lastChangedRange.getLine2();
                    }
                }

                // TODO: "afterChangedLine2 >= getLineCount(myDocument)" shouldn't ever be true, but it is sometimes for some reason
                int afterChangedLine2 = beforeChangedLine2 + linesShift - 1;
                if (afterChangedLine2 < 0) break;
                if (afterChangedLine2 >= lineCount) break;
                int offset2 = myDocument.getLineEndOffset(afterChangedLine2) + 1;

                int deltaLines = 0;
                while (offset2 < sequence.length()) {
                    char c = sequence.charAt(offset2);
                    if (!StringUtil.isWhiteSpace(c)) break;
                    if (c == '\n') deltaLines++;
                    offset2++;
                }

                if (deltaLines == 0) break;
                beforeChangedLine2 += deltaLines;

                if (myRanges.get(firstAfter).getLine1() > beforeChangedLine2) break;
                while (firstAfter != myRanges.size() && myRanges.get(firstAfter).getLine1() <= beforeChangedLine2) {
                    firstAfter++;
                }
            }


            for (int i = 0; i < myRanges.size(); i++) {
                Range range = myRanges.get(i);
                if (i <= lastBefore) {
                    rangesBeforeChange.add(range);
                }
                else if (i >= firstAfter) {
                    rangesAfterChange.add(range);
                }
                else {
                    changedRanges.add(range);
                }
            }
        }
    }

    
    Range getNextRange(final Range range) {
        synchronized (myLock) {
            final int index = myRanges.indexOf(range);
            if (index == myRanges.size() - 1) return null;
            return myRanges.get(index + 1);
        }
    }

    
    Range getPrevRange(final Range range) {
        synchronized (myLock) {
            final int index = myRanges.indexOf(range);
            if (index <= 0) return null;
            return myRanges.get(index - 1);
        }
    }

    
    public Range getNextRange(final int line) {
        synchronized (myLock) {
            for (Range range : myRanges) {
                if (line < range.getLine2() && !range.isSelectedByLine(line)) {
                    return range;
                }
            }
            return null;
        }
    }

    
    public Range getPrevRange(final int line) {
        synchronized (myLock) {
            for (int i = myRanges.size() - 1; i >= 0; i--) {
                Range range = myRanges.get(i);
                if (line > range.getLine1() && !range.isSelectedByLine(line)) {
                    return range;
                }
            }
            return null;
        }
    }

    
    public Range getRangeForLine(final int line) {
        synchronized (myLock) {
            for (final Range range : myRanges) {
                if (range.isSelectedByLine(line)) return range;
            }
            return null;
        }
    }

    private void doRollbackRange( Range range) {
        if (range.getType() == Range.MODIFIED) {
            TextRange currentTextRange = getCurrentTextRange(range);
            int offset1 = currentTextRange.getStartOffset();
            int offset2 = currentTextRange.getEndOffset();

            CharSequence vcsContent = getVcsContent(range);
            myDocument.replaceString(offset1, offset2, vcsContent);
        }
        else if (range.getType() == Range.INSERTED) {
            TextRange currentTextRange = getCurrentTextRange(range);
            int offset1 = currentTextRange.getStartOffset();
            int offset2 = currentTextRange.getEndOffset();

            if (offset1 > 0) {
                offset1--;
            }
            else if (offset2 < myDocument.getTextLength()) {
                offset2++;
            }
            myDocument.deleteString(offset1, offset2);
        }
        else if (range.getType() == Range.DELETED) {
            CharSequence content = getVcsContent(range);
            if (range.getLine2() == getLineCount(myDocument)) {
                myDocument.insertString(myDocument.getTextLength(), "\n" + content);
            }
            else {
                myDocument.insertString(myDocument.getLineStartOffset(range.getLine2()), content + "\n");
            }
        }
        else {
            throw new IllegalArgumentException("Unknown range type: " + range.getType());
        }
    }

    public void rollbackChanges( Range range) {
        myApplication.assertWriteAccessAllowed();

        synchronized (myLock) {
            if (myBulkUpdate) return;

            if (!range.isValid()) {
                LOG.warn("Rollback of invalid range");
                return;
            }

            doRollbackRange(range);
        }
    }

    public void rollbackChanges( final BitSet lines) {
        runBulkRollback(new Runnable() {
            @Override
            public void run() {
                Range first = null;
                Range last = null;

                int shift = 0;
                for (Range range : myRanges) {
                    if (!range.isValid()) {
                        LOG.warn("Rollback of invalid range");
                        break;
                    }

                    boolean check;
                    if (range.getLine1() == range.getLine2()) {
                        check = lines.get(range.getLine1());
                    }
                    else {
                        int next = lines.nextSetBit(range.getLine1());
                        check = next != -1 && next < range.getLine2();
                    }

                    if (check) {
                        if (first == null) {
                            first = range;
                        }
                        last = range;

                        Range shiftedRange = new Range(range);
                        shiftedRange.shift(shift);

                        doRollbackRange(shiftedRange);

                        shift += (range.getVcsLine2() - range.getVcsLine1()) - (range.getLine2() - range.getLine1());
                    }
                }

                if (first != null) {
                    int beforeChangedLine1 = first.getLine1();
                    int beforeChangedLine2 = last.getLine2();

                    int beforeTotalLines = getLineCount(myDocument) - shift;

                    doUpdateRanges(beforeChangedLine1, beforeChangedLine2, shift, beforeTotalLines);
                }
            }
        });
    }

    public void rollbackAllChanges() {
        runBulkRollback(new Runnable() {
            @Override
            public void run() {
                myDocument.setText(myVcsDocument.getText());

                removeAnathema();
                removeHighlightersFromMarkupModel();

                markFileUnchanged();
            }
        });
    }

    private void runBulkRollback( Runnable task) {
        myApplication.assertWriteAccessAllowed();

        synchronized (myLock) {
            if (myBulkUpdate) return;

            try {
                mySuppressUpdate = true;

                task.run();
            }
            catch (Error e) {
                reinstallRanges();
                throw e;
            }
            catch (RuntimeException e) {
                reinstallRanges();
                throw e;
            }
            finally {
                mySuppressUpdate = false;
            }
        }
    }

    public CharSequence getVcsContent( Range range) {
        synchronized (myLock) {
            TextRange textRange = getVcsRange(range);
            final int startOffset = textRange.getStartOffset();
            final int endOffset = textRange.getEndOffset();
            return myVcsDocument.getCharsSequence().subSequence(startOffset, endOffset);
        }
    }

    
    TextRange getCurrentTextRange( Range range) {
        myApplication.assertReadAccessAllowed();

        synchronized (myLock) {
            if (!range.isValid()) {
                LOG.warn("Current TextRange of invalid range");
            }

            return getRange(range.getLine1(), range.getLine2(), myDocument);
        }
    }

    
    TextRange getVcsRange( Range range) {
        synchronized (myLock) {
            if (!range.isValid()) {
                LOG.warn("Vcs TextRange of invalid range");
            }

            return getRange(range.getVcsLine1(), range.getVcsLine2(), myVcsDocument);
        }
    }

    /**
     * Return affected range, without non-internal '\n'
     * so if last line is not empty, the last symbol will be not '\n'
     * <p/>
     * So we consider '\n' not as a part of line, but a separator between lines
     */
    
    private static TextRange getRange(int line1, int line2,  Document document) {
        if (line1 == line2) {
            int lineStartOffset = line1 < getLineCount(document) ? document.getLineStartOffset(line1) : document.getTextLength();
            return new TextRange(lineStartOffset, lineStartOffset);
        }
        else {
            int startOffset = document.getLineStartOffset(line1);
            int endOffset = document.getLineEndOffset(line2 - 1);
            return new TextRange(startOffset, endOffset);
        }
    }

    public static LineStatusTracker createOn( VirtualFile virtualFile,  final Document doc, final Project project) {
        final Document document = new DocumentImpl("", true);
        document.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
        return new LineStatusTracker(doc, document, project, virtualFile);
    }

    public static class RevisionPack {
        private final long myNumber;
        private final VcsRevisionNumber myRevision;

        public RevisionPack(long number, VcsRevisionNumber revision) {
            myNumber = number;
            myRevision = revision;
        }

        public long getNumber() {
            return myNumber;
        }

        public VcsRevisionNumber getRevision() {
            return myRevision;
        }

        public boolean contains(final RevisionPack previous) {
            if (myRevision.equals(previous.getRevision()) && !myRevision.equals(VcsRevisionNumber.NULL)) return true;
            return myNumber >= previous.getNumber();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RevisionPack that = (RevisionPack)o;

            return myRevision.equals(that.getRevision());
        }

        @Override
        public int hashCode() {
            return myRevision.hashCode();
        }
    }

    public static class CanNotCalculateDiffPanel extends EditorNotificationPanel {
        public CanNotCalculateDiffPanel() {
            myLabel.setText("Can not highlight changed lines. File is too big and there are too many changes.");
        }
    }

    private static int getLineCount( Document document) {
        return Math.max(document.getLineCount(), 1);
    }
}
