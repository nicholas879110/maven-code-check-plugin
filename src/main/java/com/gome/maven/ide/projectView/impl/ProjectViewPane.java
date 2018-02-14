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

/**
 * @author cdr
 */
package com.gome.maven.ide.projectView.impl;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.SelectInTarget;
import com.gome.maven.ide.impl.ProjectPaneSelectInTarget;
import com.gome.maven.ide.projectView.ProjectView;
import com.gome.maven.ide.projectView.ProjectViewSettings;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.gome.maven.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.gome.maven.ide.util.treeView.AbstractTreeBuilder;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.ide.util.treeView.AbstractTreeUpdater;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.actionSystem.ToggleAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizerUtil;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.psi.PsiDirectory;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class ProjectViewPane extends AbstractProjectViewPSIPane {
     public static final String ID = "ProjectPane";
    public static final String SHOW_EXCLUDED_FILES_OPTION = "show-excluded-files";
    private boolean myShowExcludedFiles = true;

    public ProjectViewPane(Project project) {
        super(project);
    }

    @Override
    public String getTitle() {
        return IdeBundle.message("title.project");
    }

    @Override
    
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return AllIcons.General.ProjectTab;
    }


    @Override
    public SelectInTarget createSelectInTarget() {
        return new ProjectPaneSelectInTarget(myProject);
    }

    @Override
    protected AbstractTreeUpdater createTreeUpdater(AbstractTreeBuilder treeBuilder) {
        return new ProjectViewTreeUpdater(treeBuilder);
    }

    @Override
    protected ProjectAbstractTreeStructureBase createStructure() {
        return new ProjectViewPaneTreeStructure();
    }

    @Override
    protected ProjectViewTree createTree(DefaultTreeModel treeModel) {
        return new ProjectViewTree(myProject, treeModel) {
            public String toString() {
                return getTitle() + " " + super.toString();
            }

            @Override
            public void setFont(Font font) {
                if (Registry.is("bigger.font.in.project.view")) {
                    font = font.deriveFont(font.getSize() + 1.0f);
                }
                super.setFont(font);
            }

            @Override
            public DefaultMutableTreeNode getSelectedNode() {
                return ProjectViewPane.this.getSelectedNode();
            }
        };
    }

    
    public String getComponentName() {
        return "ProjectPane";
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        String showExcludedOption = JDOMExternalizerUtil.readField(element, SHOW_EXCLUDED_FILES_OPTION);
        myShowExcludedFiles = showExcludedOption == null || Boolean.parseBoolean(showExcludedOption);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        if (!myShowExcludedFiles) {
            JDOMExternalizerUtil.writeField(element, SHOW_EXCLUDED_FILES_OPTION, String.valueOf(false));
        }
    }

    @Override
    public void addToolbarActions(DefaultActionGroup actionGroup) {
        actionGroup.addAction(new ShowExcludedFilesAction()).setAsSecondary(true);
    }

    // should be first
    @Override
    public int getWeight() {
        return 0;
    }

    private final class ProjectViewTreeUpdater extends AbstractTreeUpdater {
        private ProjectViewTreeUpdater(final AbstractTreeBuilder treeBuilder) {
            super(treeBuilder);
        }

        @Override
        public boolean addSubtreeToUpdateByElement(Object element) {
            if (element instanceof PsiDirectory && !myProject.isDisposed()) {
                final PsiDirectory dir = (PsiDirectory)element;
                final ProjectTreeStructure treeStructure = (ProjectTreeStructure)myTreeStructure;
                PsiDirectory dirToUpdateFrom = dir;
                if (!treeStructure.isFlattenPackages() && treeStructure.isHideEmptyMiddlePackages()) {
                    // optimization: this check makes sense only if flattenPackages == false && HideEmptyMiddle == true
                    while (dirToUpdateFrom != null && ProjectViewDirectoryHelper.getInstance(myProject).isEmptyMiddleDirectory(dirToUpdateFrom, true)) {
                        dirToUpdateFrom = dirToUpdateFrom.getParentDirectory();
                    }
                }
                boolean addedOk;
                while (!(addedOk = super.addSubtreeToUpdateByElement(dirToUpdateFrom == null? myTreeStructure.getRootElement() : dirToUpdateFrom))) {
                    if (dirToUpdateFrom == null) {
                        break;
                    }
                    dirToUpdateFrom = dirToUpdateFrom.getParentDirectory();
                }
                return addedOk;
            }

            return super.addSubtreeToUpdateByElement(element);
        }
    }

    private class ProjectViewPaneTreeStructure extends ProjectTreeStructure implements ProjectViewSettings {
        public ProjectViewPaneTreeStructure() {
            super(ProjectViewPane.this.myProject, ID);
        }

        @Override
        protected AbstractTreeNode createRoot(final Project project, ViewSettings settings) {
            return new ProjectViewProjectNode(project, settings);
        }

        @Override
        public boolean isShowExcludedFiles() {
            return myShowExcludedFiles;
        }
    }

    private final class ShowExcludedFilesAction extends ToggleAction {
        private ShowExcludedFilesAction() {
            super(IdeBundle.message("action.show.excluded.files"), IdeBundle.message("action.show.hide.excluded.files"), null);
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return myShowExcludedFiles;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            if (myShowExcludedFiles != flag) {
                myShowExcludedFiles = flag;
                updateFromRoot(true);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            final Presentation presentation = e.getPresentation();
            final ProjectView projectView = ProjectView.getInstance(myProject);
            presentation.setEnabledAndVisible(projectView.getCurrentProjectViewPane() == ProjectViewPane.this);
        }
    }
}
