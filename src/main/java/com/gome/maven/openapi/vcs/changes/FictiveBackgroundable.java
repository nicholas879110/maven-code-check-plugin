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
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.util.WaitForProgressToShow;

class FictiveBackgroundable extends Task.Backgroundable {
    private final Waiter myWaiter;
    private final ModalityState myState;

    FictiveBackgroundable( final Project project,  final Runnable runnable, final boolean cancellable, final String title,
                          final ModalityState state) {
        super(project, VcsBundle.message("change.list.manager.wait.lists.synchronization", title), cancellable, BackgroundFromStartOption.getInstance());
        myState = state;
        myWaiter = new Waiter(project, runnable, state, VcsBundle.message("change.list.manager.wait.lists.synchronization", title), cancellable);
    }

    public void run( final ProgressIndicator indicator) {
        myWaiter.run(indicator);
        WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
            public void run() {
                myWaiter.onSuccess();
            }
        }, myState == null ? ModalityState.NON_MODAL : myState, myProject);
    }

    @Override
    public boolean isHeadless() {
        return false;
    }

    public void done() {
        myWaiter.done();
    }
}
