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

package com.gome.maven.openapi.vcs.checkin;

import com.gome.maven.codeInsight.CodeInsightBundle;
import com.gome.maven.codeInsight.actions.ReformatCodeProcessor;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.CheckinProjectPanel;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsConfiguration;
import com.gome.maven.openapi.vcs.ui.RefreshableOnComponent;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.NonFocusableCheckBox;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class ReformatBeforeCheckinHandler extends CheckinHandler implements CheckinMetaHandler {

    public static final String COMMAND_NAME = CodeInsightBundle.message("process.reformat.code.before.commit");

    protected final Project myProject;
    private final CheckinProjectPanel myPanel;

    public ReformatBeforeCheckinHandler(final Project project, final CheckinProjectPanel panel) {
        myProject = project;
        myPanel = panel;
    }

    @Override

    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox reformatBox = new NonFocusableCheckBox(VcsBundle.message("checkbox.checkin.options.reformat.code"));

        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                final JPanel panel = new JPanel(new GridLayout(1, 0));
                panel.add(reformatBox);
                return panel;
            }

            @Override
            public void refresh() {
            }

            @Override
            public void saveState() {
                getSettings().REFORMAT_BEFORE_PROJECT_COMMIT = reformatBox.isSelected();
            }

            @Override
            public void restoreState() {
                reformatBox.setSelected(getSettings().REFORMAT_BEFORE_PROJECT_COMMIT);
            }
        };

    }

    protected VcsConfiguration getSettings() {
        return VcsConfiguration.getInstance(myProject);
    }

    @Override
    public void runCheckinHandlers( final Runnable finishAction) {
        final VcsConfiguration configuration = VcsConfiguration.getInstance(myProject);
        final Collection<VirtualFile> files = myPanel.getVirtualFiles();

        final Runnable performCheckoutAction = new Runnable() {
            @Override
            public void run() {
                FileDocumentManager.getInstance().saveAllDocuments();
                finishAction.run();
            }
        };

        if (reformat(configuration, true)) {
            new ReformatCodeProcessor(
                    myProject, CheckinHandlerUtil.getPsiFiles(myProject, files), COMMAND_NAME, performCheckoutAction, true
            ).run();
        }
        else {
            performCheckoutAction.run();
        }

    }

    private static boolean reformat(final VcsConfiguration configuration, boolean checkinProject) {
        return checkinProject ? configuration.REFORMAT_BEFORE_PROJECT_COMMIT : configuration.REFORMAT_BEFORE_FILE_COMMIT;
    }

}
