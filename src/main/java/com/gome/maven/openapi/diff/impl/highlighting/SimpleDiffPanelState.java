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
package com.gome.maven.openapi.diff.impl.highlighting;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diff.impl.ComparisonPolicy;
import com.gome.maven.openapi.diff.impl.ContentChangeListener;
import com.gome.maven.openapi.diff.impl.fragments.FragmentHighlighterImpl;
import com.gome.maven.openapi.diff.impl.fragments.FragmentList;
import com.gome.maven.openapi.diff.impl.fragments.FragmentListImpl;
import com.gome.maven.openapi.diff.impl.fragments.LineFragment;
import com.gome.maven.openapi.diff.impl.processing.DiffPolicy;
import com.gome.maven.openapi.diff.impl.processing.HighlightMode;
import com.gome.maven.openapi.diff.impl.processing.TextCompareProcessor;
import com.gome.maven.openapi.diff.impl.splitter.LineBlocks;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.util.diff.FilesTooBigForDiffException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class SimpleDiffPanelState implements Disposable  {
    protected ComparisonPolicy myComparisonPolicy = ComparisonPolicy.DEFAULT;
    protected DiffPolicy myDiffPolicy;
    protected HighlightMode myHighlightMode;
    protected final EditorPlaceHolder myAppender1;
    protected final EditorPlaceHolder myAppender2;
    protected FragmentList myFragmentList = FragmentList.EMPTY;
    protected final Project myProject;

    public SimpleDiffPanelState(Project project, ContentChangeListener changeListener,  Disposable parentDisposable) {
        myAppender1 = createEditorWrapper(project, changeListener, FragmentSide.SIDE1);
        myAppender2 = createEditorWrapper(project, changeListener, FragmentSide.SIDE2);
        myProject = project;
        myDiffPolicy = DiffPolicy.LINES_WO_FORMATTING;
        myHighlightMode = HighlightMode.BY_WORD;
        Disposer.register(parentDisposable, this);
    }

    private EditorPlaceHolder createEditorWrapper(Project project, ContentChangeListener changeListener, FragmentSide side) {
        EditorPlaceHolder editorWrapper = new EditorPlaceHolder(side, project, this);
        editorWrapper.addListener(changeListener);
        return editorWrapper;
    }

    public void setComparisonPolicy( ComparisonPolicy comparisonPolicy) {
        myComparisonPolicy = comparisonPolicy;
    }

    public void setDiffPolicy(DiffPolicy diffPolicy) {
        myDiffPolicy = diffPolicy;
    }

    public DiffPolicy getDiffPolicy() {
        return myDiffPolicy;
    }

    public ComparisonPolicy getComparisonPolicy() {
        return myComparisonPolicy;
    }

    public HighlightMode getHighlightMode() {
        return myHighlightMode;
    }

    public void setHighlightMode(HighlightMode highlightMode) {
        myHighlightMode = highlightMode;
    }

    public void dispose() {
    }

    private LineBlocks addMarkup(final List<LineFragment> lines) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                final FragmentHighlighterImpl fragmentHighlighter = new FragmentHighlighterImpl(myAppender1, myAppender2);
                for (Iterator<LineFragment> iterator = lines.iterator(); iterator.hasNext();) {
                    LineFragment line = iterator.next();
                    fragmentHighlighter.setIsLast(!iterator.hasNext());
                    line.highlight(fragmentHighlighter);
                }
            }
        });
        ArrayList<LineFragment> allLineFragments = new ArrayList<LineFragment>();
        for (LineFragment lineFragment : lines) {
            allLineFragments.add(lineFragment);
            lineFragment.addAllDescendantsTo(allLineFragments);
        }
        myFragmentList = FragmentListImpl.fromList(allLineFragments);
        return LineBlocks.fromLineFragments(allLineFragments);
    }

    private void resetMarkup() {
        ApplicationManager.getApplication().runWriteAction(new ResetMarkupRunnable(this));
    }


    public LineBlocks updateEditors() throws FilesTooBigForDiffException {
        resetMarkup();
        if (myAppender1.getEditor() == null || myAppender2.getEditor() == null) {
            return null;
        }

        return addMarkup(
                new TextCompareProcessor(myComparisonPolicy, myDiffPolicy, myHighlightMode).process(myAppender1.getText(), myAppender2.getText()));
    }

    public Project getProject() { return myProject; }

    public FragmentList getFragmentList() { return myFragmentList; }

    private static class ResetMarkupRunnable implements Runnable {
        private final SimpleDiffPanelState myState;

        public ResetMarkupRunnable(SimpleDiffPanelState state) {
            myState = state;
        }

        public void run() {
            myState.myAppender1.resetHighlighters();
            myState.myAppender2.resetHighlighters();
        }
    }
}
