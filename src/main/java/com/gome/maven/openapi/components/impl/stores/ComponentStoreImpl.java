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
package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.*;
import com.gome.maven.openapi.application.ex.DecodeDefaultsUtil;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.components.StateStorage.SaveSession;
import com.gome.maven.openapi.components.StateStorageChooserEx.Resolution;
import com.gome.maven.openapi.components.impl.ComponentManagerImpl;
import com.gome.maven.openapi.components.impl.stores.StateStorageManager.ExternalizationSession;
import com.gome.maven.openapi.components.store.ReadOnlyModificationException;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.Project;
//import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.gome.maven.util.ArrayUtilRt;
import com.gome.maven.util.ReflectionUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.containers.SmartHashSet;
import com.gome.maven.util.lang.CompoundRuntimeException;
import com.gome.maven.util.messages.MessageBus;
import com.gome.maven.util.xmlb.JDOMXIncluder;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jdom.JDOMException;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"deprecation"})
public abstract class ComponentStoreImpl implements IComponentStore.Reloadable {
    private static final Logger LOG = Logger.getInstance(ComponentStoreImpl.class);
    private final Map<String, Object> myComponents = Collections.synchronizedMap(new THashMap<String, Object>());
    private final List<SettingsSavingComponent> mySettingsSavingComponents = new CopyOnWriteArrayList<SettingsSavingComponent>();

    @Override
    public void initComponent( Object component, boolean service) {
        if (component instanceof SettingsSavingComponent) {
            mySettingsSavingComponents.add((SettingsSavingComponent)component);
        }

        if (!(component instanceof JDOMExternalizable || component instanceof PersistentStateComponent)) {
            return;
        }

//        AccessToken token = ReadAction.start();
        try {
            if (component instanceof PersistentStateComponent) {
                initPersistentComponent((PersistentStateComponent<?>)component, null, false);
            }
            else {
                initJdomExternalizable((JDOMExternalizable)component);
            }
        }
        catch (StateStorageException e) {
            throw e;
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.error(e);
        }
        finally {
//            token.finish();
        }
    }

    @Override
    public final void save( List<Pair<StateStorage.SaveSession, VirtualFile>> readonlyFiles) {
        ExternalizationSession externalizationSession = myComponents.isEmpty() ? null : getStateStorageManager().startExternalization();
        if (externalizationSession != null) {
            String[] names = ArrayUtilRt.toStringArray(myComponents.keySet());
            Arrays.sort(names);
            for (String name : names) {
                commitComponent(externalizationSession, myComponents.get(name), name);
            }
        }

        List<Throwable> errors = null;
        for (SettingsSavingComponent settingsSavingComponent : mySettingsSavingComponents) {
            try {
                settingsSavingComponent.save();
            }
            catch (Throwable e) {
                if (errors == null) {
                    errors = new SmartList<Throwable>();
                }
                errors.add(e);
            }
        }

        errors = doSave(externalizationSession == null ? null : externalizationSession.createSaveSessions(), readonlyFiles, errors);
        CompoundRuntimeException.doThrow(errors);
    }

    
    public void saveApplicationComponent( Object component) {
        StateStorageManager.ExternalizationSession externalizationSession = getStateStorageManager().startExternalization();
        if (externalizationSession == null) {
            return;
        }

        commitComponent(externalizationSession, component, null);
        List<SaveSession> sessions = externalizationSession.createSaveSessions();
        if (sessions.isEmpty()) {
            return;
        }

        final File file;
        State state = StoreUtil.getStateSpec(component.getClass());
        if (state != null) {
            file = new File(getStateStorageManager().expandMacros(findNonDeprecated(state.storages()).file()));
        }
        else if (component instanceof ExportableApplicationComponent && component instanceof NamedJDOMExternalizable) {
            file = PathManager.getOptionsFile((NamedJDOMExternalizable)component);
        }
        else {
            throw new AssertionError(component.getClass() + " doesn't have @State annotation and doesn't implement ExportableApplicationComponent");
        }

//        AccessToken token = WriteAction.start();
        try {
            VfsRootAccess.allowRootAccess(file.getAbsolutePath());
            CompoundRuntimeException.doThrow(doSave(sessions, Collections.<Pair<SaveSession, VirtualFile>>emptyList(), null));
        }
        finally {
            try {
                VfsRootAccess.disallowRootAccess(file.getAbsolutePath());
            }
            finally {
//                token.finish();
            }
        }
    }

    private static Storage findNonDeprecated(Storage[] storages) {
        for (Storage storage : storages) {
            if (!storage.deprecated()) {
                return storage;
            }
        }
        throw new AssertionError("All storages are deprecated");
    }

    private void commitComponent( ExternalizationSession externalizationSession,  Object component,  String componentName) {
        if (component instanceof PersistentStateComponent) {
            commitPersistentComponent((PersistentStateComponent<?>)component, externalizationSession, componentName);
        }
        else if (component instanceof JDOMExternalizable) {
            externalizationSession.setStateInOldStorage(component, componentName == null ? ComponentManagerImpl.getComponentName(component) : componentName, component);
        }
    }

    
    protected List<Throwable> doSave( List<SaveSession> saveSessions,  List<Pair<SaveSession, VirtualFile>> readonlyFiles,  List<Throwable> errors) {
        if (saveSessions != null) {
            for (SaveSession session : saveSessions) {
                errors = executeSave(session, readonlyFiles, errors);
            }
        }

        return errors;
    }

    
    protected static List<Throwable> executeSave( SaveSession session,  List<Pair<SaveSession, VirtualFile>> readonlyFiles,  List<Throwable> errors) {
        try {
            session.save();
        }
        catch (ReadOnlyModificationException e) {
            LOG.warn(e);
            readonlyFiles.add(Pair.create(e.getSession() == null ? session : e.getSession(), e.getFile()));
        }
        catch (Exception e) {
            if (errors == null) {
                errors = new SmartList<Throwable>();
            }
            errors.add(e);
        }
        return errors;
    }

    private <T> void commitPersistentComponent( PersistentStateComponent<T> component,  ExternalizationSession session,  String componentName) {
        T state = component.getState();
        if (state != null) {
            Storage[] storageSpecs = getComponentStorageSpecs(component, StoreUtil.getStateSpec(component), StateStorageOperation.WRITE);
            session.setState(storageSpecs, component, componentName == null ? getComponentName(component) : componentName, state);
        }
    }

    private void initJdomExternalizable( JDOMExternalizable component) {
        String componentName = ComponentManagerImpl.getComponentName(component);
        doAddComponent(componentName, component);

        if (optimizeTestLoading()) {
            return;
        }

        loadJdomDefaults(component, componentName);

        StateStorage stateStorage = getStateStorageManager().getOldStorage(component, componentName, StateStorageOperation.READ);
        if (stateStorage == null) {
            return;
        }

        Element element = stateStorage.getState(component, componentName, Element.class, null);
        if (element == null) {
            return;
        }

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading configuration for " + component.getClass());
            }
            component.readExternal(element);
        }
        catch (InvalidDataException e) {
            LOG.error(e);
            return;
        }

        validateUnusedMacros(componentName, true);
    }

    private void doAddComponent(String componentName, Object component) {
        Object existing = myComponents.get(componentName);
        if (existing != null && existing != component) {
            LOG.error("Conflicting component name '" + componentName + "': " + existing.getClass() + " and " + component.getClass());
        }
        myComponents.put(componentName, component);
    }

    private void loadJdomDefaults( JDOMExternalizable component,  String componentName) {
        try {
            Element defaultState = getDefaultState(component, componentName, Element.class);
            if (defaultState != null) {
                component.readExternal(defaultState);
            }
        }
        catch (Exception e) {
            LOG.error("Cannot load defaults for " + component.getClass(), e);
        }
    }

    
    protected Project getProject() {
        return null;
    }

    private void validateUnusedMacros( final String componentName, final boolean service) {
        final Project project = getProject();
        if (project == null) return;

        if (!ApplicationManager.getApplication().isHeadlessEnvironment() && !ApplicationManager.getApplication().isUnitTestMode()) {
            if (service && componentName != null && project.isInitialized()) {
                final TrackingPathMacroSubstitutor substitutor = getStateStorageManager().getMacroSubstitutor();
                if (substitutor != null) {
                    StorageUtil.notifyUnknownMacros(substitutor, project, componentName);
                }
            }
        }
    }

    private <T> String initPersistentComponent( PersistentStateComponent<T> component,  Set<StateStorage> changedStorages, boolean reloadData) {
        State stateSpec = StoreUtil.getStateSpec(component);
        String name = stateSpec.name();
        if (changedStorages == null || !reloadData) {
            doAddComponent(name, component);
        }
        if (optimizeTestLoading()) {
            return name;
        }

        Class<T> stateClass = ComponentSerializationUtil.getStateClass(component.getClass());
        if (!stateSpec.defaultStateAsResource() && LOG.isDebugEnabled() && getDefaultState(component, name, stateClass) != null) {
            LOG.error(name + " has default state, but not marked to load it");
        }

        T state = stateSpec.defaultStateAsResource() ? getDefaultState(component, name, stateClass) : null;
        Storage[] storageSpecs = getComponentStorageSpecs(component, stateSpec, StateStorageOperation.READ);
        StateStorageChooserEx stateStorageChooser = component instanceof StateStorageChooserEx ? (StateStorageChooserEx)component : null;
        for (Storage storageSpec : storageSpecs) {
            Resolution resolution = stateStorageChooser == null ? Resolution.DO : stateStorageChooser.getResolution(storageSpec, StateStorageOperation.READ);
            if (resolution == Resolution.SKIP) {
                continue;
            }

            StateStorage stateStorage = getStateStorageManager().getStateStorage(storageSpec);
            if (stateStorage != null && (stateStorage.hasState(component, name, stateClass, reloadData) ||
                    (changedStorages != null && changedStorages.contains(stateStorage)))) {
                state = stateStorage.getState(component, name, stateClass, state);
                break;
            }
        }

        if (state != null) {
            component.loadState(state);
        }

        validateUnusedMacros(name, true);

        return name;
    }

    
    protected abstract PathMacroManager getPathMacroManagerForDefaults();

    
    protected <T> T getDefaultState( Object component,  String componentName,  final Class<T> stateClass) {
        URL url = DecodeDefaultsUtil.getDefaults(component, componentName);
        if (url == null) {
            return null;
        }

        try {
            Element documentElement = JDOMXIncluder.resolve(JDOMUtil.loadDocument(url), url.toExternalForm()).detachRootElement();

            PathMacroManager pathMacroManager = getPathMacroManagerForDefaults();
            if (pathMacroManager != null) {
                pathMacroManager.expandPaths(documentElement);
            }

            return DefaultStateSerializer.deserializeState(documentElement, stateClass, null);
        }
        catch (IOException e) {
            throw new StateStorageException("Error loading state from " + url, e);
        }
        catch (JDOMException e) {
            throw new StateStorageException("Error loading state from " + url, e);
        }
    }

    
    public static String getComponentName( PersistentStateComponent<?> persistentStateComponent) {
        return StoreUtil.getStateSpec(persistentStateComponent).name();
    }

    
    protected <T> Storage[] getComponentStorageSpecs( PersistentStateComponent<T> component,
                                                      State stateSpec,
                                                      StateStorageOperation operation) {
        Storage[] storages = stateSpec.storages();
        if (storages.length == 1) {
            return storages;
        }
        assert storages.length > 0;

        StateStorageChooser<PersistentStateComponent<T>> storageChooser;
        Class<? extends StateStorageChooser> storageChooserClass = stateSpec.storageChooser();
        if (storageChooserClass != StateStorageChooser.class) {
            //noinspection unchecked
            storageChooser = ReflectionUtil.newInstance(storageChooserClass);
            return storageChooser.selectStorages(storages, component, operation);
        }

        StateStorageChooser<PersistentStateComponent<?>> defaultStateStorageChooser = getDefaultStateStorageChooser();
        if (defaultStateStorageChooser != null) {
            return defaultStateStorageChooser.selectStorages(storages, component, operation);
        }

        if (component instanceof StateStorageChooserEx) {
            return storages;
        }

        int actualStorageCount = 0;
        for (Storage storage : storages) {
            if (!storage.deprecated()) {
                actualStorageCount++;
            }
        }

        if (actualStorageCount > 1) {
            LOG.error("State chooser not specified for: " + component.getClass());
        }

        if (!storages[0].deprecated()) {
            boolean othersAreDeprecated = true;
            for (int i = 1; i < storages.length; i++) {
                if (!storages[i].deprecated()) {
                    othersAreDeprecated = false;
                    break;
                }
            }

            if (othersAreDeprecated) {
                return storages;
            }
        }

        Storage[] sorted = Arrays.copyOf(storages, storages.length);
        Arrays.sort(sorted, new Comparator<Storage>() {
            @Override
            public int compare(Storage o1, Storage o2) {
                int w1 = o1.deprecated() ? 1 : 0;
                int w2 = o2.deprecated() ? 1 : 0;
                return w1 - w2;
            }
        });
        return sorted;
    }

    protected boolean optimizeTestLoading() {
        return false;
    }

    
    protected StateStorageChooser<PersistentStateComponent<?>> getDefaultStateStorageChooser() {
        return null;
    }

    @Override
    public boolean isReloadPossible( final Set<String> componentNames) {
        for (String componentName : componentNames) {
            final Object component = myComponents.get(componentName);
            if (component != null && (!(component instanceof PersistentStateComponent) || !StoreUtil.getStateSpec((PersistentStateComponent<?>)component).reloadable())) {
                return false;
            }
        }

        return true;
    }

    @Override
    
    public final Collection<String> getNotReloadableComponents( Collection<String> componentNames) {
        Set<String> notReloadableComponents = null;
        for (String componentName : componentNames) {
            Object component = myComponents.get(componentName);
            if (component != null && (!(component instanceof PersistentStateComponent) || !StoreUtil.getStateSpec((PersistentStateComponent<?>)component).reloadable())) {
                if (notReloadableComponents == null) {
                    notReloadableComponents = new LinkedHashSet<String>();
                }
                notReloadableComponents.add(componentName);
            }
        }
        return notReloadableComponents == null ? Collections.<String>emptySet() : notReloadableComponents;
    }

    @Override
    public void reinitComponents( Set<String> componentNames, boolean reloadData) {
        reinitComponents(componentNames, Collections.<String>emptySet(), Collections.<StateStorage>emptySet());
    }

    protected boolean reinitComponent( String componentName,  Set<StateStorage> changedStorages) {
        PersistentStateComponent component = (PersistentStateComponent)myComponents.get(componentName);
        if (component == null) {
            return false;
        }
        else {
            boolean changedStoragesEmpty = changedStorages.isEmpty();
            initPersistentComponent(component, changedStoragesEmpty ? null : changedStorages, changedStoragesEmpty);
            return true;
        }
    }

    
    protected abstract MessageBus getMessageBus();

    @Override
    
    public final Collection<String> reload( MultiMap<StateStorage, VirtualFile> changedStorages) {
        if (changedStorages.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> componentNames = new SmartHashSet<String>();
        for (StateStorage storage : changedStorages.keySet()) {
            try {
                // we must update (reload in-memory storage data) even if non-reloadable component will be detected later
                // not saved -> user does own modification -> new (on disk) state will be overwritten and not applied
                storage.analyzeExternalChangesAndUpdateIfNeed(changedStorages.get(storage), componentNames);
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        if (componentNames.isEmpty()) {
            return Collections.emptySet();
        }

        Collection<String> notReloadableComponents = getNotReloadableComponents(componentNames);
        reinitComponents(componentNames, notReloadableComponents, changedStorages.keySet());
        return notReloadableComponents.isEmpty() ? null : notReloadableComponents;
    }

    // used in settings repository plugin
    public void reinitComponents( Set<String> componentNames,  Collection<String> notReloadableComponents,  Set<StateStorage> changedStorages) {
        MessageBus messageBus = getMessageBus();
        messageBus.syncPublisher(BatchUpdateListener.TOPIC).onBatchUpdateStarted();
        try {
            for (String componentName : componentNames) {
                if (!notReloadableComponents.contains(componentName)) {
                    reinitComponent(componentName, changedStorages);
                }
            }
        }
        finally {
            messageBus.syncPublisher(BatchUpdateListener.TOPIC).onBatchUpdateFinished();
        }
    }

    public enum ReloadComponentStoreStatus {
        RESTART_AGREED,
        RESTART_CANCELLED,
        ERROR,
        SUCCESS,
    }

    
    public static ReloadComponentStoreStatus reloadStore( Collection<Pair<VirtualFile, StateStorage>> changedStorages,  IComponentStore.Reloadable store) {
        MultiMap<StateStorage, VirtualFile> storageToFiles = MultiMap.createLinkedSet();
        for (Pair<VirtualFile, StateStorage> pair : changedStorages) {
            storageToFiles.putValue(pair.second, pair.first);
        }

        Collection<String> notReloadableComponents;
        boolean willBeReloaded = false;
        try {
            AccessToken token = WriteAction.start();
            try {
                notReloadableComponents = store.reload(storageToFiles);
            }
            catch (Throwable e) {
//                Messages.showWarningDialog(ProjectBundle.message("project.reload.failed", e.getMessage()),
//                        ProjectBundle.message("project.reload.failed.title"));
                System.out.println("project.reload.failed");
                return ReloadComponentStoreStatus.ERROR;
            }
            finally {
                token.finish();
            }

            if (ContainerUtil.isEmpty(notReloadableComponents)) {
                return ReloadComponentStoreStatus.SUCCESS;
            }

            willBeReloaded = askToRestart(store, notReloadableComponents, changedStorages);
            return willBeReloaded ? ReloadComponentStoreStatus.RESTART_AGREED : ReloadComponentStoreStatus.RESTART_CANCELLED;
        }
        finally {
            if (!willBeReloaded) {
                for (StateStorage storage : storageToFiles.keySet()) {
                    if (storage instanceof StateStorageBase) {
                        ((StateStorageBase)storage).enableSaving();
                    }
                }
            }
        }
    }

    // used in settings repository plugin
    public static boolean askToRestart( Reloadable store,
                                        Collection<String> notReloadableComponents,
                                        Collection<Pair<VirtualFile, StateStorage>> changedStorages) {
        StringBuilder message = new StringBuilder();
        String storeName = store instanceof IApplicationStore ? "Application" : "Project";
        message.append(storeName).append(' ');
        message.append("components were changed externally and cannot be reloaded:\n\n");
        int count = 0;
        for (String component : notReloadableComponents) {
            if (count == 10) {
                message.append('\n').append("and ").append(notReloadableComponents.size() - count).append(" more").append('\n');
            }
            else {
                message.append(component).append('\n');
                count++;
            }
        }

        message.append("\nWould you like to ");
        if (store instanceof IApplicationStore) {
            message.append(ApplicationManager.getApplication().isRestartCapable() ? "restart" : "shutdown").append(' ');
            message.append(ApplicationNamesInfo.getInstance().getProductName()).append('?');
        }
        else {
            message.append("reload project?");
        }

//        if (Messages.showYesNoDialog(message.toString(),
//                storeName + " Files Changed", Messages.getQuestionIcon()) == Messages.YES) {
            if (changedStorages != null) {
                for (Pair<VirtualFile, StateStorage> cause : changedStorages) {
                    StateStorage storage = cause.getSecond();
                    if (storage instanceof StateStorageBase) {
                        ((StateStorageBase)storage).disableSaving();
                    }
                }
//            }
            return true;
        }
        return false;
    }
}
