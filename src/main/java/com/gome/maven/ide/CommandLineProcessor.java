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
package com.gome.maven.ide;

import com.gome.maven.ide.highlighter.ProjectFileType;
import com.gome.maven.ide.impl.ProjectUtil;
import com.gome.maven.idea.StartupUtil;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationStarter;
import com.gome.maven.openapi.application.ApplicationStarterEx;
import com.gome.maven.openapi.extensions.Extensions;
//import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.roots.ProjectRootManager;
//import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
//import com.gome.maven.openapi.wm.IdeFocusManager;
//import com.gome.maven.openapi.wm.IdeFrame;
import com.gome.maven.openapi.wm.IdeFocusManager;
import com.gome.maven.openapi.wm.IdeFrame;
import com.gome.maven.platform.PlatformProjectOpenProcessor;
import com.gome.maven.projectImport.ProjectOpenProcessor;
import com.gome.maven.util.ArrayUtil;


import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * @author yole
 */
public class CommandLineProcessor {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.CommandLineProcessor");

    private CommandLineProcessor() {
    }

    public static void openFileOrProject(final String name) {
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (name != null) {
                    doOpenFileOrProject(name);
                }
            }
        });
    }

    
    private static Project doOpenFileOrProject(String name) {
        final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(name);
        if (virtualFile == null) {
            System.out.println("Cannot find file '" + name + "'");
//            Messages.showErrorDialog();
            return null;
        }
        ProjectOpenProcessor provider = ProjectOpenProcessor.getImportProvider(virtualFile);
        if (provider instanceof PlatformProjectOpenProcessor && !virtualFile.isDirectory()) {
            // HACK: PlatformProjectOpenProcessor agrees to open anything
            provider = null;
        }
        if (provider != null ||
                name.endsWith(ProjectFileType.DOT_DEFAULT_EXTENSION) ||
                new File(name, Project.DIRECTORY_STORE_FOLDER).exists()) {
            final Project result = ProjectUtil.openOrImport(name, null, true);
            if (result == null) {
                System.out.println("Cannot open project '" + name + "'");
            }
            return result;
        }
        else {
            return doOpenFile(virtualFile, -1);
        }
    }

    
    private static Project doOpenFile(VirtualFile virtualFile, int line) {
        final Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) {
            final PlatformProjectOpenProcessor processor = PlatformProjectOpenProcessor.getInstanceIfItExists();
            if (processor != null) {
                return PlatformProjectOpenProcessor.doOpenProject(virtualFile, null, false, line, null, false);
            }
//            Messages.showErrorDialog("No project found to open file in", "Cannot open file");
            System.out.println("No project found to open file in");
            return null;
        }
        else {
            Project project = findBestProject(virtualFile, projects);
//            if (line == -1) {
//                new OpenFileDescriptor(project, virtualFile).navigate(true);
//            }
//            else {
//                new OpenFileDescriptor(project, virtualFile, line-1, 0).navigate(true);
//            }
            return project;
        }
    }

    
    private static Project findBestProject(VirtualFile virtualFile, Project[] projects) {
        for (Project aProject : projects) {
            if (ProjectRootManager.getInstance(aProject).getFileIndex().isInContent(virtualFile)) {
                return aProject;
            }
        }
        IdeFrame frame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
        Project project = frame == null ? null : frame.getProject();
        return project != null ?project :  projects[0];
    }

    
    public static Project processExternalCommandLine(List<String> args,  String currentDirectory) {
        if (args.size() > 0) {
            LOG.info("External command line:");
            LOG.info("Dir: " + currentDirectory);
            for (String arg : args) {
                LOG.info(arg);
            }
        }
        LOG.info("-----");

        if (args.size() > 0) {
            String command = args.get(0);
            for(ApplicationStarter starter: Extensions.getExtensions(ApplicationStarter.EP_NAME)) {
                if (command.equals(starter.getCommandName()) &&
                        starter instanceof ApplicationStarterEx &&
                        ((ApplicationStarterEx)starter).canProcessExternalCommandLine()) {
                    LOG.info("Processing command with " + starter);
                    ((ApplicationStarterEx) starter).processExternalCommandLine(ArrayUtil.toStringArray(args), currentDirectory);
                    return null;
                }
            }
        }

        Project lastOpenedProject = null;
        int line = -1;
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            String arg = args.get(i);
            if (arg.equals(StartupUtil.NO_SPLASH)) {
                continue;
            }
            if (arg.equals("-l") || arg.equals("--line")) {
                //noinspection AssignmentToForLoopParameter
                i++;
                if (i == args.size()) {
                    break;
                }
                try {
                    line = Integer.parseInt(args.get(i));
                }
                catch (NumberFormatException e) {
                    line = -1;
                }
            }
            else {
                if (StringUtil.isQuotedString(arg)) {
                    arg = StringUtil.stripQuotesAroundValue(arg);
                }
                if (!new File(arg).isAbsolute()) {
                    arg = currentDirectory != null ? new File(currentDirectory, arg).getAbsolutePath() : new File(arg).getAbsolutePath();
                }
                if (line != -1) {
                    final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(arg);
                    if (virtualFile != null) {
                        lastOpenedProject = doOpenFile(virtualFile, line);
                    }
                    else {
//                        Messages.showErrorDialog("Cannot find file '" + arg + "'", "Cannot find file");
                        System.out.println("Cannot find file '" + arg + "'");
                    }
                }
                else {
                    lastOpenedProject = doOpenFileOrProject(arg);
                }
            }
        }
        return lastOpenedProject;
    }
}
