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
package com.gome.maven.codeHighlighting;

import com.gome.maven.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.gome.maven.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Collections;
import java.util.List;

public abstract class TextEditorHighlightingPass implements HighlightingPass {
    public static final TextEditorHighlightingPass[] EMPTY_ARRAY = new TextEditorHighlightingPass[0];
     protected final Document myDocument;
     protected final Project myProject;
    private final boolean myRunIntentionPassAfter;
    private final long myInitialStamp;
    private volatile int[] myCompletionPredecessorIds = ArrayUtil.EMPTY_INT_ARRAY;
    private volatile int[] myStartingPredecessorIds = ArrayUtil.EMPTY_INT_ARRAY;
    private volatile int myId;
    private volatile boolean myDumb;
    private EditorColorsScheme myColorsScheme;

    protected TextEditorHighlightingPass( final Project project,  final Document document, boolean runIntentionPassAfter) {
        myDocument = document;
        myProject = project;
        myRunIntentionPassAfter = runIntentionPassAfter;
        myInitialStamp = document == null ? 0 : document.getModificationStamp();
    }
    protected TextEditorHighlightingPass( final Project project,  final Document document) {
        this(project, document, true);
    }

    @Override
    public final void collectInformation( ProgressIndicator progress) {
        if (!isValid()) return; //Document has changed.
        if (!(progress instanceof DaemonProgressIndicator)) {
            throw new IncorrectOperationException("Highlighting must be run under DaemonProgressIndicator, but got: "+progress);
        }
        myDumb = DumbService.getInstance(myProject).isDumb();
        doCollectInformation(progress);
    }

    
    public EditorColorsScheme getColorsScheme() {
        return myColorsScheme;
    }

    public void setColorsScheme( EditorColorsScheme colorsScheme) {
        myColorsScheme = colorsScheme;
    }

    protected boolean isDumbMode() {
        return myDumb;
    }

    protected boolean isValid() {
        if (isDumbMode() && !DumbService.isDumbAware(this)) {
            return false;
        }

        if (myDocument != null && myDocument.getModificationStamp() != myInitialStamp) return false;
        if (myDocument != null) {
            PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
            if (file == null || !file.isValid()) return false;
        }

        return true;
    }

    @Override
    public final void applyInformationToEditor() {
        if (!isValid()) return; // Document has changed.
        if (DumbService.getInstance(myProject).isDumb() && !(this instanceof DumbAware)) {
            Document document = getDocument();
            PsiFile file = document == null ? null : PsiDocumentManager.getInstance(myProject).getPsiFile(document);
            if (file != null) {
                DaemonCodeAnalyzerEx.getInstanceEx(myProject).getFileStatusMap().markFileUpToDate(getDocument(), getId());
            }
            return;
        }
        doApplyInformationToEditor();
    }

    public abstract void doCollectInformation( ProgressIndicator progress);
    public abstract void doApplyInformationToEditor();

    public final int getId() {
        return myId;
    }

    public final void setId(final int id) {
        myId = id;
    }

    
    public List<HighlightInfo> getInfos() {
        return Collections.emptyList();
    }

    
    public final int[] getCompletionPredecessorIds() {
        return myCompletionPredecessorIds;
    }

    public final void setCompletionPredecessorIds( int[] completionPredecessorIds) {
        myCompletionPredecessorIds = completionPredecessorIds;
    }

    
    public Document getDocument() {
        return myDocument;
    }

     public final int[] getStartingPredecessorIds() {
        return myStartingPredecessorIds;
    }

    public final void setStartingPredecessorIds( final int[] startingPredecessorIds) {
        myStartingPredecessorIds = startingPredecessorIds;
    }

    @Override

    public String toString() {
        return getClass() + "; id=" + getId();
    }

    public boolean isRunIntentionPassAfter() {
        return myRunIntentionPassAfter;
    }
}
