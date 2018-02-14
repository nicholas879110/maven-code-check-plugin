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

package com.gome.maven.ide.util;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileEditor.TextEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;

public class EditorHelper {

    public static Editor openInEditor( PsiElement element) {
        FileEditor editor = openInEditor(element, true);
        return editor instanceof TextEditor ? ((TextEditor)editor).getEditor() : null;
    }

    
    public static FileEditor openInEditor( PsiElement element, boolean switchToText) {
        PsiFile file;
        int offset;
        if (element instanceof PsiFile){
            file = (PsiFile)element;
            offset = -1;
        }
        else{
            file = element.getContainingFile();
            offset = element.getTextOffset();
        }
        if (file == null) return null;//SCR44414
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return null;
        OpenFileDescriptor descriptor = new OpenFileDescriptor(element.getProject(), virtualFile, offset);
        Project project = element.getProject();
        if (offset == -1 && !switchToText) {
            FileEditorManager.getInstance(project).openEditor(descriptor, false);
        }
        else {
            FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
        }
        return FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
    }
}