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
 * Date: 16-Jan-2008
 */
package com.gome.maven.packageDependencies.ui;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.ToggleAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.packageDependencies.DependencyUISettings;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.search.scope.packageSet.FilePatternPackageSet;
import com.gome.maven.psi.search.scope.packageSet.PackageSet;

import javax.swing.*;
import java.util.Set;

public class ProjectPatternProvider extends PatternDialectProvider {

     public static final String FILE = "file";

    private static final Logger LOG = Logger.getInstance("#" + ProjectPatternProvider.class.getName());


    @Override
    public TreeModel createTreeModel(final Project project, final Marker marker) {
        return FileTreeModelBuilder.createTreeModel(project, false, marker);
    }

    @Override
    public TreeModel createTreeModel(final Project project, final Set<PsiFile> deps, final Marker marker,
                                     final DependenciesPanel.DependencyPanelSettings settings) {
        return FileTreeModelBuilder.createTreeModel(project, false, deps, marker, settings);
    }

    @Override
    public String getDisplayName() {
        return IdeBundle.message("title.project");
    }

    @Override

    public String getShortName() {
        return FILE;
    }

    @Override
    public AnAction[] createActions(Project project, final Runnable update) {
        if (ProjectViewDirectoryHelper.getInstance(project).supportsHideEmptyMiddlePackages()) {
            return new AnAction[]{new CompactEmptyMiddlePackagesAction(update)};
        }
        return AnAction.EMPTY_ARRAY;
    }

    @Override

    public PackageSet createPackageSet(final PackageDependenciesNode node, final boolean recursively) {
        if (node instanceof ModuleGroupNode) {
            if (!recursively) return null;
             final String modulePattern = "group:" + ((ModuleGroupNode)node).getModuleGroup().toString();
            return new FilePatternPackageSet(modulePattern, "*//*");
        }
        else if (node instanceof ModuleNode) {
            if (!recursively) return null;
            final String modulePattern = ((ModuleNode)node).getModuleName();
            return new FilePatternPackageSet(modulePattern, "*/");
        }

        else if (node instanceof DirectoryNode) {
            String pattern = ((DirectoryNode)node).getFQName();
            if (pattern != null) {
                if (pattern.length() > 0) {
                    pattern += recursively ? "//*" : "/*";
                }
                else {
                    pattern += recursively ? "*/" : "*";
                }
            }
            final VirtualFile vDir = ((DirectoryNode)node).getDirectory();
            final PsiElement psiElement = node.getPsiElement();
            final Module module = psiElement != null ? ModuleUtilCore.findModuleForFile(vDir, psiElement.getProject()) : null;
            return new FilePatternPackageSet(module != null ? module.getName() : null, pattern);
        }
        else if (node instanceof FileNode) {
            if (recursively) return null;
            FileNode fNode = (FileNode)node;
            final PsiFile file = (PsiFile)fNode.getPsiElement();
            if (file == null) return null;
            final VirtualFile virtualFile = file.getVirtualFile();
            LOG.assertTrue(virtualFile != null);
            final VirtualFile contentRoot = ProjectRootManager.getInstance(file.getProject()).getFileIndex().getContentRootForFile(virtualFile);
            if (contentRoot == null) return null;
            final String fqName = VfsUtilCore.getRelativePath(virtualFile, contentRoot, '/');
            if (fqName != null) return new FilePatternPackageSet(getModulePattern(node), fqName);
        }
        return null;
    }

    @Override
    public Icon getIcon() {
        return AllIcons.General.ProjectTab;
    }

    private static final class CompactEmptyMiddlePackagesAction extends ToggleAction {
        private final Runnable myUpdate;

        CompactEmptyMiddlePackagesAction(Runnable update) {
            super(IdeBundle.message("action.compact.empty.middle.packages"),
                    IdeBundle.message("action.compact.empty.middle.packages"), AllIcons.ObjectBrowser.CompactEmptyPackages);
            myUpdate = update;
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return DependencyUISettings.getInstance().UI_COMPACT_EMPTY_MIDDLE_PACKAGES;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            DependencyUISettings.getInstance().UI_COMPACT_EMPTY_MIDDLE_PACKAGES = flag;
            myUpdate.run();
        }
    }
}
