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
package com.gome.maven.openapi.command.impl;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.AbnormalCommandTerminationException;
import com.gome.maven.openapi.command.undo.UndoManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.vfs.VirtualFile;

class CommandProcessorImpl extends CoreCommandProcessor {
    @Override
    public void finishCommand(final Project project, final Object command, final Throwable throwable) {
        if (myCurrentCommand != command) return;
        final boolean failed;
        try {
            if (throwable instanceof AbnormalCommandTerminationException) {
                final AbnormalCommandTerminationException rollback = (AbnormalCommandTerminationException)throwable;
                if (ApplicationManager.getApplication().isUnitTestMode()) {
                    throw new RuntimeException(rollback);
                }
                failed = true;
            }
            else if (throwable != null) {
                failed = true;
                if (throwable instanceof Error) {
                    throw (Error)throwable;
                }
                else if (throwable instanceof RuntimeException) throw (RuntimeException)throwable;
                CommandLog.LOG.error(throwable);
            }
            else {
                failed = false;
            }
        }
        finally {
            super.finishCommand(project, command, throwable);
        }
        if (failed) {
            if (project != null) {
                FileEditor editor = new FocusBasedCurrentEditorProvider().getCurrentEditor();
                final UndoManager undoManager = UndoManager.getInstance(project);
                if (undoManager.isUndoAvailable(editor)) {
                    undoManager.undo(editor);
                }
            }
            Messages.showErrorDialog(project, "Cannot perform operation. Too complex, sorry.", "Failed to Perform Operation");
        }
    }

    @Override
    public void markCurrentCommandAsGlobal(Project project) {
        getUndoManager(project).markCurrentCommandAsGlobal();
    }

    private static UndoManagerImpl getUndoManager(Project project) {
        return (UndoManagerImpl)(project != null ? UndoManager.getInstance(project) : UndoManager.getGlobalInstance());
    }

    @Override
    public void addAffectedDocuments(Project project,  Document... docs) {
        getUndoManager(project).addAffectedDocuments(docs);
    }

    @Override
    public void addAffectedFiles(Project project,  VirtualFile... files) {
        getUndoManager(project).addAffectedFiles(files);
    }
}
