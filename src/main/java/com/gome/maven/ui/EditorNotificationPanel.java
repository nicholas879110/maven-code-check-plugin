/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.ui;

import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.ActionPlaces;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.editor.colors.EditorColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.ui.components.panels.HorizontalLayout;
import com.gome.maven.ui.components.panels.NonOpaquePanel;
import com.gome.maven.util.ui.JBUI;
import com.gome.maven.util.ui.PlatformColors;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * @author Dmitry Avdeev
 */
public class EditorNotificationPanel extends JPanel {
    protected final JLabel myLabel = new JLabel();
    protected final JLabel myGearLabel = new JLabel();
    protected final JPanel myLinksPanel = new NonOpaquePanel(new HorizontalLayout(JBUI.scale(5)));

    public EditorNotificationPanel() {
        super(new BorderLayout());

        JPanel panel = new NonOpaquePanel(new BorderLayout());
        panel.add(BorderLayout.CENTER, myLabel);
        panel.add(BorderLayout.EAST, myLinksPanel);
        panel.setBorder(JBUI.Borders.empty(5, 0, 5, 5));
        panel.setMinimumSize(new Dimension(0, 0));

        add(BorderLayout.CENTER, panel);
        add(BorderLayout.EAST, myGearLabel);
        setBorder(JBUI.Borders.empty(0, 10));
    }

    public void setText(String text) {
        myLabel.setText(text);
    }

    public EditorNotificationPanel text( String text) {
        myLabel.setText(text);
        return this;
    }

    public EditorNotificationPanel icon( Icon icon) {
        myLabel.setIcon(icon);
        return this;
    }

    @Override
    public Color getBackground() {
        Color color = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.NOTIFICATION_BACKGROUND);
        return color == null ? UIUtil.getToolTipBackground() : color;
    }

    public HyperlinkLabel createActionLabel(final String text,  final String actionId) {
        return createActionLabel(text, new Runnable() {
            @Override
            public void run() {
                executeAction(actionId);
            }
        });
    }

    public HyperlinkLabel createActionLabel(final String text, final Runnable action) {
        HyperlinkLabel label = new HyperlinkLabel(text, PlatformColors.BLUE, getBackground(), PlatformColors.BLUE);
        label.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                action.run();
            }
        });
        myLinksPanel.add(label);
        return label;
    }

    protected void executeAction(final String actionId) {
        final AnAction action = ActionManager.getInstance().getAction(actionId);
        final AnActionEvent event = new AnActionEvent(null, DataManager.getInstance().getDataContext(this), ActionPlaces.UNKNOWN,
                action.getTemplatePresentation(), ActionManager.getInstance(),
                0);
        action.beforeActionPerformedUpdate(event);
        action.update(event);

        if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
            action.actionPerformed(event);
        }
    }
}
