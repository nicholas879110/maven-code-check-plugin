/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs;

import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.vcs.changes.VcsAnnotationLocalChangesListener;
import com.gome.maven.openapi.vcs.history.VcsHistoryCache;
import com.gome.maven.openapi.vcs.impl.ContentRevisionCache;
import com.gome.maven.openapi.vcs.impl.VcsDescriptor;
import com.gome.maven.openapi.vcs.impl.VcsEnvironmentsProxyCreator;
import com.gome.maven.openapi.vcs.update.UpdatedFiles;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Processor;
import com.gome.maven.util.messages.Topic;

import java.util.List;

/**
 * Manages the version control systems used by a specific project.
 */
public abstract class ProjectLevelVcsManager {

    public static final Topic<VcsListener> VCS_CONFIGURATION_CHANGED = Topic.create("VCS configuration changed", VcsListener.class);
    public static final Topic<VcsListener> VCS_CONFIGURATION_CHANGED_IN_PLUGIN = Topic.create("VCS configuration changed in VCS plugin", VcsListener.class);

    public abstract void iterateVfUnderVcsRoot(VirtualFile file, Processor<VirtualFile> processor);

    /**
     * Returns the <code>ProjectLevelVcsManager<code> instance for the specified project.
     *
     * @param project the project for which the instance is requested.
     * @return the manager instance.
     */
    public static ProjectLevelVcsManager getInstance(Project project) {
        return PeriodicalTasksCloser.getInstance().safeGetComponent(project, ProjectLevelVcsManager.class);
    }

    /**
     * Gets the instance of the component if the project wasn't disposed. If the project was
     * disposed, throws ProcessCanceledException. Should only be used for calling from background
     * threads (for example, committed changes refresh thread).
     *
     * @param project the project for which the component instance should be retrieved.
     * @return component instance
     */
    public static ProjectLevelVcsManager getInstanceChecked(final Project project) {
        return ApplicationManager.getApplication().runReadAction(new Computable<ProjectLevelVcsManager>() {
            public ProjectLevelVcsManager compute() {
                if (project.isDisposed()) throw new ProcessCanceledException();
                return getInstance(project);
            }
        });
    }

    /**
     * Returns the list of all registered version control systems.
     *
     * @return the list of registered version control systems.
     */
    public abstract VcsDescriptor[] getAllVcss();

    /**
     * Returns the version control system with the specified name.
     *
     * @param name the name of the VCS to find.
     * @return the VCS instance, or null if none is found.
     */
    
    public abstract AbstractVcs findVcsByName( String name);

    
    public abstract VcsDescriptor getDescriptor(final String name);
    /**
     * Checks if all files in the specified array are managed by the specified VCS.
     *
     * @param abstractVcs the VCS to check.
     * @param files       the files to check.
     * @return true if all files are managed by the VCS, false otherwise.
     */
    public abstract boolean checkAllFilesAreUnder(AbstractVcs abstractVcs, VirtualFile[] files);

    /**
     * Returns the VCS managing the specified file.
     *
     * @param file the file to check.
     * @return the VCS instance, or null if the file does not belong to any module or the module
     *         it belongs to is not under version control.
     */
    
    public abstract AbstractVcs getVcsFor( VirtualFile file);

    /**
     * Returns the VCS managing the specified file path.
     *
     * @param file the file to check.
     * @return the VCS instance, or null if the file does not belong to any module or the module
     *         it belongs to is not under version control.
     */
    
    public abstract AbstractVcs getVcsFor(FilePath file);

    /**
     * Return the parent directory of the specified file which is mapped to a VCS.
     *
     * @param file the file for which the root is requested.
     * @return the root, or null if the specified file is not in a VCS-managed directory.
     */
    
    public abstract VirtualFile getVcsRootFor( VirtualFile file);

    /**
     * Return the parent directory of the specified file path which is mapped to a VCS.
     *
     * @param file the file for which the root is requested.
     * @return the root, or null if the specified file is not in a VCS-managed directory.
     */
    
    public abstract VirtualFile getVcsRootFor(FilePath file);

    
    public abstract VcsRoot getVcsRootObjectFor(final VirtualFile file);

    
    public abstract VcsRoot getVcsRootObjectFor(FilePath file);

    /**
     * Checks if the specified VCS is used by any of the modules in the project.
     *
     * @param vcs the VCS to check.
     * @return true if the VCS is used by any of the modules, false otherwise
     */
    public abstract boolean checkVcsIsActive(AbstractVcs vcs);

    /**
     * Checks if the VCS with the specified name is used by any of the modules in the project.
     *
     * @param vcsName the name of the VCS to check.
     * @return true if the VCS is used by any of the modules, false otherwise
     */
    public abstract boolean checkVcsIsActive( String vcsName);

    /**
     * Returns the list of VCSes used by at least one module in the project.
     *
     * @return the list of VCSes used in the project.
     */
    public abstract AbstractVcs[] getAllActiveVcss();

    public abstract boolean hasActiveVcss();

    public abstract boolean hasAnyMappings();

    public abstract void addMessageToConsoleWindow(String message, TextAttributes attributes);

    
    public abstract VcsShowSettingOption getStandardOption( VcsConfiguration.StandardOption option,
                                                            AbstractVcs vcs);

    
    public abstract VcsShowConfirmationOption getStandardConfirmation( VcsConfiguration.StandardConfirmation option,
                                                                      AbstractVcs vcs);

    
    public abstract VcsShowSettingOption getOrCreateCustomOption( String vcsActionName,
                                                                  AbstractVcs vcs);


    public abstract void showProjectOperationInfo(final UpdatedFiles updatedFiles, String displayActionName);

    /**
     * Adds a listener for receiving notifications about changes in VCS configuration for the project.
     *
     * @param listener the listener instance.
     * @deprecated use {@link #VCS_CONFIGURATION_CHANGED} instead
     * @since 6.0
     */
    public abstract void addVcsListener(VcsListener listener);

    /**
     * Removes a listener for receiving notifications about changes in VCS configuration for the project.
     *
     * @param listener the listener instance.
     * @deprecated use {@link #VCS_CONFIGURATION_CHANGED} instead
     * @since 6.0
     */
    public abstract void removeVcsListener(VcsListener listener);

    /**
     * Marks the beginning of a background VCS operation (commit or update).
     *
     * @since 6.0
     */
    public abstract void startBackgroundVcsOperation();

    /**
     * Marks the end of a background VCS operation (commit or update).
     *
     * @since 6.0
     */
    public abstract void stopBackgroundVcsOperation();

    /**
     * Checks if a background VCS operation (commit or update) is currently in progress.
     *
     * @return true if a background operation is in progress, false otherwise.
     * @since 6.0
     */
    public abstract boolean isBackgroundVcsOperationRunning();

    public abstract List<VirtualFile> getRootsUnderVcsWithoutFiltering(final AbstractVcs vcs);

    public abstract VirtualFile[] getRootsUnderVcs( AbstractVcs vcs);

    /**
     * Also includes into list all modules under roots
     */
    public abstract List<VirtualFile> getDetailedVcsMappings(final AbstractVcs vcs);

    public abstract VirtualFile[] getAllVersionedRoots();

    
    public abstract VcsRoot[] getAllVcsRoots();

    public abstract void updateActiveVcss();

    public abstract List<VcsDirectoryMapping> getDirectoryMappings();
    public abstract List<VcsDirectoryMapping> getDirectoryMappings(AbstractVcs vcs);

    
    public abstract VcsDirectoryMapping getDirectoryMappingFor(FilePath path);

    /**
     * This method can be used only when initially loading the project configuration!
     */
    public abstract void setDirectoryMapping(final String path, final String activeVcsName);

    public abstract void setDirectoryMappings(final List<VcsDirectoryMapping> items);

    public abstract void iterateVcsRoot(final VirtualFile root, final Processor<FilePath> iterator);

    public abstract void iterateVcsRoot(final VirtualFile root, final Processor<FilePath> iterator,
                                         VirtualFileFilter directoryFilter);

    
    public abstract AbstractVcs findVersioningVcs(VirtualFile file);

    public abstract CheckoutProvider.Listener getCompositeCheckoutListener();

    public abstract VcsEventsListenerManager getVcsEventsListenerManager();
    protected abstract VcsEnvironmentsProxyCreator getProxyCreator();

    public abstract VcsHistoryCache getVcsHistoryCache();
    public abstract ContentRevisionCache getContentRevisionCache();
    public abstract boolean isFileInContent(final VirtualFile vf);
    public abstract boolean isIgnored(VirtualFile vf);

    public abstract boolean dvcsUsedInProject();

    
    public abstract VcsAnnotationLocalChangesListener getAnnotationLocalChangesListener();
}
