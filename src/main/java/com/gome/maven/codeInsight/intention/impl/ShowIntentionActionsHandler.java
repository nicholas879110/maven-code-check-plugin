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

package com.gome.maven.codeInsight.intention.impl;

import com.gome.maven.codeInsight.CodeInsightActionHandler;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.gome.maven.codeInsight.daemon.impl.ShowIntentionsPass;
import com.gome.maven.codeInsight.hint.HintManagerImpl;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInsight.intention.PsiElementBaseIntentionAction;
import com.gome.maven.codeInsight.lookup.LookupEx;
import com.gome.maven.codeInsight.lookup.LookupManager;
import com.gome.maven.codeInsight.template.impl.TemplateManagerImpl;
import com.gome.maven.codeInsight.template.impl.TemplateState;
import com.gome.maven.codeInspection.SuppressIntentionActionFromFix;
import com.gome.maven.featureStatistics.FeatureUsageTracker;
import com.gome.maven.featureStatistics.FeatureUsageTrackerImpl;
import com.gome.maven.injected.editor.EditorWindow;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.psi.PsiCodeFragment;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.PairProcessor;
import com.gome.maven.util.ThreeState;

/**
 * @author mike
 */
public class ShowIntentionActionsHandler implements CodeInsightActionHandler {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler");

    @Override
    public void invoke( final Project project,  Editor editor,  PsiFile file) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        if (editor instanceof EditorWindow) {
            editor = ((EditorWindow)editor).getDelegate();
            file = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file);
        }

        final LookupEx lookup = LookupManager.getActiveLookup(editor);
        if (lookup != null) {
            lookup.showElementActions();
            return;
        }

        if (HintManagerImpl.getInstanceImpl().performCurrentQuestionAction()) return;

        //intentions check isWritable before modification: if (!file.isWritable()) return;
        if (file instanceof PsiCodeFragment) return;

        TemplateState state = TemplateManagerImpl.getTemplateState(editor);
        if (state != null && !state.isFinished()) {
            return;
        }

        final DaemonCodeAnalyzerImpl codeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
        codeAnalyzer.autoImportReferenceAtCursor(editor, file); //let autoimport complete

        ShowIntentionsPass.IntentionsInfo intentions = new ShowIntentionsPass.IntentionsInfo();
        ShowIntentionsPass.getActionsToShow(editor, file, intentions, -1);

        if (!intentions.isEmpty()) {
            IntentionHintComponent.showIntentionHint(project, file, editor, intentions, true);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    // returns editor,file where the action is available or null if there are none
    public static boolean availableFor( PsiFile file,  Editor editor,  IntentionAction action) {
        if (!file.isValid()) return false;

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        boolean inProject = file.getManager().isInProject(file);
        return isAvailableHere(editor, file, element, inProject, action);
    }

    private static boolean isAvailableHere(Editor editor, PsiFile psiFile, PsiElement psiElement, boolean inProject, IntentionAction action) {
        try {
            Project project = psiFile.getProject();
            if (action instanceof SuppressIntentionActionFromFix) {
                final ThreeState shouldBeAppliedToInjectionHost = ((SuppressIntentionActionFromFix)action).isShouldBeAppliedToInjectionHost();
                if (editor instanceof EditorWindow && shouldBeAppliedToInjectionHost == ThreeState.YES) {
                    return false;
                }
                if (!(editor instanceof EditorWindow) && shouldBeAppliedToInjectionHost == ThreeState.NO) {
                    return false;
                }
            }

            if (action instanceof PsiElementBaseIntentionAction) {
                if (!inProject || psiElement == null || !((PsiElementBaseIntentionAction)action).isAvailable(project, editor, psiElement)) return false;
            }
            else if (!action.isAvailable(project, editor, psiFile)) {
                return false;
            }
        }
        catch (IndexNotReadyException e) {
            return false;
        }
        return true;
    }


    public static Pair<PsiFile,Editor> chooseBetweenHostAndInjected( PsiFile hostFile,  Editor hostEditor,  PairProcessor<PsiFile, Editor> predicate) {
        Editor editorToApply = null;
        PsiFile fileToApply = null;

        int offset = hostEditor.getCaretModel().getOffset();
        PsiFile injectedFile = InjectedLanguageUtil.findInjectedPsiNoCommit(hostFile, offset);
        if (injectedFile != null) {
            Editor injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(hostEditor, injectedFile);
            if (predicate.process(injectedFile, injectedEditor)) {
                editorToApply = injectedEditor;
                fileToApply = injectedFile;
            }
        }

        if (editorToApply == null && predicate.process(hostFile, hostEditor)) {
            editorToApply = hostEditor;
            fileToApply = hostFile;
        }
        if (editorToApply == null) return null;
        return Pair.create(fileToApply, editorToApply);
    }

    public static boolean chooseActionAndInvoke( PsiFile hostFile,
                                                 final Editor hostEditor,
                                                 final IntentionAction action,
                                                 String text) {
        if (!hostFile.isValid()) return false;
        final Project project = hostFile.getProject();
        FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickFix");
        ((FeatureUsageTrackerImpl)FeatureUsageTracker.getInstance()).getFixesStats().registerInvocation();

        Pair<PsiFile, Editor> pair = chooseBetweenHostAndInjected(hostFile, hostEditor, new PairProcessor<PsiFile, Editor>() {
            @Override
            public boolean process(PsiFile psiFile, Editor editor) {
                return availableFor(psiFile, editor, action);
            }
        });
        if (pair == null) return false;
        final Editor editorToApply = pair.second;
        final PsiFile fileToApply = pair.first;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    action.invoke(project, editorToApply, fileToApply);
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
                DaemonCodeAnalyzer.getInstance(project).updateVisibleHighlighters(hostEditor);
            }
        };

        if (action.startInWriteAction()) {
            final Runnable _runnable = runnable;
            runnable = new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(_runnable);
                }
            };
        }

        CommandProcessor.getInstance().executeCommand(project, runnable, text, null);
        return true;
    }
}
