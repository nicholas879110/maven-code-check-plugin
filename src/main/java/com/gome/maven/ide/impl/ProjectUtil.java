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
package com.gome.maven.ide.impl;

//import com.gome.maven.ide.GeneralSettings;
import com.gome.maven.CommonBundle;
import com.gome.maven.ide.GeneralSettings;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.RecentProjectsManager;
import com.gome.maven.ide.highlighter.ProjectFileType;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.StorageScheme;
import com.gome.maven.openapi.components.impl.stores.IProjectStore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.project.ex.ProjectManagerEx;
//import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.*;
import com.gome.maven.projectImport.ProjectOpenProcessor;
//import com.gome.maven.ui.AppIcon;
import com.gome.maven.ui.AppIcon;
import com.gome.maven.util.SystemProperties;
import org.jdom.JDOMException;


import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author Eugene Belyaev
 */
public class ProjectUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.impl.ProjectUtil");

    private ProjectUtil() { }

    public static void updateLastProjectLocation(final String projectFilePath) {
        File lastProjectLocation = new File(projectFilePath);
        if (lastProjectLocation.isFile()) {
            lastProjectLocation = lastProjectLocation.getParentFile(); // for directory-based project storage
        }
        if (lastProjectLocation == null) { // the immediate parent of the ipr file
            return;
        }
        lastProjectLocation = lastProjectLocation.getParentFile(); // the candidate directory to be saved
        if (lastProjectLocation == null) {
            return;
        }
        String path = lastProjectLocation.getPath();
        try {
            path = FileUtil.resolveShortWindowsName(path);
        }
        catch (IOException e) {
            LOG.info(e);
            return;
        }
        RecentProjectsManager.getInstance().setLastProjectCreationLocation(path.replace(File.separatorChar, '/'));
    }

    /**
     * @param project cannot be null
     */
    public static boolean closeAndDispose( final Project project) {
        return ProjectManagerEx.getInstanceEx().closeAndDispose(project);
    }

    /**
     * @param path                project file path
     * @param projectToClose      currently active project
     * @param forceOpenInNewFrame forces opening in new frame
     * @return project by path if the path was recognized as IDEA project file or one of the project formats supported by
     *         installed importers (regardless of opening/import result)
     *         null otherwise
     */
    
    public static Project openOrImport( String path, Project projectToClose, boolean forceOpenInNewFrame) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (virtualFile == null) return null;
        virtualFile.refresh(false, false);

        ProjectOpenProcessor strong = ProjectOpenProcessor.getStrongImportProvider(virtualFile);
        if (strong != null) {
            return strong.doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame);
        }

        if (path.endsWith(ProjectFileType.DOT_DEFAULT_EXTENSION) ||
                virtualFile.isDirectory() && virtualFile.findChild(Project.DIRECTORY_STORE_FOLDER) != null) {
            return openProject(path, projectToClose, forceOpenInNewFrame);
        }

        if (virtualFile.isDirectory()) {
            for (VirtualFile child : virtualFile.getChildren()) {
                final String childPath = child.getPath();
                if (childPath.endsWith(ProjectFileType.DOT_DEFAULT_EXTENSION)) {
                    return openProject(childPath, projectToClose, forceOpenInNewFrame);
                }
            }
        }

        ProjectOpenProcessor provider = ProjectOpenProcessor.getImportProvider(virtualFile);
        if (provider != null) {
            final Project project = provider.doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame);

            if (project != null) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!project.isDisposed()) {
                            final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
                            if (toolWindow != null) {
                                toolWindow.activate(null);
                            }
                        }
                    }
                }, ModalityState.NON_MODAL);
            }

            return project;
        }

        return null;
    }

    
    public static Project openProject(final String path,  Project projectToClose, boolean forceOpenInNewFrame) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("error.project.file.does.not.exist:"+path);
//            Messages.showErrorDialog(IdeBundle.message("error.project.file.does.not.exist", path), CommonBundle.getErrorTitle());
            return null;
        }

        if (file.isDirectory() && !new File(file, Project.DIRECTORY_STORE_FOLDER).exists()) {
            String message = IdeBundle.message("error.project.file.does.not.exist", new File(file, Project.DIRECTORY_STORE_FOLDER).getPath());
//            Messages.showErrorDialog(message, CommonBundle.getErrorTitle());
            System.out.println("error.project.file.does.not.exist:"+ new File(file, Project.DIRECTORY_STORE_FOLDER).getPath());
            return null;
        }

        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            if (isSameProject(path, project)) {
                focusProjectWindow(project, false);
                return project;
            }
        }

        if (!forceOpenInNewFrame && openProjects.length > 0) {
            int exitCode = confirmOpenNewProject(false);
            if (exitCode == GeneralSettings.OPEN_PROJECT_SAME_WINDOW) {
                final Project toClose = projectToClose != null ? projectToClose : openProjects[openProjects.length - 1];
                if (!closeAndDispose(toClose)) return null;
            }
            else if (exitCode != GeneralSettings.OPEN_PROJECT_NEW_WINDOW) {
                return null;
            }
        }

        ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
        Project project = null;
        try {
            project = projectManager.loadAndOpenProject(path);
        }
        catch (IOException e) {
            Messages.showMessageDialog(IdeBundle.message("error.cannot.load.project", e.getMessage()),
                    IdeBundle.message("title.cannot.load.project"), Messages.getErrorIcon());
        }
        catch (JDOMException e) {
            LOG.info(e);
            Messages.showMessageDialog(IdeBundle.message("error.project.file.is.corrupted"), IdeBundle.message("title.cannot.load.project"),
                    Messages.getErrorIcon());
        }
        catch (InvalidDataException e) {
            LOG.info(e);
            Messages.showMessageDialog(IdeBundle.message("error.project.file.is.corrupted"), IdeBundle.message("title.cannot.load.project"),
                    Messages.getErrorIcon());
        }
        return project;
    }

    public static boolean confirmLoadingFromRemotePath( String path, String msgKey, String titleKey) {
        return showYesNoDialog(IdeBundle.message(msgKey, path), titleKey);
    }

    public static boolean showYesNoDialog( String message,  String titleKey) {
        final Window window = getActiveFrameOrWelcomeScreen();
        final Icon icon = Messages.getWarningIcon();
        String title = IdeBundle.message(titleKey);
        final int answer = window == null ? Messages.showYesNoDialog(message, title, icon) : Messages.showYesNoDialog(window, message, title, icon);
        return answer == Messages.YES;
    }

    private static Window getActiveFrameOrWelcomeScreen() {
        Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        if (window != null)  return window;

        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof IdeFrame && frame.isVisible()) {
                return frame;
            }
        }

        return null;
    }

    public static boolean isRemotePath( String path) {
        return path.contains("//") || path.contains("\\\\");
    }

    /**
     * @return {@link com.gome.maven.ide.GeneralSettings#OPEN_PROJECT_SAME_WINDOW}
     *         {@link com.gome.maven.ide.GeneralSettings#OPEN_PROJECT_NEW_WINDOW}
     *         {@link com.gome.maven.openapi.ui.Messages#CANCEL} - if user canceled the dialog
     * @param isNewProject
     */
    public static int confirmOpenNewProject(boolean isNewProject) {
        final GeneralSettings settings = GeneralSettings.getInstance();
        int confirmOpenNewProject = ApplicationManager.getApplication().isUnitTestMode() ? GeneralSettings.OPEN_PROJECT_NEW_WINDOW : settings.getConfirmOpenNewProject();
        if (confirmOpenNewProject == GeneralSettings.OPEN_PROJECT_ASK) {
            if (isNewProject) {
                int exitCode = Messages.showYesNoDialog(IdeBundle.message("prompt.open.project.in.new.frame"),
                        IdeBundle.message("title.new.project"),
                        IdeBundle.message("button.existingframe"),
                        IdeBundle.message("button.newframe"),
                        Messages.getQuestionIcon(),
                        new ProjectNewWindowDoNotAskOption());
                return exitCode == Messages.YES ? GeneralSettings.OPEN_PROJECT_SAME_WINDOW : GeneralSettings.OPEN_PROJECT_NEW_WINDOW;
            }
            else {
                int exitCode = Messages.showYesNoCancelDialog(IdeBundle.message("prompt.open.project.in.new.frame"),
                        IdeBundle.message("title.open.project"),
                        IdeBundle.message("button.existingframe"),
                        IdeBundle.message("button.newframe"),
                        CommonBundle.getCancelButtonText(),
                        Messages.getQuestionIcon(),
                        new ProjectNewWindowDoNotAskOption());
                return exitCode == Messages.YES ? GeneralSettings.OPEN_PROJECT_SAME_WINDOW :
                        exitCode == Messages.NO ? GeneralSettings.OPEN_PROJECT_NEW_WINDOW : Messages.CANCEL;
            }
        }
        return confirmOpenNewProject;
    }

    private static boolean isSameProject(String path, Project p) {
        final IProjectStore projectStore = ((ProjectEx)p).getStateStore();

        String toOpen = FileUtil.toSystemIndependentName(path);
        String existing = FileUtil.toSystemIndependentName(projectStore.getProjectFilePath());

        final VirtualFile existingBaseDir = projectStore.getProjectBaseDir();
        if (existingBaseDir == null) return false; // could be null if not yet initialized

        final File openFile = new File(toOpen);
        if (openFile.isDirectory()) {
            return FileUtil.pathsEqual(toOpen, existingBaseDir.getPath());
        }
        if (StorageScheme.DIRECTORY_BASED == projectStore.getStorageScheme()) {
            // todo: check if IPR is located not under the project base dir
            return FileUtil.pathsEqual(FileUtil.toSystemIndependentName(openFile.getParentFile().getPath()), existingBaseDir.getPath());
        }

        return FileUtil.pathsEqual(toOpen, existing);
    }

    public static void focusProjectWindow(final Project p, boolean executeIfAppInactive) {
        FocusCommand cmd = new FocusCommand() {

            @Override
            public ActionCallback run() {
                JFrame f = WindowManager.getInstance().getFrame(p);
                if (f != null) {
                    f.toFront();
                    //f.requestFocus();
                }
                return new ActionCallback.Done();
            }
        };

        if (executeIfAppInactive) {
            AppIcon.getInstance().requestFocus((IdeFrame)WindowManager.getInstance().getFrame(p));
            cmd.run();
        } else {
            IdeFocusManager.getInstance(p).requestFocus(cmd, true);
        }
    }

    public static String getBaseDir() {
        final String lastProjectLocation = RecentProjectsManager.getInstance().getLastProjectCreationLocation();
        if (lastProjectLocation != null) {
            return lastProjectLocation.replace('/', File.separatorChar);
        }
        final String userHome = SystemProperties.getUserHome();
        //noinspection HardCodedStringLiteral
        return userHome.replace('/', File.separatorChar) + File.separator + ApplicationNamesInfo.getInstance().getLowercaseProductName() +
                "Projects";
    }
}
