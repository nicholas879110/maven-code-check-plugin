/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.command.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.command.undo.DocumentReference;
import com.gome.maven.openapi.command.undo.DocumentReferenceManager;
import com.gome.maven.openapi.command.undo.UndoConstants;
import com.gome.maven.openapi.command.undo.UndoManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.event.DocumentListener;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;

public class DocumentUndoProvider implements Disposable {
    private static final Key<Boolean> UNDOING_EDITOR_CHANGE = Key.create("DocumentUndoProvider.UNDOING_EDITOR_CHANGE");

    private final Project myProject;

    DocumentUndoProvider(Project project) {
        MyEditorDocumentListener documentListener = new MyEditorDocumentListener();
        myProject = project;

        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener, this);
    }

    @Override
    public void dispose() {
    }

    private UndoManagerImpl getUndoManager() {
        return (UndoManagerImpl)(myProject == null ? UndoManager.getGlobalInstance() : UndoManager.getInstance(myProject));
    }

    public static void startDocumentUndo( Document doc) {
        if (doc != null) doc.putUserData(UNDOING_EDITOR_CHANGE, Boolean.TRUE);
    }

    public static void finishDocumentUndo( Document doc) {
        if (doc != null) doc.putUserData(UNDOING_EDITOR_CHANGE, null);
    }

    private class MyEditorDocumentListener implements DocumentListener {
        @Override
        public void beforeDocumentChange(DocumentEvent e) {
            Document document = e.getDocument();
            if (shouldBeIgnored(document)) return;

            UndoManagerImpl undoManager = getUndoManager();
            if (undoManager.isActive() && isUndoable(document) && (undoManager.isUndoInProgress() || undoManager.isRedoInProgress()) &&
                    document.getUserData(UNDOING_EDITOR_CHANGE) != Boolean.TRUE) {
                throw new IllegalStateException("Do not change documents during undo as it will break undo sequence.");
            }
        }

        @Override
        public void documentChanged(final DocumentEvent e) {
            Document document = e.getDocument();
            if (shouldBeIgnored(document)) return;

            UndoManagerImpl undoManager = getUndoManager();
            if (undoManager.isActive() && isUndoable(document)) {
                registerUndoableAction(e);
            }
            else {
                registerNonUndoableAction(document);
            }
        }

        private boolean shouldBeIgnored(Document document) {
            return UndoManagerImpl.isCopy(document) // if we don't ignore copy's events, we will receive notification
                    // for the same event twice (from original document too)
                    // and undo will work incorrectly
                    || allEditorsAreViewersFor(document)
                    || !shouldRecordActions(document);
        }

        private boolean shouldRecordActions(final Document document) {
            if (document.getUserData(UndoConstants.DONT_RECORD_UNDO) == Boolean.TRUE) return false;

            final VirtualFile vFile = FileDocumentManager.getInstance().getFile(document);
            return vFile == null || vFile.getUserData(UndoConstants.DONT_RECORD_UNDO) != Boolean.TRUE;
        }

        private boolean allEditorsAreViewersFor(Document document) {
            Editor[] editors = EditorFactory.getInstance().getEditors(document);
            if (editors.length == 0) return false;
            for (Editor editor : editors) {
                if (!editor.isViewer()) return false;
            }
            return true;
        }

        private void registerUndoableAction(DocumentEvent e) {
            getUndoManager().undoableActionPerformed(new EditorChangeAction(e));
        }

        private void registerNonUndoableAction(final Document document) {
            DocumentReference ref = DocumentReferenceManager.getInstance().create(document);
            getUndoManager().nonundoableActionPerformed(ref, false);
        }

        private boolean isUndoable(Document document) {
            if (!UndoManagerImpl.isRefresh()) return true;

            return getUndoManager().isUndoOrRedoAvailable(DocumentReferenceManager.getInstance().create(document));
        }
    }
}
