/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.ide.IconProvider;
import com.gome.maven.ide.bookmarks.Bookmark;
import com.gome.maven.ide.bookmarks.BookmarkManager;
import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.ide.projectView.ProjectView;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.projectView.impl.ProjectRootsUtil;
import com.gome.maven.ide.projectView.impl.ProjectViewImpl;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileTypes.FileTypeRegistry;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.roots.SourceFolder;
import com.gome.maven.openapi.roots.libraries.LibraryUtil;
import com.gome.maven.openapi.roots.ui.configuration.ModuleSourceRootEditHandler;
import com.gome.maven.openapi.roots.ui.configuration.ProjectSettingsService;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.NavigatableWithText;
import com.gome.maven.projectImport.ProjectAttachProcessor;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.impl.file.PsiDirectoryFactory;
import com.gome.maven.ui.LayeredIcon;
import com.gome.maven.ui.RowIcon;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.util.IconUtil;
import com.gome.maven.util.PathUtil;
import com.gome.maven.util.PlatformIcons;
import com.gome.maven.util.PlatformUtils;

import javax.swing.*;
import java.util.Collection;
import java.util.Locale;

public class PsiDirectoryNode extends BasePsiNode<PsiDirectory> implements NavigatableWithText {
    public PsiDirectoryNode(Project project, PsiDirectory value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    protected boolean shouldShowModuleName() {
        return !PlatformUtils.isCidr();
    }

    @Override
    protected void updateImpl(PresentationData data) {
        Project project = getProject();
        assert project != null : this;
        PsiDirectory psiDirectory = getValue();
        assert psiDirectory != null : this;
        VirtualFile directoryFile = psiDirectory.getVirtualFile();
        Object parentValue = getParentValue();

        if (ProjectRootsUtil.isModuleContentRoot(directoryFile, project)) {
            ProjectFileIndex fi = ProjectRootManager.getInstance(project).getFileIndex();
            Module module = fi.getModuleForFile(directoryFile);

            data.setPresentableText(directoryFile.getName());
            if (module != null) {
                if (!(parentValue instanceof Module )) {
                    if (!shouldShowModuleName()) {
                        data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    }
                    else if (Comparing.equal(module.getName(), directoryFile.getName())) {
                        data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    }
                    else {
                        data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                        data.addText("[" + module.getName() + "]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    }
                }
                else {
                    data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }

                if (parentValue instanceof Module || parentValue instanceof Project) {
                    final String location = FileUtil.getLocationRelativeToUserHome(directoryFile.getPresentableUrl());
                    data.addText(" (" + location + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                else {
                    SourceFolder sourceRoot = ProjectRootsUtil.getModuleSourceRoot(directoryFile, project);
                    if (sourceRoot != null) {
                        String rootTypeName = ModuleSourceRootEditHandler.getEditHandler(sourceRoot.getRootType()).getRootTypeName();
                        data.addText(" (" + rootTypeName.toLowerCase(Locale.getDefault()) + " root)",  SimpleTextAttributes.GRAY_ATTRIBUTES);
                    }
                }

                setupIcon(data, psiDirectory);

                return;
            }
        }

        String name = parentValue instanceof Project
                ? psiDirectory.getVirtualFile().getPresentableUrl()
                : ProjectViewDirectoryHelper.getInstance(psiDirectory.getProject()).getNodeName(getSettings(), parentValue, psiDirectory);
        if (name == null) {
            setValue(null);
            return;
        }

        data.setPresentableText(name);
        data.setLocationString(ProjectViewDirectoryHelper.getInstance(project).getLocationString(psiDirectory));

        setupIcon(data, psiDirectory);
    }

    protected void setupIcon(PresentationData data, PsiDirectory psiDirectory) {
        final VirtualFile virtualFile = psiDirectory.getVirtualFile();
        if (PlatformUtils.isAppCode()) {
            final Icon icon = IconUtil.getIcon(virtualFile, 0, myProject);
            if (icon != null) {
                data.setIcon(patchIcon(icon, virtualFile));
            }
        }
        else {
            for (final IconProvider provider : Extensions.getExtensions(IconProvider.EXTENSION_POINT_NAME)) {
                final Icon icon = provider.getIcon(psiDirectory, 0);
                if (icon != null) {
                    data.setIcon(patchIcon(icon, virtualFile));
                    return;
                }
            }
        }
    }

    @Override
    public Collection<AbstractTreeNode> getChildrenImpl() {
        return ProjectViewDirectoryHelper.getInstance(myProject).getDirectoryChildren(getValue(), getSettings(), true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTestPresentation() {
        return "PsiDirectory: " + getValue().getName();
    }

    public boolean isFQNameShown() {
        return ProjectViewDirectoryHelper.getInstance(getProject()).isShowFQName(getSettings(), getParentValue(), getValue());
    }

    @Override
    public boolean contains( VirtualFile file) {
        final PsiDirectory value = getValue();
        if (value == null) {
            return false;
        }

        VirtualFile directory = value.getVirtualFile();
        if (directory.getFileSystem() instanceof LocalFileSystem) {
            file = PathUtil.getLocalFile(file);
        }

        if (!VfsUtilCore.isAncestor(directory, file, false)) {
            return false;
        }

        if (Registry.is("ide.hide.excluded.files")) {
            final Project project = value.getProject();
            final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            return !fileIndex.isExcluded(file);
        }
        else {
            return !FileTypeRegistry.getInstance().isFileIgnored(file);
        }
    }

    @Override
    public VirtualFile getVirtualFile() {
        PsiDirectory directory = getValue();
        if (directory == null) return null;
        return directory.getVirtualFile();
    }

    @Override
    public boolean canRepresent(final Object element) {
        if (super.canRepresent(element)) return true;
        PsiDirectory directory = getValue();
        if (directory == null) return false;
        return ProjectViewDirectoryHelper.getInstance(getProject()).canRepresent(element, directory);
    }

    @Override
    public boolean canNavigate() {
        VirtualFile file = getVirtualFile();
        Project project = getProject();

        ProjectSettingsService service = ProjectSettingsService.getInstance(myProject);
        return file != null && ((ProjectRootsUtil.isModuleContentRoot(file, project) && service.canOpenModuleSettings()) ||
                (ProjectRootsUtil.isModuleSourceRoot(file, project)  && service.canOpenContentEntriesSettings()) ||
                (ProjectRootsUtil.isLibraryRoot(file, project) && service.canOpenModuleLibrarySettings()));
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public void navigate(final boolean requestFocus) {
        Module module = ModuleUtil.findModuleForPsiElement(getValue());
        if (module != null) {
            final VirtualFile file = getVirtualFile();
            final Project project = getProject();
            ProjectSettingsService service = ProjectSettingsService.getInstance(myProject);
            if (ProjectRootsUtil.isModuleContentRoot(file, project)) {
                service.openModuleSettings(module);
            }
            else if (ProjectRootsUtil.isLibraryRoot(file, project)) {
                final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(file, module.getProject());
                if (orderEntry != null) {
                    service.openLibraryOrSdkSettings(orderEntry);
                }
            }
            else {
                service.openContentEntriesSettings(module);
            }
        }
    }

    @Override
    public String getNavigateActionText(boolean focusEditor) {
        VirtualFile file = getVirtualFile();
        Project project = getProject();

        if (file != null && project != null) {
            if (ProjectRootsUtil.isModuleContentRoot(file, project) || ProjectRootsUtil.isModuleSourceRoot(file, project)) {
                return "Open Module Settings";
            }
            if (ProjectRootsUtil.isLibraryRoot(file, project)) {
                return "Open Library Settings";
            }
        }

        return null;
    }

    @Override
    public int getWeight() {
        final ProjectView projectView = ProjectView.getInstance(myProject);
        if (projectView instanceof ProjectViewImpl && !((ProjectViewImpl)projectView).isFoldersAlwaysOnTop()) {
            return 20;
        }
        return isFQNameShown() ? 70 : 0;
    }

    @Override
    public String getTitle() {
        final PsiDirectory directory = getValue();
        if (directory != null) {
            return PsiDirectoryFactory.getInstance(getProject()).getQualifiedName(directory, true);
        }
        return super.getTitle();
    }

    protected Icon patchIcon(Icon original, VirtualFile file) {
        Icon icon = original;

        final Bookmark bookmarkAtFile = BookmarkManager.getInstance(myProject).findFileBookmark(file);
        if (bookmarkAtFile != null) {
            final RowIcon composite = new RowIcon(2, RowIcon.Alignment.CENTER);
            composite.setIcon(icon, 0);
            composite.setIcon(bookmarkAtFile.getIcon(), 1);
            icon = composite;
        }

        if (!file.isWritable()) {
            icon = LayeredIcon.create(icon, PlatformIcons.LOCKED_ICON);
        }

        if (file.is(VFileProperty.SYMLINK)) {
            icon = LayeredIcon.create(icon, PlatformIcons.SYMLINK_ICON);
        }

        return icon;
    }

    @Override
    public Comparable getSortKey() {
        if (ProjectAttachProcessor.canAttachToProject()) {
            // primary module is always on top; attached modules are sorted alphabetically
            final VirtualFile file = getVirtualFile();
            if (Comparing.equal(file, myProject.getBaseDir())) {
                return "";    // sorts before any other name
            }
            return getTitle();
        }
        return null;
    }

    @Override
    public Comparable getTypeSortKey() {
        VirtualFile file = getVirtualFile();
        if (file != null) {
            String extension = file.getExtension();
            if (extension != null) {
                return new PsiFileNode.ExtensionSortKey(extension);
            }
        }
        return null;
    }

    @Override
    public String getQualifiedNameSortKey() {
        final PsiDirectoryFactory factory = PsiDirectoryFactory.getInstance(getProject());
        return factory.getQualifiedName(getValue(), true);
    }

    @Override
    public int getTypeSortWeight(final boolean sortByType) {
        return 3;
    }

    @Override
    public boolean shouldDrillDownOnEmptyElement() {
        return true;
    }

    @Override
    public boolean isAlwaysShowPlus() {
        final VirtualFile file = getVirtualFile();
        return file == null || file.getChildren().length > 0;
    }
}
