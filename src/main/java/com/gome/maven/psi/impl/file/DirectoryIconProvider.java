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

/*
 * User: anna
 * Date: 23-Jan-2008
 */
package com.gome.maven.psi.impl.file;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IconProvider;
import com.gome.maven.ide.projectView.impl.ProjectRootsUtil;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.roots.SourceFolder;
import com.gome.maven.openapi.roots.ui.configuration.SourceRootPresentation;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;

public class DirectoryIconProvider extends IconProvider implements DumbAware {
    @Override
    public Icon getIcon( final PsiElement element, final int flags) {
        if (element instanceof PsiDirectory) {
            final PsiDirectory psiDirectory = (PsiDirectory)element;
            final VirtualFile vFile = psiDirectory.getVirtualFile();
            Project project = psiDirectory.getProject();
            SourceFolder sourceFolder = ProjectRootsUtil.getModuleSourceRoot(vFile, project);
            if (sourceFolder != null) {
                return SourceRootPresentation.getSourceRootIcon(sourceFolder);
            }
            else {
                if (!Registry.is("ide.hide.excluded.files")) {
                    boolean ignored = ProjectRootManager.getInstance(project).getFileIndex().isExcluded(vFile);
                    if (ignored) {
                        return AllIcons.Modules.ExcludeRoot;
                    }
                }
                return PlatformIcons.DIRECTORY_CLOSED_ICON;
            }
        }
        return null;
    }
}
