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
package com.gome.maven.ide.projectView.impl.nodes;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.highlighter.ArchiveFileType;
import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.projectView.impl.ProjectRootsUtil;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.editor.colors.CodeInsightColors;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.roots.libraries.LibraryUtil;
import com.gome.maven.openapi.roots.ui.configuration.ProjectSettingsService;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.JarFileSystem;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.NavigatableWithText;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.PathUtil;

import java.util.ArrayList;
import java.util.Collection;

public class PsiFileNode extends BasePsiNode<PsiFile> implements NavigatableWithText {
    public PsiFileNode(Project project, PsiFile value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    public Collection<AbstractTreeNode> getChildrenImpl() {
        Project project = getProject();
        VirtualFile jarRoot = getJarRoot();
        if (project != null && jarRoot != null) {
            PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(jarRoot);
            if (psiDirectory != null) {
                return ProjectViewDirectoryHelper.getInstance(project).getDirectoryChildren(psiDirectory, getSettings(), true);
            }
        }

        return new ArrayList<AbstractTreeNode>();
    }

    private boolean isArchive() {
        VirtualFile file = getVirtualFile();
        return file != null && file.isValid() && file.getFileType() instanceof ArchiveFileType;
    }

    @Override
    protected void updateImpl(PresentationData data) {
        PsiFile value = getValue();
        data.setPresentableText(value.getName());
        data.setIcon(value.getIcon(Iconable.ICON_FLAG_READ_STATUS));

        VirtualFile file = getVirtualFile();
        if (file != null && file.is(VFileProperty.SYMLINK)) {
            String target = file.getCanonicalPath();
            if (target == null) {
                data.setAttributesKey(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
                data.setTooltip(CommonBundle.message("vfs.broken.link"));
            }
            else {
                data.setTooltip(FileUtil.toSystemDependentName(target));
            }
        }
    }

    @Override
    public VirtualFile getVirtualFile() {
        PsiFile value = getValue();
        return value != null ? value.getVirtualFile() : null;
    }

    @Override
    public boolean canNavigate() {
        return isNavigatableLibraryRoot() || super.canNavigate();
    }

    private boolean isNavigatableLibraryRoot() {
        VirtualFile jarRoot = getJarRoot();
        final Project project = getProject();
        if (jarRoot != null && ProjectRootsUtil.isLibraryRoot(jarRoot, project)) {
            final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(jarRoot, project);
            return orderEntry != null && ProjectSettingsService.getInstance(project).canOpenLibraryOrSdkSettings(orderEntry);
        }
        return false;
    }

    
    private VirtualFile getJarRoot() {
        final VirtualFile file = getVirtualFile();
        if (file == null || !file.isValid() || !(file.getFileType() instanceof ArchiveFileType)) {
            return null;
        }
        return JarFileSystem.getInstance().getJarRootForLocalFile(file);
    }

    @Override
    public void navigate(boolean requestFocus) {
        final VirtualFile jarRoot = getJarRoot();
        final Project project = getProject();
        if (requestFocus && jarRoot != null && ProjectRootsUtil.isLibraryRoot(jarRoot, project)) {
            final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(jarRoot, project);
            if (orderEntry != null) {
                ProjectSettingsService.getInstance(project).openLibraryOrSdkSettings(orderEntry);
                return;
            }
        }

        super.navigate(requestFocus);
    }

    @Override
    public String getNavigateActionText(boolean focusEditor) {
        if (isNavigatableLibraryRoot()) {
            return "Open Library Settings";
        }
        return null;
    }

    @Override
    public int getWeight() {
        return 20;
    }

    @Override
    public String getTitle() {
        VirtualFile file = getVirtualFile();
        if (file != null) {
            return FileUtil.getLocationRelativeToUserHome(file.getPresentableUrl());
        }

        return super.getTitle();
    }

    @Override
    protected boolean isMarkReadOnly() {
        return true;
    }

    @Override
    public Comparable getTypeSortKey() {
        String extension = extension(getValue());
        return extension == null ? null : new ExtensionSortKey(extension);
    }

    
    public static String extension( PsiFile file) {
        if (file != null) {
            VirtualFile vFile = file.getVirtualFile();
            if (vFile != null) {
                return vFile.getFileType().getDefaultExtension();
            }
        }

        return null;
    }

    public static class ExtensionSortKey implements Comparable {
        private final String myExtension;

        public ExtensionSortKey(final String extension) {
            myExtension = extension;
        }

        @Override
        public int compareTo(final Object o) {
            if (!(o instanceof ExtensionSortKey)) return 0;
            ExtensionSortKey rhs = (ExtensionSortKey) o;
            return myExtension.compareTo(rhs.myExtension);
        }
    }

    @Override
    public boolean shouldDrillDownOnEmptyElement() {
        final PsiFile file = getValue();
        return file != null && file.getFileType() == StdFileTypes.JAVA;
    }

    @Override
    public boolean canRepresent(final Object element) {
        return super.canRepresent(element) || getValue() != null && element != null && element.equals(getValue().getVirtualFile());
    }

    @Override
    public boolean contains( VirtualFile file) {
        return super.contains(file) || isArchive() && Comparing.equal(PathUtil.getLocalFile(file), getVirtualFile());
    }
}
