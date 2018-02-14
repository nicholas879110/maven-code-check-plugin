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
package com.gome.maven.ide.startup.impl;

import com.gome.maven.ide.caches.CacheUpdater;
import com.gome.maven.ide.startup.StartupManagerEx;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.notification.Notifications;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationBundle;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.*;
import com.gome.maven.openapi.project.impl.ProjectLifecycleListener;
import com.gome.maven.openapi.roots.ProjectRootManager;
//import com.gome.maven.openapi.startup.StartupActivity;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.impl.local.FileWatcher;
import com.gome.maven.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.gome.maven.openapi.vfs.newvfs.RefreshQueue;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
//import com.gome.maven.util.io.storage.HeavyProcessLatch;
import com.gome.maven.util.messages.MessageBusConnection;
//import com.gome.maven.util.ui.UIUtil;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class StartupManagerImpl extends StartupManagerEx {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.startup.impl.StartupManagerImpl");

    private final List<Runnable> myPreStartupActivities = Collections.synchronizedList(new LinkedList<Runnable>());
    private final List<Runnable> myStartupActivities = Collections.synchronizedList(new LinkedList<Runnable>());

    private final List<Runnable> myDumbAwarePostStartupActivities = Collections.synchronizedList(new LinkedList<Runnable>());
    private final List<Runnable> myNotDumbAwarePostStartupActivities = Collections.synchronizedList(new LinkedList<Runnable>());
    private boolean myPostStartupActivitiesPassed = false; // guarded by this

    @SuppressWarnings("deprecation") private final List<CacheUpdater> myCacheUpdaters = ContainerUtil.newLinkedList();

    private volatile boolean myPreStartupActivitiesPassed = false;
    private volatile boolean myStartupActivitiesRunning = false;
    private volatile boolean myStartupActivitiesPassed = false;

    private final Project myProject;

    public StartupManagerImpl(Project project) {
        myProject = project;
    }

    @Override
    public void registerPreStartupActivity( Runnable runnable) {
        LOG.assertTrue(!myPreStartupActivitiesPassed, "Registering pre startup activity that will never be run");
        myPreStartupActivities.add(runnable);
    }

    @Override
    public void registerStartupActivity( Runnable runnable) {
        LOG.assertTrue(!myStartupActivitiesPassed, "Registering startup activity that will never be run");
        myStartupActivities.add(runnable);
    }

    @Override
    public synchronized void registerPostStartupActivity( Runnable runnable) {
        LOG.assertTrue(!myPostStartupActivitiesPassed, "Registering post-startup activity that will never be run");
        (DumbService.isDumbAware(runnable) ? myDumbAwarePostStartupActivities : myNotDumbAwarePostStartupActivities).add(runnable);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void registerCacheUpdater( CacheUpdater updater) {
        LOG.assertTrue(!myStartupActivitiesPassed, CacheUpdater.class.getSimpleName() + " must be registered before startup activity finished");
        myCacheUpdaters.add(updater);
    }

    @Override
    public boolean startupActivityRunning() {
        return myStartupActivitiesRunning;
    }

    @Override
    public boolean startupActivityPassed() {
        return myStartupActivitiesPassed;
    }

    @Override
    public synchronized boolean postStartupActivityPassed() {
        return myPostStartupActivitiesPassed;
    }

    public void runStartupActivities() {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            @SuppressWarnings("SynchronizeOnThis")
            public void run() {
//                AccessToken token = HeavyProcessLatch.INSTANCE.processStarted("Running Startup Activities");
                try {
                    runActivities(myPreStartupActivities);

                    // to avoid atomicity issues if runWhenProjectIsInitialized() is run at the same time
                    synchronized (StartupManagerImpl.this) {
                        myPreStartupActivitiesPassed = true;
                        myStartupActivitiesRunning = true;
                    }

                    runActivities(myStartupActivities);

                    synchronized (StartupManagerImpl.this) {
                        myStartupActivitiesRunning = false;
                        myStartupActivitiesPassed = true;
                    }
                }
                finally {
//                    token.finish();
                }
            }
        });
    }

    public void runPostStartupActivitiesFromExtensions() {
//        for (final StartupActivity extension : Extensions.getExtensions(StartupActivity.POST_STARTUP_ACTIVITY)) {
//            final Runnable runnable = new Runnable() {
//                @Override
//                public void run() {
//                    if (!myProject.isDisposed()) {
//                        extension.runActivity(myProject);
//                    }
//                }
//            };
//            if (extension instanceof DumbAware) {
//                runActivity(runnable);
//            }
//            else {
//                queueSmartModeActivity(runnable);
//            }
//        }
    }

    // queue each activity in smart mode separately so that if one of them starts dumb mode, the next ones just wait for it to finish
    private void queueSmartModeActivity(final Runnable activity) {
        DumbService.getInstance(myProject).runWhenSmart(new Runnable() {
            @Override
            public void run() {
                runActivity(activity);
            }
        });
    }

    public void runPostStartupActivities() {
        if (postStartupActivityPassed()) {
            return;
        }

        final Application app = ApplicationManager.getApplication();

        if (!app.isHeadlessEnvironment()) {
            checkFsSanity();
            checkProjectRoots();
        }

        runActivities(myDumbAwarePostStartupActivities);

        DumbService.getInstance(myProject).runWhenSmart(new Runnable() {
            @Override
            public void run() {
                app.assertIsDispatchThread();

                // myDumbAwarePostStartupActivities might be non-empty if new activities were registered during dumb mode
                runActivities(myDumbAwarePostStartupActivities);

                //noinspection SynchronizeOnThis
                synchronized (StartupManagerImpl.this) {
                    if (!myNotDumbAwarePostStartupActivities.isEmpty()) {
                        while (!myNotDumbAwarePostStartupActivities.isEmpty()) {
                            queueSmartModeActivity(myNotDumbAwarePostStartupActivities.remove(0));
                        }

                        // return here later to set myPostStartupActivitiesPassed
                        DumbService.getInstance(myProject).runWhenSmart(this);
                    }
                    else {
                        myPostStartupActivitiesPassed = true;
                    }
                }
            }
        });

        // otherwise will be stored - we must not create config files in tests
        if (!app.isUnitTestMode()) {
            Registry.get("ide.firstStartup").setValue(false);
        }
    }

    public void scheduleInitialVfsRefresh() {
//        UIUtil.invokeLaterIfNeeded(new Runnable() {
//            @Override
//            public void run() {
                if (myProject.isDisposed()) return;

                Application app = ApplicationManager.getApplication();
                if (!app.isHeadlessEnvironment()) {
                    final long sessionId = VirtualFileManager.getInstance().asyncRefresh(null);
                    final MessageBusConnection connection = app.getMessageBus().connect();
                    connection.subscribe(ProjectLifecycleListener.TOPIC, new ProjectLifecycleListener.Adapter() {
                        @Override
                        public void afterProjectClosed( Project project) {
                            RefreshQueue.getInstance().cancelSession(sessionId);
                            connection.disconnect();
                        }
                    });
                }
                else {
                    VirtualFileManager.getInstance().syncRefresh();
                }
//            }
//        });
    }

    private void checkFsSanity() {
        try {
            String path = myProject.getProjectFilePath();
            boolean actual = FileUtil.isFileSystemCaseSensitive(path);
            LOG.info(path + " case-sensitivity: " + actual);
            if (actual != SystemInfo.isFileSystemCaseSensitive) {
                int prefix = SystemInfo.isFileSystemCaseSensitive ? 1 : 0;  // IDE=true -> FS=false -> prefix='in'
                String title = ApplicationBundle.message("fs.case.sensitivity.mismatch.title");
                String text = ApplicationBundle.message("fs.case.sensitivity.mismatch.message", prefix);
                Notifications.Bus.notify(
                        new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, title, text, NotificationType.WARNING, NotificationListener.URL_OPENING_LISTENER),
                        myProject);
            }
        }
        catch (FileNotFoundException e) {
            LOG.warn(e);
        }
    }

    private void checkProjectRoots() {
        LocalFileSystem fs = LocalFileSystem.getInstance();
        if (!(fs instanceof LocalFileSystemImpl)) return;
        FileWatcher watcher = ((LocalFileSystemImpl)fs).getFileWatcher();
        if (!watcher.isOperational()) return;
        List<String> manualWatchRoots = watcher.getManualWatchRoots();
        if (manualWatchRoots.isEmpty()) return;
        VirtualFile[] roots = ProjectRootManager.getInstance(myProject).getContentRoots();
        if (roots.length == 0) return;

        List<String> nonWatched = new SmartList<String>();
        for (VirtualFile root : roots) {
            if (!(root.getFileSystem() instanceof LocalFileSystem)) continue;
            String rootPath = root.getPath();
            for (String manualWatchRoot : manualWatchRoots) {
                if (FileUtil.isAncestor(manualWatchRoot, rootPath, false)) {
                    nonWatched.add(rootPath);
                }
            }
        }

        if (!nonWatched.isEmpty()) {
            String message = ApplicationBundle.message("watcher.non.watchable.project");
            watcher.notifyOnFailure(message, null);
            LOG.info("unwatched roots: " + nonWatched);
            LOG.info("manual watches: " + manualWatchRoots);
        }
    }

    public void startCacheUpdate() {
        try {
            DumbServiceImpl dumbService = DumbServiceImpl.getInstance(myProject);

            if (!ApplicationManager.getApplication().isUnitTestMode()) {
                // pre-startup activities have registered dumb tasks that load VFS (scanning files to index)
                // only after these tasks pass does VFS refresh make sense
                dumbService.queueTask(new DumbModeTask() {
                    @Override
                    public void performInDumbMode( ProgressIndicator indicator) {
                        scheduleInitialVfsRefresh();
                    }

                    @Override
                    public String toString() {
                        return "initial refresh";
                    }
                });
            }

            if (!myCacheUpdaters.isEmpty()) {
                dumbService.queueCacheUpdateInDumbMode(myCacheUpdaters);
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    private static void runActivities( List<Runnable> activities) {
        while (!activities.isEmpty()) {
            runActivity(activities.remove(0));
        }
    }

    private static void runActivity(Runnable runnable) {
        ProgressManager.checkCanceled();

        try {
            runnable.run();
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable ex) {
            LOG.error(ex);
        }
    }

    @Override
    public void runWhenProjectIsInitialized( final Runnable action) {
        final Application application = ApplicationManager.getApplication();
        if (application == null) return;

        //noinspection SynchronizeOnThis
        synchronized (this) {
            // in tests which simulate project opening, post-startup activities could have been run already.
            // Then we should act as if the project was initialized
            boolean initialized = myProject.isInitialized() || application.isUnitTestMode() && myPostStartupActivitiesPassed;
            if (!initialized) {
                registerPostStartupActivity(action);
                return;
            }
        }

//        UIUtil.invokeLaterIfNeeded(new Runnable() {
//            @Override
//            public void run() {
                if (!myProject.isDisposed()) {
                    action.run();
                }
//            }
//        });
    }

    public synchronized void prepareForNextTest() {
        myPreStartupActivities.clear();
        myStartupActivities.clear();
        myDumbAwarePostStartupActivities.clear();
        myNotDumbAwarePostStartupActivities.clear();
        myCacheUpdaters.clear();
    }

    public synchronized void checkCleared() {
        try {
            assert myStartupActivities.isEmpty() : "Activities: " + myStartupActivities;
            assert myDumbAwarePostStartupActivities.isEmpty() : "DumbAware Post Activities: " + myDumbAwarePostStartupActivities;
            assert myNotDumbAwarePostStartupActivities.isEmpty() : "Post Activities: " + myNotDumbAwarePostStartupActivities;
            assert myPreStartupActivities.isEmpty() : "Pre Activities: " + myPreStartupActivities;
        }
        finally {
            prepareForNextTest();
        }
    }
}
