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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeHighlighting.DirtyScopeTrackingHighlightingPassFactory;
import com.gome.maven.codeHighlighting.TextEditorHighlightingPass;
import com.gome.maven.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;

import java.util.List;

/**
 * User: anna
 * Date: 21-Jun-2006
 */
public abstract class TextEditorHighlightingPassRegistrarEx extends TextEditorHighlightingPassRegistrar {

    public static TextEditorHighlightingPassRegistrarEx getInstanceEx(Project project) {
        return (TextEditorHighlightingPassRegistrarEx)getInstance(project);
    }

    
    public abstract List<TextEditorHighlightingPass> instantiatePasses( PsiFile psiFile,  Editor editor,  int[] passesToIgnore);
    
    public abstract List<TextEditorHighlightingPass> instantiateMainPasses( PsiFile psiFile,
                                                                            Document document,
                                                                            HighlightInfoProcessor highlightInfoProcessor);

    
    public abstract List<DirtyScopeTrackingHighlightingPassFactory> getDirtyScopeTrackingFactories();
}
