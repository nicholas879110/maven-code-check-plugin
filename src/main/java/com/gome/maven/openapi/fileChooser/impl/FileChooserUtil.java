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
package com.gome.maven.openapi.fileChooser.impl;

import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.fileChooser.FileSaverDescriptor;
import com.gome.maven.openapi.fileChooser.PathChooserDialog;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.List;

public final class FileChooserUtil {
    private static final String LAST_OPENED_FILE_PATH = "last_opened_file_path";

    
    public static VirtualFile getLastOpenedFile( final Project project) {
        if (project != null) {
            final String path = PropertiesComponent.getInstance(project).getValue(LAST_OPENED_FILE_PATH);
            if (path != null) {
                return LocalFileSystem.getInstance().findFileByPath(path);
            }
        }
        return null;
    }

    public static void setLastOpenedFile( final Project project,  final VirtualFile file) {
        if (project != null && !project.isDisposed() && file != null) {
            PropertiesComponent.getInstance(project).setValue(LAST_OPENED_FILE_PATH, file.getPath());
        }
    }

    
    public static VirtualFile getFileToSelect( FileChooserDescriptor descriptor,  Project project,
                                               VirtualFile toSelect,  VirtualFile lastPath) {
        boolean chooseDir = descriptor instanceof FileSaverDescriptor;
        VirtualFile result;

        if (toSelect == null && lastPath == null) {
            result = project == null? null : project.getBaseDir();
        }
        else if (toSelect != null && lastPath != null) {
            if (Boolean.TRUE.equals(descriptor.getUserData(PathChooserDialog.PREFER_LAST_OVER_EXPLICIT))) {
                result = lastPath;
            }
            else {
                result = toSelect;
            }
        }
        else if (toSelect == null) {
            result = lastPath;
        }
        else {
            result = toSelect;
        }

        if (result != null) {
            if (chooseDir && !result.isDirectory()) {
                result = result.getParent();
            }
        }
        else if (SystemInfo.isUnix) {
            result = VfsUtil.getUserHomeDir();
        }

        return result;
    }

    
    public static List<VirtualFile> getChosenFiles( final FileChooserDescriptor descriptor,
                                                    final List<VirtualFile> selectedFiles) {
        return ContainerUtil.mapNotNull(selectedFiles, new NullableFunction<VirtualFile, VirtualFile>() {
            @Override
            public VirtualFile fun(final VirtualFile file) {
                return file != null && file.isValid() ? descriptor.getFileToSelect(file) : null;
            }
        });
    }
}
