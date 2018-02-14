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

package com.gome.maven.find.replaceInProject;

import com.gome.maven.find.*;
import com.gome.maven.find.actions.FindInPathAction;
import com.gome.maven.find.findInProject.FindInProjectManager;
import com.gome.maven.find.impl.FindInProjectUtil;
import com.gome.maven.find.impl.FindManagerImpl;
import com.gome.maven.ide.DataManager;
import com.gome.maven.notification.NotificationGroup;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.KeyboardShortcut;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Factory;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.ReadonlyStatusHandler;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.ui.content.Content;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usages.*;
import com.gome.maven.usages.impl.UsageViewImpl;
import com.gome.maven.usages.rules.UsageInFile;
import com.gome.maven.util.AdapterProcessor;
import com.gome.maven.util.Processor;

import javax.swing.*;
import java.util.*;

public class ReplaceInProjectManager {
    static final NotificationGroup NOTIFICATION_GROUP = FindInPathAction.NOTIFICATION_GROUP;

    private final Project myProject;
    private boolean myIsFindInProgress = false;

    public static ReplaceInProjectManager getInstance(Project project) {
        return ServiceManager.getService(project, ReplaceInProjectManager.class);
    }

    public ReplaceInProjectManager(Project project) {
        myProject = project;
    }

    public static boolean hasReadOnlyUsages(final Collection<Usage> usages) {
        for (Usage usage : usages) {
            if (usage.isReadOnly()) return true;
        }

        return false;
    }

    static class ReplaceContext {
        private final UsageView usageView;
        private final FindModel findModel;
        private Set<Usage> excludedSet;

        ReplaceContext( UsageView usageView,  FindModel findModel) {
            this.usageView = usageView;
            this.findModel = findModel;
        }

        
        public FindModel getFindModel() {
            return findModel;
        }

        
        public UsageView getUsageView() {
            return usageView;
        }

        
        public Set<Usage> getExcludedSetCached() {
            if (excludedSet == null) excludedSet = usageView.getExcludedUsages();
            return excludedSet;
        }

        public void invalidateExcludedSetCache() {
            excludedSet = null;
        }
    }

    public void replaceInProject( DataContext dataContext) {
        final boolean isOpenInNewTabEnabled;
        final boolean toOpenInNewTab;
        final Content selectedContent = com.gome.maven.usageView.UsageViewManager.getInstance(myProject).getSelectedContent(true);
        if (selectedContent != null && selectedContent.isPinned()) {
            toOpenInNewTab = true;
            isOpenInNewTabEnabled = false;
        }
        else {
            toOpenInNewTab = FindSettings.getInstance().isShowResultsInSeparateView();
            isOpenInNewTabEnabled = com.gome.maven.usageView.UsageViewManager.getInstance(myProject).getReusableContentsCount() > 0;
        }
        final FindManager findManager = FindManager.getInstance(myProject);
        final FindModel findModel = findManager.getFindInProjectModel().clone();
        findModel.setReplaceState(true);
        findModel.setOpenInNewTabVisible(true);
        findModel.setOpenInNewTabEnabled(isOpenInNewTabEnabled);
        findModel.setOpenInNewTab(toOpenInNewTab);
        FindInProjectUtil.setDirectoryName(findModel, dataContext);

        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        FindUtil.initStringToFindWithSelection(findModel, editor);

        findManager.showFindDialog(findModel, new Runnable() {
            @Override
            public void run() {
                final PsiDirectory psiDirectory = FindInProjectUtil.getPsiDirectory(findModel, myProject);
                if (!findModel.isProjectScope() &&
                        psiDirectory == null &&
                        findModel.getModuleName() == null &&
                        findModel.getCustomScope() == null) {
                    return;
                }

                UsageViewManager manager = UsageViewManager.getInstance(myProject);

                if (manager == null) return;
                findManager.getFindInProjectModel().copyFrom(findModel);
                final FindModel findModelCopy = findModel.clone();

                final UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(findModel.isOpenInNewTab(), findModelCopy);
                final FindUsagesProcessPresentation processPresentation = FindInProjectUtil.setupProcessPresentation(myProject, true, presentation);

                UsageSearcherFactory factory = new UsageSearcherFactory(findModelCopy, psiDirectory, processPresentation);
                searchAndShowUsages(manager, factory, findModelCopy, presentation, processPresentation, findManager);
            }
        });
    }

    public void searchAndShowUsages( UsageViewManager manager,
                                     Factory<UsageSearcher> usageSearcherFactory,
                                     FindModel findModelCopy,
                                     FindManager findManager) {
        final UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(true, findModelCopy);
        final FindUsagesProcessPresentation processPresentation = FindInProjectUtil.setupProcessPresentation(myProject, true, presentation);

        searchAndShowUsages(manager, usageSearcherFactory, findModelCopy, presentation, processPresentation, findManager);
    }

    private static class ReplaceInProjectTarget extends FindInProjectUtil.StringUsageTarget {
        public ReplaceInProjectTarget( Project project,  FindModel findModel) {
            super(project, findModel);
        }

        
        @Override
        public String getLongDescriptiveName() {
            UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(false, myFindModel);
            return "Replace "+ StringUtil.decapitalize(presentation.getToolwindowTitle())+" with '"+ myFindModel.getStringToReplace()+"'";
        }

        @Override
        public KeyboardShortcut getShortcut() {
            return ActionManager.getInstance().getKeyboardShortcut("ReplaceInPath");
        }

        @Override
        public void showSettings() {
            Content selectedContent = com.gome.maven.usageView.UsageViewManager.getInstance(myProject).getSelectedContent(true);
            JComponent component = selectedContent == null ? null : selectedContent.getComponent();
            ReplaceInProjectManager findInProjectManager = getInstance(myProject);
            findInProjectManager.replaceInProject(DataManager.getInstance().getDataContext(component));
        }
    }

    public void searchAndShowUsages( UsageViewManager manager,
                                     Factory<UsageSearcher> usageSearcherFactory,
                                     final FindModel findModelCopy,
                                     UsageViewPresentation presentation,
                                     FindUsagesProcessPresentation processPresentation,
                                    final FindManager findManager) {
        presentation.setMergeDupLinesAvailable(false);
        final ReplaceContext[] context = new ReplaceContext[1];
        final ReplaceInProjectTarget target = new ReplaceInProjectTarget(myProject, findModelCopy);
        ((FindManagerImpl)FindManager.getInstance(myProject)).getFindUsagesManager().addToHistory(target);
        manager.searchAndShowUsages(new UsageTarget[]{target},
                usageSearcherFactory, processPresentation, presentation, new UsageViewManager.UsageViewStateListener() {
                    @Override
                    public void usageViewCreated( UsageView usageView) {
                        context[0] = new ReplaceContext(usageView, findModelCopy);
                        addReplaceActions(context[0]);
                    }

                    @Override
                    public void findingUsagesFinished(final UsageView usageView) {
                        if (context[0] != null && findManager.getFindInProjectModel().isPromptOnReplace()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    replaceWithPrompt(context[0]);
                                    context[0].invalidateExcludedSetCache();
                                }
                            });
                        }
                    }
                });
    }

    private void replaceWithPrompt(final ReplaceContext replaceContext) {
        final List<Usage> _usages = replaceContext.getUsageView().getSortedUsages();

        if (hasReadOnlyUsages(_usages)) {
            WindowManager.getInstance().getStatusBar(myProject)
                    .setInfo(FindBundle.message("find.replace.occurrences.found.in.read.only.files.status"));
            return;
        }

        final Usage[] usages = _usages.toArray(new Usage[_usages.size()]);

        //usageView.expandAll();
        for (int i = 0; i < usages.length; ++i) {
            final Usage usage = usages[i];
            final UsageInfo usageInfo = ((UsageInfo2UsageAdapter)usage).getUsageInfo();

            final PsiElement elt = usageInfo.getElement();
            if (elt == null) continue;
            final PsiFile psiFile = elt.getContainingFile();
            if (!psiFile.isWritable()) continue;

            final VirtualFile virtualFile = psiFile.getVirtualFile();

            Runnable selectOnEditorRunnable = new Runnable() {
                @Override
                public void run() {
                    if (virtualFile != null && ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
                        @Override
                        public Boolean compute() {
                            return virtualFile.isValid() ? Boolean.TRUE : Boolean.FALSE;
                        }
                    }).booleanValue()) {

                        if (usage.isValid()) {
                            usage.highlightInEditor();
                            replaceContext.getUsageView().selectUsages(new Usage[]{usage});
                        }
                    }
                }
            };

            String path = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                @Override
                public String compute() {
                    return virtualFile != null ? virtualFile.getPath() : null;
                }
            });
            CommandProcessor.getInstance()
                    .executeCommand(myProject, selectOnEditorRunnable, FindBundle.message("find.replace.select.on.editor.command"), null);
            String title = FindBundle.message("find.replace.found.usage.title", i + 1, usages.length, path);

            int result;
            try {
                replaceUsage(usage, replaceContext.getFindModel(), replaceContext.getExcludedSetCached(), true);
                result = FindManager.getInstance(myProject).showPromptDialog(replaceContext.getFindModel(), title);
            }
            catch (FindManager.MalformedReplacementStringException e) {
                markAsMalformedReplacement(replaceContext, usage);
                result = FindManager.getInstance(myProject).showMalformedReplacementPrompt(replaceContext.getFindModel(), title, e);
            }

            if (result == FindManager.PromptResult.CANCEL) {
                return;
            }
            if (result == FindManager.PromptResult.SKIP) {
                continue;
            }

            final int currentNumber = i;
            if (result == FindManager.PromptResult.OK) {
                final Ref<Boolean> success = Ref.create();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        success.set(replaceUsageAndRemoveFromView(usage, replaceContext));
                    }
                };
                CommandProcessor.getInstance().executeCommand(myProject, runnable, FindBundle.message("find.replace.command"), null);
                if (closeUsageViewIfEmpty(replaceContext.getUsageView(), success.get())) {
                    return;
                }
            }

            if (result == FindManager.PromptResult.SKIP_ALL_IN_THIS_FILE) {
                int j;
                for(j = i + 1;j < usages.length; ++j) {
                    final PsiElement nextElt = ((UsageInfo2UsageAdapter)usages[j]).getUsageInfo().getElement();
                    if (nextElt == null) continue;
                    if (nextElt.getContainingFile() == psiFile) continue;
                    break;
                }
                i = j -1;
            }

            if (result == FindManager.PromptResult.ALL_IN_THIS_FILE) {
                final int[] nextNumber = new int[1];

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        int j = currentNumber;
                        boolean  success = true;
                        for (; j < usages.length; j++) {
                            final Usage usage = usages[j];
                            final UsageInfo usageInfo = ((UsageInfo2UsageAdapter)usage).getUsageInfo();

                            final PsiElement elt = usageInfo.getElement();
                            if (elt == null) continue;
                            PsiFile otherPsiFile = elt.getContainingFile();
                            if (!otherPsiFile.equals(psiFile)) {
                                break;
                            }
                            if (!replaceUsageAndRemoveFromView(usage, replaceContext)) {
                                success = false;
                            }
                        }
                        closeUsageViewIfEmpty(replaceContext.getUsageView(), success);
                        nextNumber[0] = j;
                    }
                };

                CommandProcessor.getInstance().executeCommand(myProject, runnable, FindBundle.message("find.replace.command"), null);

                //noinspection AssignmentToForLoopParameter
                i = nextNumber[0] - 1;
            }

            if (result == FindManager.PromptResult.ALL_FILES) {
                CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
                    @Override
                    public void run() {
                        final boolean success = replaceUsages(replaceContext, _usages);
                        closeUsageViewIfEmpty(replaceContext.getUsageView(), success);
                    }
                }, FindBundle.message("find.replace.command"), null);
                break;
            }
        }
    }

    private boolean replaceUsageAndRemoveFromView(Usage usage, ReplaceContext replaceContext) {
        try {
            if (replaceUsage(usage, replaceContext.getFindModel(), replaceContext.getExcludedSetCached(), false)) {
                replaceContext.getUsageView().removeUsage(usage);
            }
        }
        catch (FindManager.MalformedReplacementStringException e) {
            markAsMalformedReplacement(replaceContext, usage);
            return false;
        }
        return true;
    }

    private void addReplaceActions(final ReplaceContext replaceContext) {
        final Runnable replaceRunnable = new Runnable() {
            @Override
            public void run() {
                replaceUsagesUnderCommand(replaceContext, replaceContext.getUsageView().getUsages());
            }
        };
        replaceContext.getUsageView().addButtonToLowerPane(replaceRunnable, FindBundle.message("find.replace.all.action"));

        final Runnable replaceSelectedRunnable = new Runnable() {
            @Override
            public void run() {
                replaceUsagesUnderCommand(replaceContext, replaceContext.getUsageView().getSelectedUsages());
            }
        };

        replaceContext.getUsageView().addButtonToLowerPane(replaceSelectedRunnable, FindBundle.message("find.replace.selected.action"));
    }

    private boolean replaceUsages( ReplaceContext replaceContext,  Collection<Usage> usages) {
        if (!ensureUsagesWritable(replaceContext, usages)) {
            return true;
        }
        int replacedCount = 0;
        boolean success = true;
        for (final Usage usage : usages) {
            try {
                if (replaceUsage(usage, replaceContext.getFindModel(), replaceContext.getExcludedSetCached(), false)) {
                    replacedCount++;
                }
            }
            catch (FindManager.MalformedReplacementStringException e) {
                markAsMalformedReplacement(replaceContext, usage);
                success = false;
            }
        }
        replaceContext.getUsageView().removeUsagesBulk(usages);
        reportNumberReplacedOccurrences(myProject, replacedCount);
        return success;
    }

    private static void markAsMalformedReplacement(ReplaceContext replaceContext, Usage usage) {
        replaceContext.getUsageView().excludeUsages(new Usage[]{usage});
    }

    public static void reportNumberReplacedOccurrences(Project project, int occurrences) {
        if (occurrences != 0) {
            final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(FindBundle.message("0.occurrences.replaced", occurrences));
            }
        }
    }

    public boolean replaceUsage( final Usage usage,
                                 final FindModel findModel,
                                 final Set<Usage> excludedSet,
                                final boolean justCheck)
            throws FindManager.MalformedReplacementStringException {
        final Ref<FindManager.MalformedReplacementStringException> exceptionResult = Ref.create();
        final boolean result = ApplicationManager.getApplication().runWriteAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                if (excludedSet.contains(usage)) {
                    return false;
                }

                final Document document = ((UsageInfo2UsageAdapter)usage).getDocument();
                if (!document.isWritable()) return false;

                boolean result = ((UsageInfo2UsageAdapter)usage).processRangeMarkers(new Processor<Segment>() {
                    @Override
                    public boolean process(Segment segment) {
                        final int textOffset = segment.getStartOffset();
                        final int textEndOffset = segment.getEndOffset();
                        final Ref<String> stringToReplace = Ref.create();
                        try {
                            if (!getStringToReplace(textOffset, textEndOffset, document, findModel, stringToReplace)) return true;
                            if (!stringToReplace.isNull() && !justCheck) {
                                document.replaceString(textOffset, textEndOffset, stringToReplace.get());
                            }
                        }
                        catch (FindManager.MalformedReplacementStringException e) {
                            exceptionResult.set(e);
                            return false;
                        }
                        return true;
                    }
                });
                return result;
            }
        });

        if (!exceptionResult.isNull()) {
            throw exceptionResult.get();
        }
        return result;
    }

    private boolean getStringToReplace(int textOffset,
                                       int textEndOffset,
                                       Document document, FindModel findModel, Ref<String> stringToReplace)
            throws FindManager.MalformedReplacementStringException {
        if (textOffset < 0 || textOffset >= document.getTextLength()) {
            return false;
        }
        if (textEndOffset < 0 || textOffset > document.getTextLength()) {
            return false;
        }
        FindManager findManager = FindManager.getInstance(myProject);
        final CharSequence foundString = document.getCharsSequence().subSequence(textOffset, textEndOffset);
        PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
        FindResult findResult = findManager.findString(document.getCharsSequence(), textOffset, findModel, file != null ? file.getVirtualFile() : null);
        if (!findResult.isStringFound() ||
                // find result should be in needed range
                !(findResult.getStartOffset() >= textOffset && findResult.getEndOffset() <= textEndOffset) ) {
            return false;
        }

        stringToReplace.set(
                FindManager.getInstance(myProject).getStringToReplace(foundString.toString(), findModel, textOffset, document.getText()));

        return true;
    }

    private void replaceUsagesUnderCommand( final ReplaceContext replaceContext,  final Set<Usage> usagesSet) {
        if (usagesSet == null) {
            return;
        }

        final List<Usage> usages = new ArrayList<Usage>(usagesSet);
        Collections.sort(usages, UsageViewImpl.USAGE_COMPARATOR);

        if (!ensureUsagesWritable(replaceContext, usages)) return;

        CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
            @Override
            public void run() {
                final boolean success = replaceUsages(replaceContext, usages);
                final UsageView usageView = replaceContext.getUsageView();

                if (closeUsageViewIfEmpty(usageView, success)) return;
                usageView.getComponent().requestFocus();
            }
        }, FindBundle.message("find.replace.command"), null);

        replaceContext.invalidateExcludedSetCache();
    }

    private boolean ensureUsagesWritable(ReplaceContext replaceContext, Collection<Usage> selectedUsages) {
        Set<VirtualFile> readOnlyFiles = null;
        for (final Usage usage : selectedUsages) {
            final VirtualFile file = ((UsageInFile)usage).getFile();

            if (file != null && !file.isWritable()) {
                if (readOnlyFiles == null) readOnlyFiles = new HashSet<VirtualFile>();
                readOnlyFiles.add(file);
            }
        }

        if (readOnlyFiles != null) {
            ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(VfsUtilCore.toVirtualFileArray(readOnlyFiles));
        }

        if (hasReadOnlyUsages(selectedUsages)) {
            int result = Messages.showOkCancelDialog(replaceContext.getUsageView().getComponent(),
                    FindBundle.message("find.replace.occurrences.in.read.only.files.prompt"),
                    FindBundle.message("find.replace.occurrences.in.read.only.files.title"),
                    Messages.getWarningIcon());
            if (result != Messages.OK) {
                return false;
            }
        }
        return true;
    }

    private boolean closeUsageViewIfEmpty(UsageView usageView, boolean success) {
        if (usageView.getUsages().isEmpty()) {
            usageView.close();
            return true;
        }
        if (!success) {
            NOTIFICATION_GROUP.createNotification("One or more malformed replacement strings", MessageType.ERROR).notify(myProject);
        }
        return false;
    }

    public boolean isWorkInProgress() {
        return myIsFindInProgress;
    }

    public boolean isEnabled() {
        return !myIsFindInProgress && !FindInProjectManager.getInstance(myProject).isWorkInProgress();
    }

    private class UsageSearcherFactory implements Factory<UsageSearcher> {
        private final FindModel myFindModelCopy;
        private final PsiDirectory myPsiDirectory;
        private final FindUsagesProcessPresentation myProcessPresentation;

        private UsageSearcherFactory( FindModel findModelCopy,
                                     PsiDirectory psiDirectory,
                                      FindUsagesProcessPresentation processPresentation) {
            myFindModelCopy = findModelCopy;
            myPsiDirectory = psiDirectory;
            myProcessPresentation = processPresentation;
        }

        @Override
        public UsageSearcher create() {
            return new UsageSearcher() {

                @Override
                public void generate( final Processor<Usage> processor) {
                    try {
                        myIsFindInProgress = true;

                        FindInProjectUtil.findUsages(myFindModelCopy, myPsiDirectory, myProject,
                                new AdapterProcessor<UsageInfo, Usage>(processor, UsageInfo2UsageAdapter.CONVERTER),
                                myProcessPresentation);
                    }
                    finally {
                        myIsFindInProgress = false;
                    }
                }
            };
        }
    }
}