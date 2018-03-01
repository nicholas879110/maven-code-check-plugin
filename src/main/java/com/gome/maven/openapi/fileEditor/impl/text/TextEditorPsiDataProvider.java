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

package com.gome.maven.openapi.fileEditor.impl.text;

import com.gome.maven.codeInsight.TargetElementUtilBase;
import com.gome.maven.ide.IdeView;
import com.gome.maven.ide.util.EditorHelper;
import com.gome.maven.injected.editor.EditorWindow;
import com.gome.maven.injected.editor.InjectedCaret;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Caret;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.fileEditor.EditorDataProvider;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.psi.util.PsiUtilCore;

import java.util.LinkedHashSet;

import static com.gome.maven.openapi.actionSystem.AnActionEvent.injectedId;
import static com.gome.maven.openapi.actionSystem.LangDataKeys.*;
import static com.gome.maven.util.containers.ContainerUtil.addIfNotNull;

public class TextEditorPsiDataProvider implements EditorDataProvider {
    @Override
    
    public Object getData( final String dataId,  final Editor e,  final Caret caret) {
        if (!(e instanceof EditorEx)) {
            return null;
        }
        VirtualFile file = ((EditorEx)e).getVirtualFile();
        if (file == null || !file.isValid()) return null;

        Project project = e.getProject();
        if (dataId.equals(injectedId(EDITOR.getName()))) {
            if (project == null || PsiDocumentManager.getInstance(project).isUncommited(e.getDocument())) {
                return e;
            }
            else {
                return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(e, caret, getPsiFile(e, file));
            }
        }
        if (HOST_EDITOR.is(dataId)) {
            return e instanceof EditorWindow ? ((EditorWindow)e).getDelegate() : e;
        }
        if (CARET.is(dataId)) {
            return caret;
        }
        if (dataId.equals(injectedId(CARET.getName()))) {
            Editor editor = (Editor)getData(injectedId(EDITOR.getName()), e, caret);
            assert editor != null;
            return getInjectedCaret(editor, caret);
        }
        if (dataId.equals(injectedId(PSI_ELEMENT.getName()))) {
            Editor editor = (Editor)getData(injectedId(EDITOR.getName()), e, caret);
            assert editor != null;
            Caret injectedCaret = getInjectedCaret(editor, caret);
            return getPsiElementIn(editor, injectedCaret, file);
        }
        if (PSI_ELEMENT.is(dataId)){
            return getPsiElementIn(e, caret, file);
        }
        if (dataId.equals(injectedId(LANGUAGE.getName()))) {
            PsiFile psiFile = (PsiFile)getData(injectedId(PSI_FILE.getName()), e, caret);
            Editor editor = (Editor)getData(injectedId(EDITOR.getName()), e, caret);
            if (psiFile == null || editor == null) return null;
            Caret injectedCaret = getInjectedCaret(editor, caret);
            return getLanguageAtCurrentPositionInEditor(injectedCaret, psiFile);
        }
        if (LANGUAGE.is(dataId)) {
            final PsiFile psiFile = getPsiFile(e, file);
            if (psiFile == null) return null;
            return getLanguageAtCurrentPositionInEditor(caret, psiFile);
        }
        if (dataId.equals(injectedId(VIRTUAL_FILE.getName()))) {
            PsiFile psiFile = (PsiFile)getData(injectedId(PSI_FILE.getName()), e, caret);
            if (psiFile == null) return null;
            return psiFile.getVirtualFile();
        }
        if (dataId.equals(injectedId(PSI_FILE.getName()))) {
            Editor editor = (Editor)getData(injectedId(EDITOR.getName()), e, caret);
            if (editor == null) {
                return null;
            }
            if (project == null) {
                return null;
            }
            return PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        }
        if (PSI_FILE.is(dataId)) {
            return getPsiFile(e, file);
        }
        if (IDE_VIEW.is(dataId)) {
            final PsiFile psiFile = project == null ? null : PsiManager.getInstance(project).findFile(file);
            final PsiDirectory psiDirectory = psiFile != null ? psiFile.getParent() : null;
            if (psiDirectory != null && (psiDirectory.isPhysical() || ApplicationManager.getApplication().isUnitTestMode())) {
                return new IdeView() {

                    @Override
                    public void selectElement(final PsiElement element) {
                        Editor editor = EditorHelper.openInEditor(element);
                        if (editor != null) {
                            ToolWindowManager.getInstance(element.getProject()).activateEditorComponent();
                        }
                    }

                    
                    @Override
                    public PsiDirectory[] getDirectories() {
                        return new PsiDirectory[]{psiDirectory};
                    }

                    @Override
                    public PsiDirectory getOrChooseDirectory() {
                        return psiDirectory;
                    }
                };
            }
        }
        if (CONTEXT_LANGUAGES.is(dataId)) {
            return computeLanguages(e, caret);
        }
        return null;
    }

    
    private static Caret getInjectedCaret( Editor editor,  Caret hostCaret) {
        if (!(editor instanceof EditorWindow) || hostCaret instanceof InjectedCaret) {
            return hostCaret;
        }
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            if (((InjectedCaret)caret).getDelegate() == hostCaret) {
                return caret;
            }
        }
        throw new IllegalArgumentException("Cannot find injected caret corresponding to " + hostCaret);
    }

    private static Language getLanguageAtCurrentPositionInEditor(Caret caret, final PsiFile psiFile) {
        int caretOffset = caret.getOffset();
        int mostProbablyCorrectLanguageOffset = caretOffset == caret.getSelectionStart() ||
                caretOffset == caret.getSelectionEnd()
                ? caret.getSelectionStart()
                : caretOffset;
        if (caret.hasSelection()) {
            return getLanguageAtOffset(psiFile, mostProbablyCorrectLanguageOffset, caret.getSelectionEnd());
        }

        return PsiUtilCore.getLanguageAtOffset(psiFile, mostProbablyCorrectLanguageOffset);
    }

    private static Language getLanguageAtOffset(PsiFile psiFile, int mostProbablyCorrectLanguageOffset, int end) {
        final PsiElement elt = psiFile.findElementAt(mostProbablyCorrectLanguageOffset);
        if (elt == null) return psiFile.getLanguage();
        if (elt instanceof PsiWhiteSpace) {
            final int incremented = elt.getTextRange().getEndOffset() + 1;
            if (incremented <= end) {
                return getLanguageAtOffset(psiFile, incremented, end);
            }
        }
        return PsiUtilCore.findLanguageFromElement(elt);
    }

    
    private static PsiElement getPsiElementIn( Editor editor,  Caret caret,  VirtualFile file) {
        final PsiFile psiFile = getPsiFile(editor, file);
        if (psiFile == null) return null;

        try {
            TargetElementUtilBase util = TargetElementUtilBase.getInstance();
            return util.findTargetElement(editor, util.getReferenceSearchFlags(), caret.getOffset());
        }
        catch (IndexNotReadyException e) {
            return null;
        }
    }

    
    private static PsiFile getPsiFile( Editor e,  VirtualFile file) {
        if (!file.isValid()) {
            return null; // fix for SCR 40329
        }
        final Project project = e.getProject();
        if (project == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return psiFile != null && psiFile.isValid() ? psiFile : null;
    }

    private Language[] computeLanguages( Editor editor,  Caret caret) {
        LinkedHashSet<Language> set = new LinkedHashSet<Language>(4);
        Language injectedLanguage = (Language)getData(injectedId(LANGUAGE.getName()), editor, caret);
        addIfNotNull(injectedLanguage, set);
        Language language = (Language)getData(LANGUAGE.getName(), editor, caret);
        addIfNotNull(language, set);
        PsiFile psiFile = (PsiFile)getData(PSI_FILE.getName(), editor, caret);
        if (psiFile != null) {
            addIfNotNull(psiFile.getViewProvider().getBaseLanguage(), set);
        }
        return set.toArray(new Language[set.size()]);
    }
}
