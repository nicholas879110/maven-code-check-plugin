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
package com.gome.maven.ide.projectView.impl.nodes;

import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.ide.projectView.ProjectViewNode;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.projectView.impl.ModuleGroup;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.PlatformIcons;
import gnu.trove.THashMap;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class AbstractProjectNode extends ProjectViewNode<Project> {
    protected AbstractProjectNode(Project project, Project value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    protected Collection<AbstractTreeNode> modulesAndGroups(Module[] modules) {
        Map<String, List<Module>> groups = new THashMap<String, List<Module>>();
        List<Module> nonGroupedModules = new ArrayList<Module>(Arrays.asList(modules));
        for (final Module module : modules) {
            final String[] path = ModuleManager.getInstance(getProject()).getModuleGroupPath(module);
            if (path != null) {
                final String topLevelGroupName = path[0];
                List<Module> moduleList = groups.get(topLevelGroupName);
                if (moduleList == null) {
                    moduleList = new ArrayList<Module>();
                    groups.put(topLevelGroupName, moduleList);
                }
                moduleList.add(module);
                nonGroupedModules.remove(module);
            }
        }
        List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
        try {
            for (String groupPath : groups.keySet()) {
                result.add(createModuleGroupNode(new ModuleGroup(new String[]{groupPath})));
            }
            for (Module module : nonGroupedModules) {
                result.add(createModuleGroup(module));
            }
        }
        catch (Exception e) {
            LOG.error(e);
            return new ArrayList<AbstractTreeNode>();
        }
        return result;
    }

    protected abstract AbstractTreeNode createModuleGroup(final Module module)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    protected abstract AbstractTreeNode createModuleGroupNode(final ModuleGroup moduleGroup)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    @Override
    public void update(PresentationData presentation) {
        presentation.setIcon(PlatformIcons.PROJECT_ICON);
        presentation.setPresentableText(getProject().getName());
    }

    @Override
    public String getTestPresentation() {
        return "Project";
    }

    @Override
    public boolean contains( VirtualFile file) {
        ProjectFileIndex index = ProjectRootManager.getInstance(getProject()).getFileIndex();
        final VirtualFile baseDir = getProject().getBaseDir();
        return index.isInContent(file) || index.isInLibraryClasses(file) || index.isInLibrarySource(file) ||
                (baseDir != null && VfsUtil.isAncestor(baseDir, file, false));
    }
}
