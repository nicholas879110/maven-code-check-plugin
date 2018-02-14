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

package com.gome.maven.ide.scopeView;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.SelectInTarget;
import com.gome.maven.ide.projectView.ProjectView;
import com.gome.maven.ide.projectView.impl.AbstractProjectViewPane;
import com.gome.maven.ide.projectView.impl.ShowModulesAction;
import com.gome.maven.ide.ui.customization.CustomizationUtil;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.ActionPlaces;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.packageDependencies.DependencyValidationManager;
import com.gome.maven.packageDependencies.ui.PackageDependenciesNode;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiFileSystemItem;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.search.scope.NonProjectFilesScope;
import com.gome.maven.psi.search.scope.packageSet.*;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collection;
import java.util.List;

/**
 * @author cdr
 */
public class ScopeViewPane extends AbstractProjectViewPane {
     public static final String ID = "Scope";
    private final ProjectView myProjectView;
    private ScopeTreeViewPanel myViewPanel;
    private final DependencyValidationManager myDependencyValidationManager;
    private final NamedScopeManager myNamedScopeManager;
    private final NamedScopesHolder.ScopeListener myScopeListener;

    public ScopeViewPane(final Project project, ProjectView projectView, DependencyValidationManager dependencyValidationManager, NamedScopeManager namedScopeManager) {
        super(project);
        myProjectView = projectView;
        myDependencyValidationManager = dependencyValidationManager;
        myNamedScopeManager = namedScopeManager;
        myScopeListener = new NamedScopesHolder.ScopeListener() {
            Alarm refreshProjectViewAlarm = new Alarm();
            @Override
            public void scopesChanged() {
                // amortize batch scope changes
                refreshProjectViewAlarm.cancelAllRequests();
                refreshProjectViewAlarm.addRequest(new Runnable(){
                    @Override
                    public void run() {
                        if (myProject.isDisposed()) return;
                        final String subId = getSubId();
                        final String id = myProjectView.getCurrentViewId();
                        myProjectView.removeProjectPane(ScopeViewPane.this);
                        myProjectView.addProjectPane(ScopeViewPane.this);
                        if (id != null) {
                            if (Comparing.strEqual(id, getId())) {
                                myProjectView.changeView(id, subId);
                            } else {
                                myProjectView.changeView(id);
                            }
                        }
                    }
                },10);
            }
        };
        myDependencyValidationManager.addScopeListener(myScopeListener);
        myNamedScopeManager.addScopeListener(myScopeListener);
    }

    @Override
    public String getTitle() {
        return IdeBundle.message("scope.view.title");
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Ide.LocalScope;
    }

    @Override
    
    public String getId() {
        return ID;
    }

    @Override
    public JComponent createComponent() {
        if (myViewPanel == null) {
            myViewPanel = new ScopeTreeViewPanel(myProject);
            Disposer.register(this, myViewPanel);
            myViewPanel.initListeners();
            myTree = myViewPanel.getTree();
            CustomizationUtil.installPopupHandler(myTree, IdeActions.GROUP_SCOPE_VIEW_POPUP, ActionPlaces.SCOPE_VIEW_POPUP);
            enableDnD();
        }

        myViewPanel.selectScope(NamedScopesHolder.getScope(myProject, getSubId()));
        return myViewPanel.getPanel();
    }

    @Override
    public void dispose() {
        myViewPanel = null;
        myDependencyValidationManager.removeScopeListener(myScopeListener);
        myNamedScopeManager.removeScopeListener(myScopeListener);
        super.dispose();
    }

    @Override
    
    public String[] getSubIds() {
        return ContainerUtil.map2Array(getShownScopes(), String.class, new Function<NamedScope, String>() {
            @Override
            public String fun(NamedScope scope) {
                return scope.getName();
            }
        });
    }

    
    public static Collection<NamedScope> getShownScopes( Project project) {
        return getShownScopes(DependencyValidationManager.getInstance(project), NamedScopeManager.getInstance(project));
    }

    private Collection<NamedScope> getShownScopes() {
        return getShownScopes(myDependencyValidationManager, myNamedScopeManager);
    }

    
    private static Collection<NamedScope> getShownScopes(DependencyValidationManager dependencyValidationManager, NamedScopeManager namedScopeManager) {
        List<NamedScope> list = ContainerUtil.newArrayList();
        for (NamedScope scope : ContainerUtil.concat(dependencyValidationManager.getScopes(), namedScopeManager.getScopes())) {
            if (scope instanceof NonProjectFilesScope) continue;
            if (scope == CustomScopesProviderEx.getAllScope()) continue;
            list.add(scope);
        }
        return list;
    }

    @Override
    
    public String getPresentableSubIdName( final String subId) {
        return subId;
    }

    @Override
    public void addToolbarActions(DefaultActionGroup actionGroup) {
        actionGroup.add(ActionManager.getInstance().getAction("ScopeView.EditScopes"));
        actionGroup.addAction(new ShowModulesAction(myProject){
            
            @Override
            protected String getId() {
                return ScopeViewPane.this.getId();
            }
        }).setAsSecondary(true);
    }

    
    @Override
    public ActionCallback updateFromRoot(boolean restoreExpandedPaths) {
        saveExpandedPaths();
        myViewPanel.selectScope(NamedScopesHolder.getScope(myProject, getSubId()));
        restoreExpandedPaths();
        return new ActionCallback.Done();
    }

    @Override
    public void select(Object element, VirtualFile file, boolean requestFocus) {
        if (file == null) return;
        PsiFileSystemItem psiFile = file.isDirectory() ? PsiManager.getInstance(myProject).findDirectory(file)
                : PsiManager.getInstance(myProject).findFile(file);
        if (psiFile == null) return;
        if (!(element instanceof PsiElement)) return;

        List<NamedScope> allScopes = ContainerUtil.newArrayList(getShownScopes());
        for (NamedScope scope : allScopes) {
            String name = scope.getName();
            if (name.equals(getSubId())) {
                allScopes.remove(scope);
                allScopes.add(0, scope);
                break;
            }
        }
        for (NamedScope scope : allScopes) {
            String name = scope.getName();
            PackageSet packageSet = scope.getValue();
            if (packageSet == null) continue;
            if (changeView(packageSet, ((PsiElement)element), psiFile, name, myNamedScopeManager, requestFocus)) break;
            if (changeView(packageSet, ((PsiElement)element), psiFile, name, myDependencyValidationManager, requestFocus)) break;
        }
    }

    private boolean changeView(final PackageSet packageSet, final PsiElement element, final PsiFileSystemItem psiFileSystemItem, final String name, final NamedScopesHolder holder,
                               boolean requestFocus) {
        if ((packageSet instanceof PackageSetBase && ((PackageSetBase)packageSet).contains(psiFileSystemItem.getVirtualFile(), myProject, holder)) ||
                (psiFileSystemItem instanceof PsiFile && packageSet.contains((PsiFile)psiFileSystemItem, holder))) {
            if (!name.equals(getSubId())) {
                if (!requestFocus) return true;
                myProjectView.changeView(getId(), name);
            }
            myViewPanel.selectNode(element, psiFileSystemItem, requestFocus);
            return true;
        }
        return false;
    }



    @Override
    public int getWeight() {
        return 3;
    }

    @Override
    public void installComparator() {
        myViewPanel.setSortByType();
    }

    @Override
    public SelectInTarget createSelectInTarget() {
        return new ScopePaneSelectInTarget(myProject);
    }

    @Override
    protected Object exhumeElementFromNode(final DefaultMutableTreeNode node) {
        if (node instanceof PackageDependenciesNode) {
            return ((PackageDependenciesNode)node).getPsiElement();
        }
        return super.exhumeElementFromNode(node);
    }

    @Override
    public Object getData(final String dataId) {
        final Object data = super.getData(dataId);
        if (data != null) {
            return data;
        }
        return myViewPanel == null ? null : myViewPanel.getData(dataId);
    }

    
    @Override
    public ActionCallback getReady( Object requestor) {
        final ActionCallback callback = myViewPanel.getActionCallback();
        return callback == null ? new ActionCallback.Done() : callback;
    }
}
