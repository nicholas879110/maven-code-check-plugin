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
package com.gome.maven.openapi.diff.impl;

import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighterFactory;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.testFramework.LightVirtualFile;

public class DiffHighlighterFactoryImpl implements DiffHighlighterFactory {
    private final Project myProject;
    private final FileType myFileType;
    private final VirtualFile myFile;

    public DiffHighlighterFactoryImpl(FileType fileType, VirtualFile file, Project project) {
        myFileType = fileType;
        myProject = project;
        myFile = file;
    }

    public EditorHighlighter createHighlighter() {
        if (myFileType == null || myProject == null) return null;
        if ((myFile != null && myFile.getFileType() == myFileType) || myFile instanceof LightVirtualFile) {
            return EditorHighlighterFactory.getInstance().createEditorHighlighter(myProject, myFile);
        }
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(myProject, myFileType);
    }
}
