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
package com.gome.maven.openapi.project;

import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.fileEditor.UniqueVFilePathBuilder;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFilePathWrapper;

import javax.swing.*;

/**
 * @author max
 */
public class ProjectUtil {
    private ProjectUtil() {
    }


    public static String getProjectLocationString( final Project project) {
        return FileUtil.getLocationRelativeToUserHome(project.getBasePath());
    }


    public static String calcRelativeToProjectPath( final VirtualFile file,
                                                    final Project project,
                                                   final boolean includeFilePath) {
        return calcRelativeToProjectPath(file, project, includeFilePath, false, false);
    }


    public static String calcRelativeToProjectPath( final VirtualFile file,
                                                    final Project project,
                                                   final boolean includeFilePath,
                                                   final boolean includeUniqueFilePath,
                                                   final boolean keepModuleAlwaysOnTheLeft) {
        if (file instanceof VirtualFilePathWrapper && ((VirtualFilePathWrapper)file).enforcePresentableName()) {
            return includeFilePath ? ((VirtualFilePathWrapper)file).getPresentablePath() : file.getName();
        }
        String url;
        if (includeFilePath) {
            url = file.getPresentableUrl();
        }
        else if (includeUniqueFilePath) {
            url = UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(project, file);
        }
        else {
            url = file.getName();
        }
        if (project == null) {
            return url;
        }
        return ProjectUtilCore.displayUrlRelativeToProject(file, url, project, includeFilePath, keepModuleAlwaysOnTheLeft);
    }

    public static String calcRelativeToProjectPath(final VirtualFile file, final Project project) {
        return calcRelativeToProjectPath(file, project, true);
    }


    public static Project guessProjectForFile(VirtualFile file) {
        return ProjectLocator.getInstance().guessProjectForFile(file);
    }


    public static Project guessProjectForContentFile( VirtualFile file) {
        return guessProjectForContentFile(file, file.getFileType());
    }


    /***
     * guessProjectForFile works incorrectly - even if file is config (idea config file) first opened project will be returned
     */
    public static Project guessProjectForContentFile( VirtualFile file,  FileType fileType) {
        if (isProjectOrWorkspaceFile(file, fileType)) {
            return null;
        }

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDefault() && project.isInitialized() && !project.isDisposed() && ProjectRootManager.getInstance(project).getFileIndex().isInContent(file)) {
                return project;
            }
        }

        return null;
    }

    public static boolean isProjectOrWorkspaceFile(final VirtualFile file) {
        // do not use file.getFileType() to avoid autodetection by content loading for arbitrary files
        return isProjectOrWorkspaceFile(file, FileTypeManager.getInstance().getFileTypeByFileName(file.getName()));
    }

    public static boolean isProjectOrWorkspaceFile( VirtualFile file,  FileType fileType) {
        return ProjectCoreUtil.isProjectOrWorkspaceFile(file, fileType);
    }


    public static Project guessCurrentProject( JComponent component) {
        Project project = null;
        if (component != null) {
            project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(component));
        }
        if (project == null) {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            if (openProjects.length > 0) project = openProjects[0];
            if (project == null) {
                DataContext dataContext = DataManager.getInstance().getDataContext();
                project = CommonDataKeys.PROJECT.getData(dataContext);
            }
            if (project == null) {
                project = ProjectManager.getInstance().getDefaultProject();
            }
        }
        return project;
    }
}
