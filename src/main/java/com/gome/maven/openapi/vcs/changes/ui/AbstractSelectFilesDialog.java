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

package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.CommonBundle;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.MultiLineLabelUI;
import com.gome.maven.openapi.vcs.VcsShowConfirmationOption;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author yole
 */
public abstract class AbstractSelectFilesDialog<T> extends DialogWrapper {
    protected JCheckBox myDoNotShowCheckbox;
    protected final VcsShowConfirmationOption myConfirmationOption;
    private final String myPrompt;
    private final boolean myShowDoNotAskOption;

    public AbstractSelectFilesDialog(Project project, boolean canBeParent, final VcsShowConfirmationOption confirmationOption,
                                     final String prompt, boolean showDoNotAskOption) {
        super(project, canBeParent);
        myConfirmationOption = confirmationOption;
        myPrompt = prompt;
        myShowDoNotAskOption = showDoNotAskOption;
    }

    
    protected abstract ChangesTreeList getFileList();

    
    private JLabel createPromptLabel() {
        if (myPrompt != null) {
            final JLabel label = new JLabel(myPrompt);
            label.setUI(new MultiLineLabelUI());
            label.setBorder(new EmptyBorder(5, 1, 5, 1));
            return label;
        }
        return null;
    }

    @Override
    protected JComponent createNorthPanel() {
        return createPromptLabel();
    }

    protected void doOKAction() {
        if (myDoNotShowCheckbox != null && myDoNotShowCheckbox.isSelected()) {
            myConfirmationOption.setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
        }
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        if (myDoNotShowCheckbox != null && myDoNotShowCheckbox.isSelected()) {
            myConfirmationOption.setValue(VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY);
        }
        super.doCancelAction();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return getFileList();
    }

    
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createToolbar(), BorderLayout.NORTH);

        panel.add(getFileList(), BorderLayout.CENTER);

        if (myShowDoNotAskOption) {
            myDoNotShowCheckbox = new JCheckBox(CommonBundle.message("dialog.options.do.not.ask"));
            panel.add(myDoNotShowCheckbox, BorderLayout.SOUTH);
        }
        return panel;
    }

    private JComponent createToolbar() {
        DefaultActionGroup group = createToolbarActions();
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        return toolbar.getComponent();
    }

    
    protected DefaultActionGroup createToolbarActions() {
        DefaultActionGroup group = new DefaultActionGroup();
        final AnAction[] actions = getFileList().getTreeActions();
        for(AnAction action: actions) {
            group.add(action);
        }
        return group;
    }
}