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

package com.gome.maven.ide.projectView;

import com.gome.maven.ide.SelectInTarget;
import com.gome.maven.ide.projectView.impl.AbstractProjectViewPane;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;

import java.util.Collection;

public abstract class ProjectView {
    public static ProjectView getInstance(Project project) {
        return ServiceManager.getService(project, ProjectView.class);
    }

    public abstract void select(Object element, VirtualFile file, boolean requestFocus);

    
    public abstract ActionCallback selectCB(Object element, VirtualFile file, boolean requestFocus);

    
    public abstract ActionCallback changeViewCB( String viewId, String subId);

    
    public abstract PsiElement getParentOfCurrentSelection();

    // show pane identified by id using default(or currently selected) subId
    public abstract void changeView( String viewId);
    public abstract void changeView( String viewId, String subId);

    public abstract void changeView();

    public abstract void refresh();

    public abstract boolean isAutoscrollToSource(String paneId);

    public abstract boolean isFlattenPackages(String paneId);

    public abstract boolean isShowMembers(String paneId);

    public abstract boolean isHideEmptyMiddlePackages(String paneId);

    public abstract void setHideEmptyPackages(boolean hideEmptyPackages,  String paneId);

    public abstract boolean isShowLibraryContents(String paneId);

    public abstract void setShowLibraryContents(boolean showLibraryContents,  String paneId);

    public abstract boolean isShowModules(String paneId);

    public abstract void setShowModules(boolean showModules,  String paneId);

    public abstract void addProjectPane( AbstractProjectViewPane pane);

    public abstract void removeProjectPane( AbstractProjectViewPane pane);

    public abstract AbstractProjectViewPane getProjectViewPaneById(String id);

    public abstract boolean isAutoscrollFromSource(String paneId);

    public abstract boolean isAbbreviatePackageNames(String paneId);

    public abstract void setAbbreviatePackageNames(boolean abbreviatePackageNames,  String paneId);

    /**
     * e.g. {@link com.gome.maven.ide.projectView.impl.ProjectViewPane#ID}
     * @see com.gome.maven.ide.projectView.impl.AbstractProjectViewPane#getId()
     */
    public abstract String getCurrentViewId();

    public abstract void selectPsiElement(PsiElement element, boolean requestFocus);

    public abstract boolean isManualOrder(String paneId);
    public abstract void setManualOrder( String paneId, final boolean enabled);

    public abstract boolean isSortByType(String paneId);
    public abstract void setSortByType( String paneId, final boolean sortByType);

    public abstract AbstractProjectViewPane getCurrentProjectViewPane();

    
    public abstract Collection<String> getPaneIds();

    
    public abstract Collection<SelectInTarget> getSelectInTargets();
}
