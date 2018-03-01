/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.diff.chains.DiffRequestProducerException;
import com.gome.maven.diff.impl.DiffRequestProcessor;
import com.gome.maven.diff.requests.*;
import com.gome.maven.diff.tools.util.SoftHardCacheMap;
import com.gome.maven.diff.util.DiffUserDataKeys;
import com.gome.maven.diff.util.DiffUserDataKeysEx.ScrollToPolicy;
import com.gome.maven.diff.util.WaitingBackgroundableTaskExecutor;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.util.ProgressWindow;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.Convertor;

import java.util.Collections;
import java.util.List;

public abstract class CacheChangeProcessor extends DiffRequestProcessor {
    private static final Logger LOG = Logger.getInstance(CacheChangeProcessor.class);

     private final Project myProject;

     private final SoftHardCacheMap<Change, Pair<Change, DiffRequest>> myRequestCache =
            new SoftHardCacheMap<Change, Pair<Change, DiffRequest>>(5, 5);

     private Change myCurrentChange;

     private final WaitingBackgroundableTaskExecutor myTaskExecutor = new WaitingBackgroundableTaskExecutor();

    public CacheChangeProcessor( Project project) {
        super(project);
        myProject = project;
    }

    public CacheChangeProcessor( Project project,  String place) {
        super(project, place);
        myProject = project;
    }

    //
    // Abstract
    //

    
    protected abstract List<Change> getSelectedChanges();

    
    protected abstract List<Change> getAllChanges();

    protected abstract void selectChange( Change change);

    //
    // Update
    //

    public void updateRequest(final boolean force,  final ScrollToPolicy scrollToChangePolicy) {
        final Change change = myCurrentChange;
        DiffRequest cachedRequest = loadRequestFast(change);
        if (cachedRequest != null) {
            applyRequest(cachedRequest, force, scrollToChangePolicy);
            return;
        }

        myTaskExecutor.execute(
                new Convertor<ProgressIndicator, Runnable>() {
                    @Override
                    public Runnable convert(ProgressIndicator indicator) {
                        final DiffRequest request = loadRequest(change, indicator);
                        return new Runnable() {
                            @Override
                            public void run() {
                                myRequestCache.put(change, Pair.create(change, request));
                                applyRequest(request, force, scrollToChangePolicy);
                            }
                        };
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        applyRequest(new LoadingDiffRequest(ChangeDiffRequestProducer.getRequestTitle(change)), force, scrollToChangePolicy);
                    }
                },
                ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS
        );
    }

    

    protected DiffRequest loadRequestFast( Change change) {
        if (change == null) return NoDiffRequest.INSTANCE;

        Pair<Change, DiffRequest> pair = myRequestCache.get(change);
        if (pair != null) {
            Change oldChange = pair.first;
            if (ChangeDiffRequestProducer.isEquals(oldChange, change)) {
                return pair.second;
            }
        }

        if (change.getBeforeRevision() instanceof FakeRevision || change.getAfterRevision() instanceof FakeRevision) {
            return new LoadingDiffRequest(ChangeDiffRequestProducer.getRequestTitle(change));
        }

        return null;
    }

    
//    @CalledInBackground
    private DiffRequest loadRequest( Change change,  ProgressIndicator indicator) {
        ChangeDiffRequestProducer presentable = ChangeDiffRequestProducer.create(myProject, change);
        if (presentable == null) return new ErrorDiffRequest("Can't show diff");
        try {
            return presentable.process(getContext(), indicator);
        }
        catch (ProcessCanceledException e) {
            OperationCanceledDiffRequest request = new OperationCanceledDiffRequest(presentable.getName());
            request.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, Collections.<AnAction>singletonList(new ReloadRequestAction(change)));
            return request;
        }
        catch (DiffRequestProducerException e) {
            return new ErrorDiffRequest(presentable, e);
        }
        catch (Exception e) {
            return new ErrorDiffRequest(presentable, e);
        }
    }

    //
    // Impl
    //

    @Override
    protected void onDispose() {
        super.onDispose();
        myTaskExecutor.abort();
        myRequestCache.clear();
    }

    //
    // Navigation
    //

  /*
   * Multiple selection:
   * - iterate inside selection
   *
   * Single selection:
   * - iterate all changes
   * - update selection after movement
   *
   * current element should always be among allChanges and selection (if they are not empty)
   */

    public void clear() {
        myCurrentChange = null;
        updateRequest();
    }

//    @CalledInAwt
    public void refresh() {
        List<Change> selectedChanges = getSelectedChanges();

        if (selectedChanges.isEmpty()) {
            myCurrentChange = null;
            updateRequest();
            return;
        }

        Change selectedChange = myCurrentChange != null ? ContainerUtil.find(selectedChanges, myCurrentChange) : null;
        if (selectedChange == null) {
            myCurrentChange = selectedChanges.get(0);
            updateRequest();
            return;
        }

        if (!ChangeDiffRequestProducer.isEquals(myCurrentChange, selectedChange)) {
            myCurrentChange = selectedChange;
            updateRequest();
        }
    }

    @Override
    protected boolean hasNextChange() {
        if (myCurrentChange == null) return false;

        List<Change> selectedChanges = getSelectedChanges();
        if (selectedChanges.isEmpty()) return false;

        if (selectedChanges.size() > 1) {
            int index = selectedChanges.indexOf(myCurrentChange);
            return index != -1 && index < selectedChanges.size() - 1;
        }
        else {
            List<Change> allChanges = getAllChanges();
            int index = allChanges.indexOf(myCurrentChange);
            return index != -1 && index < allChanges.size() - 1;
        }
    }

    @Override
    protected boolean hasPrevChange() {
        if (myCurrentChange == null) return false;

        List<Change> selectedChanges = getSelectedChanges();
        if (selectedChanges.isEmpty()) return false;

        if (selectedChanges.size() > 1) {
            int index = selectedChanges.indexOf(myCurrentChange);
            return index != -1 && index > 0;
        }
        else {
            List<Change> allChanges = getAllChanges();
            int index = allChanges.indexOf(myCurrentChange);
            return index != -1 && index > 0;
        }
    }

    @Override
    protected void goToNextChange(boolean fromDifferences) {
        List<Change> selectedChanges = getSelectedChanges();
        List<Change> allChanges = getAllChanges();

        if (selectedChanges.size() > 1) {
            int index = selectedChanges.indexOf(myCurrentChange);
            myCurrentChange = selectedChanges.get(index + 1);
        }
        else {
            int index = allChanges.indexOf(myCurrentChange);
            myCurrentChange = allChanges.get(index + 1);
            selectChange(myCurrentChange);
        }

        updateRequest(false, fromDifferences ? ScrollToPolicy.FIRST_CHANGE : null);
    }

    @Override
    protected void goToPrevChange(boolean fromDifferences) {
        List<Change> selectedChanges = getSelectedChanges();
        List<Change> allChanges = getAllChanges();

        if (selectedChanges.size() > 1) {
            int index = selectedChanges.indexOf(myCurrentChange);
            myCurrentChange = selectedChanges.get(index - 1);
        }
        else {
            int index = allChanges.indexOf(myCurrentChange);
            myCurrentChange = allChanges.get(index - 1);
            selectChange(myCurrentChange);
        }

        updateRequest(false, fromDifferences ? ScrollToPolicy.LAST_CHANGE : null);
    }

    @Override
    protected boolean isNavigationEnabled() {
        return getSelectedChanges().size() > 1 || getAllChanges().size() > 1;
    }

    //
    // Actions
    //

    protected class ReloadRequestAction extends DumbAwareAction {
         private final Change myChange;

        public ReloadRequestAction( Change change) {
            super("Reload", null, AllIcons.Actions.Refresh);
            myChange = change;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            myRequestCache.remove(myChange);
            updateRequest(true);
        }
    }
}
