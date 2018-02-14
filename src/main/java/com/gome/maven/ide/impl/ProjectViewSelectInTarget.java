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

package com.gome.maven.ide.impl;

import com.gome.maven.ide.CompositeSelectInTarget;
import com.gome.maven.ide.SelectInContext;
import com.gome.maven.ide.SelectInTarget;
import com.gome.maven.ide.projectView.ProjectView;
import com.gome.maven.ide.projectView.SelectableTreeStructureProvider;
import com.gome.maven.ide.projectView.TreeStructureProvider;
import com.gome.maven.ide.projectView.impl.AbstractProjectViewPane;
import com.gome.maven.ide.projectView.impl.ProjectViewPane;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowId;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PsiUtilBase;
import com.gome.maven.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ProjectViewSelectInTarget extends SelectInTargetPsiWrapper implements CompositeSelectInTarget {
    private String mySubId;

    protected ProjectViewSelectInTarget(Project project) {
        super(project);
    }

    @Override
    protected final void select(final Object selector, final VirtualFile virtualFile, final boolean requestFocus) {
        select(myProject, selector, getMinorViewId(), mySubId, virtualFile, requestFocus);
    }

    
    public static ActionCallback select( Project project,
                                        final Object toSelect,
                                         final String viewId,
                                         final String subviewId,
                                        final VirtualFile virtualFile,
                                        final boolean requestFocus) {
        final ActionCallback result = new ActionCallback();


        final ProjectView projectView = ProjectView.getInstance(project);
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            AbstractProjectViewPane pane = projectView.getProjectViewPaneById(ProjectViewPane.ID);
            pane.select(toSelect, virtualFile, requestFocus);
            return result;
        }

        ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
        final ToolWindow projectViewToolWindow = windowManager.getToolWindow(ToolWindowId.PROJECT_VIEW);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        projectView.selectCB(toSelect, virtualFile, requestFocus).notify(result);
                    }
                };
                if (requestFocus) {
                    projectView.changeViewCB(ObjectUtils.chooseNotNull(viewId, ProjectViewPane.ID), subviewId).doWhenProcessed(r);
                }
                else {
                    r.run();
                }
            }
        };

        if (requestFocus) {
            projectViewToolWindow.activate(runnable, false);
        }
        else {
            projectViewToolWindow.show(runnable);
        }

        return result;
    }


    @Override
    
    public Collection<SelectInTarget> getSubTargets( SelectInContext context) {
        List<SelectInTarget> result = new ArrayList<SelectInTarget>();
        AbstractProjectViewPane pane = ProjectView.getInstance(myProject).getProjectViewPaneById(getMinorViewId());
        int index = 0;
        for (String subId : pane.getSubIds()) {
            result.add(new ProjectSubViewSelectInTarget(this, subId, index++));
        }
        return result;
    }

    public boolean isSubIdSelectable(String subId, SelectInContext context) {
        return false;
    }

    @Override
    protected boolean canSelect(PsiFileSystemItem file) {
        return true;
    }

    public String getSubIdPresentableName(String subId) {
        AbstractProjectViewPane pane = ProjectView.getInstance(myProject).getProjectViewPaneById(getMinorViewId());
        return pane.getPresentableSubIdName(subId);
    }

    @Override
    public void select(PsiElement element, final boolean requestFocus) {
        PsiElement toSelect = null;
        for (TreeStructureProvider provider : getProvidersDumbAware()) {
            if (provider instanceof SelectableTreeStructureProvider) {
                toSelect = ((SelectableTreeStructureProvider) provider).getTopLevelElement(element);
            }
            if (toSelect != null) break;
        }
        if (toSelect == null) {
            if (element instanceof PsiFile || element instanceof PsiDirectory) {
                toSelect = element;
            }
            else {
                final PsiFile containingFile = element.getContainingFile();
                if (containingFile == null) return;
                final FileViewProvider viewProvider = containingFile.getViewProvider();
                toSelect = viewProvider.getPsi(viewProvider.getBaseLanguage());
            }
        }
        if (toSelect == null) return;
        PsiElement originalElement;
        try {
            originalElement = toSelect.getOriginalElement();
        }
        catch (IndexNotReadyException e) {
            originalElement = toSelect;
        }
        final VirtualFile virtualFile = PsiUtilBase.getVirtualFile(originalElement);
        select(originalElement, virtualFile, requestFocus);
    }

    private TreeStructureProvider[] getProvidersDumbAware() {
        TreeStructureProvider[] allProviders = Extensions.getExtensions(TreeStructureProvider.EP_NAME, myProject);
        List<TreeStructureProvider> dumbAware = DumbService.getInstance(myProject).filterByDumbAwareness(allProviders);
        return dumbAware.toArray(new TreeStructureProvider[dumbAware.size()]);
    }

    @Override
    public final String getToolWindowId() {
        return ToolWindowId.PROJECT_VIEW;
    }

    @Override
    protected boolean canWorkWithCustomObjects() {
        return true;
    }

    public final void setSubId(String subId) {
        mySubId = subId;
    }

    public final String getSubId() {
        return mySubId;
    }
}
