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

package com.gome.maven.refactoring;

import com.gome.maven.find.findUsages.PsiElement2UsageTargetAdapter;
import com.gome.maven.history.LocalHistory;
import com.gome.maven.history.LocalHistoryAction;
import com.gome.maven.ide.DataManager;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.command.UndoConfirmationPolicy;
import com.gome.maven.openapi.command.undo.BasicUndoableAction;
import com.gome.maven.openapi.command.undo.UndoManager;
import com.gome.maven.openapi.command.undo.UndoableAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.EmptyRunnable;
import com.gome.maven.openapi.util.Factory;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.wm.impl.status.StatusBarUtil;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.refactoring.listeners.RefactoringEventData;
import com.gome.maven.refactoring.listeners.RefactoringEventListener;
import com.gome.maven.refactoring.listeners.RefactoringListenerManager;
import com.gome.maven.refactoring.listeners.impl.RefactoringListenerManagerImpl;
import com.gome.maven.refactoring.listeners.impl.RefactoringTransaction;
import com.gome.maven.refactoring.ui.ConflictsDialog;
import com.gome.maven.refactoring.util.CommonRefactoringUtil;
import com.gome.maven.refactoring.util.MoveRenameUsageInfo;
import com.gome.maven.ui.GuiUtils;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usageView.UsageViewDescriptor;
import com.gome.maven.usageView.UsageViewUtil;
import com.gome.maven.usages.*;
import com.gome.maven.usages.rules.PsiElementUsage;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.HashSet;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.ui.UIUtil;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class BaseRefactoringProcessor implements Runnable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.refactoring.BaseRefactoringProcessor");

    
    protected final Project myProject;

    private RefactoringTransaction myTransaction;
    private boolean myIsPreviewUsages;
    protected Runnable myPrepareSuccessfulSwingThreadCallback = EmptyRunnable.INSTANCE;

    protected BaseRefactoringProcessor( Project project) {
        this(project, null);
    }

    protected BaseRefactoringProcessor( Project project,  Runnable prepareSuccessfulCallback) {
        myProject = project;
        myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulCallback;
    }

    
    protected abstract UsageViewDescriptor createUsageViewDescriptor(UsageInfo[] usages);

    /**
     * Is called inside atomic action.
     */
    
    protected abstract UsageInfo[] findUsages();

    /**
     * is called when usage search is re-run.
     *
     * @param elements - refreshed elements that are returned by UsageViewDescriptor.getElements()
     */
    protected void refreshElements(PsiElement[] elements) {}

    /**
     * Is called inside atomic action.
     *
     * @param refUsages usages to be filtered
     * @return true if preprocessed successfully
     */
    protected boolean preprocessUsages(Ref<UsageInfo[]> refUsages) {
        prepareSuccessful();
        return true;
    }

    /**
     * Is called inside atomic action.
     */
    protected boolean isPreviewUsages(UsageInfo[] usages) {
        return myIsPreviewUsages;
    }

    protected boolean isPreviewUsages() {
        return myIsPreviewUsages;
    }


    public void setPreviewUsages(boolean isPreviewUsages) {
        myIsPreviewUsages = isPreviewUsages;
    }

    public void setPrepareSuccessfulSwingThreadCallback(Runnable prepareSuccessfulSwingThreadCallback) {
        myPrepareSuccessfulSwingThreadCallback = prepareSuccessfulSwingThreadCallback;
    }

    protected RefactoringTransaction getTransaction() {
        return myTransaction;
    }

    /**
     * Is called in a command and inside atomic action.
     */
    protected abstract void performRefactoring(UsageInfo[] usages);

    protected abstract String getCommandName();

    protected void doRun() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        final Ref<UsageInfo[]> refUsages = new Ref<UsageInfo[]>();
        final Ref<Language> refErrorLanguage = new Ref<Language>();
        final Ref<Boolean> refProcessCanceled = new Ref<Boolean>();
        final Ref<Boolean> dumbModeOccurred = new Ref<Boolean>();
        final Ref<Boolean> anyException = new Ref<Boolean>();

        final Runnable findUsagesRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    refUsages.set(DumbService.getInstance(myProject).runReadActionInSmartMode(new Computable<UsageInfo[]>() {
                        @Override
                        public UsageInfo[] compute() {
                            return findUsages();
                        }
                    }));
                }
                catch (UnknownReferenceTypeException e) {
                    refErrorLanguage.set(e.getElementLanguage());
                }
                catch (ProcessCanceledException e) {
                    refProcessCanceled.set(Boolean.TRUE);
                }
                catch (IndexNotReadyException e) {
                    dumbModeOccurred.set(Boolean.TRUE);
                }
                catch (Throwable e) {
                    anyException.set(Boolean.TRUE);
                    LOG.error(e);
                }
            }
        };

        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(findUsagesRunnable, RefactoringBundle.message("progress.text"),
                true, myProject)) {
            return;
        }

        if (!refErrorLanguage.isNull()) {
            Messages.showErrorDialog(myProject, RefactoringBundle.message("unsupported.refs.found", refErrorLanguage.get().getDisplayName()), RefactoringBundle.message("error.title"));
            return;
        }
        if (!dumbModeOccurred.isNull()) {
            DumbService.getInstance(myProject).showDumbModeNotification("Usage search is not available until indices are ready");
            return;
        }
        if (!refProcessCanceled.isNull()) {
            Messages.showErrorDialog(myProject, "Index corruption detected. Please retry the refactoring - indexes will be rebuilt automatically", RefactoringBundle.message("error.title"));
            return;
        }

        if (!anyException.isNull()) {
            //do not proceed if find usages fails
            return;
        }
        assert !refUsages.isNull(): "Null usages from processor " + this;
        if (!preprocessUsages(refUsages)) return;
        final UsageInfo[] usages = refUsages.get();
        assert usages != null;
        UsageViewDescriptor descriptor = createUsageViewDescriptor(usages);

        boolean isPreview = isPreviewUsages(usages);
        if (!isPreview) {
            isPreview = !ensureElementsWritable(usages, descriptor) || UsageViewUtil.hasReadOnlyUsages(usages);
            if (isPreview) {
                StatusBarUtil.setStatusBarInfo(myProject, RefactoringBundle.message("readonly.occurences.found"));
            }
        }
        if (isPreview) {
            previewRefactoring(usages);
        }
        else {
            execute(usages);
        }
    }

    protected void previewRefactoring(final UsageInfo[] usages) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            execute(usages);
            return;
        }
        final UsageViewDescriptor viewDescriptor = createUsageViewDescriptor(usages);
        final PsiElement[] elements = viewDescriptor.getElements();
        final PsiElement2UsageTargetAdapter[] targets = PsiElement2UsageTargetAdapter.convert(elements);
        Factory<UsageSearcher> factory = new Factory<UsageSearcher>() {
            @Override
            public UsageSearcher create() {
                return new UsageInfoSearcherAdapter() {
                    @Override
                    public void generate( final Processor<Usage> processor) {
                        ApplicationManager.getApplication().runReadAction(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < elements.length; i++) {
                                    elements[i] = targets[i].getElement();
                                }
                                refreshElements(elements);
                            }
                        });
                        processUsages(processor, myProject);
                    }

                    @Override
                    protected UsageInfo[] findUsages() {
                        return BaseRefactoringProcessor.this.findUsages();
                    }
                };
            }
        };

        showUsageView(viewDescriptor, factory, usages);
    }

    protected boolean skipNonCodeUsages() {
        return false;
    }

    private boolean ensureElementsWritable( final UsageInfo[] usages, final UsageViewDescriptor descriptor) {
        Set<PsiElement> elements = new THashSet<PsiElement>(TObjectHashingStrategy.IDENTITY); // protect against poorly implemented equality
        for (UsageInfo usage : usages) {
            assert usage != null: "Found null element in usages array";
            if (skipNonCodeUsages() && usage.isNonCodeUsage()) continue;
            PsiElement element = usage.getElement();
            if (element != null) elements.add(element);
        }
        elements.addAll(getElementsToWrite(descriptor));
        return ensureFilesWritable(myProject, elements);
    }

    private static boolean ensureFilesWritable(final Project project, Collection<? extends PsiElement> elements) {
        PsiElement[] psiElements = PsiUtilCore.toPsiElementArray(elements);
        return CommonRefactoringUtil.checkReadOnlyStatus(project, psiElements);
    }

    protected void execute(final UsageInfo[] usages) {
        CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
            @Override
            public void run() {
                Collection<UsageInfo> usageInfos = new LinkedHashSet<UsageInfo>(Arrays.asList(usages));
                doRefactoring(usageInfos);
                if (isGlobalUndoAction()) CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject);
            }
        }, getCommandName(), null, getUndoConfirmationPolicy());
    }

    protected boolean isGlobalUndoAction() {
        return CommonDataKeys.EDITOR.getData(DataManager.getInstance().getDataContext()) == null;
    }

    @SuppressWarnings("MethodMayBeStatic")
    protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
        return UndoConfirmationPolicy.DEFAULT;
    }

    private static UsageViewPresentation createPresentation(UsageViewDescriptor descriptor, final Usage[] usages) {
        UsageViewPresentation presentation = new UsageViewPresentation();
        presentation.setTabText(RefactoringBundle.message("usageView.tabText"));
        presentation.setTargetsNodeText(descriptor.getProcessedElementsHeader());
        presentation.setShowReadOnlyStatusAsRed(true);
        presentation.setShowCancelButton(true);
        presentation.setUsagesString(RefactoringBundle.message("usageView.usagesText"));
        int codeUsageCount = 0;
        int nonCodeUsageCount = 0;
        int dynamicUsagesCount = 0;
        Set<PsiFile> codeFiles = new HashSet<PsiFile>();
        Set<PsiFile> nonCodeFiles = new HashSet<PsiFile>();
        Set<PsiFile> dynamicUsagesCodeFiles = new HashSet<PsiFile>();

        for (Usage usage : usages) {
            if (usage instanceof PsiElementUsage) {
                final PsiElementUsage elementUsage = (PsiElementUsage)usage;
                final PsiElement element = elementUsage.getElement();
                if (element == null) continue;
                final PsiFile containingFile = element.getContainingFile();
                if (elementUsage.isNonCodeUsage()) {
                    nonCodeUsageCount++;
                    nonCodeFiles.add(containingFile);
                }
                else {
                    codeUsageCount++;
                    codeFiles.add(containingFile);
                }
                if (usage instanceof UsageInfo2UsageAdapter) {
                    final UsageInfo usageInfo = ((UsageInfo2UsageAdapter)usage).getUsageInfo();
                    if (usageInfo instanceof MoveRenameUsageInfo && usageInfo.isDynamicUsage()) {
                        dynamicUsagesCount++;
                        dynamicUsagesCodeFiles.add(containingFile);
                    }
                }
            }
        }
        codeFiles.remove(null);
        nonCodeFiles.remove(null);
        dynamicUsagesCodeFiles.remove(null);

        String codeReferencesText = descriptor.getCodeReferencesText(codeUsageCount, codeFiles.size());
        presentation.setCodeUsagesString(codeReferencesText);
        final String commentReferencesText = descriptor.getCommentReferencesText(nonCodeUsageCount, nonCodeFiles.size());
        if (commentReferencesText != null) {
            presentation.setNonCodeUsagesString(commentReferencesText);
        }
        presentation.setDynamicUsagesString("Dynamic " + StringUtil.decapitalize(descriptor.getCodeReferencesText(dynamicUsagesCount, dynamicUsagesCodeFiles.size())));
        String generatedCodeString;
        if (codeReferencesText.contains("in code")) {
            generatedCodeString = StringUtil.replace(codeReferencesText, "in code", "in generated code");
        }
        else {
            generatedCodeString = codeReferencesText + " in generated code";
        }
        presentation.setUsagesInGeneratedCodeString(generatedCodeString);
        return presentation;
    }

    private void showUsageView(final UsageViewDescriptor viewDescriptor, final Factory<UsageSearcher> factory, final UsageInfo[] usageInfos) {
        UsageViewManager viewManager = UsageViewManager.getInstance(myProject);

        final PsiElement[] initialElements = viewDescriptor.getElements();
        final UsageTarget[] targets = PsiElement2UsageTargetAdapter.convert(initialElements);
        final Ref<Usage[]> convertUsagesRef = new Ref<Usage[]>();
        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                    @Override
                    public void run() {
                        convertUsagesRef.set(UsageInfo2UsageAdapter.convert(usageInfos));
                    }
                });
            }
        }, "Preprocess usages", true, myProject)) return;

        if (convertUsagesRef.isNull()) return;

        final Usage[] usages = convertUsagesRef.get();

        final UsageViewPresentation presentation = createPresentation(viewDescriptor, usages);

        final UsageView usageView = viewManager.showUsages(targets, usages, presentation, factory);

        final Runnable refactoringRunnable = new Runnable() {
            @Override
            public void run() {
                Set<UsageInfo> usagesToRefactor = UsageViewUtil.getNotExcludedUsageInfos(usageView);
                final UsageInfo[] infos = usagesToRefactor.toArray(new UsageInfo[usagesToRefactor.size()]);
                if (ensureElementsWritable(infos, viewDescriptor)) {
                    execute(infos);
                }
            }
        };

        String canNotMakeString = RefactoringBundle.message("usageView.need.reRun");

        addDoRefactoringAction(usageView, refactoringRunnable, canNotMakeString);
    }

    protected void addDoRefactoringAction(UsageView usageView, Runnable refactoringRunnable, String canNotMakeString) {
        usageView.addPerformOperationAction(refactoringRunnable, getCommandName(), canNotMakeString,
                RefactoringBundle.message("usageView.doAction"), false);
    }

    private void doRefactoring( final Collection<UsageInfo> usageInfoSet) {
        for (Iterator<UsageInfo> iterator = usageInfoSet.iterator(); iterator.hasNext();) {
            UsageInfo usageInfo = iterator.next();
            final PsiElement element = usageInfo.getElement();
            if (element == null || !isToBeChanged(usageInfo)) {
                iterator.remove();
            }
        }

        LocalHistoryAction action = LocalHistory.getInstance().startAction(getCommandName());

        final UsageInfo[] writableUsageInfos = usageInfoSet.toArray(new UsageInfo[usageInfoSet.size()]);
        try {
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
            RefactoringListenerManagerImpl listenerManager = (RefactoringListenerManagerImpl)RefactoringListenerManager.getInstance(myProject);
            myTransaction = listenerManager.startTransaction();
            final Map<RefactoringHelper, Object> preparedData = new LinkedHashMap<RefactoringHelper, Object>();
            final Runnable prepareHelpersRunnable = new Runnable() {
                @Override
                public void run() {
                    for (final RefactoringHelper helper : Extensions.getExtensions(RefactoringHelper.EP_NAME)) {
                        Object operation = ApplicationManager.getApplication().runReadAction(new Computable<Object>() {
                            @Override
                            public Object compute() {
                                return helper.prepareOperation(writableUsageInfos);
                            }
                        });
                        preparedData.put(helper, operation);
                    }
                }
            };

            ProgressManager.getInstance().runProcessWithProgressSynchronously(prepareHelpersRunnable, "Prepare ...", false, myProject);

            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    final String refactoringId = getRefactoringId();
                    if (refactoringId != null) {
                        RefactoringEventData data = getBeforeData();
                        if (data != null) {
                            data.addUsages(usageInfoSet);
                        }
                        myProject.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringStarted(refactoringId, data);
                    }

                    try {
                        if (refactoringId != null) {
                            UndoableAction action = new BasicUndoableAction() {
                                @Override
                                public void undo() {
                                    myProject.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).undoRefactoring(refactoringId);
                                }

                                @Override
                                public void redo() {
                                }
                            };
                            UndoManager.getInstance(myProject).undoableActionPerformed(action);
                        }

                        performRefactoring(writableUsageInfos);
                    }
                    finally {
                        if (refactoringId != null) {
                            myProject.getMessageBus()
                                    .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringDone(refactoringId, getAfterData(writableUsageInfos));
                        }
                    }
                }
            });

            for(Map.Entry<RefactoringHelper, Object> e: preparedData.entrySet()) {
                //noinspection unchecked
                e.getKey().performOperation(myProject, e.getValue());
            }
            myTransaction.commit();
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    performPsiSpoilingRefactoring();
                }
            });
        }
        finally {
            action.finish();
        }

        int count = writableUsageInfos.length;
        if (count > 0) {
            StatusBarUtil.setStatusBarInfo(myProject, RefactoringBundle.message("statusBar.refactoring.result", count));
        }
        else {
            if (!isPreviewUsages(writableUsageInfos)) {
                StatusBarUtil.setStatusBarInfo(myProject, RefactoringBundle.message("statusBar.noUsages"));
            }
        }
    }

    protected boolean isToBeChanged(UsageInfo usageInfo) {
        return usageInfo.isWritable();
    }

    /**
     * Refactorings that spoil PSI (write something directly to documents etc.) should
     * do that in this method.<br>
     * This method is called immediately after
     * <code>{@link #performRefactoring(UsageInfo[])}</code>.
     */
    protected void performPsiSpoilingRefactoring() {
    }

    protected void prepareSuccessful() {
        if (myPrepareSuccessfulSwingThreadCallback != null) {
            // make sure that dialog is closed in swing thread
            try {
                GuiUtils.runOrInvokeAndWait(myPrepareSuccessfulSwingThreadCallback);
            }
            catch (InterruptedException e) {
                LOG.error(e);
            }
            catch (InvocationTargetException e) {
                LOG.error(e);
            }
        }
    }

    @Override
    public final void run() {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            ApplicationManager.getApplication().assertIsDispatchThread();
            doRun();
            UIUtil.dispatchAllInvocationEvents();
            UIUtil.dispatchAllInvocationEvents();
            return;
        }
        if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
            DumbService.getInstance(myProject).smartInvokeLater(new Runnable() {
                @Override
                public void run() {
                    doRun();
                }
            });
        }
        else {
            doRun();
        }
    }

    public static class ConflictsInTestsException extends RuntimeException {
        private final Collection<? extends String> messages;

        private static boolean myTestIgnore = false;

        public ConflictsInTestsException(Collection<? extends String> messages) {
            this.messages = messages;
        }

        public static void setTestIgnore(boolean myIgnore) {
            myTestIgnore = myIgnore;
        }

        public static boolean isTestIgnore() {
            return myTestIgnore;
        }

        public Collection<String> getMessages() {
            List<String> result = new ArrayList<String>(messages);
            for (int i = 0; i < messages.size(); i++) {
                result.set(i, result.get(i).replaceAll("<[^>]+>", ""));
            }
            return result;
        }

        @Override
        public String getMessage() {
            return StringUtil.join(messages, "\n");
        }
    }

    @Deprecated
    protected boolean showConflicts(final MultiMap<PsiElement, String> conflicts) {
        return showConflicts(conflicts, null);
    }

    protected boolean showConflicts(final MultiMap<PsiElement, String> conflicts,  final UsageInfo[] usages) {
        if (!conflicts.isEmpty() && ApplicationManager.getApplication().isUnitTestMode()) {
            throw new ConflictsInTestsException(conflicts.values());
        }

        if (myPrepareSuccessfulSwingThreadCallback != null && !conflicts.isEmpty()) {
            final String refactoringId = getRefactoringId();
            if (refactoringId != null) {
                RefactoringEventData conflictUsages = new RefactoringEventData();
                conflictUsages.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts.values());
                myProject.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                        .conflictsDetected(refactoringId, conflictUsages);
            }
            final ConflictsDialog conflictsDialog = prepareConflictsDialog(conflicts, usages);
            if (!conflictsDialog.showAndGet()) {
                if (conflictsDialog.isShowConflicts()) prepareSuccessful();
                return false;
            }
        }

        prepareSuccessful();
        return true;
    }

    
    protected ConflictsDialog prepareConflictsDialog(MultiMap<PsiElement, String> conflicts,  final UsageInfo[] usages) {
        final ConflictsDialog conflictsDialog = createConflictsDialog(conflicts, usages);
        conflictsDialog.setCommandName(getCommandName());
        return conflictsDialog;
    }

    
    protected RefactoringEventData getBeforeData() {
        return null;
    }

    
    protected RefactoringEventData getAfterData(UsageInfo[] usages) {
        return null;
    }

    
    protected String getRefactoringId() {
        return null;
    }

    
    protected ConflictsDialog createConflictsDialog(MultiMap<PsiElement, String> conflicts,  final UsageInfo[] usages) {
        return new ConflictsDialog(myProject, conflicts, usages == null ? null : new Runnable() {
            @Override
            public void run() {
                execute(usages);
            }
        }, false, true);
    }

    
    protected Collection<? extends PsiElement> getElementsToWrite( UsageViewDescriptor descriptor) {
        return Arrays.asList(descriptor.getElements());
    }

    public static class UnknownReferenceTypeException extends RuntimeException {
        private final Language myElementLanguage;

        public UnknownReferenceTypeException(final Language elementLanguage) {
            myElementLanguage = elementLanguage;
        }

        public Language getElementLanguage() {
            return myElementLanguage;
        }
    }
}
