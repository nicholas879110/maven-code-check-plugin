/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.BackgroundTaskQueue;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.vcs.Details;
import com.gome.maven.openapi.vcs.GenericDetailsLoader;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.PairConsumer;
import com.gome.maven.util.Ticket;
import com.gome.maven.util.continuation.ModalityIgnorantBackgroundableTask;

import javax.swing.*;

/**
 * For presentation, which is itself in GenericDetails (not necessarily) - shown from time to time, but cached, and
 * which is a listener to some intensive changes (a group of invalidating changes should provoke a reload, but "outdated" (loaded but already not actual) results should be thrown away)
 *
 *
 * User: Irina.Chernushina
 * Date: 9/7/11
 * Time: 3:13 PM
 */
public abstract class AbstractRefreshablePanel<T> implements RefreshablePanel<Change> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.changes.AbstractRefreshablePanel");
    protected final Ticket myTicket;
    private final DetailsPanel myDetailsPanel;
    private final GenericDetailsLoader<Ticket, T> myDetailsLoader;
    private final BackgroundTaskQueue myQueue;
    private volatile boolean myDisposed;

    protected AbstractRefreshablePanel(final Project project, final String loadingTitle, final BackgroundTaskQueue queue) {
        myQueue = queue;
        myTicket = new Ticket();
        myDetailsPanel = new DetailsPanel();
        myDetailsPanel.loading();
        myDetailsPanel.layout();

        myDetailsLoader = new GenericDetailsLoader<Ticket, T>(new Consumer<Ticket>() {
            @Override
            public void consume(Ticket ticket) {
                final Loader loader = new Loader(project, loadingTitle, myTicket.copy());
                loader.runSteadily(new Consumer<Task.Backgroundable>() {
                    @Override
                    public void consume(Task.Backgroundable backgroundable) {
                        myQueue.run(backgroundable);
                    }
                });
            }
        }, new PairConsumer<Ticket, T>() {
            @Override
            public void consume(Ticket ticket, T t) {
                acceptData(t);
            }
        });
    }

    @Override
    public boolean refreshDataSynch() {
        return false;
    }

//    @CalledInAwt
    @Override
    public void dataChanged() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        myTicket.increment();
    }

//    @CalledInAwt
    @Override
    public void refresh() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (! Comparing.equal(myDetailsLoader.getCurrentlySelected(), myTicket)) {
            final Ticket copy = myTicket.copy();
            myDetailsLoader.updateSelection(copy, false);
            myDetailsPanel.loading();
            myDetailsPanel.layout();
        } else {
            refreshPresentation();
        }
    }

    protected abstract void refreshPresentation();

//    @CalledInBackground
    protected abstract T loadImpl() throws VcsException;
//    @CalledInAwt
    protected abstract JPanel dataToPresentation(final T t);
    protected abstract void disposeImpl();

//    @CalledInAwt
    private void acceptData(final T t) {
        final JPanel panel = dataToPresentation(t);
        myDetailsPanel.data(panel);
        myDetailsPanel.layout();
    }

    @Override
    public JPanel getPanel() {
        return myDetailsPanel.getPanel();
    }

    @Override
    public boolean isStillValid(Change data) {
        return true;
    }

    private class Loader extends ModalityIgnorantBackgroundableTask {
        private final Ticket myTicketCopy;
        private T myT;

        private Loader( Project project,  String title, final Ticket ticketCopy) {
            super(project, title, false, BackgroundFromStartOption.getInstance());
            myTicketCopy = ticketCopy;
        }

        @Override
        protected void doInAwtIfFail(Exception e) {
            final Exception cause;
            if (e instanceof MyRuntime && e.getCause() != null) {
                cause = (Exception) e.getCause();
            } else {
                cause = e;
            }
            LOG.info(e);
            String message = cause.getMessage() == null ? e.getMessage() : cause.getMessage();
            message = message == null ? "Unknown error" : message;
            VcsBalloonProblemNotifier.showOverChangesView(myProject, message, MessageType.ERROR);
        }

        @Override
        protected void doInAwtIfCancel() {
        }

        @Override
        protected void doInAwtIfSuccess() {
            if (myDisposed) return;
            try {
                myDetailsLoader.take(myTicketCopy, myT);
            }
            catch (Details.AlreadyDisposedException e) {
                // t is not disposable
            }
        }

        @Override
        protected void runImpl( ProgressIndicator indicator) {
            if (myDisposed) return;
            try {
                myT = loadImpl();
            }
            catch (VcsException e) {
                throw new MyRuntime(e);
            }
        }
    }

    private static class MyRuntime extends RuntimeException {
        private MyRuntime(Throwable cause) {
            super(cause);
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
        disposeImpl();
    }
}
