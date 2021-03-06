/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.readOnlyHandler;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.IdeEventQueue;
import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.MultiValuesMap;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashSet;

import java.util.*;

@State(
        name="ReadonlyStatusHandler",
        storages= {
                @Storage(
                        file = StoragePathMacros.WORKSPACE_FILE
                )}
)
public class ReadonlyStatusHandlerImpl extends ReadonlyStatusHandler implements PersistentStateComponent<ReadonlyStatusHandlerImpl.State> {
    private final Project myProject;
    private final WritingAccessProvider[] myAccessProviders;

    public static class State {
        public boolean SHOW_DIALOG = true;
    }

    private State myState = new State();

    public ReadonlyStatusHandlerImpl(Project project) {
        myProject = project;
        myAccessProviders = WritingAccessProvider.getProvidersForProject(myProject);
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }

    @Override
    public OperationStatus ensureFilesWritable( VirtualFile... files) {
        if (files.length == 0) {
            return new OperationStatusImpl(VirtualFile.EMPTY_ARRAY);
        }
        ApplicationManager.getApplication().assertIsDispatchThread();

        Set<VirtualFile> realFiles = new THashSet<VirtualFile>(files.length);
        for (VirtualFile file : files) {
            if (file instanceof VirtualFileWindow) file = ((VirtualFileWindow)file).getDelegate();
            if (file != null) {
                realFiles.add(file);
            }
        }
        files = VfsUtilCore.toVirtualFileArray(realFiles);

        for (final WritingAccessProvider accessProvider : myAccessProviders) {
            Collection<VirtualFile> denied = ContainerUtil.filter(files, new Condition<VirtualFile>() {
                @Override
                public boolean value(final VirtualFile virtualFile) {
                    return !accessProvider.isPotentiallyWritable(virtualFile);
                }
            });

            if (denied.isEmpty()) {
                denied = accessProvider.requestWriting(files);
            }
            if (!denied.isEmpty()) {
                return new OperationStatusImpl(VfsUtilCore.toVirtualFileArray(denied));
            }
        }

        final FileInfo[] fileInfos = createFileInfos(files);
        if (fileInfos.length == 0) { // if all files are already writable
            return createResultStatus(files);
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return createResultStatus(files);
        }

        // This event count hack is necessary to allow actions that called this stuff could still get data from their data contexts.
        // Otherwise data manager stuff will fire up an assertion saying that event count has been changed (due to modal dialog show-up)
        // The hack itself is safe since we guarantee that focus will return to the same component had it before modal dialog have been shown.
        final int savedEventCount = IdeEventQueue.getInstance().getEventCount();
        if (myState.SHOW_DIALOG) {
            new ReadOnlyStatusDialog(myProject, fileInfos).show();
        }
        else {
            processFiles(new ArrayList<FileInfo>(Arrays.asList(fileInfos)), null); // the collection passed is modified
        }
        IdeEventQueue.getInstance().setEventCount(savedEventCount);
        return createResultStatus(files);
    }

    private static OperationStatus createResultStatus(final VirtualFile[] files) {
        List<VirtualFile> readOnlyFiles = new ArrayList<VirtualFile>();
        for (VirtualFile file : files) {
            if (file.exists()) {
                if (!file.isWritable()) {
                    readOnlyFiles.add(file);
                }
            }
        }

        return new OperationStatusImpl(VfsUtilCore.toVirtualFileArray(readOnlyFiles));
    }

    private FileInfo[] createFileInfos(VirtualFile[] files) {
        List<FileInfo> fileInfos = new ArrayList<FileInfo>();
        for (final VirtualFile file : files) {
            if (file != null && !file.isWritable() && file.isInLocalFileSystem()) {
                fileInfos.add(new FileInfo(file, myProject));
            }
        }
        return fileInfos.toArray(new FileInfo[fileInfos.size()]);
    }

    public static void processFiles(final List<FileInfo> fileInfos,  String changelist) {
        FileInfo[] copy = fileInfos.toArray(new FileInfo[fileInfos.size()]);
        MultiValuesMap<HandleType, VirtualFile> handleTypeToFile = new MultiValuesMap<HandleType, VirtualFile>();
        for (FileInfo fileInfo : copy) {
            handleTypeToFile.put(fileInfo.getSelectedHandleType(), fileInfo.getFile());
        }

        for (HandleType handleType : handleTypeToFile.keySet()) {
            handleType.processFiles(handleTypeToFile.get(handleType), changelist);
        }

        for (FileInfo fileInfo : copy) {
            if (!fileInfo.getFile().exists() || fileInfo.getFile().isWritable()) {
                fileInfos.remove(fileInfo);
            }
        }
    }

    private static class OperationStatusImpl extends OperationStatus {

        private final VirtualFile[] myReadonlyFiles;

        OperationStatusImpl(final VirtualFile[] readonlyFiles) {
            myReadonlyFiles = readonlyFiles;
        }

        @Override
        
        public VirtualFile[] getReadonlyFiles() {
            return myReadonlyFiles;
        }

        @Override
        public boolean hasReadonlyFiles() {
            return myReadonlyFiles.length > 0;
        }

        @Override
        
        public String getReadonlyFilesMessage() {
            if (hasReadonlyFiles()) {
                StringBuilder buf = new StringBuilder();
                if (myReadonlyFiles.length > 1) {
                    for (VirtualFile file : myReadonlyFiles) {
                        buf.append('\n');
                        buf.append(file.getPresentableUrl());
                    }

                    return CommonBundle.message("failed.to.make.the.following.files.writable.error.message", buf.toString());
                }
                else {
                    return CommonBundle.message("failed.to.make.file.writeable.error.message", myReadonlyFiles[0].getPresentableUrl());
                }
            }
            throw new RuntimeException("No readonly files");
        }
    }
}
