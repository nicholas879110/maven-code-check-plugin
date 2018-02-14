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

package com.gome.maven.find.findUsages;

import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.codeInsight.hint.HintManagerImpl;
import com.gome.maven.codeInsight.hint.HintUtil;
import com.gome.maven.find.FindBundle;
import com.gome.maven.find.FindSettings;
import com.gome.maven.lang.findUsages.LanguageFindUsages;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorLocation;
import com.gome.maven.openapi.fileEditor.TextEditor;
import com.gome.maven.openapi.keymap.KeymapUtil;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.util.ProgressIndicatorBase;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Factory;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.openapi.wm.ex.ProgressIndicatorEx;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.*;
import com.gome.maven.ui.LightweightHint;
import com.gome.maven.ui.content.Content;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usageView.UsageViewManager;
import com.gome.maven.usageView.UsageViewUtil;
import com.gome.maven.usages.*;
import com.gome.maven.util.CommonProcessors;
import com.gome.maven.util.Function;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.pico.ConstructorInjectionComponentAdapter;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * see {@link com.gome.maven.find.impl.FindManagerImpl#getFindUsagesManager()}
 */
public class FindUsagesManager {
    private static final Logger LOG = Logger.getInstance("#com.intellij.find.findParameterUsages.FindUsagesManager");

    private enum FileSearchScope {
        FROM_START,
        FROM_END,
        AFTER_CARET,
        BEFORE_CARET
    }

    private static final Key<String> KEY_START_USAGE_AGAIN = Key.create("KEY_START_USAGE_AGAIN");
     private static final String VALUE_START_USAGE_AGAIN = "START_AGAIN";
    private final Project myProject;
    private final com.gome.maven.usages.UsageViewManager myAnotherManager;

    private PsiElement2UsageTargetComposite myLastSearchInFileData; // EDT only
    private final UsageHistory myHistory = new UsageHistory();

    public FindUsagesManager( Project project,  com.gome.maven.usages.UsageViewManager anotherManager) {
        myProject = project;
        myAnotherManager = anotherManager;
    }

    public boolean canFindUsages( final PsiElement element) {
        for (FindUsagesHandlerFactory factory : Extensions.getExtensions(FindUsagesHandlerFactory.EP_NAME, myProject)) {
            try {
                if (factory.canFindUsages(element)) {
                    return true;
                }
            }
            catch (IndexNotReadyException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
        return false;
    }

    public void clearFindingNextUsageInFile() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        myLastSearchInFileData = null;
    }

    public boolean findNextUsageInFile( FileEditor editor) {
        return findUsageInFile(editor, FileSearchScope.AFTER_CARET);
    }

    public boolean findPreviousUsageInFile( FileEditor editor) {
        return findUsageInFile(editor, FileSearchScope.BEFORE_CARET);
    }

    private boolean findUsageInFile( FileEditor editor,  FileSearchScope direction) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (myLastSearchInFileData == null) return false;
        PsiElement[] primaryElements = myLastSearchInFileData.getPrimaryElements();
        PsiElement[] secondaryElements = myLastSearchInFileData.getSecondaryElements();
        if (primaryElements.length == 0) {//all elements have been invalidated
            Messages.showMessageDialog(myProject, FindBundle.message("find.searched.elements.have.been.changed.error"),
                    FindBundle.message("cannot.search.for.usages.title"), Messages.getInformationIcon());
            // SCR #10022
            //clearFindingNextUsageInFile();
            return false;
        }

        //todo
        TextEditor textEditor = (TextEditor)editor;
        Document document = textEditor.getEditor().getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
        if (psiFile == null) return false;

        final FindUsagesHandler handler = getFindUsagesHandler(primaryElements[0], false);
        if (handler == null) return false;
        findUsagesInEditor(primaryElements, secondaryElements, handler, psiFile, direction, myLastSearchInFileData.myOptions, textEditor);
        return true;
    }


    private void initLastSearchElement( FindUsagesOptions findUsagesOptions,
                                        PsiElement[] primaryElements,
                                        PsiElement[] secondaryElements) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        myLastSearchInFileData = new PsiElement2UsageTargetComposite(primaryElements, secondaryElements, findUsagesOptions);
    }

    
    public FindUsagesHandler getFindUsagesHandler( PsiElement element, final boolean forHighlightUsages) {
        for (FindUsagesHandlerFactory factory : Extensions.getExtensions(FindUsagesHandlerFactory.EP_NAME, myProject)) {
            if (factory.canFindUsages(element)) {
                final FindUsagesHandler handler = factory.createFindUsagesHandler(element, forHighlightUsages);
                if (handler == FindUsagesHandler.NULL_HANDLER) return null;
                if (handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }

    
    public FindUsagesHandler getNewFindUsagesHandler( PsiElement element, final boolean forHighlightUsages) {
        for (FindUsagesHandlerFactory factory : Extensions.getExtensions(FindUsagesHandlerFactory.EP_NAME, myProject)) {
            if (factory.canFindUsages(element)) {
                Class<? extends FindUsagesHandlerFactory> aClass = factory.getClass();
                FindUsagesHandlerFactory copy = (FindUsagesHandlerFactory)new ConstructorInjectionComponentAdapter(aClass.getName(), aClass)
                        .getComponentInstance(myProject.getPicoContainer());
                final FindUsagesHandler handler = copy.createFindUsagesHandler(element, forHighlightUsages);
                if (handler == FindUsagesHandler.NULL_HANDLER) return null;
                if (handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }

    public void findUsages( PsiElement psiElement, final PsiFile scopeFile, final FileEditor editor, boolean showDialog,  SearchScope searchScope) {
        FindUsagesHandler handler = getFindUsagesHandler(psiElement, false);
        if (handler == null) return;

        boolean singleFile = scopeFile != null;
        AbstractFindUsagesDialog dialog = handler.getFindUsagesDialog(singleFile, shouldOpenInNewTab(), mustOpenInNewTab());
        if (showDialog) {
            if (!dialog.showAndGet()) {
                return;
            }
        }
        else {
            dialog.close(DialogWrapper.OK_EXIT_CODE);
        }

        setOpenInNewTab(dialog.isShowInSeparateWindow());

        FindUsagesOptions findUsagesOptions = dialog.calcFindUsagesOptions();
        if (searchScope != null)  {
            findUsagesOptions.searchScope = searchScope;
        }

        clearFindingNextUsageInFile();

        startFindUsages(findUsagesOptions, handler, scopeFile, editor);
    }

    void startFindUsages( PsiElement psiElement,
                          FindUsagesOptions findUsagesOptions,
                         PsiFile scopeFile,
                         FileEditor editor) {
        FindUsagesHandler handler = getFindUsagesHandler(psiElement, false);
        if (handler == null) return;
        startFindUsages(findUsagesOptions, handler, scopeFile, editor);
    }

    private void startFindUsages( FindUsagesOptions findUsagesOptions,
                                  FindUsagesHandler handler,
                                 PsiFile scopeFile,
                                 FileEditor editor) {
        boolean singleFile = scopeFile != null;

        clearFindingNextUsageInFile();
        LOG.assertTrue(handler.getPsiElement().isValid());
        PsiElement[] primaryElements = handler.getPrimaryElements();
        checkNotNull(primaryElements, handler, "getPrimaryElements()");
        PsiElement[] secondaryElements = handler.getSecondaryElements();
        checkNotNull(secondaryElements, handler, "getSecondaryElements()");
        if (singleFile) {
            editor.putUserData(KEY_START_USAGE_AGAIN, null);
            findUsagesInEditor(primaryElements, secondaryElements, handler, scopeFile, FileSearchScope.FROM_START, findUsagesOptions.clone(), editor);
        }
        else {
            boolean skipResultsWithOneUsage = FindSettings.getInstance().isSkipResultsWithOneUsage();
            findUsages(primaryElements, secondaryElements, handler, findUsagesOptions, skipResultsWithOneUsage);
        }
    }

    public static void showSettingsAndFindUsages( NavigationItem[] targets) {
        if (targets.length == 0) return;
        NavigationItem target = targets[0];
        if (!(target instanceof ConfigurableUsageTarget)) return;
        ((ConfigurableUsageTarget)target).showSettings();
    }

    private static void checkNotNull( PsiElement[] elements,
                                      FindUsagesHandler handler,
                                       String methodName) {
        for (PsiElement element : elements) {
            if (element == null) {
                LOG.error(handler + "." + methodName + " has returned array with null elements: " + Arrays.asList(elements));
            }
        }
    }


    
    public static ProgressIndicator startProcessUsages( final FindUsagesHandler handler,
                                                        final PsiElement[] primaryElements,
                                                        final PsiElement[] secondaryElements,
                                                        final Processor<Usage> processor,
                                                        final FindUsagesOptions findUsagesOptions,
                                                        final Runnable onComplete) {
        final ProgressIndicatorBase indicator = new ProgressIndicatorBase();
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ProgressManager.getInstance().runProcess(new Runnable() {
                        @Override
                        public void run() {
                            final UsageSearcher usageSearcher = createUsageSearcher(primaryElements, secondaryElements, handler, findUsagesOptions, null);
                            usageSearcher.generate(processor);
                        }
                    }, indicator);
                }
                finally {
                    onComplete.run();
                }
            }
        });

        return indicator;
    }

    
    public UsageViewPresentation createPresentation( FindUsagesHandler handler,  FindUsagesOptions findUsagesOptions) {
        PsiElement element = handler.getPsiElement();
        LOG.assertTrue(element.isValid());
        return createPresentation(element, findUsagesOptions, FindSettings.getInstance().isShowResultsInSeparateView());
    }

    private void setOpenInNewTab(final boolean toOpenInNewTab) {
        if (!mustOpenInNewTab()) {
            FindSettings.getInstance().setShowResultsInSeparateView(toOpenInNewTab);
        }
    }

    private boolean shouldOpenInNewTab() {
        return mustOpenInNewTab() || FindSettings.getInstance().isShowResultsInSeparateView();
    }

    private boolean mustOpenInNewTab() {
        Content selectedContent = UsageViewManager.getInstance(myProject).getSelectedContent(true);
        return selectedContent != null && selectedContent.isPinned();
    }


    
    private static UsageSearcher createUsageSearcher( final PsiElement[] primaryElements,
                                                      final PsiElement[] secondaryElements,
                                                      final FindUsagesHandler handler,
                                                      FindUsagesOptions options,
                                                     final PsiFile scopeFile) {
        final FindUsagesOptions optionsClone = options.clone();
        return new UsageSearcher() {
            @Override
            public void generate( final Processor<Usage> processor) {
                Project project = ApplicationManager.getApplication().runReadAction(new Computable<Project>() {
                    @Override
                    public Project compute() {
                        return scopeFile != null ? scopeFile.getProject() : primaryElements[0].getProject();
                    }
                });
                dropResolveCacheRegularly(ProgressManager.getInstance().getProgressIndicator(), project);

                if (scopeFile != null) {
                    optionsClone.searchScope = new LocalSearchScope(scopeFile);
                }
                final Processor<UsageInfo> usageInfoProcessor = new CommonProcessors.UniqueProcessor<UsageInfo>(new Processor<UsageInfo>() {
                    @Override
                    public boolean process(final UsageInfo usageInfo) {
                        Usage usage = ApplicationManager.getApplication().runReadAction(new Computable<Usage>() {
                            @Override
                            public Usage compute() {
                                return UsageInfoToUsageConverter.convert(primaryElements, usageInfo);
                            }
                        });
                        return processor.process(usage);
                    }
                });
                final Iterable<PsiElement> elements = ContainerUtil.concat(primaryElements, secondaryElements);

                optionsClone.fastTrack = new SearchRequestCollector(new SearchSession());
                if (optionsClone.searchScope instanceof GlobalSearchScope) {
                    // we will search in project scope always but warn if some usage is out of scope
                    optionsClone.searchScope = optionsClone.searchScope.union(GlobalSearchScope.projectScope(project));
                }
                try {
                    for (final PsiElement element : elements) {
                        ApplicationManager.getApplication().runReadAction(new Runnable() {
                            @Override
                            public void run() {
                                LOG.assertTrue(element.isValid());
                            }
                        });
                        handler.processElementUsages(element, usageInfoProcessor, optionsClone);
                        for (CustomUsageSearcher searcher : Extensions.getExtensions(CustomUsageSearcher.EP_NAME)) {
                            try {
                                searcher.processElementUsages(element, processor, optionsClone);
                            }
                            catch (IndexNotReadyException e) {
                                DumbService.getInstance(element.getProject()).showDumbModeNotification("Find usages is not available during indexing");
                            }
                            catch (ProcessCanceledException e) {
                                throw e;
                            }
                            catch (Exception e) {
                                LOG.error(e);
                            }
                        }
                    }

                    PsiSearchHelper.SERVICE.getInstance(project)
                            .processRequests(optionsClone.fastTrack, new Processor<PsiReference>() {
                                @Override
                                public boolean process(final PsiReference ref) {
                                    UsageInfo info = ApplicationManager.getApplication().runReadAction(new Computable<UsageInfo>() {
                                        @Override
                                        public UsageInfo compute() {
                                            if (!ref.getElement().isValid()) return null;
                                            return new UsageInfo(ref);
                                        }
                                    });
                                    return info == null || usageInfoProcessor.process(info);
                                }
                            });
                }
                finally {
                    optionsClone.fastTrack = null;
                }
            }
        };
    }

    
    private static PsiElement2UsageTargetAdapter[] convertToUsageTargets( Iterable<PsiElement> elementsToSearch,
                                                                          final FindUsagesOptions findUsagesOptions) {
        final List<PsiElement2UsageTargetAdapter> targets = ContainerUtil.map(elementsToSearch,
                new Function<PsiElement, PsiElement2UsageTargetAdapter>() {
                    @Override
                    public PsiElement2UsageTargetAdapter fun(PsiElement element) {
                        return convertToUsageTarget(element, findUsagesOptions);
                    }
                });
        return targets.toArray(new PsiElement2UsageTargetAdapter[targets.size()]);
    }

    public void findUsages( final PsiElement[] primaryElements,
                            final PsiElement[] secondaryElements,
                            final FindUsagesHandler handler,
                            final FindUsagesOptions findUsagesOptions,
                           final boolean toSkipUsagePanelWhenOneUsage) {
        if (primaryElements.length == 0) {
            throw new AssertionError(handler + " " + findUsagesOptions);
        }
        Iterable<PsiElement> allElements = ContainerUtil.concat(primaryElements, secondaryElements);
        final PsiElement2UsageTargetAdapter[] targets = convertToUsageTargets(allElements, findUsagesOptions);
        myAnotherManager.searchAndShowUsages(targets, new Factory<UsageSearcher>() {
            @Override
            public UsageSearcher create() {
                return createUsageSearcher(primaryElements, secondaryElements, handler, findUsagesOptions, null);
            }
        }, !toSkipUsagePanelWhenOneUsage, true, createPresentation(primaryElements[0], findUsagesOptions, shouldOpenInNewTab()), null);
        myHistory.add(targets[0]);
    }

    private static void dropResolveCacheRegularly(ProgressIndicator indicator,  final Project project) {
        if (indicator instanceof ProgressIndicatorEx) {
            ((ProgressIndicatorEx)indicator).addStateDelegate(new ProgressIndicatorBase() {
                volatile long lastCleared = System.currentTimeMillis();

                @Override
                public void setFraction(double fraction) {
                    super.setFraction(fraction);
                    long current = System.currentTimeMillis();
                    if (current - lastCleared >= 500) {
                        lastCleared = current;
                        // fraction is changed when each file is processed =>
                        // resolve caches used when searching in that file are likely to be not needed anymore
                        PsiManager.getInstance(project).dropResolveCaches();
                    }
                }
            });
        }
    }

    
    private static UsageViewPresentation createPresentation( PsiElement psiElement,
                                                             FindUsagesOptions options,
                                                            boolean toOpenInNewTab) {
        UsageViewPresentation presentation = new UsageViewPresentation();
        String scopeString = options.searchScope.getDisplayName();
        presentation.setScopeText(scopeString);
        String usagesString = generateUsagesString(options);
        presentation.setUsagesString(usagesString);
        String title = FindBundle.message("find.usages.of.element.in.scope.panel.title", usagesString, UsageViewUtil.getLongName(psiElement),
                scopeString);
        presentation.setTabText(title);
        presentation.setTabName(FindBundle.message("find.usages.of.element.tab.name", usagesString, UsageViewUtil.getShortName(psiElement)));
        presentation.setTargetsNodeText(StringUtil.capitalize(UsageViewUtil.getType(psiElement)));
        presentation.setOpenInNewTab(toOpenInNewTab);
        return presentation;
    }

    private void findUsagesInEditor( final PsiElement[] primaryElements,
                                     final PsiElement[] secondaryElements,
                                     FindUsagesHandler handler,
                                     PsiFile scopeFile,
                                     FileSearchScope direction,
                                     final FindUsagesOptions findUsagesOptions,
                                     FileEditor fileEditor) {
        initLastSearchElement(findUsagesOptions, primaryElements, secondaryElements);

        clearStatusBar();

        final FileEditorLocation currentLocation = fileEditor.getCurrentLocation();

        final UsageSearcher usageSearcher = createUsageSearcher(primaryElements, secondaryElements, handler, findUsagesOptions, scopeFile);
        AtomicBoolean usagesWereFound = new AtomicBoolean();

        Usage fUsage = findSiblingUsage(usageSearcher, direction, currentLocation, usagesWereFound, fileEditor);

        if (fUsage != null) {
            fUsage.navigate(true);
            fUsage.selectInEditor();
        }
        else if (!usagesWereFound.get()) {
            String message = getNoUsagesFoundMessage(primaryElements[0]) + " in " + scopeFile.getName();
            showHintOrStatusBarMessage(message, fileEditor);
        }
        else {
            fileEditor.putUserData(KEY_START_USAGE_AGAIN, VALUE_START_USAGE_AGAIN);
            showHintOrStatusBarMessage(getSearchAgainMessage(primaryElements[0], direction), fileEditor);
        }
    }

    private static String getNoUsagesFoundMessage(PsiElement psiElement) {
        String elementType = UsageViewUtil.getType(psiElement);
        String elementName = UsageViewUtil.getShortName(psiElement);
        return FindBundle.message("find.usages.of.element_type.element_name.not.found.message", elementType, elementName);
    }

    private void clearStatusBar() {
        StatusBar.Info.set("", myProject);
    }

    private static String getSearchAgainMessage(PsiElement element, final FileSearchScope direction) {
        String message = getNoUsagesFoundMessage(element);
        if (direction == FileSearchScope.AFTER_CARET) {
            AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_NEXT);
            String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(action);
            if (shortcutsText.isEmpty()) {
                message = FindBundle.message("find.search.again.from.top.action.message", message);
            }
            else {
                message = FindBundle.message("find.search.again.from.top.hotkey.message", message, shortcutsText);
            }
        }
        else {
            String shortcutsText =
                    KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_PREVIOUS));
            if (shortcutsText.isEmpty()) {
                message = FindBundle.message("find.search.again.from.bottom.action.message", message);
            }
            else {
                message = FindBundle.message("find.search.again.from.bottom.hotkey.message", message, shortcutsText);
            }
        }
        return message;
    }

    private void showHintOrStatusBarMessage(String message, FileEditor fileEditor) {
        if (fileEditor instanceof TextEditor) {
            TextEditor textEditor = (TextEditor)fileEditor;
            showEditorHint(message, textEditor.getEditor());
        }
        else {
            StatusBar.Info.set(message, myProject);
        }
    }

    private static Usage findSiblingUsage( final UsageSearcher usageSearcher,
                                           FileSearchScope dir,
                                          final FileEditorLocation currentLocation,
                                           final AtomicBoolean usagesWereFound,
                                           FileEditor fileEditor) {
        if (fileEditor.getUserData(KEY_START_USAGE_AGAIN) != null) {
            dir = dir == FileSearchScope.AFTER_CARET ? FileSearchScope.FROM_START : FileSearchScope.FROM_END;
        }

        final FileSearchScope direction = dir;

        final AtomicReference<Usage> foundUsage = new AtomicReference<Usage>();
        usageSearcher.generate(new Processor<Usage>() {
            @Override
            public boolean process(Usage usage) {
                usagesWereFound.set(true);
                if (direction == FileSearchScope.FROM_START) {
                    foundUsage.compareAndSet(null, usage);
                    return false;
                }
                if (direction == FileSearchScope.FROM_END) {
                    foundUsage.set(usage);
                }
                else if (direction == FileSearchScope.AFTER_CARET) {
                    if (Comparing.compare(usage.getLocation(), currentLocation) > 0) {
                        foundUsage.set(usage);
                        return false;
                    }
                }
                else if (direction == FileSearchScope.BEFORE_CARET) {
                    if (Comparing.compare(usage.getLocation(), currentLocation) >= 0) {
                        return false;
                    }
                    while (true) {
                        Usage found = foundUsage.get();
                        if (found == null) {
                            if (foundUsage.compareAndSet(null, usage)) break;
                        }
                        else {
                            if (Comparing.compare(found.getLocation(), usage.getLocation()) < 0 && foundUsage.compareAndSet(found, usage)) break;
                        }
                    }
                }

                return true;
            }
        });

        fileEditor.putUserData(KEY_START_USAGE_AGAIN, null);

        return foundUsage.get();
    }

    private static PsiElement2UsageTargetAdapter convertToUsageTarget( PsiElement elementToSearch,
                                                                       FindUsagesOptions findUsagesOptions) {
        if (elementToSearch instanceof NavigationItem) {
            return new PsiElement2UsageTargetAdapter(elementToSearch,findUsagesOptions);
        }
        throw new IllegalArgumentException("Wrong usage target:" + elementToSearch + "; " + elementToSearch.getClass());
    }

    
    private static String generateUsagesString( FindUsagesOptions selectedOptions) {
        return selectedOptions.generateUsagesString();
    }

    private static void showEditorHint(String message, final Editor editor) {
        JComponent component = HintUtil.createInformationLabel(message);
        final LightweightHint hint = new LightweightHint(component);
        HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, HintManager.UNDER,
                HintManager.HIDE_BY_ANY_KEY |
                        HintManager.HIDE_BY_TEXT_CHANGE |
                        HintManager.HIDE_BY_SCROLLING, 0, false);
    }

    public static String getHelpID(PsiElement element) {
        return LanguageFindUsages.INSTANCE.forLanguage(element.getLanguage()).getHelpId(element);
    }

    public void rerunAndRecallFromHistory( ConfigurableUsageTarget usageTarget) {
        usageTarget.findUsages();
        addToHistory(usageTarget);
    }

    public void addToHistory( ConfigurableUsageTarget usageTarget) {
        myHistory.add(usageTarget);
    }

    
    public UsageHistory getHistory() {
        return myHistory;
    }


    
    public static GlobalSearchScope getMaximalScope( FindUsagesHandler handler) {
        PsiElement element = handler.getPsiElement();
        Project project = element.getProject();
        PsiFile file = element.getContainingFile();
        if (file != null && ProjectFileIndex.SERVICE.getInstance(project).isInContent(file.getViewProvider().getVirtualFile())) {
            return GlobalSearchScope.projectScope(project);
        }
        return GlobalSearchScope.allScope(project);
    }
}
