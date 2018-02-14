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

import com.gome.maven.ide.highlighter.ProjectFileType;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.roots.JdkOrderEntry;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.roots.libraries.LibraryUtil;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.LocalFileProvider;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.Locale;

public class ProjectUtilCore {
    public static String displayUrlRelativeToProject( VirtualFile file,
                                                      String url,
                                                      Project project,
                                                     boolean includeFilePath,
                                                     boolean keepModuleAlwaysOnTheLeft) {
        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null && includeFilePath) {
            //noinspection ConstantConditions
            final String projectHomeUrl = baseDir.getPresentableUrl();
            if (url.startsWith(projectHomeUrl)) {
                url = "..." + url.substring(projectHomeUrl.length());
            }
        }

        if (SystemInfo.isMac && file.getFileSystem() instanceof LocalFileProvider) {
            final VirtualFile fileForJar = ((LocalFileProvider)file.getFileSystem()).getLocalVirtualFileFor(file);
            if (fileForJar != null) {
                final OrderEntry libraryEntry = LibraryUtil.findLibraryEntry(file, project);
                if (libraryEntry != null) {
                    if (libraryEntry instanceof JdkOrderEntry) {
                        url = url + " - [" + ((JdkOrderEntry)libraryEntry).getJdkName() + "]";
                    }
                    else {
                        url = url + " - [" + libraryEntry.getPresentableName() + "]";
                    }
                }
                else {
                    url = url + " - [" + fileForJar.getName() + "]";
                }
            }
        }

        final Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) return url;
        return !keepModuleAlwaysOnTheLeft && SystemInfo.isMac ?
                url + " - [" + module.getName() + "]" :
                "[" + module.getName() + "] - " + url;
    }

    public static String getPresentableName( Project project) {
        if (project.isDefault()) {
            return project.getName();
        }

        String location = project.getPresentableUrl();
        if (location == null) {
            return null;
        }

        String projectName = FileUtil.toSystemIndependentName(location);
        if (projectName.endsWith("/")) {
            projectName = projectName.substring(0, projectName.length() - 1);
        }

        final int lastSlash = projectName.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < projectName.length()) {
            projectName = projectName.substring(lastSlash + 1);
        }

        if (StringUtil.endsWithIgnoreCase(projectName, ProjectFileType.DOT_DEFAULT_EXTENSION)) {
            projectName = projectName.substring(0, projectName.length() - ProjectFileType.DOT_DEFAULT_EXTENSION.length());
        }

        projectName = projectName.toLowerCase(Locale.US).replace(':', '_'); // replace ':' from windows drive names
        return projectName;
    }
}
