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

package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.CodeInsightSettings;
import com.gome.maven.codeInsight.TargetElementUtilBase;
import com.gome.maven.codeInsight.completion.impl.CompletionServiceImpl;
import com.gome.maven.codeInsight.completion.impl.CompletionSorterImpl;
import com.gome.maven.codeInsight.editorActions.CompletionAutoPopupHandler;
import com.gome.maven.codeInsight.hint.EditorHintListener;
import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.codeInsight.lookup.*;
import com.gome.maven.codeInsight.lookup.impl.LookupImpl;
import com.gome.maven.diagnostic.PerformanceWatcher;
import com.gome.maven.featureStatistics.FeatureUsageTracker;
import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.injected.editor.EditorWindow;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.application.WriteAction;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Caret;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.util.ProgressIndicatorBase;
import com.gome.maven.openapi.progress.util.ProgressWrapper;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.patterns.ElementPattern;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.ReferenceRange;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.ui.LightweightHint;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.ObjectUtils;
import com.gome.maven.util.ThreeState;
import com.gome.maven.util.concurrency.Semaphore;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBusConnection;
import com.gome.maven.util.ui.update.MergingUpdateQueue;
import com.gome.maven.util.ui.update.Update;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * @author peter
 */
public class CompletionProgressIndicator extends ProgressIndicatorBase implements CompletionProcess, Disposable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.completion.CompletionProgressIndicator");
    private final Editor myEditor;
    
    private final Caret myCaret;
    private final CompletionParameters myParameters;
    private final CodeCompletionHandlerBase myHandler;
    private final LookupImpl myLookup;
    private final MergingUpdateQueue myQueue;
    private final Update myUpdate = new Update("update") {
        @Override
        public void run() {
            updateLookup();
            myQueue.setMergingTimeSpan(300);
        }
    };
    private final Semaphore myFreezeSemaphore;
    private final OffsetMap myOffsetMap;
    private final List<Pair<Integer, ElementPattern<String>>> myRestartingPrefixConditions = ContainerUtil.createLockFreeCopyOnWriteList();
    private final LookupAdapter myLookupListener = new LookupAdapter() {
        @Override
        public void itemSelected(LookupEvent event) {
            finishCompletionProcess(false);

            LookupElement item = event.getItem();
            if (item == null) return;

            setMergeCommand();

            myHandler.lookupItemSelected(CompletionProgressIndicator.this, item, event.getCompletionChar(), myLookup.getItems());
        }


        @Override
        public void lookupCanceled(final LookupEvent event) {
            finishCompletionProcess(true);
        }
    };
    private volatile int myCount;
    private volatile boolean myHasPsiElements;
    private boolean myLookupUpdated;
    private final ConcurrentMap<LookupElement, CompletionSorterImpl> myItemSorters =
            ContainerUtil.newConcurrentMap(ContainerUtil.<LookupElement>identityStrategy());
    private final PropertyChangeListener myLookupManagerListener;
    private final Queue<Runnable> myAdvertiserChanges = new ConcurrentLinkedQueue<Runnable>();
    private final int myStartCaret;

    public CompletionProgressIndicator(final Editor editor,
                                        Caret caret,
                                       CompletionParameters parameters,
                                       CodeCompletionHandlerBase handler,
                                       Semaphore freezeSemaphore,
                                       final OffsetMap offsetMap,
                                       boolean hasModifiers,
                                       LookupImpl lookup) {
        myEditor = editor;
        myCaret = caret;
        myParameters = parameters;
        myHandler = handler;
        myFreezeSemaphore = freezeSemaphore;
        myOffsetMap = offsetMap;
        myLookup = lookup;
        myStartCaret = myEditor.getCaretModel().getOffset();

        myAdvertiserChanges.offer(new Runnable() {
            @Override
            public void run() {
                myLookup.getAdvertiser().clearAdvertisements();
            }
        });

        myLookup.setArranger(new CompletionLookupArranger(parameters, this));

        myLookup.addLookupListener(myLookupListener);
        myLookup.setCalculating(true);

        myLookupManagerListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null) {
                    LOG.error("An attempt to change the lookup during completion, phase = " + CompletionServiceImpl.getCompletionPhase());
                }
            }
        };
        LookupManager.getInstance(getProject()).addPropertyChangeListener(myLookupManagerListener);

        myQueue = new MergingUpdateQueue("completion lookup progress", 100, true, myEditor.getContentComponent());
        myQueue.setPassThrough(false);

        ApplicationManager.getApplication().assertIsDispatchThread();
        Disposer.register(this, offsetMap);

        if (hasModifiers && !ApplicationManager.getApplication().isUnitTestMode()) {
            trackModifiers();
        }
    }

    public OffsetMap getOffsetMap() {
        return myOffsetMap;
    }

    public int getSelectionEndOffset() {
        return getOffsetMap().getOffset(CompletionInitializationContext.SELECTION_END_OFFSET);
    }

    void duringCompletion(CompletionInitializationContext initContext) {
        if (isAutopopupCompletion()) {
            if (shouldPreselectFirstSuggestion(myParameters)) {
                if (!CodeInsightSettings.getInstance().SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS) {
                    myLookup.setFocusDegree(LookupImpl.FocusDegree.SEMI_FOCUSED);
                    if (FeatureUsageTracker.getInstance().isToBeAdvertisedInLookup(CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_CONTROL_DOT, getProject())) {
                        String dotShortcut = CompletionContributor.getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_DOT);
                        if (StringUtil.isNotEmpty(dotShortcut)) {
                            addAdvertisement("Press " + dotShortcut + " to choose the selected (or first) suggestion and insert a dot afterwards", null);
                        }
                    }
                } else {
                    myLookup.setFocusDegree(LookupImpl.FocusDegree.FOCUSED);
                }
            }
            if (!myEditor.isOneLineMode() &&
                    FeatureUsageTracker.getInstance()
                            .isToBeAdvertisedInLookup(CodeCompletionFeatures.EDITING_COMPLETION_CONTROL_ARROWS, getProject())) {
                String downShortcut = CompletionContributor.getActionShortcut(IdeActions.ACTION_LOOKUP_DOWN);
                String upShortcut = CompletionContributor.getActionShortcut(IdeActions.ACTION_LOOKUP_UP);
                if (StringUtil.isNotEmpty(downShortcut) && StringUtil.isNotEmpty(upShortcut)) {
                    addAdvertisement(downShortcut + " and " + upShortcut + " will move caret down and up in the editor", null);
                }
            }
        } else if (DumbService.isDumb(getProject())) {
            addAdvertisement("The results might be incomplete while indexing is in progress", MessageType.WARNING.getPopupBackground());
        }

        ProgressManager.checkCanceled();

        if (!initContext.getOffsetMap().wasModified(CompletionInitializationContext.IDENTIFIER_END_OFFSET)) {
            try {
                final int selectionEndOffset = initContext.getSelectionEndOffset();
                final PsiReference reference = TargetElementUtilBase.findReference(myEditor, selectionEndOffset);
                if (reference != null) {
                    initContext.setReplacementOffset(findReplacementOffset(selectionEndOffset, reference));
                }
            }
            catch (IndexNotReadyException ignored) {
            }
        }

        for (CompletionContributor contributor : CompletionContributor.forLanguage(initContext.getPositionLanguage())) {
            ProgressManager.checkCanceled();
            if (DumbService.getInstance(initContext.getProject()).isDumb() && !DumbService.isDumbAware(contributor)) {
                continue;
            }

            contributor.duringCompletion(initContext);
        }
    }

    
    CompletionSorterImpl getSorter(LookupElement element) {
        return myItemSorters.get(element);
    }

    @Override
    public void dispose() {
    }

    private static int findReplacementOffset(int selectionEndOffset, PsiReference reference) {
        final List<TextRange> ranges = ReferenceRange.getAbsoluteRanges(reference);
        for (TextRange range : ranges) {
            if (range.contains(selectionEndOffset)) {
                return range.getEndOffset();
            }
        }

        return selectionEndOffset;
    }


    void scheduleAdvertising() {
        if (myLookup.isAvailableToUser()) {
            return;
        }
        for (final CompletionContributor contributor : CompletionContributor.forParameters(myParameters)) {
            if (!myLookup.isCalculating() && !myLookup.isVisible()) return;

            @SuppressWarnings("deprecation") String s = contributor.advertise(myParameters);
            if (s != null) {
                addAdvertisement(s, null);
            }
        }
    }

    private boolean isOutdated() {
        return CompletionServiceImpl.getCompletionPhase().indicator != this;
    }

    private void trackModifiers() {
        assert !isAutopopupCompletion();

        final JComponent contentComponent = myEditor.getContentComponent();
        contentComponent.addKeyListener(new ModifierTracker(contentComponent));
    }

    public void setMergeCommand() {
        CommandProcessor.getInstance().setCurrentCommandGroupId(getCompletionCommandName());
    }

    private String getCompletionCommandName() {
        return "Completion" + hashCode();
    }

    public boolean showLookup() {
        return updateLookup();
    }

    public CompletionParameters getParameters() {
        return myParameters;
    }

    public CodeCompletionHandlerBase getHandler() {
        return myHandler;
    }

    public LookupImpl getLookup() {
        return myLookup;
    }

    private boolean updateLookup() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (isOutdated() || !shouldShowLookup()) return false;

        while (true) {
            Runnable action = myAdvertiserChanges.poll();
            if (action == null) break;
            action.run();
        }

        if (!myLookupUpdated) {
            if (myLookup.getAdvertisements().isEmpty() && !isAutopopupCompletion() && !DumbService.isDumb(getProject())) {
                DefaultCompletionContributor.addDefaultAdvertisements(myParameters, myLookup, myHasPsiElements);
            }
            myLookup.getAdvertiser().showRandomText();
        }

        boolean justShown = false;
        if (!myLookup.isShown()) {
            if (hideAutopopupIfMeaningless()) {
                return false;
            }

            if (Registry.is("dump.threads.on.empty.lookup") && myLookup.isCalculating() && myLookup.getItems().isEmpty()) {
                PerformanceWatcher.getInstance().dumpThreads("emptyLookup/", true);
            }

            if (!myLookup.showLookup()) {
                return false;
            }
            justShown = true;
        }
        myLookupUpdated = true;
        myLookup.refreshUi(true, justShown);
        hideAutopopupIfMeaningless();
        if (justShown) {
            myLookup.ensureSelectionVisible(true);
        }
        return true;
    }

    private boolean shouldShowLookup() {
        if (isAutopopupCompletion()) {
            if (myCount == 0) {
                return false;
            }
            if (myLookup.isCalculating() && Registry.is("ide.completion.delay.autopopup.until.completed")) {
                return false;
            }
        }
        return true;
    }

    final boolean isInsideIdentifier() {
        return getIdentifierEndOffset() != getSelectionEndOffset();
    }

    public int getIdentifierEndOffset() {
        return myOffsetMap.getOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET);
    }

    public synchronized void addItem(final CompletionResult item) {
        if (!isRunning()) return;
        ProgressManager.checkCanceled();

        final boolean unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
        if (!unitTestMode) {
            LOG.assertTrue(!ApplicationManager.getApplication().isDispatchThread());
        }

        LOG.assertTrue(myParameters.getPosition().isValid());

        LookupElement lookupElement = item.getLookupElement();
        if (!myHasPsiElements && lookupElement.getPsiElement() != null) {
            myHasPsiElements = true;
        }
        myItemSorters.put(lookupElement, (CompletionSorterImpl)item.getSorter());
        if (!myLookup.addItem(lookupElement, item.getPrefixMatcher())) {
            return;
        }
        myCount++;

        if (myCount == 1) {
            new Alarm(Alarm.ThreadToUse.SHARED_THREAD, this).addRequest(new Runnable() {
                @Override
                public void run() {
                    myFreezeSemaphore.up();
                }
            }, 300);
        }
        myQueue.queue(myUpdate);
    }

    public void closeAndFinish(boolean hideLookup) {
        if (!myLookup.isLookupDisposed()) {
            Lookup lookup = LookupManager.getActiveLookup(myEditor);
            LOG.assertTrue(lookup == myLookup, "lookup changed: " + lookup + "; " + this);
        }
        myLookup.removeLookupListener(myLookupListener);
        finishCompletionProcess(true);
        CompletionServiceImpl.assertPhase(CompletionPhase.NoCompletion.getClass());

        if (hideLookup) {
            LookupManager.getInstance(getProject()).hideActiveLookup();
        }
    }

    private void finishCompletionProcess(boolean disposeOffsetMap) {
        cancel();

        ApplicationManager.getApplication().assertIsDispatchThread();
        Disposer.dispose(myQueue);
        LookupManager.getInstance(getProject()).removePropertyChangeListener(myLookupManagerListener);

        CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCompletionService().getCurrentCompletion();
        LOG.assertTrue(currentCompletion == this, currentCompletion + "!=" + this);

        CompletionServiceImpl
                .assertPhase(CompletionPhase.BgCalculation.class, CompletionPhase.ItemsCalculated.class, CompletionPhase.Synchronous.class,
                        CompletionPhase.CommittingDocuments.class);
        CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
        if (oldPhase instanceof CompletionPhase.CommittingDocuments) {
            LOG.assertTrue(((CompletionPhase.CommittingDocuments)oldPhase).isRestartingCompletion(), oldPhase);
            ((CompletionPhase.CommittingDocuments)oldPhase).replaced = true;
        }
        CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
        if (disposeOffsetMap) {
            disposeIndicator();
        }
    }

    void disposeIndicator() {
        // our offset map should be disposed under write action, so that duringCompletion (read action) won't access it after disposing
        AccessToken token = WriteAction.start();
        try {
            Disposer.dispose(this);
        }
        finally {
            token.finish();
        }
    }

    
    public static void cleanupForNextTest() {
        CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCompletionService().getCurrentCompletion();
        if (currentCompletion != null) {
            currentCompletion.finishCompletionProcess(true);
            CompletionServiceImpl.assertPhase(CompletionPhase.NoCompletion.getClass());
        }
        else {
            CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
        }
        CompletionLookupArranger.cancelLastCompletionStatisticsUpdate();
    }

    @Override
    public void stop() {
        super.stop();

        myQueue.cancelAllUpdates();
        myFreezeSemaphore.up();

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();
                if (!(phase instanceof CompletionPhase.BgCalculation) || phase.indicator != CompletionProgressIndicator.this) return;

                LOG.assertTrue(!getProject().isDisposed(), "project disposed");

                if (myEditor.isDisposed()) {
                    LookupManager.getInstance(getProject()).hideActiveLookup();
                    CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
                    return;
                }

                if (myEditor instanceof EditorWindow) {
                    LOG.assertTrue(((EditorWindow)myEditor).getInjectedFile().isValid(), "injected file !valid");
                    LOG.assertTrue(((DocumentWindow)myEditor.getDocument()).isValid(), "docWindow !valid");
                }
                PsiFile file = myLookup.getPsiFile();
                LOG.assertTrue(file == null || file.isValid(), "file !valid");

                myLookup.setCalculating(false);

                if (myCount == 0) {
                    LookupManager.getInstance(getProject()).hideActiveLookup();
                    if (!isAutopopupCompletion()) {
                        final CompletionProgressIndicator current = CompletionServiceImpl.getCompletionService().getCurrentCompletion();
                        LOG.assertTrue(current == null, current + "!=" + CompletionProgressIndicator.this);

                        handleEmptyLookup(!((CompletionPhase.BgCalculation)phase).modifiersChanged);
                    }
                }
                else {
                    CompletionServiceImpl.setCompletionPhase(new CompletionPhase.ItemsCalculated(CompletionProgressIndicator.this));
                    updateLookup();
                }
            }
        }, myQueue.getModalityState());
    }

    private boolean hideAutopopupIfMeaningless() {
        if (!myLookup.isLookupDisposed() && isAutopopupCompletion() && !myLookup.isSelectionTouched() && !myLookup.isCalculating()) {
            myLookup.refreshUi(true, false);
            final List<LookupElement> items = myLookup.getItems();

            for (LookupElement item : items) {
                if (!myLookup.itemPattern(item).equals(item.getLookupString())) {
                    return false;
                }

                if (item.isValid() && item.isWorthShowingInAutoPopup()) {
                    return false;
                }
            }

            myLookup.hideLookup(false);
            LOG.assertTrue(CompletionServiceImpl.getCompletionService().getCurrentCompletion() == null);
            CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
            return true;
        }
        return false;
    }

    public boolean fillInCommonPrefix(final boolean explicit) {
        if (isInsideIdentifier()) {
            return false;
        }

        final Boolean aBoolean = new WriteCommandAction<Boolean>(getProject()) {
            @Override
            protected void run( Result<Boolean> result) throws Throwable {
                if (!explicit) {
                    setMergeCommand();
                }
                try {
                    result.setResult(myLookup.fillInCommonPrefix(explicit));
                }
                catch (Exception e) {
                    LOG.error(e);
                }
            }
        }.execute().getResultObject();
        return aBoolean.booleanValue();
    }

    public void restorePrefix( final Runnable customRestore) {
        new WriteCommandAction(getProject()) {
            @Override
            protected void run( Result result) throws Throwable {
                setMergeCommand();

                customRestore.run();
            }
        }.execute();
    }

    public int nextInvocationCount(int invocation, boolean reused) {
        return reused ? Math.max(getParameters().getInvocationCount() + 1, 2) : invocation;
    }

    public Editor getEditor() {
        return myEditor;
    }

    
    public Caret getCaret() {
        return myCaret;
    }

    public boolean isRepeatedInvocation(CompletionType completionType, Editor editor) {
        if (completionType != myParameters.getCompletionType() || editor != myEditor) {
            return false;
        }

        if (isAutopopupCompletion() && !myLookup.mayBeNoticed()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isAutopopupCompletion() {
        return myParameters.getInvocationCount() == 0;
    }

    
    public Project getProject() {
        return ObjectUtils.assertNotNull(myEditor.getProject());
    }

    public void addWatchedPrefix(int startOffset, ElementPattern<String> restartCondition) {
        myRestartingPrefixConditions.add(Pair.create(startOffset, restartCondition));
    }

    public void prefixUpdated() {
        final int caretOffset = myEditor.getCaretModel().getOffset();
        if (caretOffset < myStartCaret) {
            scheduleRestart();
            myRestartingPrefixConditions.clear();
            return;
        }

        final CharSequence text = myEditor.getDocument().getCharsSequence();
        for (Pair<Integer, ElementPattern<String>> pair : myRestartingPrefixConditions) {
            int start = pair.first;
            if (caretOffset >= start && start >= 0) {
                final String newPrefix = text.subSequence(start, caretOffset).toString();
                if (pair.second.accepts(newPrefix)) {
                    scheduleRestart();
                    myRestartingPrefixConditions.clear();
                    return;
                }
            }
        }

        hideAutopopupIfMeaningless();
    }

    public void scheduleRestart() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        cancel();

        final CompletionProgressIndicator current = CompletionServiceImpl.getCompletionService().getCurrentCompletion();
        if (this != current) {
            LOG.error(current + "!=" + this);
        }

        hideAutopopupIfMeaningless();

        CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
        if (oldPhase instanceof CompletionPhase.CommittingDocuments) {
            ((CompletionPhase.CommittingDocuments)oldPhase).replaced = true;
        }

        final CompletionPhase.CommittingDocuments phase = new CompletionPhase.CommittingDocuments(this, myEditor);
        CompletionServiceImpl.setCompletionPhase(phase);
        phase.ignoreCurrentDocumentChange();

        final Project project = getProject();
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                CompletionAutoPopupHandler.runLaterWithCommitted(project, myEditor.getDocument(), new Runnable() {
                    @Override
                    public void run() {
                        if (phase.checkExpired()) return;

                        CompletionAutoPopupHandler.invokeCompletion(myParameters.getCompletionType(),
                                isAutopopupCompletion(), project, myEditor, myParameters.getInvocationCount(),
                                true);
                    }
                });
            }
        }, project.getDisposed());
    }

    @Override
    public String toString() {
        return "CompletionProgressIndicator[count=" +
                myCount +
                ",phase=" +
                CompletionServiceImpl.getCompletionPhase() +
                "]@" +
                System.identityHashCode(this);
    }

    protected void handleEmptyLookup(final boolean awaitSecondInvocation) {
        if (isAutopopupCompletion() && ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        LOG.assertTrue(!isAutopopupCompletion());

        if (ApplicationManager.getApplication().isUnitTestMode() || !myHandler.invokedExplicitly) {
            CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
            return;
        }

        for (final CompletionContributor contributor : CompletionContributor.forParameters(getParameters())) {
            final String text = contributor.handleEmptyLookup(getParameters(), getEditor());
            if (StringUtil.isNotEmpty(text)) {
                LightweightHint hint = showErrorHint(getProject(), getEditor(), text);
                CompletionServiceImpl.setCompletionPhase(
                        awaitSecondInvocation ? new CompletionPhase.NoSuggestionsHint(hint, this) : CompletionPhase.NoCompletion);
                return;
            }
        }
        CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
    }

    private static LightweightHint showErrorHint(Project project, Editor editor, String text) {
        final LightweightHint[] result = {null};
        final EditorHintListener listener = new EditorHintListener() {
            @Override
            public void hintShown(final Project project, final LightweightHint hint, final int flags) {
                result[0] = hint;
            }
        };
        final MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(EditorHintListener.TOPIC, listener);
        assert text != null;
        HintManager.getInstance().showErrorHint(editor, text, HintManager.UNDER);
        connection.disconnect();
        return result[0];
    }

    private static boolean shouldPreselectFirstSuggestion(CompletionParameters parameters) {
        if (!Registry.is("ide.completion.autopopup.choose.by.enter")) {
            return false;
        }

        if (Registry.is("ide.completion.lookup.element.preselect.depends.on.context")) {
            for (CompletionPreselectionBehaviourProvider provider : Extensions.getExtensions(CompletionPreselectionBehaviourProvider.EP_NAME)) {
                if (!provider.shouldPreselectFirstSuggestion(parameters)) {
                    return false;
                }
            }
        }

        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            return true;
        }

        switch (CodeInsightSettings.getInstance().AUTOPOPUP_FOCUS_POLICY) {
            case CodeInsightSettings.ALWAYS:
                return true;
            case CodeInsightSettings.NEVER:
                return false;
        }

        final Language language = PsiUtilCore.getLanguageAtOffset(parameters.getPosition().getContainingFile(), parameters.getOffset());
        for (CompletionConfidence confidence : CompletionConfidenceEP.forLanguage(language)) {
            //noinspection deprecation
            final ThreeState result = confidence.shouldFocusLookup(parameters);
            if (result != ThreeState.UNSURE) {
                LOG.debug(confidence + " has returned shouldFocusLookup=" + result);
                return result == ThreeState.YES;
            }
        }
        return false;
    }

    void startCompletion(final CompletionInitializationContext initContext) {
        boolean sync = ApplicationManager.getApplication().isUnitTestMode() && !CompletionAutoPopupHandler.ourTestingAutopopup;
        final CompletionThreading strategy = sync ? new SyncCompletion() : new AsyncCompletion();

        strategy.startThread(ProgressWrapper.wrap(this), new Runnable() {
            @Override
            public void run() {
                scheduleAdvertising();
            }
        });
        final WeighingDelegate weigher = strategy.delegateWeighing(this);

        class CalculateItems implements Runnable {
            @Override
            public void run() {
                try {
                    calculateItems(initContext, weigher);
                }
                catch (ProcessCanceledException ignore) {
                    cancel(); // some contributor may just throw PCE; if indicator is not canceled everything will hang
                }
                catch (Throwable t) {
                    cancel();
                    LOG.error(t);
                }
            }
        }
        strategy.startThread(this, new CalculateItems());
    }

    private LookupElement[] calculateItems(CompletionInitializationContext initContext, WeighingDelegate weigher) {
        duringCompletion(initContext);
        ProgressManager.checkCanceled();

        LookupElement[] result = CompletionService.getCompletionService().performCompletion(myParameters, weigher);
        ProgressManager.checkCanceled();

        weigher.waitFor();
        ProgressManager.checkCanceled();

        return result;
    }

    public void addAdvertisement( final String text,  final Color bgColor) {
        myAdvertiserChanges.offer(new Runnable() {
            @Override
            public void run() {
                myLookup.addAdvertisement(text, bgColor);
            }
        });

        myQueue.queue(myUpdate);
    }

    private static class ModifierTracker extends KeyAdapter {
        private final JComponent myContentComponent;

        public ModifierTracker(JComponent contentComponent) {
            myContentComponent = contentComponent;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            processModifier(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            processModifier(e);
        }

        private void processModifier(KeyEvent e) {
            final int code = e.getKeyCode();
            if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_META || code == KeyEvent.VK_ALT || code == KeyEvent.VK_SHIFT) {
                myContentComponent.removeKeyListener(this);
                final CompletionPhase phase = CompletionServiceImpl.getCompletionPhase();
                if (phase instanceof CompletionPhase.BgCalculation) {
                    ((CompletionPhase.BgCalculation)phase).modifiersChanged = true;
                }
                else if (phase instanceof CompletionPhase.InsertedSingleItem) {
                    CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
                }
            }
        }
    }
}
