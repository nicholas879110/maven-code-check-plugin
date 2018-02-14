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
package com.gome.maven.compiler.impl;

import com.gome.maven.codeInsight.daemon.impl.actions.SuppressFix;
import com.gome.maven.codeInsight.daemon.impl.actions.SuppressForClassFix;
import com.gome.maven.compiler.CompilerWorkspaceConfiguration;
import com.gome.maven.compiler.HelpID;
import com.gome.maven.ide.errorTreeView.ErrorTreeElement;
import com.gome.maven.ide.errorTreeView.NavigatableMessageElement;
import com.gome.maven.ide.errorTreeView.NewErrorTreeViewPanel;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.module.LanguageLevelUtil;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.JavaSdk;
import com.gome.maven.openapi.projectRoots.JavaSdkVersion;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.util.IncorrectOperationException;

public class CompilerErrorTreeView extends NewErrorTreeViewPanel {
    public CompilerErrorTreeView(Project project, Runnable rerunAction) {
        super(project, HelpID.COMPILER, true, true, rerunAction);
    }

    protected void fillRightToolbarGroup(DefaultActionGroup group) {
        super.fillRightToolbarGroup(group);
        group.add(new CompilerPropertiesAction());
    }

    protected void addExtraPopupMenuActions(DefaultActionGroup group) {
        group.add(new ExcludeFromCompileAction(myProject, this));
        group.addSeparator();
        group.add(new SuppressJavacWarningsAction());
        group.add(new SuppressJavacWarningForClassAction());
        group.addSeparator();
        ActionGroup popupGroup = (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_COMPILER_ERROR_VIEW_POPUP);
        if (popupGroup != null) {
            for (AnAction action : popupGroup.getChildren(null)) {
                group.add(action);
            }
        }
    }

    protected boolean shouldShowFirstErrorInEditor() {
        return CompilerWorkspaceConfiguration.getInstance(myProject).AUTO_SHOW_ERRORS_IN_EDITOR;
    }

    private class SuppressJavacWarningsAction extends AnAction {
        public void actionPerformed(final AnActionEvent e) {
            final NavigatableMessageElement messageElement = (NavigatableMessageElement)getSelectedErrorTreeElement();
            final String[] text = messageElement.getText();
            final String id = text[0].substring(1, text[0].indexOf("]"));
            final SuppressFix suppressInspectionFix = getSuppressAction(id);
            final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
            assert project != null;
            final OpenFileDescriptor navigatable = (OpenFileDescriptor)messageElement.getNavigatable();
            final PsiFile file = PsiManager.getInstance(project).findFile(navigatable.getFile());
            assert file != null;
            CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        public void run() {
                            try {
                                suppressInspectionFix.invoke(project, file.findElementAt(navigatable.getOffset()));
                            }
                            catch (IncorrectOperationException e1) {
                                LOG.error(e1);
                            }
                        }
                    });
                }
            }, suppressInspectionFix.getText(), null);
        }

        @Override
        public void update(final AnActionEvent e) {
            final Presentation presentation = e.getPresentation();
            presentation.setVisible(false);
            presentation.setEnabled(false);
            final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
            if (project == null) {
                return;
            }
            final ErrorTreeElement errorTreeElement = getSelectedErrorTreeElement();
            if (errorTreeElement instanceof NavigatableMessageElement) {
                final NavigatableMessageElement messageElement = (NavigatableMessageElement)errorTreeElement;
                final String[] text = messageElement.getText();
                if (text.length > 0) {
                    if (text[0].startsWith("[") && text[0].indexOf("]") != -1) {
                        final Navigatable navigatable = messageElement.getNavigatable();
                        if (navigatable instanceof OpenFileDescriptor) {
                            final OpenFileDescriptor fileDescriptor = (OpenFileDescriptor)navigatable;
                            final VirtualFile virtualFile = fileDescriptor.getFile();
                            final Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
                            if (module == null) {
                                return;
                            }
                            final Sdk jdk = ModuleRootManager.getInstance(module).getSdk();
                            if (jdk == null) {
                                return;
                            }
                            final boolean is_1_5 = JavaSdk.getInstance().isOfVersionOrHigher(jdk, JavaSdkVersion.JDK_1_5);
                            if (!is_1_5) {
                                return;
                            }
                            final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                            if (psiFile == null) {
                                return;
                            }
                            if (LanguageLevelUtil.getEffectiveLanguageLevel(module).compareTo(LanguageLevel.JDK_1_5) < 0) return;
                            final PsiElement context = psiFile.findElementAt(fileDescriptor.getOffset());
                            if (context == null) {
                                return;
                            }
                            final String id = text[0].substring(1, text[0].indexOf("]"));
                            final SuppressFix suppressInspectionFix = getSuppressAction(id);
                            final boolean available = suppressInspectionFix.isAvailable(project, context);
                            presentation.setEnabled(available);
                            presentation.setVisible(available);
                            if (available) {
                                presentation.setText(suppressInspectionFix.getText());
                            }
                        }
                    }
                }
            }
        }

        protected SuppressFix getSuppressAction( final String id) {
            return new SuppressFix(id) {
                @Override
                @SuppressWarnings({"SimplifiableIfStatement"})
                public boolean isAvailable( final Project project,  final PsiElement context) {
                    if (getContainer(context) instanceof PsiClass) return false;
                    return super.isAvailable(project, context);
                }

                @Override
                protected boolean use15Suppressions( final PsiDocCommentOwner container) {
                    return true;
                }
            };
        }
    }

    private class SuppressJavacWarningForClassAction extends SuppressJavacWarningsAction {
        @Override
        protected SuppressFix getSuppressAction( final String id) {
            return new SuppressForClassFix(id){
                @Override
                protected boolean use15Suppressions( final PsiDocCommentOwner container) {
                    return true;
                }
            };
        }
    }
}
