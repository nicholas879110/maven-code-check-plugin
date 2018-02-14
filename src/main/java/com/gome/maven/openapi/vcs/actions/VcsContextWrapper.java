/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.actions;

import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.FilePathImpl;
import com.gome.maven.openapi.vcs.VcsDataKeys;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ChangeList;
import com.gome.maven.openapi.vcs.ui.Refreshable;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import gnu.trove.THashSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class VcsContextWrapper implements VcsContext {
    protected final DataContext myContext;
    protected final int myModifiers;
    private final String myPlace;
    private final String myActionName;

    public VcsContextWrapper(DataContext context, int modifiers, String place, String actionName) {
        myContext = context;
        myModifiers = modifiers;
        myPlace = place;
        myActionName = actionName;
    }

    @Override
    public String getPlace() {
        return myPlace;
    }

    @Override
    public
    String getActionName() {
        return myActionName;
    }

    public static VcsContext createCachedInstanceOn(AnActionEvent event) {
        return new CachedVcsContext(createInstanceOn(event));
    }

    public static VcsContextWrapper createInstanceOn(final AnActionEvent event) {
        return new VcsContextWrapper(event.getDataContext(), event.getModifiers(), event.getPlace(), event.getPresentation().getText());
    }

    @Override
    public Project getProject() {
        return CommonDataKeys.PROJECT.getData(myContext);
    }

    @Override
    public VirtualFile getSelectedFile() {
        VirtualFile[] files = getSelectedFiles();
        return files.length == 0 ? null : files[0];
    }

    @Override
    
    public VirtualFile[] getSelectedFiles() {
        VirtualFile[] fileArray = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(myContext);
        if (fileArray != null) {
            return filterLocalFiles(fileArray);
        }

        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(myContext);
        if (virtualFile != null && isLocal(virtualFile)) {
            return new VirtualFile[]{virtualFile};
        }

        return VirtualFile.EMPTY_ARRAY;
    }

    private static boolean isLocal(VirtualFile virtualFile) {
        return virtualFile.isInLocalFileSystem();
    }

    private static VirtualFile[] filterLocalFiles(VirtualFile[] fileArray) {
        ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();
        for (VirtualFile virtualFile : fileArray) {
            if (isLocal(virtualFile)) {
                result.add(virtualFile);
            }
        }
        return VfsUtilCore.toVirtualFileArray(result);
    }

    @Override
    public Editor getEditor() {
        return CommonDataKeys.EDITOR.getData(myContext);
    }

    @Override
    public Collection<VirtualFile> getSelectedFilesCollection() {
        return Arrays.asList(getSelectedFiles());
    }

    @Override
    public File getSelectedIOFile() {
        File file = VcsDataKeys.IO_FILE.getData(myContext);
        if (file != null) return file;
        File[] files = VcsDataKeys.IO_FILE_ARRAY.getData(myContext);
        if (files == null) return null;
        if (files.length == 0) return null;
        return files[0];
    }

    @Override
    public File[] getSelectedIOFiles() {
        File[] files = VcsDataKeys.IO_FILE_ARRAY.getData(myContext);
        if (files != null && files.length > 0) return files;
        File file = getSelectedIOFile();
        if (file != null) return new File[]{file};
        return null;
    }

    @Override
    public int getModifiers() {
        return myModifiers;
    }

    @Override
    public Refreshable getRefreshableDialog() {
        return Refreshable.PANEL_KEY.getData(myContext);
    }

    
    @Override
    public FilePath[] getSelectedFilePaths() {
        Set<FilePath> result = new THashSet<FilePath>();
        FilePath path = VcsDataKeys.FILE_PATH.getData(myContext);
        if (path != null) {
            result.add(path);
        }

        FilePath[] paths = VcsDataKeys.FILE_PATH_ARRAY.getData(myContext);
        if (paths != null) {
            for (FilePath filePath : paths) {
                if (!result.contains(filePath)) {
                    result.add(filePath);
                }
            }
        }

        VirtualFile[] selectedFiles = getSelectedFiles();
        for (VirtualFile selectedFile : selectedFiles) {
            FilePathImpl filePath = new FilePathImpl(selectedFile);
            result.add(filePath);
        }

        File[] selectedIOFiles = getSelectedIOFiles();
        if (selectedIOFiles != null){
            for (File selectedFile : selectedIOFiles) {
                FilePathImpl filePath = FilePathImpl.create(selectedFile);
                if (filePath != null) {
                    result.add(filePath);
                }
            }

        }

        return result.toArray(new FilePath[result.size()]);
    }

    
    @Override
    public FilePath getSelectedFilePath() {
        FilePath[] selectedFilePaths = getSelectedFilePaths();
        if (selectedFilePaths.length == 0) {
            return null;
        }
        else {
            return selectedFilePaths[0];
        }
    }

    
    @Override
    public ChangeList[] getSelectedChangeLists() {
        return VcsDataKeys.CHANGE_LISTS.getData(myContext);
    }

    
    @Override
    public Change[] getSelectedChanges() {
        return VcsDataKeys.CHANGES.getData(myContext);
    }
}
