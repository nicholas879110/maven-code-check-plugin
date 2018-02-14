/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.updateSettings.impl;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.plugins.PluginManagerMain;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.progress.PerformInBackgroundOption;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.ui.TableUtil;
import com.gome.maven.util.containers.ContainerUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Set;

/**
 * @author pti
 */
class PluginUpdateInfoDialog extends AbstractUpdateDialog {
    private final Collection<PluginDownloader> myUploadedPlugins;
    private final boolean myPlatformUpdate;

    public PluginUpdateInfoDialog(Collection<PluginDownloader> uploadedPlugins, boolean enableLink) {
        super(enableLink);
        myUploadedPlugins = uploadedPlugins;
        myPlatformUpdate = false;
        init();
    }

    /**
     * Used from {@link UpdateInfoDialog} when both platform and plugin updates are available.
     */
    PluginUpdateInfoDialog(Component parent,  Collection<PluginDownloader> updatePlugins) {
        super(parent, false);
        myUploadedPlugins = updatePlugins;
        myPlatformUpdate = true;
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return new PluginUpdateInfoPanel().myPanel;
    }

    @Override
    protected String getOkButtonText() {
        if (!myPlatformUpdate) {
            return IdeBundle.message("update.plugins.update.action");
        }
        else {
            boolean canRestart = ApplicationManager.getApplication().isRestartCapable();
            return IdeBundle.message(canRestart ? "update.restart.plugins.update.action" : "update.shutdown.plugins.update.action");
        }
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();

        ProgressManager.getInstance().run(new Task.Backgroundable(null, IdeBundle.message("progress.downloading.plugins"), true, PerformInBackgroundOption.DEAF) {
            @Override
            public void run( ProgressIndicator indicator) {
                UpdateChecker.saveDisabledToUpdatePlugins();
                boolean updated = UpdateChecker.installPluginUpdates(myUploadedPlugins, indicator);
                if (updated && !myPlatformUpdate) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            PluginManagerMain.notifyPluginsUpdated(null);
                        }
                    }, ModalityState.NON_MODAL);
                }
            }
        });
    }

    private class PluginUpdateInfoPanel {
        private JPanel myPanel;
        private JLabel myPluginsToUpdateLabel;
        private JPanel myPluginsPanel;
        private JEditorPane myMessageArea;

        public PluginUpdateInfoPanel() {
            myPluginsToUpdateLabel.setVisible(true);
            myPluginsPanel.setVisible(true);

            final DetectedPluginsPanel foundPluginsPanel = new DetectedPluginsPanel();
            foundPluginsPanel.addAll(myUploadedPlugins);
            TableUtil.ensureSelectionExists(foundPluginsPanel.getEntryTable());
            foundPluginsPanel.addStateListener(new DetectedPluginsPanel.Listener() {
                @Override
                public void stateChanged() {
                    final Set<String> skipped = foundPluginsPanel.getSkippedPlugins();
                    final PluginDownloader any = ContainerUtil.find(myUploadedPlugins, new Condition<PluginDownloader>() {
                        @Override
                        public boolean value(PluginDownloader plugin) {
                            return !skipped.contains(plugin.getPluginId());
                        }
                    });
                    getOKAction().setEnabled(any != null);
                }
            });
            myPluginsPanel.add(foundPluginsPanel, BorderLayout.CENTER);

            configureMessageArea(myMessageArea);
        }
    }
}
