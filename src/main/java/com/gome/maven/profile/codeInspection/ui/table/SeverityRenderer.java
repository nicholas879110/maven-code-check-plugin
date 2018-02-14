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
package com.gome.maven.profile.codeInspection.ui.table;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.ui.ComboBoxTableRenderer;
import com.gome.maven.openapi.ui.popup.LightweightWindowEvent;
import com.gome.maven.profile.codeInspection.SeverityProvider;
import com.gome.maven.profile.codeInspection.ui.LevelChooserAction;
import com.gome.maven.profile.codeInspection.ui.SingleInspectionProfilePanel;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.ColorIcon;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.SortedSet;

/**
 * @author Dmitry Batkovich
 */
public class SeverityRenderer extends ComboBoxTableRenderer<SeverityState> {
    private final Runnable myOnClose;
    private Icon myDisabledIcon;

    public SeverityRenderer(final SeverityState[] values,  final Runnable onClose) {
        super(values);
        myOnClose = onClose;
        myDisabledIcon = HighlightDisplayLevel.createIconByMask(UIUtil.getLabelDisabledForeground());
    }

    public static SeverityRenderer create(final InspectionProfileImpl inspectionProfile,  final Runnable onClose) {
        final SortedSet<HighlightSeverity> severities =
                LevelChooserAction.getSeverities(((SeverityProvider)inspectionProfile.getProfileManager()).getOwnSeverityRegistrar());
        return new SeverityRenderer(ContainerUtil.map2Array(severities, new SeverityState[severities.size()], new Function<HighlightSeverity, SeverityState>() {
            @Override
            public SeverityState fun(HighlightSeverity severity) {
                return new SeverityState(severity, true, false);
            }
        }), onClose);
    }

    public static Icon getIcon( HighlightDisplayLevel level) {
        Icon icon = level.getIcon();
        return icon instanceof HighlightDisplayLevel.ColoredIcon
                ? new ColorIcon(icon.getIconWidth(), ((HighlightDisplayLevel.ColoredIcon)icon).getColor())
                : icon;
    }

    @Override
    protected void customizeComponent(SeverityState value, JTable table, boolean isSelected) {
        super.customizeComponent(value, table, isSelected);
        setPaintArrow(value.isEnabledForEditing());
        setEnabled(!value.isDisabled());
        setDisabledIcon(myDisabledIcon);
    }

    @Override
    protected String getTextFor( final SeverityState value) {
        return SingleInspectionProfilePanel.renderSeverity(value.getSeverity());
    }

    @Override
    protected Icon getIconFor( final SeverityState value) {
        return getIcon(HighlightDisplayLevel.find(value.getSeverity()));
    }

    @Override
    public boolean isCellEditable(final EventObject event) {
        return !(event instanceof MouseEvent) || ((MouseEvent)event).getClickCount() >= 1;
    }

    @Override
    public void onClosed(LightweightWindowEvent event) {
        super.onClosed(event);
        if (myOnClose != null) {
            myOnClose.run();
        }
    }
}
