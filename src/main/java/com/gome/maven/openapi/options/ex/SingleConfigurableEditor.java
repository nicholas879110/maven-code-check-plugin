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
package com.gome.maven.openapi.options.ex;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.actions.ShowSettingsUtilImpl;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.help.HelpManager;
import com.gome.maven.openapi.options.BaseConfigurable;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.gome.maven.util.Alarm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class SingleConfigurableEditor extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.options.ex.SingleConfigurableEditor");
    private Project myProject;
    private Configurable myConfigurable;
    private JComponent myCenterPanel;
    private final String myDimensionKey;
    private final boolean myShowApplyButton;
    private boolean mySaveAllOnClose;

    public SingleConfigurableEditor( Project project,
                                    Configurable configurable,
                                     String dimensionKey,
                                    final boolean showApplyButton,
                                     IdeModalityType ideModalityType) {
        super(project, true, ideModalityType);
        myDimensionKey = dimensionKey;
        myShowApplyButton = showApplyButton;
        String title = createTitleString(configurable);
        if (project != null && project.isDefault()) title = "Default " + title;
        setTitle(title);

        myProject = project;
        myConfigurable = configurable;
        init();
        myConfigurable.reset();
    }

    public SingleConfigurableEditor(Component parent,
                                    Configurable configurable,
                                    String dimensionServiceKey,
                                    final boolean showApplyButton,
                                    final IdeModalityType ideModalityType) {
        super(parent, true);
        myDimensionKey = dimensionServiceKey;
        myShowApplyButton = showApplyButton;
        setTitle(createTitleString(configurable));

        myConfigurable = configurable;
        init();
        myConfigurable.reset();
    }

    public SingleConfigurableEditor( Project project,
                                    Configurable configurable,
                                     String dimensionKey,
                                    final boolean showApplyButton) {
        this(project, configurable, dimensionKey, showApplyButton, IdeModalityType.IDE);
    }

    public SingleConfigurableEditor(Component parent,
                                    Configurable configurable,
                                    String dimensionServiceKey,
                                    final boolean showApplyButton) {
        this(parent, configurable, dimensionServiceKey, showApplyButton, IdeModalityType.IDE);
    }

    public SingleConfigurableEditor( Project project, Configurable configurable,  String dimensionKey,  IdeModalityType ideModalityType) {
        this(project, configurable, dimensionKey, true, ideModalityType);
    }

    public SingleConfigurableEditor( Project project, Configurable configurable,  String dimensionKey) {
        this(project, configurable, dimensionKey, true);
    }

    public SingleConfigurableEditor(Component parent, Configurable configurable, String dimensionServiceKey) {
        this(parent, configurable, dimensionServiceKey, true);
    }

    public SingleConfigurableEditor( Project project, Configurable configurable,  IdeModalityType ideModalityType) {
        this(project, configurable, ShowSettingsUtilImpl.createDimensionKey(configurable), ideModalityType);
    }

    public SingleConfigurableEditor( Project project, Configurable configurable) {
        this(project, configurable, ShowSettingsUtilImpl.createDimensionKey(configurable));
    }

    public SingleConfigurableEditor(Component parent, Configurable configurable) {
        this(parent, configurable, ShowSettingsUtilImpl.createDimensionKey(configurable));
    }

    public Configurable getConfigurable() {
        return myConfigurable;
    }

    public Project getProject() {
        return myProject;
    }

    private static String createTitleString(Configurable configurable) {
        String displayName = configurable.getDisplayName();
        LOG.assertTrue(displayName != null, configurable.getClass().getName());
        return displayName.replaceAll("\n", " ");
    }

    @Override
    protected String getDimensionServiceKey() {
        if (myDimensionKey == null) {
            return super.getDimensionServiceKey();
        }
        else {
            return myDimensionKey;
        }
    }

    @Override
    
    protected Action[] createActions() {
        List<Action> actions = new ArrayList<Action>();
        actions.add(getOKAction());
        actions.add(getCancelAction());
        if (myShowApplyButton) {
            actions.add(new ApplyAction());
        }
        if (myConfigurable.getHelpTopic() != null) {
            actions.add(getHelpAction());
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(myConfigurable.getHelpTopic());
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        try {
            if (myConfigurable.isModified()) {
                myConfigurable.apply();
                mySaveAllOnClose = true;
            }
        }
        catch (ConfigurationException e) {
            if (e.getMessage() != null) {
                if (myProject != null) {
                    Messages.showMessageDialog(myProject, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
                }
                else {
                    Messages.showMessageDialog(getRootPane(), e.getMessage(), e.getTitle(), Messages.getErrorIcon());
                }
            }
            return;
        }
        super.doOKAction();
    }

    protected static String createDimensionKey(Configurable configurable) {
        String displayName = configurable.getDisplayName();
        displayName = displayName.replaceAll("\n", "_").replaceAll(" ", "_");
        return "#" + displayName;
    }

    protected class ApplyAction extends AbstractAction {
        private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

        public ApplyAction() {
            super(CommonBundle.getApplyButtonText());
            final Runnable updateRequest = new Runnable() {
                @Override
                public void run() {
                    if (!isShowing()) return;
                    try {
                        setEnabled(myConfigurable != null && myConfigurable.isModified());
                    }
                    catch (IndexNotReadyException ignored) {
                    }
                    addUpdateRequest(this);
                }
            };

            // invokeLater necessary to make sure dialog is already shown so we calculate modality state correctly.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    addUpdateRequest(updateRequest);
                }
            });
        }

        private void addUpdateRequest(final Runnable updateRequest) {
            myUpdateAlarm.addRequest(updateRequest, 500, ModalityState.stateForComponent(getWindow()));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            if (myPerformAction) return;
            try {
                myPerformAction = true;
                if (myConfigurable.isModified()) {
                    myConfigurable.apply();
                    mySaveAllOnClose = true;
                    setCancelButtonText(CommonBundle.getCloseButtonText());
                }
            }
            catch (ConfigurationException e) {
                if (myProject != null) {
                    Messages.showMessageDialog(myProject, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
                }
                else {
                    Messages.showMessageDialog(getRootPane(), e.getMessage(), e.getTitle(),
                            Messages.getErrorIcon());
                }
            } finally {
                myPerformAction = false;
            }
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        myCenterPanel = myConfigurable.createComponent();
        return myCenterPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        if (myConfigurable instanceof BaseConfigurable) {
            JComponent preferred = ((BaseConfigurable)myConfigurable).getPreferredFocusedComponent();
            if (preferred != null) return preferred;
        }
        return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myCenterPanel);
    }

    @Override
    public void dispose() {
        super.dispose();
        myConfigurable.disposeUIResources();
        myConfigurable = null;

        if (mySaveAllOnClose) {
            ApplicationManager.getApplication().saveAll();
        }
    }
}
