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
package com.gome.maven.profile.codeInspection.ui.filter;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInsight.daemon.impl.SeverityRegistrar;
import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.codeInspection.ex.ScopeToolState;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.actionSystem.ex.CheckboxAction;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.profile.codeInspection.SeverityProvider;
import com.gome.maven.profile.codeInspection.ui.LevelChooserAction;
import com.gome.maven.profile.codeInspection.ui.SingleInspectionProfilePanel;
import com.gome.maven.util.containers.HashSet;

import java.util.*;

/**
 * @author Dmitry Batkovich
 */
public class InspectionFilterAction extends DefaultActionGroup implements Toggleable, DumbAware {

    private final SeverityRegistrar mySeverityRegistrar;
    private final InspectionsFilter myInspectionsFilter;

    public InspectionFilterAction(final InspectionProfileImpl profile,
                                  final InspectionsFilter inspectionsFilter,
                                  final Project project) {
        super("Filter Inspections", true);
        myInspectionsFilter = inspectionsFilter;
        mySeverityRegistrar = ((SeverityProvider)profile.getProfileManager()).getOwnSeverityRegistrar();
        getTemplatePresentation().setIcon(AllIcons.General.Filter);
        tune(profile, project);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().putClientProperty(Toggleable.SELECTED_PROPERTY, !myInspectionsFilter.isEmptyFilter());
    }

    private void tune(InspectionProfileImpl profile, Project project) {
        addAction(new ResetFilterAction());
        addSeparator();

        addAction(new ShowEnabledOrDisabledInspectionsAction(true));
        addAction(new ShowEnabledOrDisabledInspectionsAction(false));
        addSeparator();

        final SortedSet<HighlightSeverity> severities = LevelChooserAction.getSeverities(mySeverityRegistrar);
        for (final HighlightSeverity severity : severities) {
            add(new ShowWithSpecifiedSeverityInspectionsAction(severity));
        }
        addSeparator();

        final Set<String> languageIds = new HashSet<String>();
        for (ScopeToolState state : profile.getDefaultStates(project)) {
            final String languageId = state.getTool().getLanguage();
            languageIds.add(languageId);
        }

        final List<Language> languages = new ArrayList<Language>();
        for (String id : languageIds) {
            if (id != null) {
                final Language language = Language.findLanguageByID(id);
                if (language != null) {
                    languages.add(language);
                }
            }
        }

        if (!languages.isEmpty()) {
            Collections.sort(languages, new Comparator<Language>() {
                @Override
                public int compare(Language l1, Language l2) {
                    return l1.getDisplayName().compareTo(l2.getDisplayName());
                }
            });
            for (Language language : languages) {
                add(new LanguageFilterAction(language));
            }
            addSeparator();
        }

        add(new ShowAvailableOnlyOnAnalyzeInspectionsAction());
        add(new ShowOnlyCleanupInspectionsAction());
    }

    private class ResetFilterAction extends DumbAwareAction {
        public ResetFilterAction() {
            super("Reset Filter");
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            myInspectionsFilter.reset();
        }

        @Override
        public void update( AnActionEvent e) {
            final Presentation presentation = e.getPresentation();
            presentation.setEnabled(!myInspectionsFilter.isEmptyFilter());
        }
    }

    private class ShowOnlyCleanupInspectionsAction extends CheckboxAction implements DumbAware{
        public ShowOnlyCleanupInspectionsAction() {
            super("Show Only Cleanup Inspections");
        }

        @Override
        public boolean isSelected(final AnActionEvent e) {
            return myInspectionsFilter.isShowOnlyCleanupInspections();
        }

        @Override
        public void setSelected(final AnActionEvent e, final boolean state) {
            myInspectionsFilter.setShowOnlyCleanupInspections(state);
        }
    }

    private class ShowAvailableOnlyOnAnalyzeInspectionsAction extends CheckboxAction implements DumbAware {

        public ShowAvailableOnlyOnAnalyzeInspectionsAction() {
            super("Show Only \"Available only for Analyze | Inspect Code\"");
        }

        @Override
        public boolean isSelected(final AnActionEvent e) {
            return myInspectionsFilter.isAvailableOnlyForAnalyze();
        }

        @Override
        public void setSelected(final AnActionEvent e, final boolean state) {
            myInspectionsFilter.setAvailableOnlyForAnalyze(state);
        }
    }

    private class ShowWithSpecifiedSeverityInspectionsAction extends CheckboxAction implements DumbAware {

        private final HighlightSeverity mySeverity;

        private ShowWithSpecifiedSeverityInspectionsAction(final HighlightSeverity severity) {
            super(SingleInspectionProfilePanel.renderSeverity(severity),
                    null,
                    HighlightDisplayLevel.find(severity).getIcon());
            mySeverity = severity;
        }


        @Override
        public boolean isSelected(final AnActionEvent e) {
            return myInspectionsFilter.containsSeverity(mySeverity);
        }

        @Override
        public void setSelected(final AnActionEvent e, final boolean state) {
            if (state) {
                myInspectionsFilter.addSeverity(mySeverity);
            } else {
                myInspectionsFilter.removeSeverity(mySeverity);
            }
        }
    }

    private class ShowEnabledOrDisabledInspectionsAction extends CheckboxAction implements DumbAware{

        private final Boolean myShowEnabledActions;

        public ShowEnabledOrDisabledInspectionsAction(final boolean showEnabledActions) {
            super("Show Only " + (showEnabledActions ? "Enabled" : "Disabled"));
            myShowEnabledActions = showEnabledActions;
        }


        @Override
        public boolean isSelected(final AnActionEvent e) {
            return myInspectionsFilter.getSuitableInspectionsStates() == myShowEnabledActions;
        }

        @Override
        public void setSelected(final AnActionEvent e, final boolean state) {
            final boolean previousState = isSelected(e);
            myInspectionsFilter.setSuitableInspectionsStates(previousState ? null : myShowEnabledActions);
        }
    }

    private class LanguageFilterAction extends CheckboxAction implements DumbAware {

        private final String myLanguageId;

        public LanguageFilterAction(final Language language) {
            super(language.getDisplayName());
            myLanguageId = language.getID();
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myInspectionsFilter.containsLanguageId(myLanguageId);
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
                myInspectionsFilter.addLanguageId(myLanguageId);
            } else {
                myInspectionsFilter.removeLanguageId(myLanguageId);
            }
        }
    }
}