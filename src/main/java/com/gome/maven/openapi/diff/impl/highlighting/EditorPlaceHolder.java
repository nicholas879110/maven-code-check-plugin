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
package com.gome.maven.openapi.diff.impl.highlighting;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.DiffContent;
import com.gome.maven.openapi.diff.impl.ContentChangeListener;
import com.gome.maven.openapi.diff.impl.DiffUtil;
import com.gome.maven.openapi.diff.impl.DiffVersionComponent;
import com.gome.maven.openapi.diff.impl.util.ContentDocumentListener;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.impl.DocumentImpl;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorProvider;
import com.gome.maven.openapi.fileEditor.TextEditor;
import com.gome.maven.openapi.fileEditor.ex.FileEditorProviderManager;
import com.gome.maven.openapi.fileTypes.UIBasedFileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

class EditorPlaceHolder extends DiffMarkup implements DiffVersionComponent {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.diff.impl.highlighting.EditorWrapper");
    private EditorEx myEditor;
    private DiffContent myContent;
    private final FragmentSide mySide;
    private ContentChangeListener myListener = null;
    private FileEditor myFileEditor;
    private FileEditorProvider myFileEditorProvider;

    public EditorPlaceHolder(FragmentSide side, Project project,  Disposable parentDisposable) {
        super(project, parentDisposable);
        mySide = side;
        resetHighlighters();
    }

    public void addListener(ContentChangeListener listener) {
        LOG.assertTrue(myListener == null);
        myListener = listener;
    }

    protected void onDisposed() {
        LOG.assertTrue(!isDisposed());
        super.onDisposed();
        fireContentChanged();
    }

    private void fireContentChanged() {
        myListener.onContentChangedIn(this);
    }

    public void setContent(final DiffContent content) {
        runRegisteredDisposables();
        myContent = content;
        if (myContent != null) {
            Document document = myContent.getDocument();
            if (myContent.isBinary() || document == null || myContent.getContentType() instanceof UIBasedFileType) {
                final VirtualFile file = myContent.getFile();
                if (file != null) {
                    final FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(getProject(), file);
                    if (providers.length > 0) {
                        myFileEditor = providers[0].createEditor(getProject(), file);
                        if (myFileEditor instanceof TextEditor) {
                            myEditor = (EditorEx)((TextEditor)myFileEditor).getEditor();
                            ContentDocumentListener.install(myContent, this);
                        }
                        myFileEditorProvider = providers[0];
                        addDisposable(new Disposable() {
                            @Override
                            public void dispose() {
                                myFileEditorProvider.disposeEditor(myFileEditor);
                                myFileEditor = null;
                                myFileEditorProvider = null;
                                myEditor = null;
                            }
                        });
                    } else {
                        document = new DocumentImpl("Can not show", true);
                        final EditorFactory editorFactory = EditorFactory.getInstance();
                        myEditor = DiffUtil.createEditor(document, getProject(), true, content.getContentType());
                        addDisposable(new Disposable() {
                            public void dispose() {
                                editorFactory.releaseEditor(myEditor);
                                myEditor = null;
                            }
                        });
                    }
                }
            }
            else {
                final EditorFactory editorFactory = EditorFactory.getInstance();
                myEditor = DiffUtil.createEditor(document, getProject(), false, content.getContentType());
                addDisposable(new Disposable() {
                    public void dispose() {
                        editorFactory.releaseEditor(myEditor);
                        myEditor = null;
                    }
                });
                ContentDocumentListener.install(myContent, this);
            }
        }
        fireContentChanged();
    }

    public EditorEx getEditor() {
        return myEditor;
    }

    public FragmentSide getSide() {
        return mySide;
    }

    public DiffContent getContent() {
        return myContent;
    }

    public void removeContent() {
        setContent(null);
    }

    @Override
    public FileEditor getFileEditor() {
        return myFileEditor;
    }
}
