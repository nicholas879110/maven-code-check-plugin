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
package com.gome.maven.refactoring.rename.inplace;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageExtension;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.openapi.command.impl.FinishMarkAction;
import com.gome.maven.openapi.command.impl.StartMarkAction;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.SelectionModel;
import com.gome.maven.openapi.editor.impl.EditorImpl;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.refactoring.RefactoringActionHandler;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.listeners.RefactoringElementListener;
import com.gome.maven.refactoring.listeners.RefactoringEventData;
import com.gome.maven.refactoring.listeners.RefactoringEventListener;
import com.gome.maven.refactoring.rename.*;
import com.gome.maven.refactoring.rename.naming.AutomaticRenamer;
import com.gome.maven.refactoring.rename.naming.AutomaticRenamerFactory;
import com.gome.maven.refactoring.util.CommonRefactoringUtil;
import com.gome.maven.refactoring.util.TextOccurrencesUtil;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.PairProcessor;
import com.gome.maven.util.containers.MultiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author ven
 */
public class VariableInplaceRenamer extends InplaceRefactoring {
    public static final LanguageExtension<ResolveSnapshotProvider> INSTANCE = new LanguageExtension<ResolveSnapshotProvider>(
            "com.gome.maven.rename.inplace.resolveSnapshotProvider"
    );
    private ResolveSnapshotProvider.ResolveSnapshot mySnapshot;
    private TextRange mySelectedRange;
    protected Language myLanguage;

    public VariableInplaceRenamer( PsiNamedElement elementToRename, Editor editor) {
        this(elementToRename, editor, elementToRename.getProject());
    }

    public VariableInplaceRenamer(PsiNamedElement elementToRename,
                                  Editor editor,
                                  Project project) {
        this(elementToRename, editor, project, elementToRename != null ? elementToRename.getName() : null,
                elementToRename != null ? elementToRename.getName() : null);
    }

    public VariableInplaceRenamer(PsiNamedElement elementToRename,
                                  Editor editor,
                                  Project project,
                                  final String initialName,
                                  final String oldName) {
        super(editor, elementToRename, project, initialName, oldName);
    }

    @Override
    protected boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
        return super.startsOnTheSameElement(handler, element) && handler instanceof VariableInplaceRenameHandler;
    }

    public boolean performInplaceRename() {
        return performInplaceRefactoring(null);
    }

    @Override
    protected void collectAdditionalElementsToRename(final List<Pair<PsiElement, TextRange>> stringUsages) {
        final String stringToSearch = myElementToRename.getName();
        final PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        if (stringToSearch != null) {
            TextOccurrencesUtil
                    .processUsagesInStringsAndComments(myElementToRename, stringToSearch, true, new PairProcessor<PsiElement, TextRange>() {
                        @Override
                        public boolean process(PsiElement psiElement, TextRange textRange) {
                            if (psiElement.getContainingFile() == currentFile) {
                                stringUsages.add(Pair.create(psiElement, textRange));
                            }
                            return true;
                        }
                    });
        }
    }

    @Override
    protected boolean buildTemplateAndStart(final Collection<PsiReference> refs,
                                            Collection<Pair<PsiElement, TextRange>> stringUsages,
                                            final PsiElement scope,
                                            final PsiFile containingFile) {
        if (appendAdditionalElement(refs, stringUsages)) {
            return super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);
        }
        else {
            final RenameChooser renameChooser = new RenameChooser(myEditor) {
                @Override
                protected void runRenameTemplate(Collection<Pair<PsiElement, TextRange>> stringUsages) {
                    VariableInplaceRenamer.super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);
                }
            };
            renameChooser.showChooser(refs, stringUsages);
        }
        return true;
    }

    protected boolean appendAdditionalElement(Collection<PsiReference> refs, Collection<Pair<PsiElement, TextRange>> stringUsages) {
        return stringUsages.isEmpty() || StartMarkAction.canStart(myProject) != null;
    }

    protected boolean shouldCreateSnapshot() {
        return true;
    }

    protected String getRefactoringId() {
        return "refactoring.rename";
    }

    @Override
    protected void beforeTemplateStart() {
        super.beforeTemplateStart();
        myLanguage = myScope.getLanguage();
        if (shouldCreateSnapshot()) {
            final ResolveSnapshotProvider resolveSnapshotProvider = INSTANCE.forLanguage(myLanguage);
            mySnapshot = resolveSnapshotProvider != null ? resolveSnapshotProvider.createSnapshot(myScope) : null;
        }

        final SelectionModel selectionModel = myEditor.getSelectionModel();
        mySelectedRange =
                selectionModel.hasSelection() ? new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd()) : null;
    }

    @Override
    protected void restoreSelection() {
        if (mySelectedRange != null) {
            myEditor.getSelectionModel().setSelection(mySelectedRange.getStartOffset(), mySelectedRange.getEndOffset());
        }
        else if (!shouldSelectAll()) {
            myEditor.getSelectionModel().removeSelection();
        }
    }

    @Override
    protected int restoreCaretOffset(int offset) {
        if (myCaretRangeMarker.isValid()) {
            if (myCaretRangeMarker.getStartOffset() <= offset && myCaretRangeMarker.getEndOffset() >= offset) {
                return offset;
            }
            return myCaretRangeMarker.getEndOffset();
        }
        return offset;
    }

    @Override
    protected boolean shouldSelectAll() {
        if (myEditor.getSettings().isPreselectRename()) return true;
        final Boolean selectAll = myEditor.getUserData(RenameHandlerRegistry.SELECT_ALL);
        return selectAll != null && selectAll.booleanValue();
    }

    protected VariableInplaceRenamer createInplaceRenamerToRestart(PsiNamedElement variable, Editor editor, String initialName) {
        return new VariableInplaceRenamer(variable, editor, myProject, initialName, myOldName);
    }

    protected void performOnInvalidIdentifier(final String newName, final LinkedHashSet<String> nameSuggestions) {
        final PsiNamedElement variable = getVariable();
        if (variable != null) {
            final int offset = variable.getTextOffset();
            restoreCaretOffset(offset);
            JBPopupFactory.getInstance()
                    .createConfirmation("Inserted identifier is not valid", "Continue editing", "Cancel", new Runnable() {
                        @Override
                        public void run() {
                            createInplaceRenamerToRestart(variable, myEditor, newName).performInplaceRefactoring(nameSuggestions);
                        }
                    }, 0).showInBestPositionFor(myEditor);
        }
    }

    protected void renameSynthetic(String newName) {
    }

    protected void performRefactoringRename(final String newName,
                                            final StartMarkAction markAction) {
        final String refactoringId = getRefactoringId();
        try {
            PsiNamedElement elementToRename = getVariable();
            if (refactoringId != null) {
                final RefactoringEventData beforeData = new RefactoringEventData();
                beforeData.addElement(elementToRename);
                beforeData.addStringProperties(myOldName);
                myProject.getMessageBus()
                        .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringStarted(refactoringId, beforeData);
            }
            if (!isIdentifier(newName, myLanguage)) {
                return;
            }
            if (elementToRename != null) {
                new WriteCommandAction(myProject, getCommandName()) {
                    @Override
                    protected void run(Result result) throws Throwable {
                        renameSynthetic(newName);
                    }
                }.execute();
            }
            for (AutomaticRenamerFactory renamerFactory : Extensions.getExtensions(AutomaticRenamerFactory.EP_NAME)) {
                if (renamerFactory.isApplicable(elementToRename)) {
                    final List<UsageInfo> usages = new ArrayList<UsageInfo>();
                    final AutomaticRenamer renamer =
                            renamerFactory.createRenamer(elementToRename, newName, new ArrayList<UsageInfo>());
                    if (renamer.hasAnythingToRename()) {
                        if (!ApplicationManager.getApplication().isUnitTestMode()) {
                            final AutomaticRenamingDialog renamingDialog = new AutomaticRenamingDialog(myProject, renamer);
                            if (!renamingDialog.showAndGet()) {
                                return;
                            }
                        }

                        final Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                ApplicationManager.getApplication().runReadAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        renamer.findUsages(usages, false, false);
                                    }
                                });
                            }
                        };

                        if (!ProgressManager.getInstance()
                                .runProcessWithProgressSynchronously(runnable, RefactoringBundle.message("searching.for.variables"), true, myProject)) {
                            return;
                        }

                        if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, PsiUtilCore.toPsiElementArray(renamer.getElements()))) return;
                        final Runnable performAutomaticRename = new Runnable() {
                            @Override
                            public void run() {
                                CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject);
                                final UsageInfo[] usageInfos = usages.toArray(new UsageInfo[usages.size()]);
                                final MultiMap<PsiElement, UsageInfo> classified = RenameProcessor.classifyUsages(renamer.getElements(), usageInfos);
                                for (final PsiNamedElement element : renamer.getElements()) {
                                    final String newElementName = renamer.getNewName(element);
                                    if (newElementName != null) {
                                        final Collection<UsageInfo> infos = classified.get(element);
                                        RenameUtil.doRename(element, newElementName, infos.toArray(new UsageInfo[infos.size()]), myProject, RefactoringElementListener.DEAF);
                                    }
                                }
                            }
                        };
                        final WriteCommandAction writeCommandAction = new WriteCommandAction(myProject, getCommandName()) {
                            @Override
                            protected void run(Result result) throws Throwable {
                                performAutomaticRename.run();
                            }
                        };
                        if (ApplicationManager.getApplication().isUnitTestMode()) {
                            writeCommandAction.execute();
                        } else {
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    writeCommandAction.execute();
                                }
                            });
                        }
                    }
                }
            }
        }
        finally {

            if (refactoringId != null) {
                final RefactoringEventData afterData = new RefactoringEventData();
                afterData.addElement(getVariable());
                afterData.addStringProperties(newName);
                myProject.getMessageBus()
                        .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringDone(refactoringId, afterData);
            }

            try {
                ((EditorImpl)InjectedLanguageUtil.getTopLevelEditor(myEditor)).stopDumbLater();
            }
            finally {
                FinishMarkAction.finish(myProject, myEditor, markAction);
            }
        }
    }

    @Override
    protected String getCommandName() {
        return RefactoringBundle.message("renaming.command.name", myInitialName);
    }

    @Override
    protected boolean performRefactoring() {
        boolean bind = false;
        if (myInsertedName != null) {

            final CommandProcessor commandProcessor = CommandProcessor.getInstance();
            if (commandProcessor.getCurrentCommand() != null && getVariable() != null) {
                commandProcessor.setCurrentCommandName(getCommandName());
            }

            bind = true;
            if (!isIdentifier(myInsertedName, myLanguage)) {
                performOnInvalidIdentifier(myInsertedName, myNameSuggestions);
            }
            else {
                if (mySnapshot != null) {
                    if (isIdentifier(myInsertedName, myLanguage)) {
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                mySnapshot.apply(myInsertedName);
                            }
                        });
                    }
                }
            }
            performRefactoringRename(myInsertedName, myMarkAction);
        }
        return bind;
    }

    @Override
    public void finish(boolean success) {
        super.finish(success);
        if (success) {
            revertStateOnFinish();
        }
        else {
            ((EditorImpl)InjectedLanguageUtil.getTopLevelEditor(myEditor)).stopDumbLater();
        }
    }

    protected void revertStateOnFinish() {
        if (myInsertedName == null || !isIdentifier(myInsertedName, myLanguage)) {
            revertState();
        }
    }
}
