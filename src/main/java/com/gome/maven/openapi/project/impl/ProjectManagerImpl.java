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
package com.gome.maven.openapi.project.impl;

import com.gome.maven.CommonBundle;
import com.gome.maven.conversion.ConversionResult;
import com.gome.maven.conversion.ConversionService;
import com.gome.maven.ide.AppLifecycleListener;
import com.gome.maven.ide.RecentProjectsManager;
import com.gome.maven.ide.impl.ProjectUtil;
import com.gome.maven.ide.plugins.PluginManager;
import com.gome.maven.ide.startup.impl.StartupManagerImpl;
import com.gome.maven.notification.NotificationsManager;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.*;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.application.impl.ApplicationImpl;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.components.impl.stores.*;
import com.gome.maven.openapi.components.impl.stores.ComponentStoreImpl.ReloadComponentStoreStatus;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.progress.*;
import com.gome.maven.openapi.progress.util.ProgressWindow;
import com.gome.maven.openapi.project.*;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.project.ex.ProjectManagerEx;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileEvent;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.ex.VirtualFileManagerAdapter;
import com.gome.maven.openapi.vfs.impl.local.FileWatcher;
import com.gome.maven.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.gome.maven.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.SingleAlarm;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.TimeoutUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.messages.MessageBus;
import com.gome.maven.util.ui.UIUtil;
import gnu.trove.THashSet;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@State(
        name = "ProjectManager",
        storages = {
                @Storage(
                        file = StoragePathMacros.APP_CONFIG + "/project.default.xml"
                )}
)
public class ProjectManagerImpl extends ProjectManagerEx implements PersistentStateComponent<Element>, ExportableApplicationComponent {
    private static final Logger LOG = Logger.getInstance(ProjectManagerImpl.class);

    public static final int CURRENT_FORMAT_VERSION = 4;

    private static final Key<List<ProjectManagerListener>> LISTENERS_IN_PROJECT_KEY = Key.create("LISTENERS_IN_PROJECT_KEY");

    @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
    private ProjectImpl myDefaultProject; // Only used asynchronously in save and dispose, which itself are synchronized.
    @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
    private Element myDefaultProjectRootElement; // Only used asynchronously in save and dispose, which itself are synchronized.
    private boolean myDefaultProjectConfigurationChanged;

    private final List<Project> myOpenProjects = new ArrayList<Project>();
    private Project[] myOpenProjectsArrayCache = {};
    private final List<ProjectManagerListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    private final Set<Project> myTestProjects = new THashSet<Project>();

    private final MultiMap<Project, Pair<VirtualFile, StateStorage>> myChangedProjectFiles = MultiMap.createSet();
    private final SingleAlarm myChangedFilesAlarm;
    private final List<Pair<VirtualFile, StateStorage>> myChangedApplicationFiles = new SmartList<Pair<VirtualFile, StateStorage>>();
    private final AtomicInteger myReloadBlockCount = new AtomicInteger(0);

    private final ProgressManager myProgressManager;
    private volatile boolean myDefaultProjectWasDisposed = false;

    private final Runnable restartApplicationOrReloadProjectTask = new Runnable() {
        @Override
        public void run() {
            if (isReloadUnblocked() && tryToReloadApplication()) {
                askToReloadProjectIfConfigFilesChangedExternally();
            }
        }
    };

    
    private static List<ProjectManagerListener> getListeners(Project project) {
        List<ProjectManagerListener> array = project.getUserData(LISTENERS_IN_PROJECT_KEY);
        if (array == null) return Collections.emptyList();
        return array;
    }

    /** @noinspection UnusedParameters*/
    public ProjectManagerImpl( VirtualFileManager virtualFileManager,
                              RecentProjectsManager recentProjectsManager,
                              ProgressManager progressManager) {
        myProgressManager = progressManager;
        Application app = ApplicationManager.getApplication();
        MessageBus messageBus = app.getMessageBus();

        messageBus.connect(app).subscribe(StateStorage.STORAGE_TOPIC, new StateStorage.Listener() {
            @Override
            public void storageFileChanged( VirtualFileEvent event,  StateStorage storage) {
                projectStorageFileChanged(event, storage, null);
            }
        });

        final ProjectManagerListener busPublisher = messageBus.syncPublisher(TOPIC);
        addProjectManagerListener(
                new ProjectManagerListener() {
                    @Override
                    public void projectOpened(final Project project) {
                        project.getMessageBus().connect(project).subscribe(StateStorage.PROJECT_STORAGE_TOPIC, new StateStorage.Listener() {
                            @Override
                            public void storageFileChanged( VirtualFileEvent event,  StateStorage storage) {
                                projectStorageFileChanged(event, storage, project);
                            }
                        });

                        busPublisher.projectOpened(project);
                        for (ProjectManagerListener listener : getListeners(project)) {
                            listener.projectOpened(project);
                        }
                    }

                    @Override
                    public void projectClosed(Project project) {
                        busPublisher.projectClosed(project);
                        for (ProjectManagerListener listener : getListeners(project)) {
                            listener.projectClosed(project);
                        }
                    }

                    @Override
                    public boolean canCloseProject(Project project) {
                        for (ProjectManagerListener listener : getListeners(project)) {
                            if (!listener.canCloseProject(project)) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public void projectClosing(Project project) {
                        busPublisher.projectClosing(project);
                        for (ProjectManagerListener listener : getListeners(project)) {
                            listener.projectClosing(project);
                        }
                    }
                }
        );

        virtualFileManager.addVirtualFileManagerListener(new VirtualFileManagerAdapter() {
            @Override
            public void beforeRefreshStart(boolean asynchronous) {
                blockReloadingProjectOnExternalChanges();
            }

            @Override
            public void afterRefreshFinish(boolean asynchronous) {
                unblockReloadingProjectOnExternalChanges();
            }
        });
        myChangedFilesAlarm = new SingleAlarm(restartApplicationOrReloadProjectTask, 300);
    }

    private void projectStorageFileChanged( VirtualFileEvent event,  StateStorage storage,  Project project) {
        VirtualFile file = event.getFile();
        if (!StorageUtil.isChangedByStorageOrSaveSession(event) && !(event.getRequestor() instanceof ProjectManagerImpl)) {
            registerProjectToReload(project, file, storage);
        }
    }

    @Override
    public void initComponent() { }

    @Override
    public void disposeComponent() {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        Disposer.dispose(myChangedFilesAlarm);
        if (myDefaultProject != null) {
            Disposer.dispose(myDefaultProject);

            myDefaultProject = null;
            myDefaultProjectWasDisposed = true;
        }
    }

    public static int TEST_PROJECTS_CREATED = 0;
    private static final boolean LOG_PROJECT_LEAKAGE_IN_TESTS = false;
    private static final int MAX_LEAKY_PROJECTS = 42;
    @SuppressWarnings("FieldCanBeLocal") private final Map<Project, String> myProjects = new WeakHashMap<Project, String>();

    @Override
    
    public Project newProject(final String projectName,  String filePath, boolean useDefaultProjectSettings, boolean isDummy) {
        return newProject(projectName, filePath, useDefaultProjectSettings, isDummy, ApplicationManager.getApplication().isUnitTestMode());
    }

    
    public Project newProject(final String projectName,  String filePath, boolean useDefaultProjectSettings, boolean isDummy,
                              boolean optimiseTestLoadSpeed) {
        filePath = toCanonicalName(filePath);

        //noinspection ConstantConditions
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            TEST_PROJECTS_CREATED++;
            if (LOG_PROJECT_LEAKAGE_IN_TESTS) {
                for (int i = 0; i < 42; i++) {
                    if (myProjects.size() < MAX_LEAKY_PROJECTS) break;
                    System.gc();
                    TimeoutUtil.sleep(100);
                    System.gc();
                }

                if (myProjects.size() >= MAX_LEAKY_PROJECTS) {
                    List<Project> copy = new ArrayList<Project>(myProjects.keySet());
                    myProjects.clear();
                    throw new TooManyProjectLeakedException(copy);
                }
            }
        }

        File projectFile = new File(filePath);
        if (projectFile.isFile()) {
            FileUtil.delete(projectFile);
        }
        else {
            File[] files = new File(projectFile, Project.DIRECTORY_STORE_FOLDER).listFiles();
            if (files != null) {
                for (File file : files) {
                    FileUtil.delete(file);
                }
            }
        }
        ProjectImpl project = createProject(projectName, filePath, false, optimiseTestLoadSpeed);
        try {
            initProject(project, useDefaultProjectSettings ? (ProjectImpl)getDefaultProject() : null);
            if (LOG_PROJECT_LEAKAGE_IN_TESTS) {
                myProjects.put(project, null);
            }
            return project;
        }
        catch (Throwable t) {
            LOG.info(t);
            Messages.showErrorDialog(message(t), ProjectBundle.message("project.load.default.error"));
            return null;
        }
    }

    
    private static String message(Throwable e) {
        String message = e.getMessage();
        if (message != null) return message;
        message = e.getLocalizedMessage();
        //noinspection ConstantConditions
        if (message != null) return message;
        message = e.toString();
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMessage = message(cause);
            return message + " (cause: " + causeMessage + ")";
        }

        return message;
    }

    private void initProject( ProjectImpl project,  ProjectImpl template) throws IOException {
        ProgressIndicator indicator = myProgressManager.getProgressIndicator();
        if (indicator != null && !project.isDefault()) {
            indicator.setText(ProjectBundle.message("loading.components.for", project.getName()));
            indicator.setIndeterminate(true);
        }

        ApplicationManager.getApplication().getMessageBus().syncPublisher(ProjectLifecycleListener.TOPIC).beforeProjectLoaded(project);

        boolean succeed = false;
        try {
            if (template != null) {
                project.getStateStore().loadProjectFromTemplate(template);
            }
            else {
                project.getStateStore().load();
            }
            project.loadProjectComponents();
            project.init();
            succeed = true;
        }
        finally {
            if (!succeed) {
                scheduleDispose(project);
            }
        }
    }

    private ProjectImpl createProject( String projectName,
                                       String filePath,
                                      boolean isDefault,
                                      boolean isOptimiseTestLoadSpeed) {
        return isDefault ? new DefaultProject(this, "", isOptimiseTestLoadSpeed)
                : new ProjectImpl(this, new File(filePath).getAbsolutePath(), isOptimiseTestLoadSpeed, projectName);
    }

    private static void scheduleDispose(final ProjectImpl project) {
        if (project.isDefault()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!project.isDisposed()) {
                            Disposer.dispose(project);
                        }
                    }
                });
            }
        });
    }

    @Override
    
    public Project loadProject( String filePath) throws IOException, JDOMException, InvalidDataException {
        try {
            ProjectImpl project = createProject(null, filePath, false, false);
            initProject(project, null);
            return project;
        }
        catch (Throwable t) {
            LOG.info(t);
            throw new IOException(t);
        }
    }

    
    private static String toCanonicalName( final String filePath) {
        try {
            return FileUtil.resolveShortWindowsName(filePath);
        }
        catch (IOException e) {
            // OK. File does not yet exist so it's canonical path will be equal to its original path.
        }

        return filePath;
    }

    public synchronized boolean isDefaultProjectInitialized() {
        return myDefaultProject != null;
    }

    @Override
    
    public synchronized Project getDefaultProject() {
        LOG.assertTrue(!myDefaultProjectWasDisposed, "Default project has been already disposed!");
        if (myDefaultProject == null) {
            ProgressManager.getInstance().executeNonCancelableSection(new Runnable() {
                @Override
                public void run() {
                    try {
                        myDefaultProject = createProject(null, "", true, ApplicationManager.getApplication().isUnitTestMode());
                        initProject(myDefaultProject, null);
                    }
                    catch (Throwable t) {
                        PluginManager.processException(t);
                    }
                }
            });
        }
        return myDefaultProject;
    }

    
    public Element getDefaultProjectRootElement() {
        return myDefaultProjectRootElement;
    }

    @Override
    
    public Project[] getOpenProjects() {
        synchronized (myOpenProjects) {
            if (myOpenProjectsArrayCache.length != myOpenProjects.size()) {
                LOG.error("Open projects: " + myOpenProjects + "; cache: " + Arrays.asList(myOpenProjectsArrayCache));
            }
            if (myOpenProjectsArrayCache.length > 0 && myOpenProjectsArrayCache[0] != myOpenProjects.get(0)) {
                LOG.error("Open projects cache corrupted. Open projects: " + myOpenProjects + "; cache: " + Arrays.asList(myOpenProjectsArrayCache));
            }
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                Project[] testProjects = myTestProjects.toArray(new Project[myTestProjects.size()]);
                for (Project testProject : testProjects) {
                    assert !testProject.isDisposed() : testProject;
                }
                return ArrayUtil.mergeArrays(myOpenProjectsArrayCache, testProjects);
            }
            return myOpenProjectsArrayCache;
        }
    }

    @Override
    public boolean isProjectOpened(Project project) {
        synchronized (myOpenProjects) {
            return ApplicationManager.getApplication().isUnitTestMode() && myTestProjects.contains(project) || myOpenProjects.contains(project);
        }
    }

    @Override
    public boolean openProject(final Project project) {
        if (isLight(project)) {
            throw new AssertionError("must not open light project");
        }

        final Application application = ApplicationManager.getApplication();
        if (!application.isUnitTestMode() && !((ProjectEx)project).getStateStore().checkVersion()) {
            return false;
        }

        synchronized (myOpenProjects) {
            if (myOpenProjects.contains(project)) {
                return false;
            }
            myOpenProjects.add(project);
            cacheOpenProjects();
        }

        fireProjectOpened(project);
        DumbService.getInstance(project).queueTask(new DumbModeTask() {
            @Override
            public void performInDumbMode( ProgressIndicator indicator) {
                waitForFileWatcher(indicator);
            }

            @Override
            public String toString() {
                return "wait for file watcher";
            }
        });

        final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(project);
        boolean ok = myProgressManager.runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                startupManager.runStartupActivities();

                // dumb mode should start before post-startup activities
                // only when startCacheUpdate is called from UI thread, we can guarantee that
                // when the method returns, the application has entered dumb mode
                UIUtil.invokeAndWaitIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        startupManager.startCacheUpdate();
                    }
                });

                startupManager.runPostStartupActivitiesFromExtensions();

                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        if (!project.isDisposed()) {
                            startupManager.runPostStartupActivities();
                        }
                    }
                });
            }
        }, ProjectBundle.message("project.load.progress"), canCancelProjectLoading(), project);

        if (!ok) {
            closeProject(project, false, false, true);
            notifyProjectOpenFailed();
            return false;
        }

        if (!application.isHeadlessEnvironment() && !application.isUnitTestMode()) {
            // should be invoked last
            startupManager.runWhenProjectIsInitialized(new Runnable() {
                @Override
                public void run() {
                    TrackingPathMacroSubstitutor substitutor = ((ProjectEx)project).getStateStore().getStateStorageManager().getMacroSubstitutor();
                    if (substitutor != null) {
                        StorageUtil.notifyUnknownMacros(substitutor, project, null);
                    }
                }
            });
        }

        return true;
    }

    private static boolean canCancelProjectLoading() {
        ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        return !(indicator instanceof NonCancelableSection);
    }

    private void cacheOpenProjects() {
        myOpenProjectsArrayCache = myOpenProjects.toArray(new Project[myOpenProjects.size()]);
    }

    private static void waitForFileWatcher(ProgressIndicator indicator) {
        LocalFileSystem fs = LocalFileSystem.getInstance();
        if (!(fs instanceof LocalFileSystemImpl)) return;

        final FileWatcher watcher = ((LocalFileSystemImpl)fs).getFileWatcher();
        if (!watcher.isOperational() || !watcher.isSettingRoots()) return;

        LOG.info("FW/roots waiting started");
        indicator.setIndeterminate(true);
        indicator.setText(ProjectBundle.message("project.load.waiting.watcher"));
        if (indicator instanceof ProgressWindow) {
            ((ProgressWindow)indicator).setCancelButtonText(CommonBundle.message("button.skip"));
        }
        while (watcher.isSettingRoots() && !indicator.isCanceled()) {
            TimeoutUtil.sleep(10);
        }
        LOG.info("FW/roots waiting finished");
    }

    @Override
    public Project loadAndOpenProject( final String filePath) throws IOException {
        final Project project = convertAndLoadProject(filePath);
        if (project == null) {
            WelcomeFrame.showIfNoProjectOpened();
            return null;
        }

        // todo unify this logic with PlatformProjectOpenProcessor
        if (!openProject(project)) {
            WelcomeFrame.showIfNoProjectOpened();
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    Disposer.dispose(project);
                }
            });
        }

        return project;
    }

    /**
     * Converts and loads the project at the specified path.
     *
     * @param filePath the path to open the project.
     * @return the project, or null if the user has cancelled opening the project.
     */
    @Override
    
    public Project convertAndLoadProject(String filePath) throws IOException {
        final String fp = toCanonicalName(filePath);
        final ConversionResult conversionResult = ConversionService.getInstance().convert(fp);
        if (conversionResult.openingIsCanceled()) {
            return null;
        }

        final Project project;
        try {
            project = loadProjectWithProgress(filePath);
            if (project == null) return null;
        }
        catch (IOException e) {
            LOG.info(e);
            throw e;
        }
        catch (Throwable t) {
            LOG.info(t);
            throw new IOException(t);
        }

        if (!conversionResult.conversionNotNeeded()) {
            StartupManager.getInstance(project).registerPostStartupActivity(new Runnable() {
                @Override
                public void run() {
                    conversionResult.postStartupActivity(project);
                }
            });
        }
        return project;
    }

    /**
     * Opens the project at the specified path.
     *
     * @param filePath the path to open the project.
     * @return the project, or null if the user has cancelled opening the project.
     */
    
    private Project loadProjectWithProgress( final String filePath) throws IOException {
        final ProjectImpl project = createProject(null, toCanonicalName(filePath), false, false);
        try {
            myProgressManager.runProcessWithProgressSynchronously(new ThrowableComputable<Project, IOException>() {
                @Override
                
                public Project compute() throws IOException {
                    initProject(project, null);
                    return project;
                }
            }, ProjectBundle.message("project.load.progress"), canCancelProjectLoading(), project);
        }
        catch (StateStorageException e) {
            throw new IOException(e);
        }
        catch (ProcessCanceledException ignore) {
            return null;
        }

        return project;
    }

    private static void notifyProjectOpenFailed() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(AppLifecycleListener.TOPIC).projectOpenFailed();
        WelcomeFrame.showIfNoProjectOpened();
    }

    private void askToReloadProjectIfConfigFilesChangedExternally() {
        Set<Project> projects;
        synchronized (myChangedProjectFiles) {
            if (myChangedProjectFiles.isEmpty()) {
                return;
            }
            projects = new THashSet<Project>(myChangedProjectFiles.keySet());
        }

        List<Project> projectsToReload = new SmartList<Project>();
        for (Project project : projects) {
            if (shouldReloadProject(project)) {
                projectsToReload.add(project);
            }
        }

        for (Project project : projectsToReload) {
            doReloadProject(project);
        }
    }

    private boolean tryToReloadApplication() {
        if (ApplicationManager.getApplication().isDisposed()) {
            return false;
        }
        if (myChangedApplicationFiles.isEmpty()) {
            return true;
        }

        Set<Pair<VirtualFile, StateStorage>> causes = new THashSet<Pair<VirtualFile, StateStorage>>(myChangedApplicationFiles);
        myChangedApplicationFiles.clear();

        ReloadComponentStoreStatus status = ComponentStoreImpl.reloadStore(causes, ((ApplicationImpl)ApplicationManager.getApplication()).getStateStore());
        if (status == ReloadComponentStoreStatus.RESTART_AGREED) {
            ApplicationManagerEx.getApplicationEx().restart(true);
            return false;
        }
        else {
            return status == ReloadComponentStoreStatus.SUCCESS || status == ReloadComponentStoreStatus.RESTART_CANCELLED;
        }
    }

    private boolean shouldReloadProject( Project project) {
        if (project.isDisposed()) {
            return false;
        }

        Collection<Pair<VirtualFile, StateStorage>> causes = new SmartList<Pair<VirtualFile, StateStorage>>();
        Collection<Pair<VirtualFile, StateStorage>> changes;
        synchronized (myChangedProjectFiles) {
            changes = myChangedProjectFiles.remove(project);
            if (!ContainerUtil.isEmpty(changes)) {
                for (Pair<VirtualFile, StateStorage> change : changes) {
                    causes.add(change);
                }
            }
        }

        if (causes.isEmpty()) {
            return false;
        }
        return ComponentStoreImpl.reloadStore(causes, ((ProjectEx)project).getStateStore()) == ReloadComponentStoreStatus.RESTART_AGREED;
    }

    @Override
    public void blockReloadingProjectOnExternalChanges() {
        myReloadBlockCount.incrementAndGet();
    }

    @Override
    public void unblockReloadingProjectOnExternalChanges() {
        if (myReloadBlockCount.decrementAndGet() == 0 && myChangedFilesAlarm.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(restartApplicationOrReloadProjectTask, ModalityState.NON_MODAL);
        }
    }

    private boolean isReloadUnblocked() {
        int count = myReloadBlockCount.get();
        if (LOG.isDebugEnabled()) {
            LOG.debug("[RELOAD] myReloadBlockCount = " + count);
        }
        return count == 0;
    }

    @Override
    public void openTestProject( final Project project) {
        synchronized (myOpenProjects) {
            assert ApplicationManager.getApplication().isUnitTestMode();
            assert !project.isDisposed() : "Must not open already disposed project";
            myTestProjects.add(project);
        }
    }

    @Override
    public Collection<Project> closeTestProject( Project project) {
        synchronized (myOpenProjects) {
            assert ApplicationManager.getApplication().isUnitTestMode();
            myTestProjects.remove(project);
            return myTestProjects;
        }
    }

    @Override
    public void saveChangedProjectFile( VirtualFile file,  Project project) {
        StateStorageManager storageManager = ((ProjectEx)project).getStateStore().getStateStorageManager();
        String fileSpec = storageManager.collapseMacros(file.getPath());
        Couple<Collection<FileBasedStorage>> storages = storageManager.getCachedFileStateStorages(Collections.singletonList(fileSpec), Collections.<String>emptyList());
        FileBasedStorage storage = ContainerUtil.getFirstItem(storages.first);
        // if empty, so, storage is not yet loaded, so, we don't have to reload
        if (storage != null) {
            registerProjectToReload(project, file, storage);
        }
    }

    private void registerProjectToReload( Project project,  VirtualFile file,  StateStorage storage) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("[RELOAD] Registering project to reload: " + file, new Exception());
        }

        if (project == null) {
            myChangedApplicationFiles.add(Pair.create(file, storage));
        }
        else {
            myChangedProjectFiles.putValue(project, Pair.create(file, storage));
        }

        if (storage instanceof StateStorageBase) {
            ((StateStorageBase)storage).disableSaving();
        }

        if (isReloadUnblocked()) {
            myChangedFilesAlarm.cancelAndRequest();
        }
    }

    @Override
    public void reloadProject( Project project) {
        myChangedProjectFiles.remove(project);
        doReloadProject(project);
    }

    private static void doReloadProject( Project project) {
        final Ref<Project> projectRef = Ref.create(project);
        ProjectReloadState.getInstance(project).onBeforeAutomaticProjectReload();
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                LOG.debug("Reloading project.");
                Project project = projectRef.get();
                // Let it go
                projectRef.set(null);

                if (project.isDisposed()) {
                    return;
                }

                // must compute here, before project dispose
                String presentableUrl = project.getPresentableUrl();
                if (!ProjectUtil.closeAndDispose(project)) {
                    return;
                }

                ProjectUtil.openProject(presentableUrl, null, true);
            }
        }, ModalityState.NON_MODAL);
    }

    @Override
    public boolean closeProject( final Project project) {
        return closeProject(project, true, false, true);
    }

    public boolean closeProject( final Project project, final boolean save, final boolean dispose, boolean checkCanClose) {
        if (isLight(project)) {
            throw new AssertionError("must not close light project");
        }
        if (!isProjectOpened(project)) return true;
        if (checkCanClose && !canClose(project)) return false;
        final ShutDownTracker shutDownTracker = ShutDownTracker.getInstance();
        shutDownTracker.registerStopperThread(Thread.currentThread());
        try {
            if (save) {
                FileDocumentManager.getInstance().saveAllDocuments();
                project.save();
            }

            if (checkCanClose && !ensureCouldCloseIfUnableToSave(project)) {
                return false;
            }

            fireProjectClosing(project); // somebody can start progress here, do not wrap in write action

            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    synchronized (myOpenProjects) {
                        myOpenProjects.remove(project);
                        cacheOpenProjects();
                        myTestProjects.remove(project);
                    }

                    myChangedProjectFiles.remove(project);

                    fireProjectClosed(project);

                    if (dispose) {
                        Disposer.dispose(project);
                    }
                }
            });
        }
        finally {
            shutDownTracker.unregisterStopperThread(Thread.currentThread());
        }

        return true;
    }

    public static boolean isLight( Project project) {
        return project instanceof ProjectImpl && ((ProjectImpl)project).isLight();
    }

    @Override
    public boolean closeAndDispose( final Project project) {
        return closeProject(project, true, true, true);
    }

    private void fireProjectClosing(Project project) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("enter: fireProjectClosing()");
        }

        for (ProjectManagerListener listener : myListeners) {
            try {
                listener.projectClosing(project);
            }
            catch (Exception e) {
                LOG.error("From listener "+listener+" ("+listener.getClass()+")", e);
            }
        }
    }

    @Override
    public void addProjectManagerListener( ProjectManagerListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void addProjectManagerListener( final ProjectManagerListener listener,  Disposable parentDisposable) {
        addProjectManagerListener(listener);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removeProjectManagerListener(listener);
            }
        });
    }

    @Override
    public void removeProjectManagerListener( ProjectManagerListener listener) {
        boolean removed = myListeners.remove(listener);
        LOG.assertTrue(removed);
    }

    @Override
    public void addProjectManagerListener( Project project,  ProjectManagerListener listener) {
        List<ProjectManagerListener> listeners = project.getUserData(LISTENERS_IN_PROJECT_KEY);
        if (listeners == null) {
            listeners = ((UserDataHolderEx)project)
                    .putUserDataIfAbsent(LISTENERS_IN_PROJECT_KEY, ContainerUtil.<ProjectManagerListener>createLockFreeCopyOnWriteList());
        }
        listeners.add(listener);
    }

    @Override
    public void removeProjectManagerListener( Project project,  ProjectManagerListener listener) {
        List<ProjectManagerListener> listeners = project.getUserData(LISTENERS_IN_PROJECT_KEY);
        LOG.assertTrue(listeners != null);
        boolean removed = listeners.remove(listener);
        LOG.assertTrue(removed);
    }

    private void fireProjectOpened(Project project) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("projectOpened");
        }

        for (ProjectManagerListener listener : myListeners) {
            try {
                listener.projectOpened(project);
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    private void fireProjectClosed(Project project) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("projectClosed");
        }

        for (ProjectManagerListener listener : myListeners) {
            try {
                listener.projectClosed(project);
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    @Override
    public boolean canClose(Project project) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("enter: canClose()");
        }

        for (ProjectManagerListener listener : myListeners) {
            try {
                if (!listener.canCloseProject(project)) return false;
            }
            catch (Throwable e) {
                LOG.warn(e); // DO NOT LET ANY PLUGIN to prevent closing due to exception
            }
        }

        return true;
    }

    private static boolean ensureCouldCloseIfUnableToSave( final Project project) {
        final ProjectImpl.UnableToSaveProjectNotification[] notifications =
                NotificationsManager.getNotificationsManager().getNotificationsOfType(ProjectImpl.UnableToSaveProjectNotification.class, project);
        if (notifications.length == 0) return true;

        final String fileNames = StringUtil.join(notifications[0].getFileNames(), "\n");

        final String msg = String.format("%s was unable to save some project files,\nare you sure you want to close this project anyway?",
                ApplicationNamesInfo.getInstance().getProductName());
        return Messages.showDialog(project, msg, "Unsaved Project", "Read-only files:\n\n" + fileNames, new String[]{"Yes", "No"}, 0, 1,
                Messages.getWarningIcon()) == 0;
    }


    
    @Override
    public Element getState() {
        if (myDefaultProject != null) {
            myDefaultProject.save();
        }

        if (!myDefaultProjectConfigurationChanged) {
            // we are not ready to save
            return null;
        }

        Element element = new Element("state");
        myDefaultProjectRootElement.detach();
        element.addContent(myDefaultProjectRootElement);
        return element;
    }

    @Override
    public void loadState(Element state) {
        myDefaultProjectRootElement = state.getChild("defaultProject");
        if (myDefaultProjectRootElement != null) {
            myDefaultProjectRootElement.detach();
        }
        myDefaultProjectConfigurationChanged = false;
    }

    public void setDefaultProjectRootElement( Element defaultProjectRootElement) {
        myDefaultProjectRootElement = defaultProjectRootElement;
        myDefaultProjectConfigurationChanged = true;
    }

    @Override
    
    public String getComponentName() {
        return "ProjectManager";
    }

    @Override
    
    public File[] getExportFiles() {
        return new File[]{PathManager.getOptionsFile("project.default")};
    }

    @Override
    
    public String getPresentableName() {
        return ProjectBundle.message("project.default.settings");
    }
}
