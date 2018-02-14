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
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.changes.LocalChangeList;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.NotNullFunction;

import java.io.File;

public interface VcsContextFactory {
    VcsContext createCachedContextOn(AnActionEvent event);

    VcsContext createContextOn(final AnActionEvent event);

    /**
     * Creates a FilePath corresponding to the specified virtual file.
     *
     * @param virtualFile the file for which the FilePath should be created.
     * @return the FilePath instance.
     */
    FilePath createFilePathOn(VirtualFile virtualFile);

    /**
     * Creates a FilePath corresponding to the specified java.io.File.
     *
     * @param file the file for which the FilePath should be created.
     * @return the FilePath instance.
     */
    FilePath createFilePathOn(File file);

    /**
     * Creates a FilePath corresponding to the specified java.io.File. Assumes that the file does not exist in the filesystem
     * and does not try to find the corresponding VirtualFile, which provides a performance benefit.
     *
     * @param file the file for which the FilePath should be created.
     * @param isDirectory whether {@code file} specifies a file or a directory.
     * @return the FilePath instance.
     */
    FilePath createFilePathOnDeleted(File file, boolean isDirectory);

    /**
     * Creates a FilePath corresponding to the specified java.io.File. If the file does not exist, uses the value
     * of the {@code isDirectory} parameter to determine if the file is a directory.
     *
     * @param file the file for which the FilePath should be created.
     * @param isDirectory whether {@code file} specifies a file or a directory.
     * @return the FilePath instance.
     */
    FilePath createFilePathOn(File file, boolean isDirectory);

    /**
     * Creates a FilePath corresponding to the specified java.io.File. If the file does not exist, uses
     * detector to determine if the file is a directory.
     *
     * @param file the file for which the FilePath should be created.
     * @param detector - called to get to know whether the file is directory, if local file is not found
     * @return the FilePath instance.
     */
    FilePath createFilePathOn(final File file, final NotNullFunction<File, Boolean> detector);

    /**
     * Creates a FilePath corresponding to the specified path in a VCS repository. Does not try to locate
     * the file in the local filesystem.
     *
     * @param path the repository path for which the FilePath should be created.
     * @param isDirectory whether {@code file} specifies a file or a directory.
     * @return the FilePath instance.
     */
    
    FilePath createFilePathOnNonLocal(String path, boolean isDirectory);

    /**
     * Creates a FilePath corresponding to a file with the specified name in the specified directory.
     * Assumes that the file does not exist in the filesystem and does not try to find the corresponding VirtualFile,
     * which provides a performance benefit.
     *
     * @param parent the containing directory for the file.
     * @param name   the name of the file.
     * @return the FilePath instance.
     */
    FilePath createFilePathOn(VirtualFile parent, String name);

    LocalChangeList createLocalChangeList(Project project,  final String name);

    class SERVICE {
        private SERVICE() {
        }

        public static VcsContextFactory getInstance() {
            return ServiceManager.getService(VcsContextFactory.class);
        }
    }
}
