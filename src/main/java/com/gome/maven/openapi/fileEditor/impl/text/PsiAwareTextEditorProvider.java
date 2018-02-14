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

/*
 * @author max
 */
package com.gome.maven.openapi.fileEditor.impl.text;

import com.gome.maven.codeHighlighting.BackgroundEditorHighlighter;
import com.gome.maven.codeInsight.daemon.impl.TextEditorBackgroundHighlighter;
import com.gome.maven.codeInsight.folding.CodeFoldingManager;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.application.WriteAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.*;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.util.Producer;
import org.jdom.Element;

public class PsiAwareTextEditorProvider extends TextEditorProvider implements AsyncFileEditorProvider {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider");
    
    private static final String FOLDING_ELEMENT = "folding";

    @Override
    
    public FileEditor createEditor( final Project project,  final VirtualFile file) {
        return createEditorAsync(project, file).build();
    }

    
    @Override
    public Builder createEditorAsync( final Project project,  final VirtualFile file) {
        if (!accept(project, file)) {
            LOG.error("Cannot open text editor for " + file);
        }
        CodeFoldingState state = null;
        if (!project.isDefault()) { // There's no CodeFoldingManager for default project (which is used in diff command-line application)
            try {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    state = CodeFoldingManager.getInstance(project).buildInitialFoldings(document);
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error("Error building initial foldings", e);
            }
        }
        final CodeFoldingState finalState = state;
        return new Builder() {
            @Override
            public FileEditor build() {
                final PsiAwareTextEditorImpl editor = new PsiAwareTextEditorImpl(project, file, PsiAwareTextEditorProvider.this);
                if (finalState != null) {
                    finalState.setToEditor(editor.getEditor());
                }
                return editor;
            }
        };
    }

    @Override
    
    public FileEditorState readState( final Element element,  final Project project,  final VirtualFile file) {
        final TextEditorState state = (TextEditorState)super.readState(element, project, file);

        // Foldings
        Element child = element.getChild(FOLDING_ELEMENT);
        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (child != null) {
            if (document == null) {
                final Element detachedStateCopy = (Element) child.clone();
                state.setDelayedFoldState(new Producer<CodeFoldingState>() {
                    @Override
                    public CodeFoldingState produce() {
                        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
                        return document == null ? null : CodeFoldingManager.getInstance(project).readFoldingState(detachedStateCopy, document);
                    }
                });
            }
            else {
                //PsiDocumentManager.getInstance(project).commitDocument(document);
                state.setFoldingState(CodeFoldingManager.getInstance(project).readFoldingState(child, document));
            }
        }
        return state;
    }

    @Override
    public void writeState( final FileEditorState _state,  final Project project,  final Element element) {
        super.writeState(_state, project, element);

        TextEditorState state = (TextEditorState)_state;

        // Foldings
        CodeFoldingState foldingState = state.getFoldingState();
        if (foldingState != null) {
            Element e = new Element(FOLDING_ELEMENT);
            try {
                CodeFoldingManager.getInstance(project).writeFoldingState(foldingState, e);
            }
            catch (WriteExternalException e1) {
                //ignore
            }
            element.addContent(e);
        }
    }

    @Override
    protected TextEditorState getStateImpl(final Project project,  final Editor editor,  final FileEditorStateLevel level) {
        final TextEditorState state = super.getStateImpl(project, editor, level);
        // Save folding only on FULL level. It's very expensive to commit document on every
        // type (caused by undo).
        if(FileEditorStateLevel.FULL == level){
            // Folding
            if (project != null && !project.isDisposed() && !editor.isDisposed() && project.isInitialized()) {
                PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
                state.setFoldingState(CodeFoldingManager.getInstance(project).saveFoldingState(editor));
            }
            else {
                state.setFoldingState(null);
            }
        }

        return state;
    }

    @Override
    protected void setStateImpl(final Project project, final Editor editor, final TextEditorState state) {
        super.setStateImpl(project, editor, state);
        // Folding
        final CodeFoldingState foldState = state.getFoldingState();
        if (project != null && foldState != null) {
            new WriteAction() {
                @Override
                protected void run( Result result) throws Throwable {
                    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
                    editor.getFoldingModel().runBatchFoldingOperation(
                            new Runnable() {
                                @Override
                                public void run() {
                                    CodeFoldingManager.getInstance(project).restoreFoldingState(editor, foldState);
                                }
                            }
                    );
                }
            }.execute();
        }
    }

    
    @Override
    protected EditorWrapper createWrapperForEditor( final Editor editor) {
        return new PsiAwareEditorWrapper(editor);
    }

    private final class PsiAwareEditorWrapper extends EditorWrapper {
        private final TextEditorBackgroundHighlighter myBackgroundHighlighter;

        private PsiAwareEditorWrapper( Editor editor) {
            super(editor);
            final Project project = editor.getProject();
            myBackgroundHighlighter = project == null
                    ? null
                    : new TextEditorBackgroundHighlighter(project, editor);
        }

        @Override
        public BackgroundEditorHighlighter getBackgroundHighlighter() {
            return myBackgroundHighlighter;
        }
    }
}
