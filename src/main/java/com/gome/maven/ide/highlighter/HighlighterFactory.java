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
package com.gome.maven.ide.highlighter;

import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighterFactory;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighter;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

public class HighlighterFactory {
    private HighlighterFactory() {}

    
    public static EditorHighlighter createHighlighter(SyntaxHighlighter highlighter,  EditorColorsScheme settings) {
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(highlighter, settings);
    }

    
    public static EditorHighlighter createHighlighter(Project project,  String fileName) {
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileName);
    }

    
    public static EditorHighlighter createHighlighter(Project project,  VirtualFile file) {
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file);
    }

    
    public static EditorHighlighter createHighlighter(Project project,  FileType fileType) {
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType);
    }

    
    public static EditorHighlighter createHighlighter( EditorColorsScheme settings,  String fileName, Project project) {
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(settings, fileName, project);
    }

    
    public static EditorHighlighter createHighlighter( FileType fileType,  EditorColorsScheme settings, Project project) {
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(fileType, settings, project);
    }

    
    public static EditorHighlighter createHighlighter( VirtualFile vFile,  EditorColorsScheme settings, Project project) {
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(vFile, settings, project);
    }
}