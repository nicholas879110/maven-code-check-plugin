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

package com.gome.maven.ide.util;

import com.gome.maven.CommonBundle;
import com.gome.maven.history.LocalHistory;
import com.gome.maven.history.LocalHistoryAction;
import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.DeleteProvider;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.application.ApplicationBundle;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.ex.MessagesEx;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.WritingAccessProvider;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiFileSystemItem;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtilBase;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.safeDelete.SafeDeleteDialog;
import com.gome.maven.refactoring.safeDelete.SafeDeleteProcessor;
import com.gome.maven.refactoring.util.CommonRefactoringUtil;
import com.gome.maven.refactoring.util.RefactoringUIUtil;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.io.ReadOnlyAttributeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class DeleteHandler {
    private DeleteHandler() {
    }

    public static class DefaultDeleteProvider implements DeleteProvider {
        @Override
        public boolean canDeleteElement( DataContext dataContext) {
            if (CommonDataKeys.PROJECT.getData(dataContext) == null) {
                return false;
            }
            final PsiElement[] elements = getPsiElements(dataContext);
            return elements != null && shouldEnableDeleteAction(elements);
        }

        
        private static PsiElement[] getPsiElements(DataContext dataContext) {
            PsiElement[] elements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext);
            if (elements == null) {
                final Object data = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
                if (data != null) {
                    elements = new PsiElement[]{(PsiElement)data};
                }
                else {
                    final Object data1 = CommonDataKeys.PSI_FILE.getData(dataContext);
                    if (data1 != null) {
                        elements = new PsiElement[]{(PsiFile)data1};
                    }
                }
            }
            return elements;
        }

        @Override
        public void deleteElement( DataContext dataContext) {
            PsiElement[] elements = getPsiElements(dataContext);
            if (elements == null) return;
            Project project = CommonDataKeys.PROJECT.getData(dataContext);
            if (project == null) return;
            LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeBundle.message("progress.deleting"));
            try {
                deletePsiElement(elements, project);
            }
            finally {
                a.finish();
            }
        }
    }

    public static void deletePsiElement(final PsiElement[] elementsToDelete, final Project project) {
        deletePsiElement(elementsToDelete, project, true);
    }

    public static void deletePsiElement(final PsiElement[] elementsToDelete, final Project project, boolean needConfirmation) {
        if (elementsToDelete == null || elementsToDelete.length == 0) return;

        final PsiElement[] elements = PsiTreeUtil.filterAncestors(elementsToDelete);

        boolean safeDeleteApplicable = true;
        for (int i = 0; i < elements.length && safeDeleteApplicable; i++) {
            PsiElement element = elements[i];
            safeDeleteApplicable = SafeDeleteProcessor.validElement(element);
        }

        final boolean dumb = DumbService.getInstance(project).isDumb();
        if (safeDeleteApplicable && !dumb) {
            final Ref<Boolean> exit = Ref.create(false);
            final SafeDeleteDialog dialog = new SafeDeleteDialog(project, elements, new SafeDeleteDialog.Callback() {
                @Override
                public void run(final SafeDeleteDialog dialog) {
                    if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, Arrays.asList(elements), true)) return;
                    SafeDeleteProcessor.createInstance(project, new Runnable() {
                        @Override
                        public void run() {
                            exit.set(true);
                            dialog.close(DialogWrapper.OK_EXIT_CODE);
                        }
                    }, elements, dialog.isSearchInComments(), dialog.isSearchForTextOccurences(), true).run();
                }
            }) {
                @Override
                protected boolean isDelete() {
                    return true;
                }
            };
            if (needConfirmation) {
                if (!dialog.showAndGet() || exit.get()) {
                    return;
                }
            }
        }
        else {
            @SuppressWarnings({"UnresolvedPropertyKey"})
            String warningMessage = DeleteUtil.generateWarningMessage(IdeBundle.message("prompt.delete.elements"), elements);

            boolean anyDirectories = false;
            String directoryName = null;
            for (PsiElement psiElement : elementsToDelete) {
                if (psiElement instanceof PsiDirectory && !PsiUtilBase.isSymLink((PsiDirectory)psiElement)) {
                    anyDirectories = true;
                    directoryName = ((PsiDirectory)psiElement).getName();
                    break;
                }
            }
            if (anyDirectories) {
                if (elements.length == 1) {
                    warningMessage += IdeBundle.message("warning.delete.all.files.and.subdirectories", directoryName);
                }
                else {
                    warningMessage += IdeBundle.message("warning.delete.all.files.and.subdirectories.in.the.selected.directory");
                }
            }

            if (safeDeleteApplicable && dumb) {
                warningMessage += "\n\nWarning:\n  Safe delete is not available while " +
                        ApplicationNamesInfo.getInstance().getFullProductName() +
                        " updates indices,\n  no usages will be checked.";
            }

            if (needConfirmation) {
                int result = Messages.showOkCancelDialog(project, warningMessage, IdeBundle.message("title.delete"),
                        ApplicationBundle.message("button.delete"), CommonBundle.getCancelButtonText(),
                        Messages.getQuestionIcon());
                if (result != Messages.OK) return;
            }
        }

        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, Arrays.asList(elements), false)) {
                    return;
                }

                // deleted from project view or something like that.
                if (CommonDataKeys.EDITOR.getData(DataManager.getInstance().getDataContext()) == null) {
                    CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
                }

                for (final PsiElement elementToDelete : elements) {
                    if (!elementToDelete.isValid()) continue; //was already deleted
                    if (elementToDelete instanceof PsiDirectory) {
                        VirtualFile virtualFile = ((PsiDirectory)elementToDelete).getVirtualFile();
                        if (virtualFile.isInLocalFileSystem() && !virtualFile.is(VFileProperty.SYMLINK)) {
                            ArrayList<VirtualFile> readOnlyFiles = new ArrayList<VirtualFile>();
                            CommonRefactoringUtil.collectReadOnlyFiles(virtualFile, readOnlyFiles);

                            if (!readOnlyFiles.isEmpty()) {
                                String message = IdeBundle.message("prompt.directory.contains.read.only.files", virtualFile.getPresentableUrl());
                                int _result = Messages.showYesNoDialog(project, message, IdeBundle.message("title.delete"), Messages.getQuestionIcon());
                                if (_result != Messages.YES) continue;

                                boolean success = true;
                                for (VirtualFile file : readOnlyFiles) {
                                    success = clearReadOnlyFlag(file, project);
                                    if (!success) break;
                                }
                                if (!success) continue;
                            }
                        }
                    }
                    else if (!elementToDelete.isWritable() &&
                            !(elementToDelete instanceof PsiFileSystemItem && PsiUtilBase.isSymLink((PsiFileSystemItem)elementToDelete))) {
                        final PsiFile file = elementToDelete.getContainingFile();
                        if (file != null) {
                            final VirtualFile virtualFile = file.getVirtualFile();
                            if (virtualFile.isInLocalFileSystem()) {
                                int _result = MessagesEx.fileIsReadOnly(project, virtualFile)
                                        .setTitle(IdeBundle.message("title.delete"))
                                        .appendMessage(IdeBundle.message("prompt.delete.it.anyway"))
                                        .askYesNo();
                                if (_result != Messages.YES) continue;

                                boolean success = clearReadOnlyFlag(virtualFile, project);
                                if (!success) continue;
                            }
                        }
                    }

                    try {
                        elementToDelete.checkDelete();
                    }
                    catch (IncorrectOperationException ex) {
                        Messages.showMessageDialog(project, ex.getMessage(), CommonBundle.getErrorTitle(), Messages.getErrorIcon());
                        continue;
                    }

                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                elementToDelete.delete();
                            }
                            catch (final IncorrectOperationException ex) {
                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        Messages.showMessageDialog(project, ex.getMessage(), CommonBundle.getErrorTitle(), Messages.getErrorIcon());
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }, RefactoringBundle.message("safe.delete.command", RefactoringUIUtil.calculatePsiElementDescriptionList(elements)), null);
    }

    private static boolean clearReadOnlyFlag(final VirtualFile virtualFile, final Project project) {
        final boolean[] success = new boolean[1];
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                Runnable action = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualFile, false);
                            success[0] = true;
                        }
                        catch (IOException e1) {
                            Messages.showMessageDialog(project, e1.getMessage(), CommonBundle.getErrorTitle(), Messages.getErrorIcon());
                        }
                    }
                };
                ApplicationManager.getApplication().runWriteAction(action);
            }
        }, "", null);
        return success[0];
    }

    public static boolean shouldEnableDeleteAction(PsiElement[] elements) {
        if (elements == null || elements.length == 0) return false;
        for (PsiElement element : elements) {
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
            if (virtualFile == null) {
                return false;
            }
            if (!WritingAccessProvider.isPotentiallyWritable(virtualFile, element.getProject())) {
                return false;
            }
        }
        return true;
    }
}
