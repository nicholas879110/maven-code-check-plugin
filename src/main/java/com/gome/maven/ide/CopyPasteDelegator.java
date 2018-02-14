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

package com.gome.maven.ide;

import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiDirectoryContainer;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.refactoring.copy.CopyHandler;
import com.gome.maven.refactoring.move.MoveCallback;
import com.gome.maven.refactoring.move.MoveHandler;

import javax.swing.*;

public abstract class CopyPasteDelegator implements CopyPasteSupport {
    private static final ExtensionPointName<PasteProvider> EP_NAME = ExtensionPointName.create("com.intellij.filePasteProvider");
    public static final Key<Boolean> SHOW_CHOOSER_KEY = Key.create("show.dirs.chooser");

    private final Project myProject;
    private final JComponent myKeyReceiver;
    private final MyEditable myEditable;

    public CopyPasteDelegator(Project project, JComponent keyReceiver) {
        myProject = project;
        myKeyReceiver = keyReceiver;
        myEditable = new MyEditable();
    }

    
    protected abstract PsiElement[] getSelectedElements();

    
    private PsiElement[] getValidSelectedElements() {
        PsiElement[] selectedElements = getSelectedElements();
        for (PsiElement element : selectedElements) {
            if (element == null || !element.isValid()) {
                return PsiElement.EMPTY_ARRAY;
            }
        }
        return selectedElements;
    }

    private void updateView() {
        myKeyReceiver.repaint();
    }

    @Override
    public CopyProvider getCopyProvider() {
        return myEditable;
    }

    @Override
    public CutProvider getCutProvider() {
        return myEditable;
    }

    @Override
    public PasteProvider getPasteProvider() {
        return myEditable;
    }

    private class MyEditable implements CutProvider, CopyProvider, PasteProvider {
        @Override
        public void performCopy( DataContext dataContext) {
            PsiElement[] elements = getValidSelectedElements();
            PsiCopyPasteManager.getInstance().setElements(elements, true);
            updateView();
        }

        @Override
        public boolean isCopyEnabled( DataContext dataContext) {
            PsiElement[] elements = getValidSelectedElements();
            return CopyHandler.canCopy(elements) || PsiCopyPasteManager.asFileList(elements) != null;
        }

        @Override
        public boolean isCopyVisible( DataContext dataContext) {
            return true;
        }

        @Override
        public void performCut( DataContext dataContext) {
            PsiElement[] elements = getValidSelectedElements();
            if (MoveHandler.adjustForMove(myProject, elements, null) == null) {
                return;
            }
            // 'elements' passed instead of result of 'adjustForMove' because otherwise ProjectView would
            // not recognize adjusted elements when graying them
            PsiCopyPasteManager.getInstance().setElements(elements, false);
            updateView();
        }

        @Override
        public boolean isCutEnabled( DataContext dataContext) {
            final PsiElement[] elements = getValidSelectedElements();
            return elements.length != 0 && MoveHandler.canMove(elements, null);
        }

        @Override
        public boolean isCutVisible( DataContext dataContext) {
            return true;
        }

        @Override
        public void performPaste( DataContext dataContext) {
            if (!performDefaultPaste(dataContext)) {
                for(PasteProvider provider: Extensions.getExtensions(EP_NAME)) {
                    if (provider.isPasteEnabled(dataContext)) {
                        provider.performPaste(dataContext);
                        break;
                    }
                }
            }
        }

        private boolean performDefaultPaste(final DataContext dataContext) {
            final boolean[] isCopied = new boolean[1];
            final PsiElement[] elements = PsiCopyPasteManager.getInstance().getElements(isCopied);
            if (elements == null) return false;

            PsiDirectory targetDirectory = null;
            DumbService.getInstance(myProject).setAlternativeResolveEnabled(true);
            try {
                PsiElement target = LangDataKeys.PASTE_TARGET_PSI_ELEMENT.getData(dataContext);
                final Module module = LangDataKeys.MODULE.getData(dataContext);
                if (module != null && target instanceof PsiDirectoryContainer) {
                    final PsiDirectory[] directories = ((PsiDirectoryContainer)target).getDirectories(GlobalSearchScope.moduleScope(module));
                    if (directories.length == 1) {
                        target = directories[0];
                    }
                }
                if (isCopied[0]) {
                    targetDirectory = target instanceof PsiDirectory ? (PsiDirectory)target : null;
                    if (targetDirectory == null && target instanceof PsiDirectoryContainer) {
                        final PsiDirectory[] directories = module == null ? ((PsiDirectoryContainer)target).getDirectories()
                                : ((PsiDirectoryContainer)target).getDirectories(GlobalSearchScope.moduleScope(module));
                        if (directories.length > 0) {
                            targetDirectory = directories[0];
                            targetDirectory.putCopyableUserData(SHOW_CHOOSER_KEY, directories.length > 1);
                        }
                    }
                    if (CopyHandler.canCopy(elements)) {
                        CopyHandler.doCopy(elements, targetDirectory);
                    }
                }
                else if (MoveHandler.canMove(elements, target)) {
                    MoveHandler.doMove(myProject, elements, target, dataContext, new MoveCallback() {
                        @Override
                        public void refactoringCompleted() {
                            PsiCopyPasteManager.getInstance().clear();
                        }
                    });
                }
                else {
                    return false;
                }
            }
            finally {
                DumbService.getInstance(myProject).setAlternativeResolveEnabled(false);
                updateView();
                if (targetDirectory != null) {
                    targetDirectory.putCopyableUserData(SHOW_CHOOSER_KEY, null);
                }
            }
            return true;
        }

        @Override
        public boolean isPastePossible( DataContext dataContext) {
            return true;
        }

        @Override
        public boolean isPasteEnabled( DataContext dataContext){
            if (isDefaultPasteEnabled(dataContext)) {
                return true;
            }
            for(PasteProvider provider: Extensions.getExtensions(EP_NAME)) {
                if (provider.isPasteEnabled(dataContext)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isDefaultPasteEnabled(final DataContext dataContext) {
            Project project = CommonDataKeys.PROJECT.getData(dataContext);
            if (project == null) {
                return false;
            }

            if (DumbService.isDumb(project)) return false;

            Object target = LangDataKeys.PASTE_TARGET_PSI_ELEMENT.getData(dataContext);
            if (target == null) {
                return false;
            }
            PsiElement[] elements = PsiCopyPasteManager.getInstance().getElements(new boolean[]{false});
            if (elements == null) {
                return false;
            }

            // disable cross-project paste
            for (PsiElement element : elements) {
                PsiManager manager = element.getManager();
                if (manager == null || manager.getProject() != project) {
                    return false;
                }
            }

            return true;
        }
    }
}
