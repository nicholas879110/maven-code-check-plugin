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
package com.gome.maven.ide.actions;

import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.configurations.GeneralCommandLine;
import com.gome.maven.execution.util.ExecUtil;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.notification.Notifications;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.application.ApplicationBundle;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.AppUIUtil;
import com.gome.maven.util.PlatformUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static com.gome.maven.util.containers.ContainerUtil.newHashMap;
import static java.util.Arrays.asList;

public class CreateDesktopEntryAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.actions.CreateDesktopEntryAction");

    public static boolean isAvailable() {
        return SystemInfo.isUnix && SystemInfo.hasXdgOpen();
    }

    @Override
    public void update(final AnActionEvent event) {
        final boolean enabled = isAvailable();
        final Presentation presentation = event.getPresentation();
        presentation.setEnabled(enabled);
        presentation.setVisible(enabled);
    }

    @Override
    public void actionPerformed(final AnActionEvent event) {
        if (!isAvailable()) return;

        final Project project = event.getProject();
        final CreateDesktopEntryDialog dialog = new CreateDesktopEntryDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }

        final boolean globalEntry = dialog.myGlobalEntryCheckBox.isSelected();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, event.getPresentation().getText()) {
            @Override
            public void run( final ProgressIndicator indicator) {
                createDesktopEntry(getProject(), indicator, globalEntry);
            }
        });
    }

    public static void createDesktopEntry( final Project project,
                                           final ProgressIndicator indicator,
                                          final boolean globalEntry) {
        if (!isAvailable()) return;
        final double step = (1.0 - indicator.getFraction()) / 3.0;

        try {
            indicator.setText(ApplicationBundle.message("desktop.entry.checking"));
            check();
            indicator.setFraction(indicator.getFraction() + step);

            indicator.setText(ApplicationBundle.message("desktop.entry.preparing"));
            final File entry = prepare();
            indicator.setFraction(indicator.getFraction() + step);

            indicator.setText(ApplicationBundle.message("desktop.entry.installing"));
            install(entry, globalEntry);
            indicator.setFraction(indicator.getFraction() + step);

            final String message = ApplicationBundle.message("desktop.entry.success",
                    ApplicationNamesInfo.getInstance().getProductName());
            if (ApplicationManager.getApplication() != null) {
                Notifications.Bus
                        .notify(new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, "Desktop entry created", message, NotificationType.INFORMATION));
            }
        }
        catch (Exception e) {
            if (ApplicationManager.getApplication() == null) {
                throw new RuntimeException(e);
            }
            final String message = e.getMessage();
            if (!StringUtil.isEmptyOrSpaces(message)) {
                LOG.warn(e);
                Notifications.Bus.notify(
                        new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, "Failed to create desktop entry", message, NotificationType.ERROR),
                        project
                );
            }
            else {
                LOG.error(e);
            }
        }
    }

    private static void check() throws ExecutionException, InterruptedException {
        final int result = ExecUtil.execAndGetResult("which", "xdg-desktop-menu");
        if (result != 0) throw new RuntimeException(ApplicationBundle.message("desktop.entry.xdg.missing"));
    }

    private static File prepare() throws IOException {
        final String homePath = PathManager.getHomePath();
        assert new File(homePath).isDirectory() : "Invalid home path: '" + homePath + "'";
        final String binPath = homePath + "/bin";
        assert new File(binPath).isDirectory() : "Invalid bin path: '" + binPath + "'";

        String name = ApplicationNamesInfo.getInstance().getFullProductName();
        if (PlatformUtils.isIdeaCommunity()) name += " Community Edition";

        final String iconPath = AppUIUtil.findIcon(binPath);
        if (iconPath == null) {
            throw new RuntimeException(ApplicationBundle.message("desktop.entry.icon.missing", binPath));
        }

        final String execPath = findScript(binPath);
        if (execPath == null) {
            throw new RuntimeException(ApplicationBundle.message("desktop.entry.script.missing", binPath));
        }

        final String wmClass = AppUIUtil.getFrameClass();

        final String content = ExecUtil.loadTemplate(CreateDesktopEntryAction.class.getClassLoader(), "entry.desktop",
                newHashMap(asList("$NAME$", "$SCRIPT$", "$ICON$", "$WM_CLASS$"),
                        asList(name, execPath, iconPath, wmClass)));

        final String entryName = wmClass + ".desktop";
        final File entryFile = new File(FileUtil.getTempDirectory(), entryName);
        FileUtil.writeToFile(entryFile, content);
        entryFile.deleteOnExit();
        return entryFile;
    }

    
    private static String findScript(String binPath) {
        String productName = ApplicationNamesInfo.getInstance().getProductName();

        String execPath = binPath + '/' + productName + ".sh";
        if (new File(execPath).canExecute()) return execPath;

        execPath = binPath + '/' + productName.toLowerCase(Locale.US) + ".sh";
        if (new File(execPath).canExecute()) return execPath;

        String scriptName = ApplicationNamesInfo.getInstance().getScriptName();
        execPath = binPath + '/' + scriptName + ".sh";
        if (new File(execPath).canExecute()) return execPath;

        return null;
    }

    private static void install(File entryFile, boolean globalEntry) throws IOException, ExecutionException, InterruptedException {
        if (globalEntry) {
            File script = ExecUtil.createTempExecutableScript(
                    "sudo", ".sh", "#!/bin/sh\n" +
                            "xdg-desktop-menu install --mode system \"" + entryFile.getAbsolutePath() + "\"\n" +
                            "RV=$?\n" +
                            "xdg-desktop-menu forceupdate --mode system\n" +
                            "exit $RV\n");
            script.deleteOnExit();
            String prompt = ApplicationBundle.message("desktop.entry.sudo.prompt");
            int result = ExecUtil.sudoAndGetOutput(new GeneralCommandLine(script.getPath()), prompt).getExitCode();
            if (result != 0) throw new RuntimeException("'" + script.getAbsolutePath() + "' : " + result);
        }
        else {
            int result = ExecUtil.execAndGetResult("xdg-desktop-menu", "install", "--mode", "user", entryFile.getAbsolutePath());
            if (result != 0) throw new RuntimeException("'" + entryFile.getAbsolutePath() + "' : " + result);
            ExecUtil.execAndGetResult("xdg-desktop-menu", "forceupdate", "--mode", "user");
        }
    }

    public static class CreateDesktopEntryDialog extends DialogWrapper {
        private JPanel myContentPane;
        private JLabel myLabel;
        private JCheckBox myGlobalEntryCheckBox;

        public CreateDesktopEntryDialog(final Project project) {
            super(project);
            init();
            setTitle("Create Desktop Entry");
            myLabel.setText(myLabel.getText().replace("$APP_NAME$", ApplicationNamesInfo.getInstance().getProductName()));
        }

        @Override
        protected JComponent createCenterPanel() {
            return myContentPane;
        }
    }
}
