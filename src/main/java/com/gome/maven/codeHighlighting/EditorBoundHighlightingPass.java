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
package com.gome.maven.codeHighlighting;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.psi.PsiFile;

/**
 * The pass which should be applied to every editor, even if there are many for this document.
 *
 * Ordinary {@link TextEditorHighlightingPass} is document-bound,
 * i.e. after the pass finishes the markup is stored in the document.
 * For example, there is no point to recalculate syntax errors for each splitted editor of the same document.
 * This pass however is for editor-specific markup, e.g. code folding.
 */
public abstract class EditorBoundHighlightingPass extends TextEditorHighlightingPass {
     protected final Editor myEditor;
     protected final PsiFile myFile;

    protected EditorBoundHighlightingPass( Editor editor,
                                           PsiFile psiFile,
                                          boolean runIntentionPassAfter) {
        super(psiFile.getProject(), editor.getDocument(), runIntentionPassAfter);
        myEditor = editor;
        myFile = psiFile;
    }
}
