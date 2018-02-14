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
package com.gome.maven.openapi.application.impl;

import com.gome.maven.BundleBase;
import com.gome.maven.CommonBundle;
import com.gome.maven.diagnostic.ThreadDumper;
import com.gome.maven.ide.*;
import com.gome.maven.ide.plugins.IdeaPluginDescriptor;
import com.gome.maven.ide.plugins.PluginManagerCore;
import com.gome.maven.idea.IdeaApplication;
import com.gome.maven.idea.Main;
import com.gome.maven.idea.StartupUtil;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.*;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.components.StateStorageException;
import com.gome.maven.openapi.components.impl.ApplicationPathMacroManager;
import com.gome.maven.openapi.components.impl.PlatformComponentManagerImpl;
import com.gome.maven.openapi.components.impl.stores.IApplicationStore;
import com.gome.maven.openapi.components.impl.stores.IComponentStore;
import com.gome.maven.openapi.components.impl.stores.StoreUtil;
import com.gome.maven.openapi.components.impl.stores.StoresFactory;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.progress.EmptyProgressIndicator;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.util.ProgressWindow;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.project.ex.ProjectManagerEx;
import com.gome.maven.openapi.project.impl.ProjectManagerImpl;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.MessageDialogBuilder;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.wm.IdeFrame;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.psi.PsiLock;
import com.gome.maven.ui.Splash;
import com.gome.maven.util.*;
import com.gome.maven.util.containers.Stack;
import com.gome.maven.util.io.storage.HeavyProcessLatch;
import com.gome.maven.util.ui.UIUtil;
import org.jetbrains.ide.PooledThreadExecutor;
import org.picocontainer.MutablePicoContainer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ApplicationImpl extends PlatformComponentManagerImpl implements ApplicationEx {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.application.impl.ApplicationImpl");
    private final ModalityState MODALITY_STATE_NONE = ModalityState.NON_MODAL;

    // about writer preference: the way the j.u.c.l.ReentrantReadWriteLock.NonfairSync is implemented, the
    // writer thread will be always at the queue head and therefore, j.u.c.l.ReentrantReadWriteLock.NonfairSync.readerShouldBlock()
    // will return true if the write action is pending, exactly as we need
    private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock(false);

    private final ModalityInvokator myInvokator = new ModalityInvokatorImpl();

    private final EventDispatcher<ApplicationListener> myDispatcher = EventDispatcher.create(ApplicationListener.class);

    private final boolean myTestModeFlag;
    private final boolean myHeadlessMode;
    private final boolean myCommandLineMode;

    private final boolean myIsInternal;
    private final String myName;

    private final Stack<Class> myWriteActionsStack = new Stack<Class>(); // accessed from EDT only, no need to sync

    private int myInEditorPaintCounter; // EDT only
    private final long myStartTime;
    
    private final Splash mySplash;
    private boolean myDoNotSave;
    private volatile boolean myDisposeInProgress;

    private final Disposable myLastDisposable = Disposer.newDisposable(); // will be disposed last

    private final AtomicBoolean mySaveSettingsIsInProgress = new AtomicBoolean(false);

    private final ExecutorService ourThreadExecutorsService = PooledThreadExecutor.INSTANCE;
    private boolean myIsFiringLoadingEvent = false;
    private boolean myLoaded = false;
     private static final String WAS_EVER_SHOWN = "was.ever.shown";

    private Boolean myActive;

    private static final int IS_EDT_FLAG = 1<<30; // we don't mess with sign bit since we want to do arithmetic
    private static final int IS_READ_LOCK_ACQUIRED_FLAG = 1<<29;
    private static class Status {
        // higher three bits are for IS_* flags
        // lower bits are for edtSafe counter
        private int flags;
    }

    private static final ThreadLocal<Status> status = new ThreadLocal<Status>(){
        @Override
        protected Status initialValue() {
            Status status = new Status();
            status.flags = BitUtil.set(status.flags, IS_EDT_FLAG, EventQueue.isDispatchThread());
            return status;
        }
    };
    private static Status getStatus() {
        return status.get();
    }
    private static void setReadLockAcquired(Status status, boolean acquired) {
        status.flags = BitUtil.set(status.flags, IS_READ_LOCK_ACQUIRED_FLAG, acquired);
    }

    private static final ModalityState ANY = new ModalityState() {
        @Override
        public boolean dominates( ModalityState anotherState) {
            return false;
        }

        
        @Override
        public String toString() {
            return "ANY";
        }
    };

    @Override
    protected void bootstrapPicoContainer( String name) {
        super.bootstrapPicoContainer(name);
        getPicoContainer().registerComponentImplementation(IComponentStore.class, StoresFactory.getApplicationStoreClass());
        getPicoContainer().registerComponentImplementation(ApplicationPathMacroManager.class);
    }

    
    public IApplicationStore getStateStore() {
        return (IApplicationStore)getPicoContainer().getComponentInstance(IComponentStore.class);
    }

    @Override
    public void initializeComponent( Object component, boolean service) {
        getStateStore().initComponent(component, service);
    }

    public ApplicationImpl(boolean isInternal,
                           boolean isUnitTestMode,
                           boolean isHeadless,
                           boolean isCommandLine,
                            String appName,
                            Splash splash) {
        super(null);

        ApplicationManager.setApplication(this, myLastDisposable); // reset back to null only when all components already disposed

        getPicoContainer().registerComponentInstance(Application.class, this);

        BundleBase.assertKeyIsFound = IconLoader.STRICT = isUnitTestMode || isInternal;

        AWTExceptionHandler.register(); // do not crash AWT on exceptions

        String debugDisposer = System.getProperty("idea.disposer.debug");
        Disposer.setDebugMode((isInternal || isUnitTestMode || "on".equals(debugDisposer)) && !"off".equals(debugDisposer));

        myStartTime = System.currentTimeMillis();
        mySplash = splash;
        myName = appName;

        myIsInternal = isInternal;
        myTestModeFlag = isUnitTestMode;
        myHeadlessMode = isHeadless;
        myCommandLineMode = isCommandLine;

        myDoNotSave = isUnitTestMode || isHeadless;

        loadApplicationComponents();

        if (myTestModeFlag) {
            registerShutdownHook();
        }

        if (!isUnitTestMode && !isHeadless) {
            Disposer.register(this, Disposer.newDisposable(), "ui");

            StartupUtil.addExternalInstanceListener(new Consumer<List<String>>() {
                @Override
                public void consume(final List<String> args) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            LOG.info("ApplicationImpl.externalInstanceListener invocation");
                            String currentDirectory = args.isEmpty() ? null : args.get(0);
                            List<String> realArgs = args.isEmpty() ? args : args.subList(1, args.size());
                            final Project project = CommandLineProcessor.processExternalCommandLine(realArgs, currentDirectory);
                            final JFrame frame;
                            if (project != null) {
                                frame = (JFrame)WindowManager.getInstance().getIdeFrame(project);
                            }
                            else {
                                frame = WindowManager.getInstance().findVisibleFrame();
                            }
                            if (frame != null) frame.requestFocus();
                        }
                    });
                }
            });

            WindowsCommandLineProcessor.LISTENER = new WindowsCommandLineListener() {
                @Override
                public void processWindowsLauncherCommandLine(final String currentDirectory, final String commandLine) {
                    LOG.info("Received external Windows command line: current directory " + currentDirectory + ", command line " + commandLine);
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final List<String> args = StringUtil.splitHonorQuotes(commandLine, ' ');
                            args.remove(0);   // process name
                            CommandLineProcessor.processExternalCommandLine(args, currentDirectory);
                        }
                    });
                }
            };
        }
        if (isUnitTestMode && IdeaApplication.getInstance() == null) {
            String[] args = {"inspect", "", "", ""};
            Main.setFlags(args); // set both isHeadless and isCommandLine to true
            System.setProperty(IdeaApplication.IDEA_IS_UNIT_TEST, Boolean.TRUE.toString());
            assert Main.isHeadless();
            assert Main.isCommandLine();
            new IdeaApplication(args);
        }
    }

    private void registerShutdownHook() {
        ShutDownTracker.getInstance().registerShutdownTask(new Runnable() {
            @Override
            public void run() {
                if (isDisposed() || myDisposeInProgress) {
                    return;
                }
                ShutDownTracker.invokeAndWait(isUnitTestMode(), true, new Runnable() {
                    @Override
                    public void run() {
                        if (ApplicationManager.getApplication() != ApplicationImpl.this) return;
                        try {
                            myDisposeInProgress = true;
                            saveAll();
                        }
                        finally {
                            if (!disposeSelf(true)) {
                                myDisposeInProgress = false;
                            }
                        }
                    }
                });
            }
        });
    }

    private boolean disposeSelf(final boolean checkCanCloseProject) {
        final ProjectManagerImpl manager = (ProjectManagerImpl)ProjectManagerEx.getInstanceEx();
        if (manager != null) {
            final boolean[] canClose = {true};
            for (final Project project : manager.getOpenProjects()) {
                try {
                    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                        @Override
                        public void run() {
                            if (!manager.closeProject(project, true, true, checkCanCloseProject)) {
                                canClose[0] = false;
                            }
                        }
                    }, ApplicationBundle.message("command.exit"), null);
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
                if (!canClose[0]) {
                    return false;
                }
            }
        }
        runWriteAction(new Runnable() {
            @Override
            public void run() {
                Disposer.dispose(ApplicationImpl.this);
            }
        });

        Disposer.assertIsEmpty();
        return true;
    }

    @Override
    
    public String getName() {
        return myName;
    }

    @Override
    public boolean holdsReadLock() {
        return holdsReadLock(getStatus());
    }

    private static boolean holdsReadLock(Status status) {
        return BitUtil.isSet(status.flags, IS_READ_LOCK_ACQUIRED_FLAG);
    }

    private void loadApplicationComponents() {
        PluginManagerCore.initPlugins(mySplash);
        IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
        for (IdeaPluginDescriptor plugin : plugins) {
            if (!PluginManagerCore.shouldSkipPlugin(plugin)) {
                loadComponentsConfiguration(plugin.getAppComponents(), plugin, false);
            }
        }
    }

    @Override
    protected synchronized Object createComponent( Class componentInterface) {
        Object component = super.createComponent(componentInterface);
        if (mySplash != null) {
            mySplash.showProgress("", 0.65f + getPercentageOfComponentsLoaded() * 0.35f);
        }
        return component;
    }

    
    @Override
    protected MutablePicoContainer createPicoContainer() {
        return Extensions.getRootArea().getPicoContainer();
    }

    @Override
    public boolean isInternal() {
        return myIsInternal;
    }

    @Override
    public boolean isEAP() {
        return ApplicationInfoImpl.getShadowInstance().isEAP();
    }

    @Override
    public boolean isUnitTestMode() {
        return myTestModeFlag;
    }

    @Override
    public boolean isHeadlessEnvironment() {
        return myHeadlessMode;
    }

    @Override
    public boolean isCommandLine() {
        return myCommandLineMode;
    }

    
    @Override
    public Future<?> executeOnPooledThread( final Runnable action) {
        return ourThreadExecutorsService.submit(new Runnable() {
            @Override
            public void run() {
                assert !isReadAccessAllowed(): describe(Thread.currentThread());
                try {
                    action.run();
                }
                catch (ProcessCanceledException e) {
                    // ignore
                }
                catch (Throwable t) {
                    LOG.error(t);
                }
                finally {
                    //ReflectionUtil.resetThreadLocals();
                    Thread.interrupted(); // reset interrupted status
                    assert !isReadAccessAllowed(): describe(Thread.currentThread());
                }
            }
        });
    }

    
    @Override
    public <T> Future<T> executeOnPooledThread( final Callable<T> action) {
        return ourThreadExecutorsService.submit(new Callable<T>() {
            @Override
            public T call() {
                assert !isReadAccessAllowed(): describe(Thread.currentThread());
                try {
                    return action.call();
                }
                catch (ProcessCanceledException e) {
                    // ignore
                }
                catch (Throwable t) {
                    LOG.error(t);
                }
                finally {
                    //ReflectionUtil.resetThreadLocals();
                    Thread.interrupted(); // reset interrupted status
                    assert !isReadAccessAllowed(): describe(Thread.currentThread());
                }
                return null;
            }
        });
    }

    @Override
    public boolean isDispatchThread() {
        return isDispatchThread(getStatus());
    }

    private static boolean isDispatchThread(Status status) {
        return BitUtil.isSet(status.flags, IS_EDT_FLAG);
    }

    @Override
    
    public ModalityInvokator getInvokator() {
        return myInvokator;
    }


    @Override
    public void invokeLater( final Runnable runnable) {
        myInvokator.invokeLater(runnable);
    }

    @Override
    public void invokeLater( final Runnable runnable,  final Condition expired) {
        myInvokator.invokeLater(runnable, expired);
    }

    @Override
    public void invokeLater( final Runnable runnable,  final ModalityState state) {
        myInvokator.invokeLater(runnable, state);
    }

    @Override
    public void invokeLater( final Runnable runnable,  final ModalityState state,  final Condition expired) {
        myInvokator.invokeLater(runnable, state, expired);
    }

    @Override
    public void load( String optionsPath) throws IOException {
        load(PathManager.getConfigPath(), optionsPath == null ? PathManager.getOptionsPath() : optionsPath);
    }

    public void load( String configPath,  String optionsPath) throws IOException {
        IApplicationStore store = getStateStore();
        store.setOptionsPath(optionsPath);
        store.setConfigPath(configPath);

        myIsFiringLoadingEvent = true;
        try {
            fireBeforeApplicationLoaded();
        }
        finally {
            myIsFiringLoadingEvent = false;
        }

        AccessToken token = HeavyProcessLatch.INSTANCE.processStarted("Loading application components");
        try {
            store.load();
        }
        catch (StateStorageException e) {
            throw new IOException(e.getMessage());
        }
        finally {
            token.finish();
        }
        myLoaded = true;

        createLocatorFile();
    }

    private static void createLocatorFile() {
        File locatorFile = new File(PathManager.getSystemPath() + "/" + ApplicationEx.LOCATOR_FILE_NAME);
        try {
            byte[] data = PathManager.getHomePath().getBytes(CharsetToolkit.UTF8_CHARSET);
            FileUtil.writeToFile(locatorFile, data);
        }
        catch (IOException e) {
            LOG.warn("can't store a location in '" + locatorFile + "'", e);
        }
    }

    @Override
    public boolean isLoaded() {
        return myLoaded;
    }

    @Override
    protected <T> T getComponentFromContainer( final Class<T> interfaceClass) {
        if (myIsFiringLoadingEvent) {
            return null;
        }
        return super.getComponentFromContainer(interfaceClass);
    }

    private void fireBeforeApplicationLoaded() {
        for (ApplicationLoadListener listener : ApplicationLoadListener.EP_NAME.getExtensions()) {
            try {
                listener.beforeApplicationLoaded(this);
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    @Override
    public void dispose() {
        fireApplicationExiting();

        ShutDownTracker.getInstance().ensureStopperThreadsFinished();

        disposeComponents();

        ourThreadExecutorsService.shutdownNow();
        super.dispose();
        Disposer.dispose(myLastDisposable); // dispose it last
    }

    @Override
    public boolean runProcessWithProgressSynchronously( final Runnable process,
                                                        String progressTitle,
                                                       boolean canBeCanceled,
                                                       Project project) {
        return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, null);
    }

    @Override
    public boolean runProcessWithProgressSynchronously( final Runnable process,
                                                        final String progressTitle,
                                                       final boolean canBeCanceled,
                                                        final Project project,
                                                       final JComponent parentComponent) {
        return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, parentComponent, null);
    }

    @Override
    public boolean runProcessWithProgressSynchronously( final Runnable process,
                                                        final String progressTitle,
                                                       final boolean canBeCanceled,
                                                        final Project project,
                                                       final JComponent parentComponent,
                                                       final String cancelText) {
        assertIsDispatchThread();
        boolean writeAccessAllowed = isInsideWriteActionEDTOnly();
        if (writeAccessAllowed // Disallow running process in separate thread from under write action.
                // The thread will deadlock trying to get read action otherwise.
                || isHeadlessEnvironment() && !isUnitTestMode()
                ) {
            LOG.debug("Starting process with progress from within write action makes no sense");
            try {
                ProgressManager.getInstance().runProcess(process, new EmptyProgressIndicator());
            }
            catch (ProcessCanceledException e) {
                // ok to ignore.
                return false;
            }
            return true;
        }

        final ProgressWindow progress = new ProgressWindow(canBeCanceled, false, project, parentComponent, cancelText);
        // in case of abrupt application exit when 'ProgressManager.getInstance().runProcess(process, progress)' below
        // does not have a chance to run, and as a result the progress won't be disposed
        Disposer.register(this, progress);

        progress.setTitle(progressTitle);

        final AtomicBoolean threadStarted = new AtomicBoolean();
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProgressManager.getInstance().runProcess(process, progress);
                        }
                        catch (ProcessCanceledException e) {
                            progress.cancel();
                            // ok to ignore.
                        }
                        catch (RuntimeException e) {
                            progress.cancel();
                            throw e;
                        }
                    }
                });
                threadStarted.set(true);
            }
        });

        progress.startBlocking();

        LOG.assertTrue(threadStarted.get());
        LOG.assertTrue(!progress.isRunning());

        return !progress.isCanceled();
    }

    @Override
    public void invokeAndWait( Runnable runnable,  ModalityState modalityState) {
        Status status = getStatus();
        if (isDispatchThread(status)) {
            runnable.run();
            return;
        }

        if (holdsReadLock()) {
            LOG.error("Calling invokeAndWait from read-action leads to possible deadlock.");
        }

        LaterInvocator.invokeAndWait(runnable, modalityState);
    }

    @Override
    
    public ModalityState getCurrentModalityState() {
        Object[] entities = LaterInvocator.getCurrentModalEntities();
        return entities.length > 0 ? new ModalityStateEx(entities) : getNoneModalityState();
    }

    @Override
    
    public ModalityState getModalityStateForComponent( Component c) {
        Window window = UIUtil.getWindow(c);
        if (window == null) return getNoneModalityState(); //?
        return LaterInvocator.modalityStateForWindow(window);
    }

    @Override
    
    public ModalityState getAnyModalityState() {
        return ANY;
    }

    @Override
    
    public ModalityState getDefaultModalityState() {
        if (isDispatchThread()) {
            return getCurrentModalityState();
        }
        ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
        return progress == null ? getNoneModalityState() : progress.getModalityState();
    }

    @Override
    
    public ModalityState getNoneModalityState() {
        return MODALITY_STATE_NONE;
    }

    @Override
    public long getStartTime() {
        return myStartTime;
    }

    @Override
    public long getIdleTime() {
        assertIsDispatchThread();
        return IdeEventQueue.getInstance().getIdleTime();
    }

    @Override
    public void exit() {
        exit(false, false);
    }

    @Override
    public void exit(boolean force, final boolean exitConfirmed) {
        exit(false, exitConfirmed, true, false);
    }

    @Override
    public void restart() {
        restart(false);
    }

    @Override
    public void restart(final boolean exitConfirmed) {
        exit(false, exitConfirmed, true, true);
    }

    /*
     * There are two ways we can get an exit notification.
     *  1. From user input i.e. ExitAction
     *  2. From the native system.
     *  We should not process any quit notifications if we are handling another one
     *
     *  Note: there are possible scenarios when we get a quit notification at a moment when another
     *  quit message is shown. In that case, showing multiple messages sounds contra-intuitive as well
     */
    private static volatile boolean exiting = false;

    public void exit(final boolean force, final boolean exitConfirmed, final boolean allowListenersToCancel, final boolean restart) {
        if (!force && exiting) {
            return;
        }

        exiting = true;
        try {
            if (!force && !exitConfirmed && getDefaultModalityState() != ModalityState.NON_MODAL) {
                return;
            }

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (!force && !confirmExitIfNeeded(exitConfirmed)) {
                        saveAll();
                        return;
                    }

                    getMessageBus().syncPublisher(AppLifecycleListener.TOPIC).appClosing();
                    myDisposeInProgress = true;
                    doExit(allowListenersToCancel, restart);
                    myDisposeInProgress = false;
                }
            };

            if (isDispatchThread()) {
                runnable.run();
            }
            else {
                invokeLater(runnable, ModalityState.NON_MODAL);
            }
        }
        finally {
            exiting = false;
        }
    }

    private boolean doExit(boolean allowListenersToCancel, boolean restart) {
        saveSettings();

        if (allowListenersToCancel && !canExit()) {
            return false;
        }

        final boolean success = disposeSelf(allowListenersToCancel);
        if (!success || isUnitTestMode()) {
            return false;
        }

        int exitCode = 0;
        if (restart && Restarter.isSupported()) {
            try {
                exitCode = Restarter.scheduleRestart();
            }
            catch (IOException e) {
                LOG.warn("Cannot restart", e);
            }
        }
        System.exit(exitCode);
        return true;
    }

    private static boolean confirmExitIfNeeded(boolean exitConfirmed) {
        final boolean hasUnsafeBgTasks = ProgressManager.getInstance().hasUnsafeProgressIndicator();
        if (exitConfirmed && !hasUnsafeBgTasks) {
            return true;
        }

        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return GeneralSettings.getInstance().isConfirmExit() && ProjectManager.getInstance().getOpenProjects().length > 0;
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                GeneralSettings.getInstance().setConfirmExit(value);
            }

            @Override
            public boolean canBeHidden() {
                return !hasUnsafeBgTasks;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return false;
            }

            
            @Override
            public String getDoNotShowMessage() {
                return "Do not ask me again";
            }
        };

        if (hasUnsafeBgTasks || option.isToBeShown()) {
            String message = ApplicationBundle
                    .message(hasUnsafeBgTasks ? "exit.confirm.prompt.tasks" : "exit.confirm.prompt",
                            ApplicationNamesInfo.getInstance().getFullProductName());

            if (MessageDialogBuilder.yesNo(ApplicationBundle.message("exit.confirm.title"), message).yesText(ApplicationBundle.message("command.exit")).noText(CommonBundle.message("button.cancel"))
                    .doNotAsk(option).show() != Messages.YES) {
                return false;
            }
        }
        return true;
    }

    private boolean canExit() {
        for (ApplicationListener applicationListener : myDispatcher.getListeners()) {
            if (!applicationListener.canExitApplication()) {
                return false;
            }
        }

        ProjectManagerEx projectManager = (ProjectManagerEx)ProjectManager.getInstance();
        Project[] projects = projectManager.getOpenProjects();
        for (Project project : projects) {
            if (!projectManager.canClose(project)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void runReadAction( final Runnable action) {
        Status status = getStatus();
        if (isReadAccessAllowed(status)) {
            action.run();
        }
        else {
            startRead(status);
            try {
                action.run();
            }
            finally {
                endRead(status);
            }
        }
    }

    @Override
    public <T> T runReadAction( final Computable<T> computation) {
        Status status = getStatus();
        if (isReadAccessAllowed(status)) {
            return computation.compute();
        }
        startRead(status);
        try {
            return computation.compute();
        }
        finally {
            endRead(status);
        }
    }

    @Override
    public <T, E extends Throwable> T runReadAction( ThrowableComputable<T, E> computation) throws E {
        Status status = getStatus();
        if (isReadAccessAllowed(status)) {
            return computation.compute();
        }
        startRead(status);
        try {
            return computation.compute();
        }
        finally {
            endRead(status);
        }
    }

    private void startRead(Status status) {
        assertNoPsiLock();
        try {
            myLock.readLock().lockInterruptibly();
            setReadLockAcquired(status, true);
        }
        catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    private void endRead(Status status) {
        setReadLockAcquired(status, false);
        myLock.readLock().unlock();
    }

    @Override
    public void runWriteAction( final Runnable action) {
        Class<? extends Runnable> clazz = action.getClass();
        startWrite(clazz);
        try {
            action.run();
        }
        finally {
            endWrite(clazz);
        }
    }

    @Override
    public <T> T runWriteAction( final Computable<T> computation) {
        Class<? extends Computable> clazz = computation.getClass();
        startWrite(clazz);
        try {
            return computation.compute();
        }
        finally {
            endWrite(clazz);
        }
    }

    @Override
    public <T, E extends Throwable> T runWriteAction( ThrowableComputable<T, E> computation) throws E {
        Class<? extends ThrowableComputable> clazz = computation.getClass();
        startWrite(clazz);
        try {
            return computation.compute();
        }
        finally {
            endWrite(clazz);
        }
    }

    @Override
    public boolean hasWriteAction( Class<?> actionClass) {
        assertIsDispatchThread();

        for (int i = myWriteActionsStack.size() - 1; i >= 0; i--) {
            Class action = myWriteActionsStack.get(i);
            if (actionClass == action || action != null && actionClass != null && ReflectionUtil.isAssignable(actionClass, action)) return true;
        }
        return false;
    }

    @Override
    public void assertReadAccessAllowed() {
        if (!isReadAccessAllowed()) {
            LOG.error(
                    "Read access is allowed from event dispatch thread or inside read-action only" +
                            " (see com.gome.maven.openapi.application.Application.runReadAction())",
                    "Current thread: " + describe(Thread.currentThread()), "; dispatch thread: " + EventQueue.isDispatchThread() +"; isDispatchThread(): "+isDispatchThread(),
                    "SystemEventQueueThread: " + describe(getEventQueueThread()));
        }
    }

    
    private static String describe(Thread o) {
        if (o == null) return "null";
        return o + " " + System.identityHashCode(o);
    }

    private static Thread getEventQueueThread() {
        EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        try {
            Method method = ReflectionUtil.getDeclaredMethod(EventQueue.class, "getDispatchThread");
            return (Thread)method.invoke(eventQueue);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isReadAccessAllowed() {
        return isReadAccessAllowed(getStatus());
    }

    private static boolean isReadAccessAllowed(Status status) {
        return (status.flags & (IS_EDT_FLAG | IS_READ_LOCK_ACQUIRED_FLAG)) != 0;
    }

    @Override
    public void assertIsDispatchThread() {
        assertIsDispatchThread(getStatus());
    }

    private static void assertIsDispatchThread(Status status) {
        if (isDispatchThread(status)) return;
        if (ShutDownTracker.isShutdownHookRunning()) return;
        int safeCounter = getSafeCounter(status);
        if (safeCounter == 0) {
            assertIsDispatchThread(status, "Access is allowed from event dispatch thread only.");
        }
    }

    private static int getSafeCounter(Status status) {
        return status.flags & 0x1fffffff;
    }

    private static void assertIsDispatchThread(Status status,  String message) {
        if (isDispatchThread(status)) return;
        LOG.error(message,
                "EventQueue.isDispatchThread()="+EventQueue.isDispatchThread(),
                "isDispatchThread()="+isDispatchThread(getStatus()),
                "Toolkit.getEventQueue()="+Toolkit.getDefaultToolkit().getSystemEventQueue(),
                "Current thread: " + describe(Thread.currentThread()),
                "SystemEventQueueThread: " + describe(getEventQueueThread()) +"\n"+ ThreadDumper.dumpThreadsToString()+"\n-----------");
    }

    @Override
    public void runEdtSafeAction( Runnable runnable) {
        Status status = getStatus();
        LOG.assertTrue(getSafeCounter(status) < 1<<26);
        status.flags++;

        try {
            runnable.run();
        }
        finally {
            status.flags--;
        }
    }

    @Override
    public void assertIsDispatchThread( final JComponent component) {
        if (component == null) return;

        Status status = getStatus();
        if (isDispatchThread(status)) {
            return;
        }

        if (Boolean.TRUE.equals(component.getClientProperty(WAS_EVER_SHOWN))) {
            assertIsDispatchThread(status);
        }
        else {
            final JRootPane root = component.getRootPane();
            if (root != null) {
                component.putClientProperty(WAS_EVER_SHOWN, Boolean.TRUE);
                assertIsDispatchThread(status);
            }
        }
    }

    @Override
    public void assertTimeConsuming() {
        if (myTestModeFlag || myHeadlessMode || ShutDownTracker.isShutdownHookRunning()) return;
        LOG.assertTrue(!isDispatchThread(), "This operation is time consuming and must not be called on EDT");
    }

    @Override
    public boolean tryRunReadAction( Runnable action) {
        Status status = getStatus();
        //if we are inside read action, do not try to acquire read lock again since it will deadlock if there is a pending writeAction
        boolean mustAcquire = !isReadAccessAllowed(status);

        if (mustAcquire) {
            assertNoPsiLock();
            try {
                // timed version of tryLock() respects fairness unlike the no-args method
                if (!myLock.readLock().tryLock(0, TimeUnit.MILLISECONDS)) return false;
                setReadLockAcquired(status, true);
            }
            catch (InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            }
        }

        try {
            action.run();
        }
        finally {
            if (mustAcquire) {
                endRead(status);
            }
        }
        return true;
    }

    public boolean tryToApplyActivationState(boolean active, Window window) {
        final Component frame = UIUtil.findUltimateParent(window);

        if (frame instanceof IdeFrame) {
            final IdeFrame ideFrame = (IdeFrame)frame;
            if (isActive() != active) {
                myActive = active;
                System.setProperty("idea.active", myActive.toString());
                ApplicationActivationListener publisher = getMessageBus().syncPublisher(ApplicationActivationListener.TOPIC);
                if (active) {
                    publisher.applicationActivated(ideFrame);
                }
                else {
                    publisher.applicationDeactivated(ideFrame);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isActive() {
        if (isUnitTestMode()) return true;

        if (myActive == null) {
            Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            return active != null;
        }

        return myActive;
    }

    
    @Override
    public AccessToken acquireReadActionLock() {
        Status status = getStatus();
        // if we are inside read action, do not try to acquire read lock again since it will deadlock if there is a pending writeAction
        if (isReadAccessAllowed(status)) return AccessToken.EMPTY_ACCESS_TOKEN;

        return new ReadAccessToken(status);
    }

    private volatile boolean myWriteActionPending;

    @Override
    public boolean isWriteActionPending() {
        return myWriteActionPending;
    }

    private void startWrite(Class clazz) {
        boolean writeActionPending = myWriteActionPending;
        myWriteActionPending = true;

        try {
            ActivityTracker.getInstance().inc();
            fireBeforeWriteActionStart(clazz);

            try {
                if (!isWriteAccessAllowed()) {
                    assertNoPsiLock();
                }
                if (!myLock.writeLock().tryLock()) {
                    final AtomicBoolean lockAcquired = new AtomicBoolean(false);
                    myLock.writeLock().lockInterruptibly();
                    lockAcquired.set(true);
                }
            }
            catch (InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            }
        }
        finally {
            myWriteActionPending = writeActionPending;
        }

        myWriteActionsStack.push(clazz);
        fireWriteActionStarted(clazz);
    }

    private void endWrite(Class clazz) {
        try {
            myWriteActionsStack.pop();
            fireWriteActionFinished(clazz);
        }
        finally {
            myLock.writeLock().unlock();
        }
    }

    
    @Override
    public AccessToken acquireWriteActionLock(Class clazz) {
        assertIsDispatchThread(getStatus(), "Write access is allowed from event dispatch thread only");
        return new WriteAccessToken(clazz);
    }

    private class WriteAccessToken extends AccessToken {
        private final Class clazz;

        public WriteAccessToken(Class clazz) {
            this.clazz = clazz;
            startWrite(clazz);
            markThreadNameInStackTrace();
            acquired();
        }

        @Override
        public void finish() {
            try {
                endWrite(clazz);
            }
            finally {
                unmarkThreadNameInStackTrace();
                released();
            }
        }

        private void markThreadNameInStackTrace() {
            String id = id();

            if (id != null) {
                final Thread thread = Thread.currentThread();
                thread.setName(thread.getName() + id);
            }
        }

        private void unmarkThreadNameInStackTrace() {
            String id = id();

            if (id != null) {
                final Thread thread = Thread.currentThread();
                String name = thread.getName();
                name = StringUtil.replace(name, id, "");
                thread.setName(name);
            }
        }

        private String id() {
            Class aClass = getClass();
            String name = aClass.getName();
            while (name == null) {
                aClass = aClass.getSuperclass();
                name = aClass.getName();
            }

            name = name.substring(name.lastIndexOf('.') + 1);
            name = name.substring(name.lastIndexOf('$') + 1);
            if (!name.equals("AccessToken")) {
                return " [" + name+"]";
            }
            return null;
        }
    }

    private class ReadAccessToken extends AccessToken {
        private final Status myStatus;

        private ReadAccessToken(Status status) {
            myStatus = status;
            startRead(status);
            acquired();
        }

        @Override
        public void finish() {
            endRead(myStatus);
            released();
        }
    }

    private final boolean myExtraChecks = isUnitTestMode();

    private void assertNoPsiLock() {
        if (myExtraChecks) {
            LOG.assertTrue(!Thread.holdsLock(PsiLock.LOCK), "Thread must not hold PsiLock while performing readAction");
        }
    }

    @Override
    public void assertWriteAccessAllowed() {
        LOG.assertTrue(isWriteAccessAllowed(),
                "Write access is allowed inside write-action only (see com.gome.maven.openapi.application.Application.runWriteAction())");
    }

    @Override
    public boolean isWriteAccessAllowed() {
        return myLock.isWriteLockedByCurrentThread();
    }

    // cheaper version of isWriteAccessAllowed(). must be called from EDT
    private boolean isInsideWriteActionEDTOnly() {
        return !myWriteActionsStack.isEmpty();
    }

    @Override
    public boolean isWriteActionInProgress() {
        return myLock.isWriteLocked();
    }

    public void editorPaintStart() {
        myInEditorPaintCounter++;
    }

    public void editorPaintFinish() {
        myInEditorPaintCounter--;
        LOG.assertTrue(myInEditorPaintCounter >= 0);
    }

    @Override
    public void addApplicationListener( ApplicationListener l) {
        myDispatcher.addListener(l);
    }

    @Override
    public void addApplicationListener( ApplicationListener l,  Disposable parent) {
        myDispatcher.addListener(l, parent);
    }

    @Override
    public void removeApplicationListener( ApplicationListener l) {
        myDispatcher.removeListener(l);
    }

    private void fireApplicationExiting() {
        myDispatcher.getMulticaster().applicationExiting();
    }

    private void fireBeforeWriteActionStart(Class action) {
        myDispatcher.getMulticaster().beforeWriteActionStart(action);
    }

    private void fireWriteActionStarted(Class action) {
        myDispatcher.getMulticaster().writeActionStarted(action);
    }

    private void fireWriteActionFinished(Class action) {
        myDispatcher.getMulticaster().writeActionFinished(action);
    }

    @Override
    public void saveSettings() {
        if (myDoNotSave) return;

        if (mySaveSettingsIsInProgress.compareAndSet(false, true)) {
            try {
                StoreUtil.save(getStateStore(), null);
            }
            finally {
                mySaveSettingsIsInProgress.set(false);
            }
        }
    }

    @Override
    public void saveAll() {
        if (myDoNotSave) return;

        FileDocumentManager.getInstance().saveAllDocuments();

        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project openProject : openProjects) {
            ProjectEx project = (ProjectEx)openProject;
            project.save();
        }

        saveSettings();
    }

    @Override
    public void doNotSave() {
        doNotSave(true);
    }

    @Override
    public void doNotSave(boolean value) {
        myDoNotSave = value;
    }

    @Override
    public boolean isDoNotSave() {
        return myDoNotSave;
    }

    
    @Override
    public <T> T[] getExtensions( final ExtensionPointName<T> extensionPointName) {
        return Extensions.getRootArea().getExtensionPoint(extensionPointName).getExtensions();
    }

    @Override
    public boolean isDisposeInProgress() {
        return myDisposeInProgress || ShutDownTracker.isShutdownHookRunning();
    }

    @Override
    public boolean isRestartCapable() {
        return Restarter.isSupported();
    }

    @Override
    protected boolean logSlowComponents() {
        return super.logSlowComponents() || ApplicationInfoImpl.getShadowInstance().isEAP();
    }

    
    public void setDisposeInProgress(boolean disposeInProgress) {
        myDisposeInProgress = disposeInProgress;
    }

    
    @Override
    public String toString() {
        return "Application" +
                (isDisposed() ? " (Disposed)" : "") +
                (isUnitTestMode() ? " (Unit test)" : "") +
                (isInternal() ? " (Internal)" : "") +
                (isHeadlessEnvironment() ? " (Headless)" : "") +
                (isCommandLine() ? " (Command line)" : "");
    }
}
