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

import com.gome.maven.ide.RecentProjectsManager;
import com.gome.maven.ide.plugins.IdeaPluginDescriptor;
import com.gome.maven.ide.plugins.PluginManagerCore;
import com.gome.maven.ide.startup.StartupManagerEx;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.notification.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathMacros;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.application.impl.ApplicationInfoImpl;
import com.gome.maven.openapi.components.ExtensionAreas;
import com.gome.maven.openapi.components.ProjectComponent;
import com.gome.maven.openapi.components.StorageScheme;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.components.impl.PlatformComponentManagerImpl;
import com.gome.maven.openapi.components.impl.ProjectPathMacroManager;
import com.gome.maven.openapi.components.impl.stores.IComponentStore;
import com.gome.maven.openapi.components.impl.stores.IProjectStore;
import com.gome.maven.openapi.components.impl.stores.StoreUtil;
import com.gome.maven.openapi.components.impl.stores.UnknownMacroNotification;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
//import com.gome.maven.openapi.project.DumbAwareRunnable;
import com.gome.maven.openapi.project.DumbAwareRunnable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.project.ProjectManagerAdapter;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.project.ex.ProjectManagerEx;
//import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
//import com.gome.maven.openapi.wm.WindowManager;
//import com.gome.maven.openapi.wm.impl.FrameTitleBuilder;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.openapi.wm.impl.FrameTitleBuilder;
import com.gome.maven.util.Function;
import com.gome.maven.util.TimedReference;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.pico.ConstructorInjectionComponentAdapter;
import gnu.trove.THashSet;
import org.picocontainer.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectImpl extends PlatformComponentManagerImpl implements ProjectEx {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.project.impl.ProjectImpl");

    public static final String NAME_FILE = ".name";
    public static Key<Long> CREATION_TIME = Key.create("ProjectImpl.CREATION_TIME");

    private ProjectManager myManager;
    private MyProjectManagerListener myProjectManagerListener;
    private final AtomicBoolean mySavingInProgress = new AtomicBoolean(false);
    public boolean myOptimiseTestLoadSpeed;
    private String myName;
    private String myOldName;
    private final boolean myLight;

    protected ProjectImpl( ProjectManager manager,  String filePath, boolean optimiseTestLoadSpeed,  String projectName) {
        super(ApplicationManager.getApplication(), "Project " + (projectName == null ? filePath : projectName));

        putUserData(CREATION_TIME, System.nanoTime());

        getPicoContainer().registerComponentInstance(Project.class, this);

        if (!isDefault()) {
            getStateStore().setProjectFilePath(filePath);
        }

        myOptimiseTestLoadSpeed = optimiseTestLoadSpeed;
        myManager = manager;

        myName = projectName == null ? getStateStore().getProjectName() : projectName;
        if (!isDefault() && projectName != null && getStateStore().getStorageScheme().equals(StorageScheme.DIRECTORY_BASED)) {
            myOldName = "";  // new project
        }

        // light project may be changed later during test, so we need to remember its initial state
        myLight = ApplicationManager.getApplication().isUnitTestMode() && filePath.contains("light_temp_");
    }

    public boolean isLight() {
        return myLight;
    }

    @Override
    public void setProjectName( String projectName) {
        if (!projectName.equals(myName)) {
            myOldName = myName;
            myName = projectName;
            StartupManager.getInstance(this).runWhenProjectIsInitialized(new DumbAwareRunnable() {
                @Override
                public void run() {
                    if (isDisposed()) return;

                    JFrame frame = WindowManager.getInstance().getFrame(ProjectImpl.this);
                    String title = FrameTitleBuilder.getInstance().getProjectTitle(ProjectImpl.this);
                    if (frame != null && title != null) {
                        frame.setTitle(title);
                    }
                }
            });
        }
    }

    @Override
    protected void bootstrapPicoContainer( String name) {
        Extensions.instantiateArea(ExtensionAreas.IDEA_PROJECT, this, null);
        super.bootstrapPicoContainer(name);
        final MutablePicoContainer picoContainer = getPicoContainer();

        final ProjectStoreClassProvider projectStoreClassProvider =
                (ProjectStoreClassProvider)picoContainer.getComponentInstanceOfType(ProjectStoreClassProvider.class);

        picoContainer.registerComponentImplementation(ProjectPathMacroManager.class);
        picoContainer.registerComponent(new ComponentAdapter() {
            ComponentAdapter myDelegate;

            public ComponentAdapter getDelegate() {
                if (myDelegate == null) {
                    final Class storeClass = projectStoreClassProvider.getProjectStoreClass(isDefault());
                    myDelegate = new ConstructorInjectionComponentAdapter(storeClass, storeClass, null, true);
                }

                return myDelegate;
            }

            @Override
            public Object getComponentKey() {
                return IComponentStore.class;
            }

            @Override
            public Class getComponentImplementation() {
                return getDelegate().getComponentImplementation();
            }

            @Override
            public Object getComponentInstance(final PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
                return getDelegate().getComponentInstance(container);
            }

            @Override
            public void verify(final PicoContainer container) throws PicoIntrospectionException {
                getDelegate().verify(container);
            }

            @Override
            public void accept(final PicoVisitor visitor) {
                visitor.visitComponentAdapter(this);
                getDelegate().accept(visitor);
            }
        });
    }

    
    @Override
    public IProjectStore getStateStore() {
        return (IProjectStore)getPicoContainer().getComponentInstance(IComponentStore.class);
    }

    @Override
    public void initializeComponent( Object component, boolean service) {
        if (!service) {
            ProgressIndicator indicator = getProgressIndicator();
            if (indicator != null) {
                //      indicator.setText2(getComponentName(component));
                indicator.setIndeterminate(false);
                indicator.setFraction(getPercentageOfComponentsLoaded());
            }
        }

        getStateStore().initComponent(component, service);
    }

    @Override
    public boolean isOpen() {
        return ProjectManagerEx.getInstanceEx().isProjectOpened(this);
    }

    @Override
    public boolean isInitialized() {
        return !isDisposed() && isOpen() && StartupManagerEx.getInstanceEx(this).startupActivityPassed();
    }

    public void loadProjectComponents() {
        final IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
        for (IdeaPluginDescriptor plugin : plugins) {
            if (PluginManagerCore.shouldSkipPlugin(plugin)) continue;
            loadComponentsConfiguration(plugin.getProjectComponents(), plugin, isDefault());
        }
    }

    @Override
    
    public String getProjectFilePath() {
        return getStateStore().getProjectFilePath();
    }

    @Override
    public VirtualFile getProjectFile() {
        return getStateStore().getProjectFile();
    }

    @Override
    public VirtualFile getBaseDir() {
        return getStateStore().getProjectBaseDir();
    }

    
    @Override
    public String getBasePath() {
        return getStateStore().getProjectBasePath();
    }

    
    @Override
    public String getName() {
        return myName;
    }

    
    @Override
    public String getPresentableUrl() {
        if (myName == null) return null;  // not yet initialized
        return getStateStore().getPresentableUrl();
    }

    
    
    @Override
    public String getLocationHash() {
        String str = getPresentableUrl();
        if (str == null) str = getName();

        final String prefix = getStateStore().getStorageScheme() == StorageScheme.DIRECTORY_BASED ? "" : getName();
        return prefix + Integer.toHexString(str.hashCode());
    }

    @Override
    
    public VirtualFile getWorkspaceFile() {
        return getStateStore().getWorkspaceFile();
    }

    @Override
    public boolean isOptimiseTestLoadSpeed() {
        return myOptimiseTestLoadSpeed;
    }

    @Override
    public void setOptimiseTestLoadSpeed(final boolean optimiseTestLoadSpeed) {
        myOptimiseTestLoadSpeed = optimiseTestLoadSpeed;
    }

    @Override
    public void init() {
        long start = System.currentTimeMillis();

        final ProgressIndicator progressIndicator = isDefault() ? null : ProgressIndicatorProvider.getGlobalProgressIndicator();
        if (progressIndicator != null) {
            progressIndicator.pushState();
        }
        super.init();
        if (progressIndicator != null) {
            progressIndicator.popState();
        }

        long time = System.currentTimeMillis() - start;
        LOG.info(getComponentConfigurations().length + " project components initialized in " + time + " ms");

        getMessageBus().syncPublisher(ProjectLifecycleListener.TOPIC).projectComponentsInitialized(this);

        //noinspection SynchronizeOnThis
        synchronized (this) {
            myProjectManagerListener = new MyProjectManagerListener();
            myManager.addProjectManagerListener(this, myProjectManagerListener);
        }
    }

    public boolean isToSaveProjectName() {
        if (!isDefault()) {
            final IProjectStore stateStore = getStateStore();
            if (stateStore.getStorageScheme().equals(StorageScheme.DIRECTORY_BASED)) {
                final VirtualFile baseDir = stateStore.getProjectBaseDir();
                if (baseDir != null && baseDir.isValid()) {
                    return myOldName != null && !myOldName.equals(getName());
                }
            }
        }

        return false;
    }

    @Override
    public void save() {
        if (ApplicationManagerEx.getApplicationEx().isDoNotSave()) {
            // no need to save
            return;
        }

        if (!mySavingInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            if (isToSaveProjectName()) {
                try {
                    VirtualFile baseDir = getStateStore().getProjectBaseDir();
                    if (baseDir != null && baseDir.isValid()) {
                        VirtualFile ideaDir = baseDir.findChild(DIRECTORY_STORE_FOLDER);
                        if (ideaDir != null && ideaDir.isValid() && ideaDir.isDirectory()) {
                            File nameFile = new File(ideaDir.getPath(), NAME_FILE);

                            FileUtil.writeToFile(nameFile, getName());
                            myOldName = null;

                            RecentProjectsManager.getInstance().clearNameCache();
                        }
                    }
                }
                catch (Throwable e) {
                    LOG.error("Unable to store project name", e);
                }
            }

            StoreUtil.save(getStateStore(), this);
        }
        finally {
            mySavingInProgress.set(false);
            ApplicationManager.getApplication().getMessageBus().syncPublisher(ProjectSaved.TOPIC).saved(this);
        }
    }

    @Override
    public synchronized void dispose() {
        ApplicationEx application = ApplicationManagerEx.getApplicationEx();
        assert application.isWriteAccessAllowed();  // dispose must be under write action

        // can call dispose only via com.gome.maven.ide.impl.ProjectUtil.closeAndDispose()
        LOG.assertTrue(application.isUnitTestMode() || !ProjectManagerEx.getInstanceEx().isProjectOpened(this));

        LOG.assertTrue(!isDisposed());
        if (myProjectManagerListener != null) {
            myManager.removeProjectManagerListener(this, myProjectManagerListener);
        }

        disposeComponents();
        Extensions.disposeArea(this);
        myManager = null;
        myProjectManagerListener = null;

        super.dispose();

        if (!application.isDisposed()) {
            application.getMessageBus().syncPublisher(ProjectLifecycleListener.TOPIC).afterProjectClosed(this);
        }
        TimedReference.disposeTimed();
    }

    private void projectOpened() {
        final ProjectComponent[] components = getComponents(ProjectComponent.class);
        for (ProjectComponent component : components) {
            try {
                component.projectOpened();
            }
            catch (Throwable e) {
                LOG.error(component.toString(), e);
            }
        }
    }

    private void projectClosed() {
        List<ProjectComponent> components = new ArrayList<ProjectComponent>(Arrays.asList(getComponents(ProjectComponent.class)));
        Collections.reverse(components);
        for (ProjectComponent component : components) {
            try {
                component.projectClosed();
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
    }

    
    @Override
    public <T> T[] getExtensions( final ExtensionPointName<T> extensionPointName) {
        return Extensions.getArea(this).getExtensionPoint(extensionPointName).getExtensions();
    }

    public String getDefaultName() {
        return isDefault() ? myName : getStateStore().getProjectName();
    }

    private class MyProjectManagerListener extends ProjectManagerAdapter {
        @Override
        public void projectOpened(Project project) {
            LOG.assertTrue(project == ProjectImpl.this);
            ProjectImpl.this.projectOpened();
        }

        @Override
        public void projectClosed(Project project) {
            LOG.assertTrue(project == ProjectImpl.this);
            ProjectImpl.this.projectClosed();
        }
    }

    
    @Override
    protected MutablePicoContainer createPicoContainer() {
        return Extensions.getArea(this).getPicoContainer();
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public void checkUnknownMacros(final boolean showDialog) {
        final IProjectStore stateStore = getStateStore();
        TrackingPathMacroSubstitutor[] substitutors = stateStore.getSubstitutors();
        Set<String> unknownMacros = new THashSet<String>();
        for (TrackingPathMacroSubstitutor substitutor : substitutors) {
            unknownMacros.addAll(substitutor.getUnknownMacros(null));
        }

        if (unknownMacros.isEmpty() || (showDialog && !ProjectMacrosUtil.checkMacros(this, new THashSet<String>(unknownMacros)))) {
            return;
        }

        final PathMacros pathMacros = PathMacros.getInstance();
        final Set<String> macrosToInvalidate = new THashSet<String>(unknownMacros);
        for (Iterator<String> it = macrosToInvalidate.iterator(); it.hasNext(); ) {
            String macro = it.next();
            if (StringUtil.isEmptyOrSpaces(pathMacros.getValue(macro)) && !pathMacros.isIgnoredMacroName(macro)) {
                it.remove();
            }
        }

        if (!macrosToInvalidate.isEmpty()) {
            final Set<String> components = new THashSet<String>();
            for (TrackingPathMacroSubstitutor substitutor : substitutors) {
                components.addAll(substitutor.getComponents(macrosToInvalidate));
            }

            if (stateStore.isReloadPossible(components)) {
                for (TrackingPathMacroSubstitutor substitutor : substitutors) {
                    substitutor.invalidateUnknownMacros(macrosToInvalidate);
                }

                for (UnknownMacroNotification notification : NotificationsManager.getNotificationsManager().getNotificationsOfType(UnknownMacroNotification.class, this)) {
                    if (macrosToInvalidate.containsAll(notification.getMacros())) {
                        notification.expire();
                    }
                }

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        stateStore.reinitComponents(components, true);
                    }
                });
            }
            else {
//                if (Messages.showYesNoDialog(this, "Component could not be reloaded. Reload project?", "Configuration Changed",
//                        Messages.getQuestionIcon()) == Messages.YES) {
                    ProjectManagerEx.getInstanceEx().reloadProject(this);
//                }
            }
        }
    }

    
    @Override
    public String toString() {
        return "Project" +
                (isDisposed() ? " (Disposed" + (temporarilyDisposed ? " temporarily" : "") + ")"
                        : isDefault() ? "" : " '" + getPresentableUrl() + "'") +
                (isDefault() ? " (Default)" : "") +
                " " + myName;
    }

    @Override
    protected boolean logSlowComponents() {
        return super.logSlowComponents() || ApplicationInfoImpl.getShadowInstance().isEAP();
    }

    public static void dropUnableToSaveProjectNotification( final Project project, final VirtualFile[] readOnlyFiles) {
        final UnableToSaveProjectNotification[] notifications =
                NotificationsManager.getNotificationsManager().getNotificationsOfType(UnableToSaveProjectNotification.class, project);
        if (notifications.length == 0) {
            Notifications.Bus.notify(new UnableToSaveProjectNotification(project, readOnlyFiles), project);
        }
    }

    public static class UnableToSaveProjectNotification extends Notification {
        private Project myProject;
        private final String[] myFileNames;

        private UnableToSaveProjectNotification( final Project project, final VirtualFile[] readOnlyFiles) {
            super("Project Settings", "Could not save project!", buildMessage(), NotificationType.ERROR, new NotificationListener() {
                @Override
                public void hyperlinkUpdate( Notification notification,  HyperlinkEvent event) {
                    final UnableToSaveProjectNotification unableToSaveProjectNotification = (UnableToSaveProjectNotification)notification;
                    final Project _project = unableToSaveProjectNotification.getProject();
                    notification.expire();

                    if (_project != null && !_project.isDisposed()) {
                        _project.save();
                    }
                }
            });

            myProject = project;
            myFileNames = ContainerUtil.map(readOnlyFiles, new Function<VirtualFile, String>() {
                @Override
                public String fun(VirtualFile file) {
                    return file.getPresentableUrl();
                }
            }, new String[readOnlyFiles.length]);
        }

        public String[] getFileNames() {
            return myFileNames;
        }

        private static String buildMessage() {
            return "<p>Unable to save project files. Please ensure project files are writable and you have permissions to modify them." +
                    " <a href=\"\">Try to save project again</a>.</p>";
        }

        public Project getProject() {
            return myProject;
        }

        @Override
        public void expire() {
            myProject = null;
            super.expire();
        }
    }
}
