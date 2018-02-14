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
package com.gome.maven.openapi.editor.highlighter;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighter;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * @author yole
 */
public abstract class EditorHighlighterFactory {

    public static EditorHighlighterFactory getInstance() {
        return ServiceManager.getService(EditorHighlighterFactory.class);
    }

    
    public abstract EditorHighlighter createEditorHighlighter(final SyntaxHighlighter syntaxHighlighter,  EditorColorsScheme colors);

    
    public abstract EditorHighlighter createEditorHighlighter( FileType fileType,  EditorColorsScheme settings, final Project project);

    
    public abstract EditorHighlighter createEditorHighlighter(final Project project,  FileType fileType);

    
    public abstract EditorHighlighter createEditorHighlighter( final VirtualFile file,  EditorColorsScheme globalScheme,  final Project project);

    
    public abstract EditorHighlighter createEditorHighlighter(final Project project,  VirtualFile file);

    
    public abstract EditorHighlighter createEditorHighlighter(final Project project,  String fileName);

    
    public abstract EditorHighlighter createEditorHighlighter( EditorColorsScheme settings,  String fileName,  final Project project);
}