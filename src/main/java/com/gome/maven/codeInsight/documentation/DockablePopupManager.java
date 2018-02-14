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
package com.gome.maven.codeInsight.documentation;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.IdeEventQueue;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowAnchor;
import com.gome.maven.openapi.wm.ToolWindowType;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.openapi.wm.ex.ToolWindowManagerEx;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.psi.util.PsiUtilBase;
import com.gome.maven.ui.content.*;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.update.Activatable;
import com.gome.maven.util.ui.update.UiNotifyConnector;

import javax.swing.*;
import java.awt.*;

/**
 * User: anna
 * Date: 5/7/12
 */
public abstract class DockablePopupManager<T extends JComponent & Disposable> {
    protected ToolWindow myToolWindow = null;
    protected boolean myAutoUpdateDocumentation = PropertiesComponent.getInstance().isTrueValue(getAutoUpdateEnabledProperty());
    protected Runnable myAutoUpdateRequest;
     protected final Project myProject;

    public DockablePopupManager( Project project) {
        myProject = project;
    }

    protected abstract String getShowInToolWindowProperty();
    protected abstract String getAutoUpdateEnabledProperty();

    protected abstract String getAutoUpdateTitle();
    protected abstract String getRestorePopupDescription();
    protected abstract String getAutoUpdateDescription();

    protected abstract T createComponent();
    protected abstract void doUpdateComponent(PsiElement element, PsiElement originalElement, T component);
    protected abstract void doUpdateComponent(Editor editor, PsiFile psiFile);
    protected abstract void doUpdateComponent( PsiElement element);

    protected abstract String getTitle(PsiElement element);
    protected abstract String getToolwindowId();

    public Content recreateToolWindow(PsiElement element, PsiElement originalElement) {
        if (myToolWindow == null) {
            createToolWindow(element, originalElement);
            return null;
        }

        final Content content = myToolWindow.getContentManager().getSelectedContent();
        if (content == null || !myToolWindow.isVisible()) {
            restorePopupBehavior();
            createToolWindow(element, originalElement);
            return null;
        }
        return content;
    }

    public void createToolWindow(final PsiElement element, PsiElement originalElement) {
        assert myToolWindow == null;

        final T component = createComponent();

        final ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(myProject);
        final ToolWindow toolWindow = toolWindowManagerEx.getToolWindow(getToolwindowId());
        myToolWindow = toolWindow == null
                ? toolWindowManagerEx.registerToolWindow(getToolwindowId(), true, ToolWindowAnchor.RIGHT, myProject)
                : toolWindow;
        myToolWindow.setIcon(AllIcons.Toolwindows.Documentation);

        myToolWindow.setAvailable(true, null);
        myToolWindow.setToHideOnEmptyContent(false);

        final Rectangle rectangle = WindowManager.getInstance().getIdeFrame(myProject).suggestChildFrameBounds();
        myToolWindow.setDefaultState(ToolWindowAnchor.RIGHT, ToolWindowType.FLOATING, rectangle);

        final ContentManager contentManager = myToolWindow.getContentManager();
        final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        final Content content = contentFactory.createContent(component, getTitle(element), false);
        contentManager.addContent(content);

        contentManager.addContentManagerListener(new ContentManagerAdapter() {
            @Override
            public void contentRemoved(ContentManagerEvent event) {
                restorePopupBehavior();
            }
        });

        new UiNotifyConnector(component, new Activatable() {
            @Override
            public void showNotify() {
                restartAutoUpdate(myAutoUpdateDocumentation);
            }

            @Override
            public void hideNotify() {
                restartAutoUpdate(false);
            }
        });

        myToolWindow.show(null);
        PropertiesComponent.getInstance().setValue(getShowInToolWindowProperty(), Boolean.TRUE.toString());
        restartAutoUpdate(PropertiesComponent.getInstance().getBoolean(getAutoUpdateEnabledProperty(), true));
        doUpdateComponent(element, originalElement, component);
    }


    protected AnAction[] createActions() {
        ToggleAction toggleAutoUpdateAction = new ToggleAction(getAutoUpdateTitle(), getAutoUpdateDescription(),
                AllIcons.General.AutoscrollFromSource) {
            @Override
            public boolean isSelected(AnActionEvent e) {
                return myAutoUpdateDocumentation;
            }

            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                PropertiesComponent.getInstance().setValue(getAutoUpdateEnabledProperty(), String.valueOf(state));
                myAutoUpdateDocumentation = state;
                restartAutoUpdate(state);
            }
        };
        return new AnAction[]{toggleAutoUpdateAction, createRestorePopupAction()};
    }

    
    protected AnAction createRestorePopupAction() {
        return new AnAction("Restore Popup", getRestorePopupDescription(), AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                restorePopupBehavior();
            }
        };
    }

    protected void restartAutoUpdate(final boolean state) {
        if (state && myToolWindow != null) {
            if (myAutoUpdateRequest == null) {
                myAutoUpdateRequest = new Runnable() {
                    @Override
                    public void run() {
                        updateComponent();
                    }
                };

                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        IdeEventQueue.getInstance().addIdleListener(myAutoUpdateRequest, 500);
                    }
                });
            }
        }
        else {
            if (myAutoUpdateRequest != null) {
                IdeEventQueue.getInstance().removeIdleListener(myAutoUpdateRequest);
                myAutoUpdateRequest = null;
            }
        }
    }

    public void updateComponent() {
        if (myProject.isDisposed()) return;

        DataManager.getInstance().getDataContextFromFocus().doWhenDone(new Consumer<DataContext>() {
            @Override
            public void consume( DataContext dataContext) {
                if (!myProject.isOpen()) return;
                updateComponentInner(dataContext);
            }
        });
    }

    private void updateComponentInner( DataContext dataContext) {
        if (CommonDataKeys.PROJECT.getData(dataContext) != myProject) {
            return;
        }

        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor == null) {
            PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
            if (element != null) {
                doUpdateComponent(element);
            }
            return;
        }

        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        final PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, myProject);

        final Editor injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file);
        if (injectedEditor != null) {
            final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(injectedEditor, myProject);
            if (psiFile != null) {
                doUpdateComponent(injectedEditor, psiFile);
                return;
            }
        }

        if (file != null) {
            doUpdateComponent(editor, file);
        }
    }


    protected void restorePopupBehavior() {
        if (myToolWindow != null) {
            PropertiesComponent.getInstance().setValue(getShowInToolWindowProperty(), Boolean.FALSE.toString());
            ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(myProject);
            toolWindowManagerEx.hideToolWindow(getToolwindowId(), false);
            toolWindowManagerEx.unregisterToolWindow(getToolwindowId());
            Disposer.dispose(myToolWindow.getContentManager());
            myToolWindow = null;
            restartAutoUpdate(false);
        }
    }

    public boolean hasActiveDockedDocWindow() {
        return myToolWindow != null && myToolWindow.isVisible();
    }
}
