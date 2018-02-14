/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.codeHighlighting.BackgroundEditorHighlighter;
import com.gome.maven.codeHighlighting.Pass;
import com.gome.maven.codeHighlighting.TextEditorHighlightingPass;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiCompiledFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.PsiFileEx;
import com.gome.maven.util.ArrayUtil;

import java.util.Collections;
import java.util.List;

public class TextEditorBackgroundHighlighter implements BackgroundEditorHighlighter {
    private static final int[] EXCEPT_OVERRIDDEN = {
            Pass.UPDATE_FOLDING,
            Pass.POPUP_HINTS,
            Pass.UPDATE_ALL,
            Pass.LOCAL_INSPECTIONS,
            Pass.WHOLE_FILE_LOCAL_INSPECTIONS,
            Pass.EXTERNAL_TOOLS,
    };

    private final Editor myEditor;
    private final Document myDocument;
    private PsiFile myFile;
    private final Project myProject;
    private boolean myCompiled;

    public TextEditorBackgroundHighlighter( Project project,  Editor editor) {
        myProject = project;
        myEditor = editor;
        myDocument = myEditor.getDocument();
        renewFile();
    }

    private void renewFile() {
        if (myFile == null || !myFile.isValid()) {
            myFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
            myCompiled = myFile instanceof PsiCompiledFile;
            if (myCompiled) {
                myFile = ((PsiCompiledFile)myFile).getDecompiledPsiFile();
            }
            if (myFile != null && !myFile.isValid()) {
                myFile = null;
            }
        }

        if (myFile != null) {
            myFile.putUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING, Boolean.TRUE);
        }
    }

    
    List<TextEditorHighlightingPass> getPasses( int[] passesToIgnore) {
        if (myProject.isDisposed()) return Collections.emptyList();
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        renewFile();
        if (myFile == null) return Collections.emptyList();
        if (myCompiled) {
            passesToIgnore = EXCEPT_OVERRIDDEN;
        }
        else if (!DaemonCodeAnalyzer.getInstance(myProject).isHighlightingAvailable(myFile)) {
            return Collections.emptyList();
        }

        TextEditorHighlightingPassRegistrarEx passRegistrar = TextEditorHighlightingPassRegistrarEx.getInstanceEx(myProject);

        return passRegistrar.instantiatePasses(myFile, myEditor, passesToIgnore);
    }

    @Override
    
    public TextEditorHighlightingPass[] createPassesForVisibleArea() {
        return createPassesForEditor();
    }

    @Override
    
    public TextEditorHighlightingPass[] createPassesForEditor() {
        List<TextEditorHighlightingPass> passes = getPasses(ArrayUtil.EMPTY_INT_ARRAY);
        return passes.isEmpty() ? TextEditorHighlightingPass.EMPTY_ARRAY : passes.toArray(new TextEditorHighlightingPass[passes.size()]);
    }
}
