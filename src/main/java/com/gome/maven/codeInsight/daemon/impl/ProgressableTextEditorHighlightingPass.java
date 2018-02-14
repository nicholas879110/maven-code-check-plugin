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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeHighlighting.TextEditorHighlightingPass;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.WrappedProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author cdr
 */
public abstract class ProgressableTextEditorHighlightingPass extends TextEditorHighlightingPass {
    private volatile boolean myFinished;
    private volatile long myProgressLimit = 0;
    private final AtomicLong myProgressCount = new AtomicLong();
    private volatile long myNextChunkThreshold; // the value myProgressCount should exceed to generate next fireProgressAdvanced event
    private final String myPresentableName;
    protected final PsiFile myFile;
     private final Editor myEditor;
     protected final TextRange myRestrictRange;
     protected final HighlightInfoProcessor myHighlightInfoProcessor;
    protected HighlightingSession myHighlightingSession;

    protected ProgressableTextEditorHighlightingPass( Project project,
                                                      final Document document,
                                                      String presentableName,
                                                      PsiFile file,
                                                      Editor editor,
                                                      TextRange restrictRange,
                                                     boolean runIntentionPassAfter,
                                                      HighlightInfoProcessor highlightInfoProcessor) {
        super(project, document, runIntentionPassAfter);
        myPresentableName = presentableName;
        myFile = file;
        myEditor = editor;
        myRestrictRange = restrictRange;
        myHighlightInfoProcessor = highlightInfoProcessor;
    }

    @Override
    protected boolean isValid() {
        return super.isValid() && (myFile == null || myFile.isValid());
    }

    private void sessionFinished() {
        advanceProgress(Math.max(1, myProgressLimit - myProgressCount.get()));
    }

    @Override
    public final void doCollectInformation( final ProgressIndicator progress) {
        if (!(progress instanceof DaemonProgressIndicator)) {
            throw new IncorrectOperationException("Highlighting must be run under DaemonProgressIndicator, but got: "+progress);
        }
        myFinished = false;
        if (myFile != null) {
            myHighlightingSession = new HighlightingSessionImpl(myFile, myEditor, progress, getColorsScheme(), getId(), myRestrictRange);
            if (!progress.isCanceled()) {
                Disposer.register((Disposable)progress, myHighlightingSession);
                if (progress.isCanceled()) {
                    Disposer.dispose(myHighlightingSession);
                    Disposer.dispose((Disposable)progress);
                }
            }
            progress.checkCanceled();

            ((DaemonProgressIndicator)progress).putUserData(HIGHLIGHTING_SESSION, myHighlightingSession);
        }
        try {
            collectInformationWithProgress(progress);
        }
        finally {
            if (myFile != null) {
                sessionFinished();
            }
        }
    }
    private static final Key<HighlightingSession> HIGHLIGHTING_SESSION = Key.create("HIGHLIGHTING_SESSION");
    public static HighlightingSession getHighlightingSession( ProgressIndicator indicator) {
        return indicator instanceof WrappedProgressIndicator ?
                getHighlightingSession(((WrappedProgressIndicator)indicator).getOriginalProgressIndicator()) :
                ((UserDataHolder)indicator).getUserData(HIGHLIGHTING_SESSION);
    }

    protected abstract void collectInformationWithProgress( ProgressIndicator progress);

    @Override
    public final void doApplyInformationToEditor() {
        myFinished = true;
        applyInformationWithProgress();
        DaemonCodeAnalyzerEx daemonCodeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(myProject);
        daemonCodeAnalyzer.getFileStatusMap().markFileUpToDate(myDocument, getId());
        myHighlightInfoProcessor.progressIsAdvanced(myHighlightingSession, 1);  //causes traffic light repaint
    }

    protected abstract void applyInformationWithProgress();

    /**
     * @return number in the [0..1] range;
     * <0 means progress is not available
     */
    public double getProgress() {
        long progressLimit = getProgressLimit();
        if (progressLimit == 0) return -1;
        long progressCount = getProgressCount();
        return progressCount > progressLimit ? 1 : (double)progressCount / progressLimit;
    }

    public long getProgressLimit() {
        return myProgressLimit;
    }

    public long getProgressCount() {
        return myProgressCount.get();
    }

    public boolean isFinished() {
        return myFinished;
    }

    protected String getPresentableName() {
        return myPresentableName;
    }

    protected Editor getEditor() {
        return myEditor;
    }

    public void setProgressLimit(long limit) {
        myProgressLimit = limit;
        myNextChunkThreshold = Math.max(1, limit / 100); // 1% precision
    }

    public void advanceProgress(long delta) {
        if (myHighlightingSession != null) {
            // session can be null in e.g. inspection batch mode
            long current = myProgressCount.addAndGet(delta);
            if (current >= myNextChunkThreshold) {
                double progress = getProgress();
                myNextChunkThreshold += Math.max(1, myProgressLimit / 100);
                myHighlightInfoProcessor.progressIsAdvanced(myHighlightingSession, progress);
            }
        }
    }

    static class EmptyPass extends TextEditorHighlightingPass {
        public EmptyPass(final Project project,  final Document document) {
            super(project, document, false);
        }

        @Override
        public void doCollectInformation( final ProgressIndicator progress) {
        }

        @Override
        public void doApplyInformationToEditor() {
            FileStatusMap statusMap = DaemonCodeAnalyzerEx.getInstanceEx(myProject).getFileStatusMap();
            statusMap.markFileUpToDate(getDocument(), getId());
        }
    }
}
