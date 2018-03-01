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
package com.gome.maven.openapi.vcs.history;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.annotate.AnnotationProvider;
import com.gome.maven.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.gome.maven.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowId;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.ui.content.ContentManager;
import com.gome.maven.util.BufferedListConsumer;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.ContentUtilEx;

import java.util.List;

/**
 * @author irengrig
 */
public class FileHistorySessionPartner implements VcsAppendableHistorySessionPartner {
    private final LimitHistoryCheck myLimitHistoryCheck;
    private FileHistoryPanelImpl myFileHistoryPanel;
    private final VcsHistoryProvider myVcsHistoryProvider;
    private final AnnotationProvider myAnnotationProvider;
     private final FilePath myPath;
    private final String myRepositoryPath;
    private final AbstractVcs myVcs;
    private final FileHistoryRefresherI myRefresherI;
    private volatile VcsAbstractHistorySession mySession;
    private final BufferedListConsumer<VcsFileRevision> myBuffer;

    public FileHistorySessionPartner(final VcsHistoryProvider vcsHistoryProvider, final AnnotationProvider annotationProvider,
                                      final FilePath path,
                                     final String repositoryPath,
                                     final AbstractVcs vcs,
                                     final FileHistoryRefresherI refresherI) {
        myVcsHistoryProvider = vcsHistoryProvider;
        myAnnotationProvider = annotationProvider;
        myPath = path;
        myLimitHistoryCheck = new LimitHistoryCheck(vcs.getProject(), path.getPath());
        myRepositoryPath = repositoryPath;
        myVcs = vcs;
        myRefresherI = refresherI;
        Consumer<List<VcsFileRevision>> sessionRefresher = new Consumer<List<VcsFileRevision>>() {
            public void consume(List<VcsFileRevision> vcsFileRevisions) {
                // TODO: Logic should be revised to we could just append some revisions to history panel instead of creating and showing new history
                // TODO: session
                mySession.getRevisionList().addAll(vcsFileRevisions);
                final VcsHistorySession copy = mySession.copyWithCachedRevision();
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run() {
                        ensureHistoryPanelCreated().getHistoryPanelRefresh().consume(copy);
                    }
                });
            }
        };
        myBuffer = new BufferedListConsumer<VcsFileRevision>(5, sessionRefresher, 1000) {
            @Override
            protected void invokeConsumer( Runnable consumerRunnable) {
                // Do not invoke in arbitrary background thread as due to parallel execution this could lead to cases when invokeLater() (from
                // sessionRefresher) is scheduled at first for history session with (as an example) 10 revisions (new buffered list) and then with
                // 5 revisions (previous buffered list). And so incorrect UI is shown to the user.
                consumerRunnable.run();
            }
        };
    }

    public void acceptRevision(VcsFileRevision revision) {
        myLimitHistoryCheck.checkNumber();
        myBuffer.consumeOne(revision);
    }

    private FileHistoryPanelImpl ensureHistoryPanelCreated() {
        if (myFileHistoryPanel == null) {
            ContentManager contentManager = ProjectLevelVcsManagerEx.getInstanceEx(myVcs.getProject()).getContentManager();
            final VcsHistorySession copy = mySession.copyWithCachedRevision();
            myFileHistoryPanel = new FileHistoryPanelImpl(myVcs, myPath, copy, myVcsHistoryProvider,
                    contentManager, myRefresherI);
        }
        return myFileHistoryPanel;
    }

    private FileHistoryPanelImpl resetHistoryPanel() {
        final VcsHistorySession copy = mySession.copyWithCachedRevision();
        if (myFileHistoryPanel == null) {
            ContentManager contentManager = ProjectLevelVcsManagerEx.getInstanceEx(myVcs.getProject()).getContentManager();
            myFileHistoryPanel = new FileHistoryPanelImpl(myVcs, myPath, copy, myVcsHistoryProvider,
                    contentManager, myRefresherI);
        } else {
            myFileHistoryPanel.getHistoryPanelRefresh().consume(copy);
        }
        return myFileHistoryPanel;
    }

    public void reportCreatedEmptySession(final VcsAbstractHistorySession session) {
        if (mySession != null && session != null && mySession.getRevisionList().equals(session.getRevisionList())) return;
        mySession = session;
        if (mySession != null) {
            mySession.shouldBeRefreshed();  // to init current revision!
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                String actionName = VcsBundle.message(myPath.isDirectory() ? "action.name.file.history.dir" : "action.name.file.history",
                        myPath.getName());
                ContentManager contentManager = ProjectLevelVcsManagerEx.getInstanceEx(myVcs.getProject()).getContentManager();

                myFileHistoryPanel = resetHistoryPanel();
                ToolWindow toolWindow = ToolWindowManager.getInstance(myVcs.getProject()).getToolWindow(ToolWindowId.VCS);
                assert toolWindow != null : "Version Control ToolWindow should be available at this point.";

                ContentUtilEx.addTabbedContent(toolWindow.getContentManager(), myFileHistoryPanel, "History", myPath.getName(),
                        myRefresherI.isFirstTime());

                if (myRefresherI.isFirstTime()) {
                    toolWindow.activate(null);
                }
            }
        });
    }

    public void reportException(VcsException exception) {
        VcsBalloonProblemNotifier.showOverVersionControlView(myVcs.getProject(),
                VcsBundle.message("message.title.could.not.load.file.history") + ": " +
                        exception.getMessage(), MessageType.ERROR);
    }

    @Override
    public void beforeRefresh() {
        myLimitHistoryCheck.reset();
    }

    public void finished() {
        myBuffer.flush();
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                if (mySession == null) {
                    // nothing to be done, exit
                    return;
                }
                ensureHistoryPanelCreated().getHistoryPanelRefresh().finished();
            }
        });
    }

    @Override
    public void forceRefresh() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                if (mySession == null) {
                    // nothing to be done, exit
                    return;
                }
                ensureHistoryPanelCreated().scheduleRefresh(false);
            }
        });
    }
}
