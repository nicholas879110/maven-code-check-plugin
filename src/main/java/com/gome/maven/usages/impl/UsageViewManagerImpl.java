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
package com.gome.maven.usages.impl;

import com.gome.maven.find.SearchInBackgroundOption;
import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.actionSystem.DataSink;
import com.gome.maven.openapi.actionSystem.TypeSafeDataProvider;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.progress.util.TooManyUsagesStatus;
import com.gome.maven.openapi.project.DumbModeAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Factory;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowId;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.search.*;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.ui.content.Content;
import com.gome.maven.usageView.UsageViewBundle;
import com.gome.maven.usages.*;
import com.gome.maven.usages.rules.PsiElementUsage;
import com.gome.maven.usages.rules.UsageInFile;
import com.gome.maven.util.ui.UIUtil;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author max
 */
public class UsageViewManagerImpl extends UsageViewManager {
    private final Project myProject;
    private static final Key<UsageView> USAGE_VIEW_KEY = Key.create("USAGE_VIEW");

    public UsageViewManagerImpl( Project project) {
        myProject = project;
    }

    @Override
    
    public UsageView createUsageView( UsageTarget[] targets,
                                      Usage[] usages,
                                      UsageViewPresentation presentation,
                                     Factory<UsageSearcher> usageSearcherFactory) {
        UsageViewImpl usageView = new UsageViewImpl(myProject, presentation, targets, usageSearcherFactory);
        appendUsages(usages, usageView);
        usageView.setSearchInProgress(false);
        return usageView;
    }

    @Override
    
    public UsageView showUsages( UsageTarget[] searchedFor,
                                 Usage[] foundUsages,
                                 UsageViewPresentation presentation,
                                Factory<UsageSearcher> factory) {
        UsageView usageView = createUsageView(searchedFor, foundUsages, presentation, factory);
        addContent((UsageViewImpl)usageView, presentation);
        showToolWindow(true);
        return usageView;
    }

    @Override
    
    public UsageView showUsages( UsageTarget[] searchedFor,  Usage[] foundUsages,  UsageViewPresentation presentation) {
        return showUsages(searchedFor, foundUsages, presentation, null);
    }

    void addContent( UsageViewImpl usageView,  UsageViewPresentation presentation) {
        Content content = com.gome.maven.usageView.UsageViewManager.getInstance(myProject).addContent(
                presentation.getTabText(),
                presentation.getTabName(),
                presentation.getToolwindowTitle(),
                true,
                usageView.getComponent(),
                presentation.isOpenInNewTab(),
                true
        );
        usageView.setContent(content);
        content.putUserData(USAGE_VIEW_KEY, usageView);
    }

    @Override
    public UsageView searchAndShowUsages( final UsageTarget[] searchFor,
                                          final Factory<UsageSearcher> searcherFactory,
                                         final boolean showPanelIfOnlyOneUsage,
                                         final boolean showNotFoundMessage,
                                          final UsageViewPresentation presentation,
                                          final UsageViewStateListener listener) {
        final FindUsagesProcessPresentation processPresentation = new FindUsagesProcessPresentation(presentation);
        processPresentation.setShowNotFoundMessage(showNotFoundMessage);
        processPresentation.setShowPanelIfOnlyOneUsage(showPanelIfOnlyOneUsage);

        return doSearchAndShow(searchFor, searcherFactory, presentation, processPresentation, listener);
    }

    private UsageView doSearchAndShow( final UsageTarget[] searchFor,
                                       final Factory<UsageSearcher> searcherFactory,
                                       final UsageViewPresentation presentation,
                                       final FindUsagesProcessPresentation processPresentation,
                                       final UsageViewStateListener listener) {
        final SearchScope searchScopeToWarnOfFallingOutOf = getMaxSearchScopeToWarnOfFallingOutOf(searchFor);
        final AtomicReference<UsageViewImpl> usageViewRef = new AtomicReference<UsageViewImpl>();

        Task.Backgroundable task = new Task.Backgroundable(myProject, getProgressTitle(presentation), true, new SearchInBackgroundOption()) {
            @Override
            public void run( final ProgressIndicator indicator) {
                new SearchForUsagesRunnable(UsageViewManagerImpl.this, UsageViewManagerImpl.this.myProject, usageViewRef, presentation, searchFor, searcherFactory,
                        processPresentation, searchScopeToWarnOfFallingOutOf, listener).run();
            }

            
            @Override
            public DumbModeAction getDumbModeAction() {
                return DumbModeAction.CANCEL;
            }

            @Override
            
            public NotificationInfo getNotificationInfo() {
                String notification = usageViewRef.get() != null ? usageViewRef.get().getUsagesCount() + " Usage(s) Found" : "No Usages Found";
                return new NotificationInfo("Find Usages", "Find Usages Finished", notification);
            }
        };
        ProgressManager.getInstance().run(task);
        return usageViewRef.get();
    }

    
    SearchScope getMaxSearchScopeToWarnOfFallingOutOf( UsageTarget[] searchFor) {
        UsageTarget target = searchFor[0];
        if (target instanceof TypeSafeDataProvider) {
            final SearchScope[] scope = new SearchScope[1];
            ((TypeSafeDataProvider)target).calcData(UsageView.USAGE_SCOPE, new DataSink() {
                @Override
                public <T> void put(DataKey<T> key, T data) {
                    scope[0] = (SearchScope)data;
                }
            });
            return scope[0];
        }
        return GlobalSearchScope.allScope(myProject); // by default do not warn of falling out of scope
    }

    @Override
    public void searchAndShowUsages( UsageTarget[] searchFor,
                                     Factory<UsageSearcher> searcherFactory,
                                     FindUsagesProcessPresentation processPresentation,
                                     UsageViewPresentation presentation,
                                     UsageViewStateListener listener) {
        doSearchAndShow(searchFor, searcherFactory, presentation, processPresentation, listener);
    }

    @Override
    public UsageView getSelectedUsageView() {
        final Content content = com.gome.maven.usageView.UsageViewManager.getInstance(myProject).getSelectedContent();
        if (content != null) {
            return content.getUserData(USAGE_VIEW_KEY);
        }

        return null;
    }

    
    public static String getProgressTitle( UsageViewPresentation presentation) {
        final String scopeText = presentation.getScopeText();
        String usagesString = StringUtil.capitalize(presentation.getUsagesString());
        return UsageViewBundle.message("progress.searching.for.in", usagesString, scopeText, presentation.getContextText());
    }

    void showToolWindow(boolean activateWindow) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.FIND);
        toolWindow.show(null);
        if (activateWindow && !toolWindow.isActive()) {
            toolWindow.activate(null);
        }
    }

    private static void appendUsages( final Usage[] foundUsages,  final UsageViewImpl usageView) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                for (Usage foundUsage : foundUsages) {
                    usageView.appendUsage(foundUsage);
                }
            }
        });
    }


    public static void showTooManyUsagesWarning( final Project project,
                                                 final TooManyUsagesStatus tooManyUsagesStatus,
                                                 final ProgressIndicator indicator,
                                                 final UsageViewPresentation presentation,
                                                final int usageCount,
                                                 final UsageViewImpl usageView) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (usageView != null && usageView.searchHasBeenCancelled() || indicator.isCanceled()) return;
                String message = UsageViewBundle.message("find.excessive.usage.count.prompt", usageCount, StringUtil.pluralize(presentation.getUsagesWord()));
                UsageLimitUtil.Result ret = UsageLimitUtil.showTooManyUsagesWarning(project, message, presentation);
                if (ret == UsageLimitUtil.Result.ABORT) {
                    if (usageView != null) {
                        usageView.cancelCurrentSearch();
                    }
                    indicator.cancel();
                }
                tooManyUsagesStatus.userResponded();
            }
        });
    }

    public static long getFileLength( final VirtualFile virtualFile) {
        final long[] length = {-1L};
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                if (!virtualFile.isValid()) return;
                if (virtualFile.getFileType().isBinary()) return;
                length[0] = virtualFile.getLength();
            }
        });
        return length[0];
    }

    
    public static String presentableSize(long bytes) {
        long megabytes = bytes / (1024 * 1024);
        return UsageViewBundle.message("find.file.size.megabytes", Long.toString(megabytes));
    }

    public static boolean isInScope( Usage usage,  SearchScope searchScope) {
        PsiElement element = null;
        VirtualFile file = usage instanceof UsageInFile ? ((UsageInFile)usage).getFile() :
                usage instanceof PsiElementUsage ? PsiUtilCore.getVirtualFile(element = ((PsiElementUsage)usage).getElement()) : null;
        if (file != null) {
            return isFileInScope(file, searchScope);
        }
        else if(element != null) {
            return searchScope instanceof EverythingGlobalScope ||
                    searchScope instanceof ProjectScopeImpl ||
                    searchScope instanceof ProjectAndLibrariesScope;
        }
        return false;
    }

    private static boolean isFileInScope( VirtualFile file,  SearchScope searchScope) {
        if (file instanceof VirtualFileWindow) {
            file = ((VirtualFileWindow)file).getDelegate();
        }
        if (searchScope instanceof LocalSearchScope) {
            return ((LocalSearchScope)searchScope).isInScope(file);
        }
        else {
            return ((GlobalSearchScope)searchScope).contains(file);
        }
    }

    
    public static String outOfScopeMessage(int nUsages,  SearchScope searchScope) {
        return (nUsages == 1 ? "One usage is" : nUsages + " usages are") +
                " out of scope '"+ searchScope.getDisplayName()+"'";
    }

}
