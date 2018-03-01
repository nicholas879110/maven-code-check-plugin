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

package com.gome.maven.psi.util;

import com.gome.maven.ide.DataManager;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Caret;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.TextEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.AsyncResult;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.NonPhysicalFileSystem;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PsiUtilBase extends PsiUtilCore implements PsiEditorUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.util.PsiUtilBase");
    public static final Comparator<Language> LANGUAGE_COMPARATOR = new Comparator<Language>() {
        @Override
        public int compare( Language o1,  Language o2) {
            return o1.getID().compareTo(o2.getID());
        }
    };

    public static int getRootIndex(PsiElement root) {
        ASTNode node = root.getNode();
        while(node != null && node.getTreeParent() != null) {
            node = node.getTreeParent();
        }
        if(node != null) root = node.getPsi();
        final PsiFile containingFile = root.getContainingFile();
        FileViewProvider provider = containingFile.getViewProvider();
        Set<Language> languages = provider.getLanguages();
        if (languages.size() == 1) {
            return 0;
        }
        List<Language> array = new ArrayList<Language>(languages);
        Collections.sort(array, LANGUAGE_COMPARATOR);
        for (int i = 0; i < array.size(); i++) {
            Language language = array.get(i);
            if (provider.getPsi(language) == containingFile) return i;
        }
        throw new RuntimeException("Cannot find root for: "+root);
    }

    public static boolean isUnderPsiRoot(PsiFile root, PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == root) return true;
        for (PsiFile psiRoot : root.getPsiRoots()) {
            if (containingFile == psiRoot) return true;
        }
        PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(root.getProject()).getInjectionHost(element);
        return host != null && isUnderPsiRoot(root, host);
    }

    
    public static Language getLanguageInEditor( final Editor editor,  final Project project) {
        return getLanguageInEditor(editor.getCaretModel().getCurrentCaret(), project);
    }

    
    public static Language getLanguageInEditor( Caret caret,  final Project project) {
        Editor editor = caret.getEditor();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) return null;

        int caretOffset = caret.getOffset();
        int mostProbablyCorrectLanguageOffset = caretOffset == caret.getSelectionEnd() ? caret.getSelectionStart() : caretOffset;
        PsiElement elt = getElementAtOffset(file, mostProbablyCorrectLanguageOffset);
        Language lang = findLanguageFromElement(elt);

        if (caret.hasSelection()) {
            final Language rangeLanguage = evaluateLanguageInRange(caret.getSelectionStart(), caret.getSelectionEnd(), file);
            if (rangeLanguage == null) return file.getLanguage();

            lang = rangeLanguage;
        }

        return narrowLanguage(lang, file.getLanguage());
    }

    
    public static PsiElement getElementAtCaret( Editor editor) {
        Project project = editor.getProject();
        if (project == null) return null;
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        return file == null ? null : file.findElementAt(editor.getCaretModel().getOffset());
    }

    
    public static PsiFile getPsiFileInEditor( final Editor editor,  final Project project) {
        return getPsiFileInEditor(editor.getCaretModel().getCurrentCaret(), project);
    }

    
    public static PsiFile getPsiFileInEditor( Caret caret,  final Project project) {
        Editor editor = caret.getEditor();
        final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) return null;

        PsiUtilCore.ensureValid(file);

        final Language language = getLanguageInEditor(caret, project);
        if (language == null) return file;

        if (language == file.getLanguage()) return file;

        int caretOffset = caret.getOffset();
        int mostProbablyCorrectLanguageOffset = caretOffset == caret.getSelectionEnd() ? caret.getSelectionStart() : caretOffset;
        return getPsiFileAtOffset(file, mostProbablyCorrectLanguageOffset);
    }

    public static PsiFile getPsiFileAtOffset(final PsiFile file, final int offset) {
        PsiElement elt = getElementAtOffset(file, offset);

        assert elt.isValid() : elt + "; file: "+file + "; isvalid: "+file.isValid();
        return elt.getContainingFile();
    }

    
    public static Language reallyEvaluateLanguageInRange(final int start, final int end,  PsiFile file) {
        if (file instanceof PsiBinaryFile) {
            return file.getLanguage();
        }
        Language lang = null;
        int curOffset = start;
        do {
            PsiElement elt = getElementAtOffset(file, curOffset);

            if (!(elt instanceof PsiWhiteSpace)) {
                final Language language = findLanguageFromElement(elt);
                if (lang == null) {
                    lang = language;
                }
                else if (lang != language) {
                    return null;
                }
            }
            TextRange range = elt.getTextRange();
            if (range == null) {
                LOG.error("Null range for element " + elt + " of " + elt.getClass() + " in file " + file + " at offset " + curOffset);
                return file.getLanguage();
            }
            int endOffset = range.getEndOffset();
            curOffset = endOffset <= curOffset ? curOffset + 1 : endOffset;
        }
        while (curOffset < end);
        return narrowLanguage(lang, file.getLanguage());
    }

    
    public static Language evaluateLanguageInRange(final int start, final int end,  PsiFile file) {
        PsiElement elt = getElementAtOffset(file, start);

        TextRange selectionRange = new TextRange(start, end);
        if (!(elt instanceof PsiFile)) {
            elt = elt.getParent();
            TextRange range = elt.getTextRange();
            assert range != null : "Range is null for " + elt + "; " + elt.getClass();
            while(!range.contains(selectionRange) && !(elt instanceof PsiFile)) {
                elt = elt.getParent();
                if (elt == null) break;
                range = elt.getTextRange();
                assert range != null : "Range is null for " + elt + "; " + elt.getClass();
            }

            if (elt != null) {
                return elt.getLanguage();
            }
        }

        return reallyEvaluateLanguageInRange(start, end, file);
    }

    
    public static ASTNode getRoot( ASTNode node) {
        ASTNode child = node;
        do {
            final ASTNode parent = child.getTreeParent();
            if (parent == null) return child;
            child = parent;
        }
        while (true);
    }

    
    @Override
    public Editor findEditorByPsiElement( PsiElement element) {
        return findEditor(element);
    }

    /**
     * Tries to find editor for the given element.
     * <p/>
     * There are at least two approaches to achieve the target. Current method is intended to encapsulate both of them:
     * <ul>
     *   <li>target editor works with a real file that remains at file system;</li>
     *   <li>target editor works with a virtual file;</li>
     * </ul>
     * <p/>
     * Please don't use this method for finding an editor for quick fix.
     * @see {@link com.gome.maven.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement}
     *
     * @param element   target element
     * @return          editor that works with a given element if the one is found; <code>null</code> otherwise
     */
    
    public static Editor findEditor( PsiElement element) {
        if (!EventQueue.isDispatchThread()) {
            LOG.warn("Invoke findEditor() from EDT only. Otherwise, it causes deadlocks.");
        }
        PsiFile psiFile = element.getContainingFile();
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
        if (virtualFile == null) {
            return null;
        }

        Project project = psiFile.getProject();
        if (virtualFile.isInLocalFileSystem() || virtualFile.getFileSystem() instanceof NonPhysicalFileSystem) {
            // Try to find editor for the real file.
            final FileEditor[] editors = FileEditorManager.getInstance(project).getEditors(virtualFile);
            for (FileEditor editor : editors) {
                if (editor instanceof TextEditor) {
                    return ((TextEditor)editor).getEditor();
                }
            }
        }
        if (SwingUtilities.isEventDispatchThread()) {
            // We assume that data context from focus-based retrieval should success if performed from EDT.
            AsyncResult<DataContext> asyncResult = DataManager.getInstance().getDataContextFromFocus();
            if (asyncResult.isDone()) {
                Editor editor = CommonDataKeys.EDITOR.getData(asyncResult.getResult());
                if (editor != null) {
                    Document cachedDocument = PsiDocumentManager.getInstance(project).getCachedDocument(psiFile);
                    // Ensure that target editor is found by checking its document against the one from given PSI element.
                    if (cachedDocument == editor.getDocument()) {
                        return editor;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isSymLink( final PsiFileSystemItem element) {
        final VirtualFile virtualFile = element.getVirtualFile();
        return virtualFile != null && virtualFile.is(VFileProperty.SYMLINK);
    }

    
    public static VirtualFile asVirtualFile( PsiElement element) {
        if (element instanceof PsiFileSystemItem) {
            PsiFileSystemItem psiFileSystemItem = (PsiFileSystemItem)element;
            return psiFileSystemItem.isValid() ? psiFileSystemItem.getVirtualFile() : null;
        }
        return null;
    }
}
