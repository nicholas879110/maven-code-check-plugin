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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 05.12.2006
 * Time: 19:39:22
 */
package com.gome.maven.openapi.vcs.changes.committed;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.changes.BackgroundFromStartOption;
import com.gome.maven.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.gome.maven.openapi.vcs.versionBrowser.CommittedChangeList;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.FilterComponent;
import com.gome.maven.ui.LightColors;
import com.gome.maven.util.AsynchConsumer;
import com.gome.maven.util.BufferedListConsumer;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.WaitForProgressToShow;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CommittedChangesPanel extends JPanel implements TypeSafeDataProvider, Disposable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.changes.committed.CommittedChangesPanel");

    private final CommittedChangesTreeBrowser myBrowser;
    private final Project myProject;
    private CommittedChangesProvider myProvider;
    private ChangeBrowserSettings mySettings;
    private final RepositoryLocation myLocation;
    private int myMaxCount = 0;
    private final MyFilterComponent myFilterComponent = new MyFilterComponent();
    private final JCheckBox myRegexCheckbox;
    private final List<Runnable> myShouldBeCalledOnDispose;
    private volatile boolean myDisposed;
    private volatile boolean myInLoad;
    private Consumer<String> myIfNotCachedReloader;
    private boolean myChangesLoaded;

    public CommittedChangesPanel(Project project, final CommittedChangesProvider provider, final ChangeBrowserSettings settings,
                                  final RepositoryLocation location,  ActionGroup extraActions) {
        super(new BorderLayout());
        mySettings = settings;
        myProject = project;
        myProvider = provider;
        myLocation = location;
        myShouldBeCalledOnDispose = new ArrayList<Runnable>();
        myBrowser = new CommittedChangesTreeBrowser(project, new ArrayList<CommittedChangeList>());
        Disposer.register(this, myBrowser);
        add(myBrowser, BorderLayout.CENTER);

        final VcsCommittedViewAuxiliary auxiliary = provider.createActions(myBrowser, location);

        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.X_AXIS));

        ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction("CommittedChangesToolbar");

        ActionToolbar toolBar = myBrowser.createGroupFilterToolbar(project, group, extraActions,
                auxiliary != null ? auxiliary.getToolbarActions() : Collections.<AnAction>emptyList());
        toolbarPanel.add(toolBar.getComponent());
        toolbarPanel.add(Box.createHorizontalGlue());
        myRegexCheckbox = new JCheckBox(VcsBundle.message("committed.changes.regex.title"));
        myRegexCheckbox.setSelected(false);
        myRegexCheckbox.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                myFilterComponent.filter();
            }
        });
        toolbarPanel.add(myFilterComponent);
        toolbarPanel.add(myRegexCheckbox);
        myFilterComponent.setMinimumSize(myFilterComponent.getPreferredSize());
        myFilterComponent.setMaximumSize(myFilterComponent.getPreferredSize());
        myBrowser.setToolBar(toolbarPanel);

        if (auxiliary != null) {
            myShouldBeCalledOnDispose.add(auxiliary.getCalledOnViewDispose());
            myBrowser.setTableContextMenu(group, auxiliary.getPopupActions());
        } else {
            myBrowser.setTableContextMenu(group, Collections.<AnAction>emptyList());
        }

        final AnAction anAction = ActionManager.getInstance().getAction("CommittedChanges.Refresh");
        anAction.registerCustomShortcutSet(CommonShortcuts.getRerun(), this);
        myBrowser.addFilter(myFilterComponent);
        myIfNotCachedReloader = myLocation == null ? null : new Consumer<String>() {
            @Override
            public void consume(String s) {
                refreshChanges(false);
            }
        };
    }

    public RepositoryLocation getRepositoryLocation() {
        return myLocation;
    }

    public void setMaxCount(final int maxCount) {
        myMaxCount = maxCount;
    }

    public void setProvider(final CommittedChangesProvider provider) {
        if (myProvider != provider) {
            myProvider = provider;
            mySettings = provider.createDefaultSettings();
        }
    }

    public void refreshChanges(final boolean cacheOnly) {
        if (myLocation != null) {
            refreshChangesFromLocation();
        }
        else {
            refreshChangesFromCache(cacheOnly);
        }
    }

    private void refreshChangesFromLocation() {
        myBrowser.reset();

        myInLoad = true;
        myBrowser.setLoading(true);
        ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Loading changes", true, BackgroundFromStartOption.getInstance()) {

            public void run( final ProgressIndicator indicator) {
                try {
                    final AsynchConsumer<List<CommittedChangeList>> appender = new AsynchConsumer<List<CommittedChangeList>>() {
                        public void finished() {
                        }

                        public void consume(final List<CommittedChangeList> list) {
                            new AbstractCalledLater(myProject, ModalityState.stateForComponent(myBrowser)) {
                                public void run() {
                                    myBrowser.append(list);
                                }
                            }.callMe();
                        }
                    };
                    final BufferedListConsumer<CommittedChangeList> bufferedListConsumer = new BufferedListConsumer<CommittedChangeList>(30, appender,-1);

                    myProvider.loadCommittedChanges(mySettings, myLocation, myMaxCount, new AsynchConsumer<CommittedChangeList>() {
                        public void finished() {
                            bufferedListConsumer.flush();
                        }
                        public void consume(CommittedChangeList committedChangeList) {
                            if (myDisposed) {
                                indicator.cancel();
                            }
                            ProgressManager.checkCanceled();
                            bufferedListConsumer.consumeOne(committedChangeList);
                        }
                    });
                }
                catch (final VcsException e) {
                    LOG.info(e);
                    WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                        public void run() {
                            Messages.showErrorDialog(myProject, "Error refreshing view: " + StringUtil.join(e.getMessages(), "\n"), "Committed Changes");
                        }
                    }, null, myProject);
                } finally {
                    myInLoad = false;
                    myBrowser.setLoading(false);
                }
            }
        });
    }

    public void clearCaches() {
        final CommittedChangesCache cache = CommittedChangesCache.getInstance(myProject);
        cache.clearCaches(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateFilteredModel(Collections.<CommittedChangeList>emptyList(), true);
                    }
                }, ModalityState.NON_MODAL, myProject.getDisposed());
            }
        });
    }

    private void refreshChangesFromCache(final boolean cacheOnly) {
        final CommittedChangesCache cache = CommittedChangesCache.getInstance(myProject);
        cache.hasCachesForAnyRoot(new Consumer<Boolean>() {
            public void consume(final Boolean notEmpty) {
                if (! notEmpty) {
                    if (cacheOnly) {
                        myBrowser.getEmptyText().setText(VcsBundle.message("committed.changes.not.loaded.message"));
                        return;
                    }
                    if (!CacheSettingsDialog.showSettingsDialog(myProject)) return;
                }
                cache.getProjectChangesAsync(mySettings, myMaxCount, cacheOnly,
                        new Consumer<List<CommittedChangeList>>() {
                            public void consume(final List<CommittedChangeList> committedChangeLists) {
                                updateFilteredModel(committedChangeLists, false);
                            }
                        },
                        new Consumer<List<VcsException>>() {
                            public void consume(final List<VcsException> vcsExceptions) {
                                AbstractVcsHelper.getInstance(myProject).showErrors(vcsExceptions, "Error refreshing VCS history");
                            }
                        });
            }
        });
    }

    private interface FilterHelper {
        boolean filter( final CommittedChangeList cl);
    }

    private class RegexFilterHelper implements FilterHelper {
        private final Pattern myPattern;

        RegexFilterHelper( final String regex) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                pattern = null;
                myBrowser.getEmptyText().setText(VcsBundle.message("committed.changes.incorrect.regex.message"));
            }
            this.myPattern = pattern;
        }

        @Override
        public boolean filter( CommittedChangeList cl) {
            return changeListMatches(cl);
        }

        private boolean changeListMatches( CommittedChangeList cl) {
            if (myPattern == null) {
                return false;
            }
            boolean commentMatches = myPattern.matcher(cl.getComment()).find();
            boolean committerMatches = myPattern.matcher(cl.getCommitterName()).find();
            boolean revisionMatches = myPattern.matcher(Long.toString(cl.getNumber())).find();
            return commentMatches || committerMatches || revisionMatches;
        }
    }

    private static class WordMatchFilterHelper implements FilterHelper {
        private final String[] myParts;

        WordMatchFilterHelper(final String filterString) {
            myParts = filterString.split(" ");
            for(int i = 0; i < myParts.length; ++ i) {
                myParts [i] = myParts [i].toLowerCase();
            }
        }

        public boolean filter( final CommittedChangeList cl) {
            return changeListMatches(cl, myParts);
        }

        private static boolean changeListMatches( final CommittedChangeList changeList, final String[] filterWords) {
            for(String word: filterWords) {
                final String comment = changeList.getComment();
                final String committer = changeList.getCommitterName();
                if ((comment != null && comment.toLowerCase().contains(word)) ||
                        (committer != null && committer.toLowerCase().contains(word)) ||
                        Long.toString(changeList.getNumber()).contains(word)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void updateFilteredModel(List<CommittedChangeList> committedChangeLists, final boolean reset) {
        if (committedChangeLists == null) {
            return;
        }
        myChangesLoaded = !reset;
        setEmptyMessage(myChangesLoaded);
        myBrowser.setItems(committedChangeLists, CommittedChangesBrowserUseCase.COMMITTED);
    }

    private void setEmptyMessage(boolean changesLoaded) {
        String emptyText;
        if (!changesLoaded) {
            emptyText = VcsBundle.message("committed.changes.not.loaded.message");
        } else {
            emptyText = VcsBundle.message("committed.changes.empty.message");
        }
        myBrowser.getEmptyText().setText(emptyText);
    }

    public void setChangesFilter() {
        CommittedChangesFilterDialog filterDialog = new CommittedChangesFilterDialog(myProject, myProvider.createFilterUI(true), mySettings);
        if (filterDialog.showAndGet()) {
            mySettings = filterDialog.getSettings();
            refreshChanges(false);
        }
    }

    public void calcData(DataKey key, DataSink sink) {
        if (key.equals(VcsDataKeys.REMOTE_HISTORY_CHANGED_LISTENER)) {
            sink.put(VcsDataKeys.REMOTE_HISTORY_CHANGED_LISTENER, myIfNotCachedReloader);
        } else if (VcsDataKeys.REMOTE_HISTORY_LOCATION.equals(key)) {
            sink.put(VcsDataKeys.REMOTE_HISTORY_LOCATION, myLocation);
        }
        //if (key.equals(VcsDataKeys.CHANGES) || key.equals(VcsDataKeys.CHANGE_LISTS)) {
        myBrowser.calcData(key, sink);
        //}
    }

    public void dispose() {
        for (Runnable runnable : myShouldBeCalledOnDispose) {
            runnable.run();
        }
        myDisposed = true;
    }

    private void setRegularFilterBackground() {
        myFilterComponent.getTextEditor().setBackground(UIUtil.getTextFieldBackground());
    }

    private void setNotFoundFilterBackground() {
        myFilterComponent.getTextEditor().setBackground(LightColors.RED);
    }

    private class MyFilterComponent extends FilterComponent implements ChangeListFilteringStrategy {
        private final List<ChangeListener> myList = ContainerUtil.createLockFreeCopyOnWriteList();

        public MyFilterComponent() {
            super("COMMITTED_CHANGES_FILTER_HISTORY", 20);
        }

        @Override
        public CommittedChangesFilterKey getKey() {
            return new CommittedChangesFilterKey("text", CommittedChangesFilterPriority.TEXT);
        }

        public void filter() {
            for (ChangeListener changeListener : myList) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }
        public JComponent getFilterUI() {
            return null;
        }
        public void setFilterBase(List<CommittedChangeList> changeLists) {
        }
        public void addChangeListener(ChangeListener listener) {
            myList.add(listener);
        }
        public void removeChangeListener(ChangeListener listener) {
            myList.remove(listener);
        }
        public void resetFilterBase() {
        }
        public void appendFilterBase(List<CommittedChangeList> changeLists) {
        }
        
        public List<CommittedChangeList> filterChangeLists(List<CommittedChangeList> changeLists) {
            final FilterHelper filterHelper;
            setEmptyMessage(myChangesLoaded);
            if (myRegexCheckbox.isSelected()) {
                filterHelper = new RegexFilterHelper(myFilterComponent.getFilter());
            } else {
                filterHelper = new WordMatchFilterHelper(myFilterComponent.getFilter());
            }
            final List<CommittedChangeList> result = new ArrayList<CommittedChangeList>();
            for (CommittedChangeList list : changeLists) {
                if (filterHelper.filter(list)) {
                    result.add(list);
                }
            }
            if (result.size() == 0 && !myFilterComponent.getFilter().isEmpty()) {
                setNotFoundFilterBackground();
            } else {
                setRegularFilterBackground();
            }
            return result;
        }
    }

    public void passCachedListsToListener(final VcsConfigurationChangeListener.DetailedNotification notification,
                                          final Project project, final VirtualFile root) {
        final LinkedList<CommittedChangeList> resultList = new LinkedList<CommittedChangeList>();
        myBrowser.reportLoadedLists(new CommittedChangeListsListener() {
            public void onBeforeStartReport() {
            }
            public boolean report(CommittedChangeList list) {
                resultList.add(list);
                return false;
            }
            public void onAfterEndReport() {
                if (! resultList.isEmpty()) {
                    notification.execute(project, root, resultList);
                }
            }
        });
    }

    public boolean isInLoad() {
        return myInLoad;
    }
}
