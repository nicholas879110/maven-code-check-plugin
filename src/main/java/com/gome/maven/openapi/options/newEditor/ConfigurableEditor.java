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
package com.gome.maven.openapi.options.newEditor;

import com.gome.maven.AbstractBundle;
import com.gome.maven.CommonBundle;
import com.gome.maven.internal.statistic.UsageTrigger;
import com.gome.maven.internal.statistic.beans.ConvertUsagesUtil;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.ex.AnActionListener;
import com.gome.maven.openapi.options.*;
import com.gome.maven.openapi.options.ex.ConfigurableCardPanel;
import com.gome.maven.openapi.options.ex.ConfigurableVisitor;
import com.gome.maven.openapi.options.ex.ConfigurableWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.RelativeFont;
import com.gome.maven.ui.components.labels.LinkLabel;
import com.gome.maven.util.ui.JBUI;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.update.MergingUpdateQueue;
import com.gome.maven.util.ui.update.Update;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

import static java.awt.Toolkit.getDefaultToolkit;
import static javax.swing.SwingUtilities.isDescendingFrom;

/**
 * @author Sergey.Malenkov
 */
class ConfigurableEditor extends AbstractEditor implements AnActionListener, AWTEventListener {
    private static final JBColor ERROR_BACKGROUND = new JBColor(0xffbfbf, 0x591f1f);
    private static final String RESET_NAME = "Reset";
    private static final String RESET_DESCRIPTION = "Rollback changes for this configuration element";
    private final MergingUpdateQueue myQueue = new MergingUpdateQueue("SettingsModification", 1000, false, this, this, this);
    private final ConfigurableCardPanel myCardPanel = new ConfigurableCardPanel() {
        @Override
        protected JComponent create(Configurable configurable) {
            JComponent content = super.create(configurable);
            return content != null ? content : createDefaultContent(configurable);
        }
    };
    private final JLabel myErrorLabel = new JLabel();
    private final AbstractAction myApplyAction = new AbstractAction(CommonBundle.getApplyButtonText()) {
        @Override
        public void actionPerformed(ActionEvent event) {
            apply();
        }
    };
    private final AbstractAction myResetAction = new AbstractAction(RESET_NAME) {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (myConfigurable != null) {
                myConfigurable.reset();
                updateCurrent(myConfigurable, true);
            }
        }
    };
    private Configurable myConfigurable;

    ConfigurableEditor(Disposable parent, Configurable configurable) {
        super(parent);
        myApplyAction.setEnabled(false);
        myResetAction.putValue(Action.SHORT_DESCRIPTION, RESET_DESCRIPTION);
        myResetAction.setEnabled(false);
        myErrorLabel.setOpaque(true);
        myErrorLabel.setEnabled(parent instanceof SettingsEditor);
        myErrorLabel.setVisible(false);
        myErrorLabel.setVerticalTextPosition(SwingConstants.TOP);
        myErrorLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        myErrorLabel.setBackground(ERROR_BACKGROUND);
        add(BorderLayout.SOUTH, RelativeFont.HUGE.install(myErrorLabel));
        add(BorderLayout.CENTER, myCardPanel);
        ActionManager.getInstance().addAnActionListener(this, this);
        getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        myConfigurable = configurable;
        myCardPanel.select(configurable, true);
        updateCurrent(configurable, false);
    }

    @Override
    void disposeOnce() {
        getDefaultToolkit().removeAWTEventListener(this);
        myCardPanel.removeAll();
    }

    @Override
    String getHelpTopic() {
        return myConfigurable == null ? null : myConfigurable.getHelpTopic();
    }

    @Override
    Action getApplyAction() {
        return myApplyAction;
    }

    @Override
    Action getResetAction() {
        return myResetAction;
    }

    @Override
    boolean apply() {
        return setError(apply(myConfigurable));
    }

    void openLink(Configurable configurable) {
        ShowSettingsUtil.getInstance().editConfigurable(this, configurable);
    }

    @Override
    public final void beforeEditorTyping(char ch, DataContext context) {
    }

    @Override
    public final void beforeActionPerformed(AnAction action, DataContext context, AnActionEvent event) {
    }

    @Override
    public final void afterActionPerformed(AnAction action, DataContext context, AnActionEvent event) {
        requestUpdate();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        if (myConfigurable instanceof BaseConfigurable) {
            JComponent preferred = ((BaseConfigurable)myConfigurable).getPreferredFocusedComponent();
            if (preferred != null) return preferred;
        }
        return super.getPreferredFocusedComponent();
    }

    @Override
    public final void eventDispatched(AWTEvent event) {
        switch (event.getID()) {
            case MouseEvent.MOUSE_PRESSED:
            case MouseEvent.MOUSE_RELEASED:
            case MouseEvent.MOUSE_DRAGGED:
                MouseEvent me = (MouseEvent)event;
                if (isDescendingFrom(me.getComponent(), this) || isPopupOverEditor(me.getComponent())) {
                    requestUpdate();
                }
                break;
            case KeyEvent.KEY_PRESSED:
            case KeyEvent.KEY_RELEASED:
                KeyEvent ke = (KeyEvent)event;
                if (isDescendingFrom(ke.getComponent(), this)) {
                    requestUpdate();
                }
                break;
        }
    }

    private void requestUpdate() {
        final Configurable configurable = myConfigurable;
        myQueue.queue(new Update(this) {
            @Override
            public void run() {
                updateIfCurrent(configurable);
            }

            @Override
            public boolean isExpired() {
                return myConfigurable != configurable;
            }
        });
    }

    private boolean isPopupOverEditor(Component component) {
        Window editor = UIUtil.getWindow(this);
        if (editor != null) {
            Window popup = UIUtil.getWindow(component);
            if (popup != null && editor == popup.getParent()) {
                if (popup instanceof JDialog) {
                    JDialog dialog = (JDialog)popup;
                    return Dialog.ModalityType.MODELESS == dialog.getModalityType();
                }
                return popup instanceof JWindow;
            }
        }
        return false;
    }

    void updateCurrent(Configurable configurable, boolean reset) {
        boolean modified = configurable != null && configurable.isModified();
        myApplyAction.setEnabled(modified);
        myResetAction.setEnabled(modified);
        if (!modified && reset) {
            setError(null);
        }
    }

    final boolean updateIfCurrent(Configurable configurable) {
        if (myConfigurable != configurable) {
            return false;
        }
        updateCurrent(configurable, false);
        return true;
    }

    final ActionCallback select(final Configurable configurable) {
        ActionCallback callback = myCardPanel.select(configurable, false);
        callback.doWhenDone(new Runnable() {
            @Override
            public void run() {
                myConfigurable = configurable;
                updateCurrent(configurable, false);
            }
        });
        return callback;
    }

    final boolean setError(ConfigurationException exception) {
        if (exception == null) {
            myErrorLabel.setVisible(false);
            return true;
        }
        if (myErrorLabel.isEnabled()) {
            myErrorLabel.setText("<html><body><strong>" + exception.getTitle() + "</strong>:<br>" + exception.getMessage());
            myErrorLabel.setVisible(true);
        }
        else {
            Messages.showMessageDialog(this, exception.getMessage(), exception.getTitle(), Messages.getErrorIcon());
        }
        return false;
    }

    final JComponent getContent(Configurable configurable) {
        return myCardPanel.getValue(configurable, false);
    }

    final JComponent readContent(Configurable configurable) {
        return myCardPanel.getValue(configurable, true);
    }

    private JComponent createDefaultContent(Configurable configurable) {
        JComponent content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String key = configurable == null ? null : ConfigurableVisitor.ByID.getID(configurable) + ".settings.description";
        String description = key == null ? null : getString(configurable, key);
        if (description == null) {
            description = "Select configuration element in the tree to edit its settings";
            content.add(BorderLayout.CENTER, new JLabel(description, SwingConstants.CENTER));
            content.setPreferredSize(JBUI.size(800, 600));
        }
        else {
            content.add(BorderLayout.NORTH, new JLabel(description));
            if (configurable instanceof Configurable.Composite) {
                Configurable.Composite composite = (Configurable.Composite)configurable;

                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                content.add(BorderLayout.CENTER, panel);
                panel.add(Box.createVerticalStrut(10));
                for (final Configurable current : composite.getConfigurables()) {
                    LinkLabel label = new LinkLabel(current.getDisplayName(), null) {
                        @Override
                        public void doClick() {
                            openLink(current);
                        }
                    };
                    label.setBorder(BorderFactory.createEmptyBorder(1, 17, 3, 1));
                    panel.add(label);
                }
            }
        }
        return content;
    }

    private static String getString(Configurable configurable, String key) {
        try {
            if (configurable instanceof ConfigurableWrapper) {
                ConfigurableWrapper wrapper = (ConfigurableWrapper)configurable;
                ConfigurableEP ep = wrapper.getExtensionPoint();
                ResourceBundle bundle = AbstractBundle.getResourceBundle(ep.bundle, ep.getPluginDescriptor().getPluginClassLoader());
                return CommonBundle.message(bundle, key);
            }
            return OptionsBundle.message(key);
        }
        catch (AssertionError error) {
            return null;
        }
    }

    static ConfigurationException apply(Configurable configurable) {
        if (configurable != null) {
            try {
                configurable.apply();
                UsageTrigger.trigger("ide.settings." + ConvertUsagesUtil.escapeDescriptorName(configurable.getDisplayName()));
            }
            catch (ConfigurationException exception) {
                return exception;
            }
        }
        return null;
    }
}