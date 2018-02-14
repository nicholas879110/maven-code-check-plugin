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
package com.gome.maven.refactoring.copy;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.CopyPasteDelegator;
import com.gome.maven.ide.util.EditorHelper;
import com.gome.maven.ide.util.PlatformPackageUtil;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.ThrowableComputable;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.encoding.EncodingRegistry;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.file.PsiDirectoryFactory;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import com.gome.maven.refactoring.util.CommonRefactoringUtil;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.IncorrectOperationException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CopyFilesOrDirectoriesHandler extends CopyHandlerDelegateBase {

    private static Logger LOG = Logger.getInstance("com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler");

    @Override
    public boolean canCopy(PsiElement[] elements, boolean fromUpdate) {
        Set<String> names = new HashSet<String>();
        for (PsiElement element : elements) {
            if (!(element instanceof PsiFileSystemItem)) return false;
            if (!element.isValid()) return false;
            if (element instanceof PsiCompiledFile) return false;

            String name = ((PsiFileSystemItem) element).getName();
            if (names.contains(name)) {
                return false;
            }
            names.add(name);
        }

        PsiElement[] filteredElements = PsiTreeUtil.filterAncestors(elements);
        return filteredElements.length == elements.length;
    }

    @Override
    public void doCopy(final PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
        if (defaultTargetDirectory == null) {
            defaultTargetDirectory = getCommonParentDirectory(elements);
        }
        Project project = defaultTargetDirectory != null ? defaultTargetDirectory.getProject() : elements [0].getProject();
        if (defaultTargetDirectory != null) {
            defaultTargetDirectory = resolveDirectory(defaultTargetDirectory);
            if (defaultTargetDirectory == null) return;
        }

        defaultTargetDirectory = tryNotNullizeDirectory(project, defaultTargetDirectory);

        copyAsFiles(elements, defaultTargetDirectory, project);
    }

    
    private static PsiDirectory tryNotNullizeDirectory( Project project,  PsiDirectory defaultTargetDirectory) {
        if (defaultTargetDirectory == null) {
            VirtualFile root = ArrayUtil.getFirstElement(ProjectRootManager.getInstance(project).getContentRoots());
            if (root == null) root = project.getBaseDir();
            if (root == null) root = VfsUtil.getUserHomeDir();
            defaultTargetDirectory = root != null ? PsiManager.getInstance(project).findDirectory(root) : null;

            if (defaultTargetDirectory == null) {
                LOG.warn("No directory found for project: " + project.getName() +", root: " + root);
            }
        }
        return defaultTargetDirectory;
    }

    public static void copyAsFiles(PsiElement[] elements,  PsiDirectory defaultTargetDirectory, Project project) {
        PsiDirectory targetDirectory = null;
        String newName = null;
        boolean openInEditor = true;

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            targetDirectory = defaultTargetDirectory;
        }
        else {
            CopyFilesOrDirectoriesDialog dialog = new CopyFilesOrDirectoriesDialog(elements, defaultTargetDirectory, project, false);
            if (dialog.showAndGet()) {
                newName = elements.length == 1 ? dialog.getNewName() : null;
                targetDirectory = dialog.getTargetDirectory();
                openInEditor = dialog.openInEditor();
            }
        }

        if (targetDirectory != null) {
            try {
                for (PsiElement element : elements) {
                    PsiFileSystemItem psiElement = (PsiFileSystemItem)element;
                    if (psiElement.isDirectory()) {
                        MoveFilesOrDirectoriesUtil.checkIfMoveIntoSelf(psiElement, targetDirectory);
                    }
                }
            }
            catch (IncorrectOperationException e) {
                CommonRefactoringUtil.showErrorHint(project, null, e.getMessage(), CommonBundle.getErrorTitle(), null);
                return;
            }

            copyImpl(elements, newName, targetDirectory, false, openInEditor);
        }
    }

    @Override
    public void doClone(final PsiElement element) {
        doCloneFile(element);
    }

    public static void doCloneFile(PsiElement element) {
        PsiDirectory targetDirectory;
        if (element instanceof PsiDirectory) {
            targetDirectory = ((PsiDirectory)element).getParentDirectory();
        }
        else {
            targetDirectory = PlatformPackageUtil.getDirectory(element);
        }
        targetDirectory = tryNotNullizeDirectory(element.getProject(), targetDirectory);
        if (targetDirectory == null) return;

        PsiElement[] elements = {element};
        CopyFilesOrDirectoriesDialog dialog = new CopyFilesOrDirectoriesDialog(elements, null, element.getProject(), true);
        if (dialog.showAndGet()) {
            String newName = dialog.getNewName();
            copyImpl(elements, newName, targetDirectory, true, true);
        }
    }

    
    private static PsiDirectory getCommonParentDirectory(PsiElement[] elements){
        PsiDirectory result = null;

        for (PsiElement element : elements) {
            PsiDirectory directory;

            if (element instanceof PsiDirectory) {
                directory = (PsiDirectory)element;
                directory = directory.getParentDirectory();
            }
            else if (element instanceof PsiFile) {
                directory = PlatformPackageUtil.getDirectory(element);
            }
            else {
                throw new IllegalArgumentException("unexpected element " + element);
            }

            if (directory == null) continue;

            if (result == null) {
                result = directory;
            }
            else {
                if (PsiTreeUtil.isAncestor(directory, result, true)) {
                    result = directory;
                }
            }
        }

        return result;
    }

    /**
     * @param elements
     * @param newName can be not null only if elements.length == 1
     * @param targetDirectory
     * @param openInEditor
     */
    private static void copyImpl( final PsiElement[] elements,
                                  final String newName,
                                  final PsiDirectory targetDirectory,
                                 final boolean doClone,
                                 final boolean openInEditor) {
        if (doClone && elements.length != 1) {
            throw new IllegalArgumentException("invalid number of elements to clone:" + elements.length);
        }

        if (newName != null && elements.length != 1) {
            throw new IllegalArgumentException("no new name should be set; number of elements is: " + elements.length);
        }

        final Project project = targetDirectory.getProject();
        Runnable command = new Runnable() {
            @Override
            public void run() {
                final Runnable action = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            PsiFile firstFile = null;
                            final int[] choice = elements.length > 1 || elements[0] instanceof PsiDirectory ? new int[]{-1} : null;
                            for (PsiElement element : elements) {
                                PsiFile f = copyToDirectory((PsiFileSystemItem)element, newName, targetDirectory, choice);
                                if (firstFile == null) {
                                    firstFile = f;
                                }
                            }

                            if (firstFile != null && openInEditor) {
                                CopyHandler.updateSelectionInActiveProjectView(firstFile, project, doClone);
                                if (!(firstFile instanceof PsiBinaryFile)) {
                                    EditorHelper.openInEditor(firstFile);
                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToolWindowManager.getInstance(project).activateEditorComponent();
                                        }
                                    });
                                }
                            }
                        }
                        catch (final IncorrectOperationException ex) {
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    Messages.showErrorDialog(project, ex.getMessage(), RefactoringBundle.message("error.title"));
                                }
                            });
                        }
                        catch (final IOException ex) {
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    Messages.showErrorDialog(project, ex.getMessage(), RefactoringBundle.message("error.title"));
                                }
                            });
                        }
                    }
                };
                ApplicationManager.getApplication().runWriteAction(action);
            }
        };

        String title = RefactoringBundle.message(doClone ? "copy,handler.clone.files.directories" : "copy.handler.copy.files.directories");
        CommandProcessor.getInstance().executeCommand(project, command, title, null);
    }

    /**
     * @param elementToCopy PsiFile or PsiDirectory
     * @param newName can be not null only if elements.length == 1
     * @return first copied PsiFile (recursively); null if no PsiFiles copied
     */
    
    public static PsiFile copyToDirectory( PsiFileSystemItem elementToCopy,
                                           String newName,
                                           PsiDirectory targetDirectory) throws IncorrectOperationException, IOException {
        return copyToDirectory(elementToCopy, newName, targetDirectory, null);
    }

    /**
     * @param elementToCopy PsiFile or PsiDirectory
     * @param newName can be not null only if elements.length == 1
     * @param choice a horrible way to pass/keep user preference
     * @return first copied PsiFile (recursively); null if no PsiFiles copied
     */
    
    public static PsiFile copyToDirectory( PsiFileSystemItem elementToCopy,
                                           String newName,
                                           PsiDirectory targetDirectory,
                                           int[] choice) throws IncorrectOperationException, IOException {
        if (elementToCopy instanceof PsiFile) {
            PsiFile file = (PsiFile)elementToCopy;
            String name = newName == null ? file.getName() : newName;
            if (checkFileExist(targetDirectory, choice, file, name, "Copy")) return null;
            return targetDirectory.copyFileFrom(name, file);
        }
        else if (elementToCopy instanceof PsiDirectory) {
            PsiDirectory directory = (PsiDirectory)elementToCopy;
            if (directory.equals(targetDirectory)) {
                return null;
            }
            if (newName == null) newName = directory.getName();
            final PsiDirectory existing = targetDirectory.findSubdirectory(newName);
            final PsiDirectory subdirectory = existing == null ? targetDirectory.createSubdirectory(newName) : existing;
            EncodingRegistry.doActionAndRestoreEncoding(directory.getVirtualFile(), new ThrowableComputable<VirtualFile, IOException>() {
                @Override
                public VirtualFile compute() {
                    return subdirectory.getVirtualFile();
                }
            });

            PsiFile firstFile = null;
            PsiElement[] children = directory.getChildren();
            for (PsiElement child : children) {
                PsiFileSystemItem item = (PsiFileSystemItem)child;
                PsiFile f = copyToDirectory(item, item.getName(), subdirectory, choice);
                if (firstFile == null) {
                    firstFile = f;
                }
            }
            return firstFile;
        }
        else {
            throw new IllegalArgumentException("unexpected elementToCopy: " + elementToCopy);
        }
    }

    public static boolean checkFileExist( PsiDirectory targetDirectory, int[] choice, PsiFile file, String name, String title) {
        if (targetDirectory == null) return false;
        final PsiFile existing = targetDirectory.findFile(name);
        if (existing != null && !existing.equals(file)) {
            int selection;
            if (choice == null || choice[0] == -1) {
                String message = String.format("File '%s' already exists in directory '%s'", name, targetDirectory.getVirtualFile().getPath());
                String[] options = choice == null ? new String[]{"Overwrite", "Skip"}
                        : new String[]{"Overwrite", "Skip", "Overwrite for all", "Skip for all"};
                selection = Messages.showDialog(message, title, options, 0, Messages.getQuestionIcon());
            }
            else {
                selection = choice[0];
            }

            if (choice != null && selection > 1) {
                choice[0] = selection % 2;
                selection = choice[0];
            }

            if (selection == 0 && file != existing) {
                existing.delete();
            }
            else {
                return true;
            }
        }

        return false;
    }

    
    protected static PsiDirectory resolveDirectory( PsiDirectory defaultTargetDirectory) {
        final Project project = defaultTargetDirectory.getProject();
        final Boolean showDirsChooser = defaultTargetDirectory.getCopyableUserData(CopyPasteDelegator.SHOW_CHOOSER_KEY);
        if (showDirsChooser != null && showDirsChooser.booleanValue()) {
            final PsiDirectoryContainer directoryContainer =
                    PsiDirectoryFactory.getInstance(project).getDirectoryContainer(defaultTargetDirectory);
            if (directoryContainer == null) {
                return defaultTargetDirectory;
            }
            return MoveFilesOrDirectoriesUtil.resolveToDirectory(project, directoryContainer);
        }
        return defaultTargetDirectory;
    }
}
