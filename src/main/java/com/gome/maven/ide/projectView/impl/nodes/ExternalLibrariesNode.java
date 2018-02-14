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
 * @author max
 */
package com.gome.maven.ide.projectView.impl.nodes;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.ide.projectView.ProjectViewNode;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.roots.libraries.Library;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.PathUtil;
import com.gome.maven.util.PlatformIcons;
import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ExternalLibrariesNode extends ProjectViewNode<String> {
    public ExternalLibrariesNode(Project project, ViewSettings viewSettings) {
        super(project, "External Libraries", viewSettings);
    }

    @Override
    public boolean contains( VirtualFile file) {
        ProjectFileIndex index = ProjectRootManager.getInstance(getProject()).getFileIndex();
        if (!index.isInLibrarySource(file) && !index.isInLibraryClasses(file)) return false;

        return someChildContainsFile(file, false);
    }

    
    @Override
    public Collection<? extends AbstractTreeNode> getChildren() {
        final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        Set<Library> processedLibraries = new THashSet<Library>();
        Set<Sdk> processedSdk = new THashSet<Sdk>();

        for (Module module : modules) {
            final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
            for (final OrderEntry orderEntry : orderEntries) {
                if (orderEntry instanceof LibraryOrderEntry) {
                    final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
                    final Library library = libraryOrderEntry.getLibrary();
                    if (library == null) continue;
                    if (processedLibraries.contains(library)) continue;
                    processedLibraries.add(library);

                    if (!hasExternalEntries(fileIndex, libraryOrderEntry)) continue;

                    final String libraryName = library.getName();
                    if (libraryName == null || libraryName.length() == 0) {
                        addLibraryChildren(libraryOrderEntry, children, getProject(), this);
                    }
                    else {
                        children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(null, libraryOrderEntry), getSettings()));
                    }
                }
                else if (orderEntry instanceof JdkOrderEntry) {
                    final JdkOrderEntry jdkOrderEntry = (JdkOrderEntry)orderEntry;
                    final Sdk jdk = jdkOrderEntry.getJdk();
                    if (jdk != null) {
                        if (processedSdk.contains(jdk)) continue;
                        processedSdk.add(jdk);
                        children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(null, jdkOrderEntry), getSettings()));
                    }
                }
            }
        }
        return children;
    }

    public static void addLibraryChildren(final LibraryOrderEntry entry, final List<AbstractTreeNode> children, Project project, ProjectViewNode node) {
        final PsiManager psiManager = PsiManager.getInstance(project);
        final VirtualFile[] files = entry.getRootFiles(OrderRootType.CLASSES);
        for (final VirtualFile file : files) {
            final PsiDirectory psiDir = psiManager.findDirectory(file);
            if (psiDir == null) {
                continue;
            }
            children.add(new PsiDirectoryNode(project, psiDir, node.getSettings()));
        }
    }

    private static boolean hasExternalEntries(ProjectFileIndex index, LibraryOrderEntry orderEntry) {
        for (VirtualFile file : LibraryGroupNode.getLibraryRoots(orderEntry)) {
            if (!index.isInContent(PathUtil.getLocalFile(file))) return true;
        }
        return false;
    }


    @Override
    protected void update(PresentationData presentation) {
        presentation.setPresentableText(IdeBundle.message("node.projectview.external.libraries"));
        presentation.setIcon(PlatformIcons.LIBRARY_ICON);
    }
}
