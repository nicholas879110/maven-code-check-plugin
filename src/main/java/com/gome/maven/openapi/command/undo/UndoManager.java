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
package com.gome.maven.openapi.command.undo;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;

public abstract class UndoManager {

    public static UndoManager getInstance(Project project) {
        return project.getComponent(UndoManager.class);
    }

    public static UndoManager getGlobalInstance() {
        return ApplicationManager.getApplication().getComponent(UndoManager.class);
    }

    public abstract void undoableActionPerformed(UndoableAction action);
    public abstract void nonundoableActionPerformed(DocumentReference ref, boolean isGlobal);

    public abstract boolean isUndoInProgress();
    public abstract boolean isRedoInProgress();

    public abstract void undo( FileEditor editor);
    public abstract void redo( FileEditor editor);
    public abstract boolean isUndoAvailable( FileEditor editor);
    public abstract boolean isRedoAvailable( FileEditor editor);

    public abstract Pair<String, String> getUndoActionNameAndDescription(FileEditor editor);
    public abstract Pair<String, String> getRedoActionNameAndDescription(FileEditor editor);
}