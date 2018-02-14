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
package com.gome.maven.profile.codeInspection.ui;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfoType;
import com.gome.maven.codeInsight.daemon.impl.SeverityRegistrar;
import com.gome.maven.codeInsight.daemon.impl.SeverityUtil;
import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.codeInspection.ex.SeverityEditorDialog;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.actionSystem.ex.ComboBoxAction;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.profile.codeInspection.SeverityProvider;
import com.gome.maven.profile.codeInspection.ui.table.SeverityRenderer;

import javax.swing.*;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Dmitry Batkovich
 */
public abstract class LevelChooserAction extends ComboBoxAction implements DumbAware {

    private final SeverityRegistrar mySeverityRegistrar;
    private HighlightSeverity myChosen = null;

    public LevelChooserAction(final InspectionProfileImpl profile) {
        this(((SeverityProvider)profile.getProfileManager()).getOwnSeverityRegistrar());
    }

    public LevelChooserAction(final SeverityRegistrar severityRegistrar) {
        mySeverityRegistrar = severityRegistrar;
    }

    
    @Override
    public DefaultActionGroup createPopupActionGroup(final JComponent anchor) {
        final DefaultActionGroup group = new DefaultActionGroup();
        for (final HighlightSeverity severity : getSeverities(mySeverityRegistrar)) {
            final HighlightSeverityAction action = new HighlightSeverityAction(severity);
            if (myChosen == null) {
                setChosen(action.getSeverity());
            }
            group.add(action);
        }
        group.addSeparator();
        group.add(new DumbAwareAction("Edit severities...") {
            @Override
            public void actionPerformed( final AnActionEvent e) {
                final SeverityEditorDialog dlg = new SeverityEditorDialog(anchor, myChosen, mySeverityRegistrar);
                if (dlg.showAndGet()) {
                    final HighlightInfoType type = dlg.getSelectedType();
                    if (type != null) {
                        final HighlightSeverity severity = type.getSeverity(null);
                        setChosen(severity);
                        onChosen(severity);
                    }
                }
            }
        });
        return group;
    }

    public static SortedSet<HighlightSeverity> getSeverities(final SeverityRegistrar severityRegistrar) {
        final SortedSet<HighlightSeverity> severities = new TreeSet<HighlightSeverity>(severityRegistrar);
        for (final SeverityRegistrar.SeverityBasedTextAttributes type : SeverityUtil.getRegisteredHighlightingInfoTypes(severityRegistrar)) {
            severities.add(type.getSeverity());
        }
        severities.add(HighlightSeverity.ERROR);
        severities.add(HighlightSeverity.WARNING);
        severities.add(HighlightSeverity.WEAK_WARNING);
        severities.add(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING);
        return severities;
    }

    protected abstract void onChosen(final HighlightSeverity severity);

    public void setChosen(final HighlightSeverity severity) {
        myChosen = severity;
        final Presentation templatePresentation = getTemplatePresentation();
        templatePresentation.setText(SingleInspectionProfilePanel.renderSeverity(severity));
        templatePresentation.setIcon(SeverityRenderer.getIcon(HighlightDisplayLevel.find(severity)));
    }

    private class HighlightSeverityAction extends DumbAwareAction {
        private final HighlightSeverity mySeverity;

        public HighlightSeverity getSeverity() {
            return mySeverity;
        }

        private HighlightSeverityAction(final HighlightSeverity severity) {
            mySeverity = severity;
            final Presentation presentation = getTemplatePresentation();
            presentation.setText(SingleInspectionProfilePanel.renderSeverity(severity));
            presentation.setIcon(SeverityRenderer.getIcon(HighlightDisplayLevel.find(severity)));
        }

        @Override
        public void actionPerformed( final AnActionEvent e) {
            final HighlightSeverity severity = getSeverity();
            setChosen(severity);
            onChosen(severity);
        }
    }
}
