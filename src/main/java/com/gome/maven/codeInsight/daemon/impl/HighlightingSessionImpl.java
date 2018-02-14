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
package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeHighlighting.Pass;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.TransferToEDTQueue;
import gnu.trove.THashMap;

import java.util.Map;

public class HighlightingSessionImpl implements HighlightingSession {
     private final PsiFile myPsiFile;
     private final Editor myEditor;
     private final ProgressIndicator myProgressIndicator;
    private final EditorColorsScheme myEditorColorsScheme;
    private final int myPassId;
     private final TextRange myRestrictRange;
     private final Project myProject;
    private final Document myDocument;
    private final Map<TextRange,RangeMarker> myRanges2markersCache = new THashMap<TextRange, RangeMarker>();
    private volatile boolean myDisposed;

    public HighlightingSessionImpl( PsiFile psiFile,
                                    Editor editor,
                                    ProgressIndicator progressIndicator,
                                   EditorColorsScheme editorColorsScheme,
                                   int passId,
                                    TextRange restrictRange) {
        myPsiFile = psiFile;
        myEditor = editor;
        myProgressIndicator = progressIndicator;
        myEditorColorsScheme = editorColorsScheme;
        myPassId = passId;
        myRestrictRange = restrictRange;
        myProject = psiFile.getProject();
        myDocument = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
    }

    
    @Override
    public PsiFile getPsiFile() {
        return myPsiFile;
    }

    
    @Override
    public Editor getEditor() {
        return myEditor;
    }

    
    @Override
    public Document getDocument() {
        return myDocument;
    }

    
    @Override
    public ProgressIndicator getProgressIndicator() {
        return myProgressIndicator;
    }

    
    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public EditorColorsScheme getColorsScheme() {
        return myEditorColorsScheme;
    }

    @Override
    public int getPassId() {
        return myPassId;
    }

    private final TransferToEDTQueue<HighlightInfo> myAddHighlighterInEDTQueue = new TransferToEDTQueue<HighlightInfo>("Apply highlighting results", new Processor<HighlightInfo>() {
        @Override
        public boolean process(HighlightInfo info) {
            final EditorColorsScheme colorsScheme = getColorsScheme();
            UpdateHighlightersUtil.addHighlighterToEditorIncrementally(myProject, myDocument, getPsiFile(), myRestrictRange.getStartOffset(),
                    myRestrictRange.getEndOffset(),
                    info, colorsScheme, Pass.UPDATE_ALL, myRanges2markersCache);

            return true;
        }
    }, new Condition<Object>() {
        @Override
        public boolean value(Object o) {
            return myProject.isDisposed() || getProgressIndicator().isCanceled();
        }
    }, 200);
    private final TransferToEDTQueue<RangeHighlighterEx> myDisposeHighlighterInEDTQueue = new TransferToEDTQueue<RangeHighlighterEx>("Dispose abandoned highlighter", new Processor<RangeHighlighterEx>() {
        @Override
        public boolean process( RangeHighlighterEx highlighter) {
            highlighter.dispose();
            return true;
        }
    }, new Condition<Object>() {
        @Override
        public boolean value(Object o) {
            return myProject.isDisposed() || getProgressIndicator().isCanceled();
        }
    }, 200);

    void queueHighlightInfo( HighlightInfo info) {
        myAddHighlighterInEDTQueue.offer(info);
    }

    void queueDisposeHighlighter(RangeHighlighterEx highlighter) {
        if (highlighter == null) return;
        myDisposeHighlighterInEDTQueue.offer(highlighter);
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }
}
