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

package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.AbstractProjectComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.DumbAwareRunnable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.ProjectLevelVcsManager;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsListener;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowAnchor;
import com.gome.maven.openapi.wm.ToolWindowId;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.ui.content.*;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.NotNullFunction;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author yole
 */
public class ChangesViewContentManager extends AbstractProjectComponent implements ChangesViewContentI {
    public static final String TOOLWINDOW_ID = Registry.is("vcs.merge.toolwindows") ? ToolWindowId.VCS : VcsBundle.message("changes.toolwindow.name");
    private static final Key<ChangesViewContentEP> myEPKey = Key.create("ChangesViewContentEP");
    private static final Logger LOG = Logger.getInstance(ChangesViewContentManager.class);

    private MyContentManagerListener myContentManagerListener;
    private final ProjectLevelVcsManager myVcsManager;

    public static ChangesViewContentI getInstance(Project project) {
        return PeriodicalTasksCloser.getInstance().safeGetComponent(project, ChangesViewContentI.class);
    }

    private ContentManager myContentManager;
    private ToolWindow myToolWindow;
    private final VcsListener myVcsListener = new MyVcsListener();
    private final Alarm myVcsChangeAlarm;
    private final List<Content> myAddedContents = new ArrayList<Content>();
     private final CountDownLatch myInitializationWaiter = new CountDownLatch(1);

    public ChangesViewContentManager(final Project project, final ProjectLevelVcsManager vcsManager) {
        super(project);
        myVcsManager = vcsManager;
        myVcsChangeAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
    }

    public void projectOpened() {
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;
        StartupManager.getInstance(myProject).registerPostStartupActivity(new DumbAwareRunnable() {
            public void run() {
                final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
                if (toolWindowManager != null) {
                    myToolWindow = toolWindowManager.registerToolWindow(TOOLWINDOW_ID, true, ToolWindowAnchor.BOTTOM, myProject, true);
                    myToolWindow.setIcon(AllIcons.Toolwindows.ToolWindowChanges);

                    updateToolWindowAvailability();
                    final ContentManager contentManager = myToolWindow.getContentManager();
                    myContentManagerListener = new MyContentManagerListener();
                    contentManager.addContentManagerListener(myContentManagerListener);

                    myVcsManager.addVcsListener(myVcsListener);

                    Disposer.register(myProject, new Disposable(){
                        public void dispose() {
                            contentManager.removeContentManagerListener(myContentManagerListener);

                            myVcsManager.removeVcsListener(myVcsListener);
                        }
                    });

                    loadExtensionTabs();
                    myContentManager = contentManager;
                    final List<Content> ordered = doPresetOrdering(myAddedContents);
                    for(Content content: ordered) {
                        myContentManager.addContent(content);
                    }
                    myAddedContents.clear();
                    if (contentManager.getContentCount() > 0) {
                        contentManager.setSelectedContent(contentManager.getContent(0));
                    }
                    myInitializationWaiter.countDown();
                }
            }
        });
    }

    /**
     * Makes the current thread wait until the ChangesViewContentManager is initialized.
     * When it initializes, executes the given runnable.
     */
    public void executeWhenInitialized( final Runnable runnable) {
        try {
            myInitializationWaiter.await();
            runnable.run();
        }
        catch (InterruptedException e) {
            LOG.error(e);
        }
    }

    private void loadExtensionTabs() {
        final List<Content> contentList = new LinkedList<Content>();
        final ChangesViewContentEP[] contentEPs = myProject.getExtensions(ChangesViewContentEP.EP_NAME);
        for(ChangesViewContentEP ep: contentEPs) {
            final NotNullFunction<Project,Boolean> predicate = ep.newPredicateInstance(myProject);
            if (predicate == null || predicate.fun(myProject).equals(Boolean.TRUE)) {
                final Content content = ContentFactory.SERVICE.getInstance().createContent(new ContentStub(ep), ep.getTabName(), false);
                content.setCloseable(false);
                content.putUserData(myEPKey, ep);
                contentList.add(content);
            }
        }
        myAddedContents.addAll(0, contentList);
    }

    private void addExtensionTab(final ChangesViewContentEP ep) {
        final Content content = ContentFactory.SERVICE.getInstance().createContent(new ContentStub(ep), ep.getTabName(), false);
        content.setCloseable(false);
        content.putUserData(myEPKey, ep);
        addIntoCorrectPlace(content);
    }

    private void updateExtensionTabs() {
        final ChangesViewContentEP[] contentEPs = myProject.getExtensions(ChangesViewContentEP.EP_NAME);
        for(ChangesViewContentEP ep: contentEPs) {
            final NotNullFunction<Project,Boolean> predicate = ep.newPredicateInstance(myProject);
            if (predicate == null) continue;
            Content epContent = findEPContent(ep);
            final Boolean predicateResult = predicate.fun(myProject);
            if (predicateResult.equals(Boolean.TRUE) && epContent == null) {
                addExtensionTab(ep);
            }
            else if (predicateResult.equals(Boolean.FALSE) && epContent != null) {
                if (!(epContent.getComponent() instanceof ContentStub)) {
                    ep.getInstance(myProject).disposeContent();
                }
                myContentManager.removeContent(epContent, true);
            }
        }
    }

    
    private Content findEPContent(final ChangesViewContentEP ep) {
        final Content[] contents = myContentManager.getContents();
        for(Content content: contents) {
            if (content.getUserData(myEPKey) == ep) {
                return content;
            }
        }
        return null;
    }

    private void updateToolWindowAvailability() {
        final AbstractVcs[] abstractVcses = myVcsManager.getAllActiveVcss();
        myToolWindow.setAvailable(abstractVcses.length > 0, null);
    }

    public boolean isToolwindowVisible() {
        return ! myToolWindow.isDisposed() && myToolWindow.isVisible();
    }

    public void projectClosed() {
        myVcsChangeAlarm.cancelAllRequests();
    }


    public String getComponentName() {
        return "ChangesViewContentManager";
    }

    public void addContent(Content content) {
        if (myContentManager == null) {
            myAddedContents.add(content);
        }
        else {
            addIntoCorrectPlace(content);
        }
    }

    public void removeContent(final Content content) {
        if (myContentManager != null && (! myContentManager.isDisposed())) { // for unit tests
            myContentManager.removeContent(content, true);
        }
    }

    public void setSelectedContent(final Content content) {
        myContentManager.setSelectedContent(content);
    }

    
    public <T> T getActiveComponent(final Class<T> aClass) {
        final Content content = myContentManager.getSelectedContent();
        if (content != null && aClass.isInstance(content.getComponent())) {
            //noinspection unchecked
            return (T) content.getComponent();
        }
        return null;
    }

    public boolean isContentSelected(final Content content) {
        return Comparing.equal(content, myContentManager.getSelectedContent());
    }

    public void selectContent(final String tabName) {
        for(Content content: myContentManager.getContents()) {
            if (content.getDisplayName().equals(tabName)) {
                myContentManager.setSelectedContent(content);
                break;
            }
        }
    }

    private class MyVcsListener implements VcsListener {
        public void directoryMappingChanged() {
            myVcsChangeAlarm.cancelAllRequests();
            myVcsChangeAlarm.addRequest(new Runnable() {
                public void run() {
                    if (myProject.isDisposed()) return;
                    updateToolWindowAvailability();
                    updateExtensionTabs();
                }
            }, 100, ModalityState.NON_MODAL);
        }
    }

    private static class ContentStub extends JPanel {
        private final ChangesViewContentEP myEP;

        private ContentStub(final ChangesViewContentEP EP) {
            myEP = EP;
        }

        public ChangesViewContentEP getEP() {
            return myEP;
        }
    }

    private class MyContentManagerListener extends ContentManagerAdapter {
        public void selectionChanged(final ContentManagerEvent event) {
            Content content = event.getContent();
            if (content.getComponent() instanceof ContentStub) {
                ChangesViewContentEP ep = ((ContentStub) content.getComponent()).getEP();
                ChangesViewContentProvider provider = ep.getInstance(myProject);
                final JComponent contentComponent = provider.initContent();
                content.setComponent(contentComponent);
                if (contentComponent instanceof Disposable) {
                    content.setDisposer((Disposable) contentComponent);
                }
            }
        }
    }

    public static final String LOCAL_CHANGES = Registry.is("vcs.merge.toolwindows") ? "Local Changes" : "Local";
    public static final String REPOSITORY = "Repository";
    public static final String INCOMING = "Incoming";
    public static final String SHELF = "Shelf";
    private static final String[] ourPresetOrder = {LOCAL_CHANGES, REPOSITORY, INCOMING, SHELF};
    private static List<Content> doPresetOrdering(final List<Content> contents) {
        final List<Content> result = new ArrayList<Content>(contents.size());
        for (final String preset : ourPresetOrder) {
            for (Iterator<Content> iterator = contents.iterator(); iterator.hasNext();) {
                final Content current = iterator.next();
                if (preset.equals(current.getTabName())) {
                    iterator.remove();
                    result.add(current);
                }
            }
        }
        result.addAll(contents);
        return result;
    }

    private void addIntoCorrectPlace(final Content content) {
        final String name = content.getTabName();
        final Content[] contents = myContentManager.getContents();

        int idxOfBeingInserted = -1;
        for (int i = 0; i < ourPresetOrder.length; i++) {
            final String s = ourPresetOrder[i];
            if (s.equals(name)) {
                idxOfBeingInserted = i;
            }
        }
        if (idxOfBeingInserted == -1) {
            myContentManager.addContent(content);
            return;
        }

        final Set<String> existingNames = new HashSet<String>();
        for (Content existingContent : contents) {
            existingNames.add(existingContent.getTabName());
        }

        int place = idxOfBeingInserted;
        for (int i = 0; i < idxOfBeingInserted; i++) {
            if (! existingNames.contains(ourPresetOrder[i])) {
                -- place;
            }

        }
        myContentManager.addContent(content, place);
    }
}
