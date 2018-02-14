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

/*
 * @author max
 */
package com.gome.maven.openapi.command.undo;

import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;

public class UndoUtil {
    private UndoUtil() {
    }

    /**
     * make undoable action in current document in order to Undo action work from current file
     *
     * @param file to make editors of to respond to undo action.
     */
    public static void markPsiFileForUndo( final PsiFile file) {
        Project project = file.getProject();
        final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) return;
        CommandProcessor.getInstance().addAffectedDocuments(project, document);
    }

    /**
     * @deprecated please use CommandProcessor.getInstance().addAffectedFiles instead
     */
    public static void markVirtualFileForUndo( Project project,  VirtualFile file) {
        CommandProcessor.getInstance().addAffectedFiles(project, file);
    }
}
