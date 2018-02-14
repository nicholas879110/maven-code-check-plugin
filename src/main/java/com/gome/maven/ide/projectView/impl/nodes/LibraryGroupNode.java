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

package com.gome.maven.ide.projectView.impl.nodes;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.ide.projectView.ProjectViewNode;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.roots.impl.libraries.LibraryEx;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.roots.libraries.LibraryType;
import com.gome.maven.openapi.roots.libraries.PersistentLibraryKind;
import com.gome.maven.openapi.roots.ui.configuration.ProjectSettingsService;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.PlatformIcons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LibraryGroupNode extends ProjectViewNode<LibraryGroupElement> {

    public LibraryGroupNode(Project project, LibraryGroupElement value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    public LibraryGroupNode(final Project project, final Object value, final ViewSettings viewSettings) {
        this(project, (LibraryGroupElement)value, viewSettings);
    }

    @Override
    
    public Collection<AbstractTreeNode> getChildren() {
        Module module = getValue().getModule();
        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
        final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
        for (final OrderEntry orderEntry : orderEntries) {
            if (orderEntry instanceof LibraryOrderEntry) {
                final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
                final Library library = libraryOrderEntry.getLibrary();
                if (library == null) {
                    continue;
                }
                final String libraryName = library.getName();
                if (libraryName == null || libraryName.length() == 0) {
                    addLibraryChildren(libraryOrderEntry, children, getProject(), this);
                }
                else {
                    children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(module, libraryOrderEntry), getSettings()));
                }
            }
            else if (orderEntry instanceof JdkOrderEntry) {
                final JdkOrderEntry jdkOrderEntry = (JdkOrderEntry)orderEntry;
                final Sdk jdk = jdkOrderEntry.getJdk();
                if (jdk != null) {
                    children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(module, jdkOrderEntry), getSettings()));
                }
            }
        }
        return children;
    }

    public static void addLibraryChildren(final LibraryOrSdkOrderEntry entry, final List<AbstractTreeNode> children, Project project, ProjectViewNode node) {
        final PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFile[] files =
                entry instanceof LibraryOrderEntry ? getLibraryRoots((LibraryOrderEntry)entry) : entry.getRootFiles(OrderRootType.CLASSES);
        for (final VirtualFile file : files) {
            if (!file.isValid()) continue;
            if (file.isDirectory()) {
                final PsiDirectory psiDir = psiManager.findDirectory(file);
                if (psiDir == null) {
                    continue;
                }
                children.add(new PsiDirectoryNode(project, psiDir, node.getSettings()));
            }
            else {
                final PsiFile psiFile = psiManager.findFile(file);
                if (psiFile == null) continue;
                children.add(new PsiFileNode(project, psiFile, node.getSettings()));
            }
        }
    }


    @Override
    public String getTestPresentation() {
        return "Libraries";
    }

    @Override
    public boolean contains( VirtualFile file) {
        final ProjectFileIndex index = ProjectRootManager.getInstance(getProject()).getFileIndex();
        if (!index.isInLibrarySource(file) && !index.isInLibraryClasses(file)) {
            return false;
        }

        return someChildContainsFile(file, false);
    }

    @Override
    public void update(PresentationData presentation) {
        presentation.setPresentableText(IdeBundle.message("node.projectview.libraries"));
        presentation.setIcon(PlatformIcons.LIBRARY_ICON);
    }

    @Override
    public boolean canNavigate() {
        return ProjectSettingsService.getInstance(myProject).canOpenModuleLibrarySettings();
    }

    @Override
    public void navigate(final boolean requestFocus) {
        Module module = getValue().getModule();
        ProjectSettingsService.getInstance(myProject).openModuleLibrarySettings(module);
    }

    
    public static VirtualFile[] getLibraryRoots( LibraryOrderEntry orderEntry) {
        Library library = orderEntry.getLibrary();
        if (library == null) return VirtualFile.EMPTY_ARRAY;
        OrderRootType[] rootTypes = LibraryType.DEFAULT_EXTERNAL_ROOT_TYPES;
        if (library instanceof LibraryEx) {
            if (((LibraryEx)library).isDisposed()) return VirtualFile.EMPTY_ARRAY;
            PersistentLibraryKind<?> libKind = ((LibraryEx)library).getKind();
            if (libKind != null) {
                rootTypes = LibraryType.findByKind(libKind).getExternalRootTypes();
            }
        }
        final ArrayList<VirtualFile> files = new ArrayList<VirtualFile>();
        for (OrderRootType rootType : rootTypes) {
            files.addAll(Arrays.asList(library.getFiles(rootType)));
        }
        return VfsUtilCore.toVirtualFileArray(files);
    }
}
