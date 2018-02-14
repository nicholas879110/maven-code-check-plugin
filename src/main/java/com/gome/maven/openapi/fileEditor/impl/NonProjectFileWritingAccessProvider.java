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
package com.gome.maven.openapi.fileEditor.impl;

import com.gome.maven.ProjectTopics;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.StorageScheme;
import com.gome.maven.openapi.components.impl.stores.IProjectStore;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.roots.ModuleRootAdapter;
import com.gome.maven.openapi.roots.ModuleRootEvent;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.NotNullLazyKey;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.util.NotNullFunction;
import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.SmartList;

import java.util.*;

public class NonProjectFileWritingAccessProvider extends WritingAccessProvider {
    public enum AccessStatus {REQUESTED, ALLOWED}

    private static final Key<Boolean> ENABLE_IN_TESTS = Key.create("NON_PROJECT_FILE_ACCESS_ENABLE_IN_TESTS");
    private static final Key<Boolean> ALL_ACCESS_ALLOWED = Key.create("NON_PROJECT_FILE_ALL_ACCESS_STATUS");
    private static final NotNullLazyKey<Map<VirtualFile, AccessStatus>, Project> ACCESS_STATUS
            = NotNullLazyKey.create("NON_PROJECT_FILE_ACCESS_STATUS", new NotNullFunction<Project, Map<VirtualFile, AccessStatus>>() {
        
        @Override
        public Map<VirtualFile, AccessStatus> fun(Project project) {
            return new HashMap<VirtualFile, AccessStatus>();
        }
    });

     private final Project myProject;
     private static NullableFunction<List<VirtualFile>, UnlockOption> ourCustomUnlocker;

    
    public static void setCustomUnlocker( NullableFunction<List<VirtualFile>, UnlockOption> unlocker) {
        ourCustomUnlocker = unlocker;
    }

    public NonProjectFileWritingAccessProvider( final Project project) {
        myProject = project;
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            @Override
            public void fileDeleted( VirtualFileEvent event) {
                getRegisteredFiles(project).remove(event.getFile());
            }
        }, project);

        myProject.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                Map<VirtualFile, AccessStatus> files = getRegisteredFiles(project);

                // reset access status and notifications for files that became project files  
                for (VirtualFile each : new ArrayList<VirtualFile>(files.keySet())) {
                    if (isProjectFile(each, project)) {
                        files.remove(each);
                    }
                }
            }
        });
    }

    @Override
    public boolean isPotentiallyWritable( VirtualFile file) {
        return true;
    }

    
    @Override
    public Collection<VirtualFile> requestWriting(VirtualFile... files) {
        if (allAccessAllowed(myProject)) return Collections.emptyList();

        List<VirtualFile> deniedFiles = new SmartList<VirtualFile>();

        Map<VirtualFile, AccessStatus> statuses = getRegisteredFiles(myProject);
        for (VirtualFile each : files) {
            if (statuses.get(each) == AccessStatus.ALLOWED) continue;

            if (!(each.getFileSystem() instanceof LocalFileSystem)) continue; // do not block e.g., HttpFileSystem, LightFileSystem etc.  
            if (isProjectFile(each, myProject)) {
                statuses.remove(each);
                continue;
            }

            statuses.put(each, AccessStatus.REQUESTED);
            deniedFiles.add(each);
        }

        if (deniedFiles.isEmpty()) return Collections.emptyList();

        UnlockOption unlockOption = askToUnlock(deniedFiles);

        if (unlockOption == null) return deniedFiles;

        switch (unlockOption) {
            case UNLOCK:
                for (VirtualFile eachAllowed : deniedFiles) {
                    statuses.put(eachAllowed, AccessStatus.ALLOWED);
                }
                break;
            case UNLOCK_ALL:
                myProject.putUserData(ALL_ACCESS_ALLOWED, Boolean.TRUE);
                break;
        }

        return Collections.emptyList();
    }

    
    private UnlockOption askToUnlock( List<VirtualFile> files) {
        if (ourCustomUnlocker != null) return ourCustomUnlocker.fun(files);

        NonProjectFileWritingAccessDialog dialog = new NonProjectFileWritingAccessDialog(myProject, files);
        if (!dialog.showAndGet()) return null;
        return dialog.getUnlockOption();
    }

    public static boolean isWriteAccessAllowedExplicitly( VirtualFile file,  Project project) {
        if (!(file.getFileSystem() instanceof LocalFileSystem)) return false;
        Map<VirtualFile, AccessStatus> statuses = getRegisteredFiles(project);
        return statuses.get(file) == AccessStatus.ALLOWED ||
                isProjectFile(file, project);
    }

    private static boolean isProjectFile( VirtualFile file,  Project project) {
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        if (fileIndex.isInContent(file)) return true;
        if (!Registry.is("ide.hide.excluded.files") && fileIndex.isExcluded(file) && !fileIndex.isUnderIgnored(file)) return true;

        if (project instanceof ProjectEx) {
            IProjectStore store = ((ProjectEx)project).getStateStore();

            if (store.getStorageScheme() == StorageScheme.DIRECTORY_BASED) {
                VirtualFile baseDir = project.getBaseDir();
                VirtualFile dotIdea = baseDir == null ? null : baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
                if (dotIdea != null && VfsUtilCore.isAncestor(dotIdea, file, false)) return true;
            }

            if (file.equals(store.getWorkspaceFile()) || file.equals(store.getProjectFile())) return true;
            for (Module each : ModuleManager.getInstance(project).getModules()) {
                if (file.equals(each.getModuleFile())) return true;
            }
        }

        for (NonProjectFileWritingAccessExtension each : Extensions.getExtensions(NonProjectFileWritingAccessExtension.EP_NAME, project)) {
            if(each.isWritable(file)) return true;
        }

        return false;
    }

    
    public static void enableChecksInTests( Project project, boolean enable) {
        project.putUserData(ENABLE_IN_TESTS, enable ? Boolean.TRUE : null);
    }

    private static boolean allAccessAllowed( Project project) {
        // disable checks in tests, if not asked
        if (ApplicationManager.getApplication().isUnitTestMode() && project.getUserData(ENABLE_IN_TESTS) != Boolean.TRUE) {
            return true;
        }

        return project.getUserData(ALL_ACCESS_ALLOWED) == Boolean.TRUE;
    }

    
    public static AccessStatus getAccessStatus( Project project,  VirtualFile file) {
        return allAccessAllowed(project) ? AccessStatus.ALLOWED : getRegisteredFiles(project).get(file);
    }

    
    private static Map<VirtualFile, AccessStatus> getRegisteredFiles( Project project) {
        return ACCESS_STATUS.getValue(project);
    }

    public enum UnlockOption {UNLOCK, UNLOCK_ALL}
}
