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

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.ide.projectView.ProjectViewNode;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.projectRoots.SdkType;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.roots.ui.configuration.ProjectSettingsService;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.NavigatableWithText;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NamedLibraryElementNode extends ProjectViewNode<NamedLibraryElement> implements NavigatableWithText {
    public NamedLibraryElementNode(Project project, NamedLibraryElement value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    
    public Collection<AbstractTreeNode> getChildren() {
        final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
        LibraryGroupNode.addLibraryChildren(getValue().getOrderEntry(), children, getProject(), this);
        return children;
    }

    @Override
    public String getTestPresentation() {
        return "Library: " + getValue().getName();
    }

    private static Icon getJdkIcon(JdkOrderEntry entry) {
        final Sdk sdk = entry.getJdk();
        if (sdk == null) {
            return AllIcons.General.Jdk;
        }
        final SdkType sdkType = (SdkType) sdk.getSdkType();
        return sdkType.getIcon();
    }

    @Override
    public String getName() {
        return getValue().getName();
    }

    @Override
    public boolean contains( VirtualFile file) {
        return orderEntryContainsFile(getValue().getOrderEntry(), file);
    }

    private static boolean orderEntryContainsFile(LibraryOrSdkOrderEntry orderEntry, VirtualFile file) {
        for(OrderRootType rootType: OrderRootType.getAllTypes()) {
            if (containsFileInOrderType(orderEntry, rootType, file)) return true;
        }
        return false;
    }

    private static boolean containsFileInOrderType(final LibraryOrSdkOrderEntry orderEntry, final OrderRootType orderType, final VirtualFile file) {
        if (!orderEntry.isValid()) return false;
        VirtualFile[] files = orderEntry.getRootFiles(orderType);
        for (VirtualFile virtualFile : files) {
            boolean ancestor = VfsUtilCore.isAncestor(virtualFile, file, false);
            if (ancestor) return true;
        }
        return false;
    }

    @Override
    public void update(PresentationData presentation) {
        presentation.setPresentableText(getValue().getName());
        final OrderEntry orderEntry = getValue().getOrderEntry();
        Icon closedIcon = orderEntry instanceof JdkOrderEntry ? getJdkIcon((JdkOrderEntry)orderEntry) : AllIcons.Nodes.PpLibFolder;
        presentation.setIcon(closedIcon);
        if (orderEntry instanceof JdkOrderEntry) {
            final JdkOrderEntry jdkOrderEntry = (JdkOrderEntry)orderEntry;
            final Sdk projectJdk = jdkOrderEntry.getJdk();
            if (projectJdk != null) { //jdk not specified
                final String path = projectJdk.getHomePath();
                if (path != null) {
                    presentation.setLocationString(FileUtil.toSystemDependentName(path));
                }
            }
            presentation.setTooltip(null);
        }
        else {
            presentation.setTooltip(StringUtil.capitalize(IdeBundle.message("node.projectview.library", ((LibraryOrderEntry)orderEntry).getLibraryLevel())));
        }
    }

    @Override
    public void navigate(final boolean requestFocus) {
        ProjectSettingsService.getInstance(myProject).openLibraryOrSdkSettings(getValue().getOrderEntry());
    }

    @Override
    public boolean canNavigate() {
        return ProjectSettingsService.getInstance(myProject).canOpenLibraryOrSdkSettings(getValue().getOrderEntry());
    }

    @Override
    public String getNavigateActionText(boolean focusEditor) {
        return "Open Library Settings";
    }
}
