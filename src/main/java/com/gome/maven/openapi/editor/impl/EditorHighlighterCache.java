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
package com.gome.maven.openapi.editor.impl;

import com.gome.maven.lexer.Lexer;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ex.util.LexerEditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighterFactory;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.impl.cache.impl.id.PlatformIdTableBuilding;
import com.gome.maven.psi.impl.search.LexerEditorHighlighterLexer;
import com.gome.maven.reference.SoftReference;

import java.lang.ref.WeakReference;

/**
 * @author yole
 */
public class EditorHighlighterCache {
    private static final Key<WeakReference<EditorHighlighter>> ourSomeEditorSyntaxHighlighter = Key.create("some editor highlighter");

    private EditorHighlighterCache() {
    }

    public static void rememberEditorHighlighterForCachesOptimization(Document document,  final EditorHighlighter highlighter) {
        document.putUserData(ourSomeEditorSyntaxHighlighter, new WeakReference<EditorHighlighter>(highlighter));
    }

    
    public static EditorHighlighter getEditorHighlighterForCachesBuilding(Document document) {
        if (document == null) {
            return null;
        }
        final WeakReference<EditorHighlighter> editorHighlighterWeakReference = document.getUserData(ourSomeEditorSyntaxHighlighter);
        final EditorHighlighter someEditorHighlighter = SoftReference.dereference(editorHighlighterWeakReference);

        if (someEditorHighlighter instanceof LexerEditorHighlighter &&
                ((LexerEditorHighlighter)someEditorHighlighter).isValid()
                ) {
            return someEditorHighlighter;
        }
        document.putUserData(ourSomeEditorSyntaxHighlighter, null);
        return null;
    }

    
    public static Lexer getLexerBasedOnLexerHighlighter(CharSequence text, VirtualFile virtualFile, Project project) {
        EditorHighlighter highlighter = null;

        PsiFile psiFile = virtualFile != null ? PsiManager.getInstance(project).findFile(virtualFile) : null;
        final Document document = psiFile != null ? PsiDocumentManager.getInstance(project).getDocument(psiFile) : null;
        final EditorHighlighter cachedEditorHighlighter;
        boolean alreadyInitializedHighlighter = false;

        if (document != null &&
                (cachedEditorHighlighter = getEditorHighlighterForCachesBuilding(document)) != null &&
                PlatformIdTableBuilding.checkCanUseCachedEditorHighlighter(text, cachedEditorHighlighter)) {
            highlighter = cachedEditorHighlighter;
            alreadyInitializedHighlighter = true;
        }
        else if (virtualFile != null) {
            highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, virtualFile);
        }

        if (highlighter != null) {
            return new LexerEditorHighlighterLexer(highlighter, alreadyInitializedHighlighter);
        }
        return null;
    }
}
