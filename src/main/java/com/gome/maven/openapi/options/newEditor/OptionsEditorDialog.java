/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.ui.search.SearchUtil;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.help.HelpManager;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurableGroup;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.options.SearchableConfigurable;
import com.gome.maven.openapi.options.ex.Settings;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.Disposer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OptionsEditorDialog extends DialogWrapper implements DataProvider{

    private Project myProject;
    private ConfigurableGroup[] myGroups;
    private Configurable myPreselected;
    private OptionsEditor myEditor;

    private ApplyAction myApplyAction;
    public static final String DIMENSION_KEY = "OptionsEditor";
     static final String LAST_SELECTED_CONFIGURABLE = "options.lastSelected";

    /** This constructor should be eliminated after the new modality approach
     *  will have been checked. See a {@code Registry} key ide.perProjectModality
     *  @deprecated
     */
    public OptionsEditorDialog(Project project, ConfigurableGroup[] groups,
                                Configurable preselectedConfigurable, boolean applicationModalIfPossible) {
        super(project, true, applicationModalIfPossible);
        init(project, groups, preselectedConfigurable != null ? preselectedConfigurable : findLastSavedConfigurable(groups, project));
    }

    /** This constructor should be eliminated after the new modality approach
     *  will have been checked. See a {@code Registry} key ide.perProjectModality
     *  @deprecated
     */
    public OptionsEditorDialog(Project project, ConfigurableGroup[] groups,
                                String preselectedConfigurableDisplayName, boolean applicationModalIfPossible) {
        super(project, true, applicationModalIfPossible);
        init(project, groups, getPreselectedByDisplayName(groups, preselectedConfigurableDisplayName, project));
    }

    public OptionsEditorDialog(Project project, ConfigurableGroup[] groups,  Configurable preselectedConfigurable) {
        super(project, true);
        init(project, groups, preselectedConfigurable != null ? preselectedConfigurable : findLastSavedConfigurable(groups, project));
    }

    public OptionsEditorDialog(Project project, ConfigurableGroup[] groups,  String preselectedConfigurableDisplayName) {
        super(project, true);
        init(project, groups, getPreselectedByDisplayName(groups, preselectedConfigurableDisplayName, project));
    }

    private void init(final Project project, final ConfigurableGroup[] groups,  final Configurable preselected) {
        myProject = project;
        myGroups = groups;
        myPreselected = preselected;

        setTitle(CommonBundle.settingsTitle());

        init();
    }

    
    private static Configurable getPreselectedByDisplayName(final ConfigurableGroup[] groups, final String preselectedConfigurableDisplayName,
                                                            final Project project) {
        Configurable result = findPreselectedByDisplayName(preselectedConfigurableDisplayName, groups);

        return result == null ? findLastSavedConfigurable(groups, project) : result;
    }

    @Override
    public boolean isTypeAheadEnabled() {
        return true;
    }

    protected JComponent createCenterPanel() {
        myEditor = new OptionsEditor(myProject, myGroups, myPreselected);
        myEditor.getContext().addColleague(new OptionsEditorColleague.Adapter() {
            @Override
            public ActionCallback onModifiedAdded(final Configurable configurable) {
                updateStatus();
                return new ActionCallback.Done();
            }

            @Override
            public ActionCallback onModifiedRemoved(final Configurable configurable) {
                updateStatus();
                return new ActionCallback.Done();
            }

            @Override
            public ActionCallback onErrorsChanged() {
                updateStatus();
                return new ActionCallback.Done();
            }
        });
        Disposer.register(myDisposable, myEditor);
        return myEditor;
    }

    public boolean updateStatus() {
        myApplyAction.setEnabled(myEditor.canApply());

        final Map<Configurable,ConfigurationException> errors = myEditor.getContext().getErrors();
        if (errors.size() == 0) {
            setErrorText(null);
        } else {
            String text = "Changes were not applied because of an error";

            final String errorMessage = getErrorMessage(errors);
            if (errorMessage != null) {
                text += "<br>" + errorMessage;
            }

            setErrorText(text);
        }

        return errors.size() == 0;
    }

    
    private static String getErrorMessage(final Map<Configurable, ConfigurationException> errors) {
        final Collection<ConfigurationException> values = errors.values();
        final ConfigurationException[] exceptions = values.toArray(new ConfigurationException[values.size()]);
        if (exceptions.length > 0) {
            return exceptions[0].getMessage();
        }
        return null;
    }

    @Override
    protected String getDimensionServiceKey() {
        return DIMENSION_KEY;
    }

    @Override
    protected void doOKAction() {
        myEditor.flushModifications();

        if (myEditor.canApply()) {
            myEditor.apply();
            if (!updateStatus()) return;
        }

        saveCurrentConfigurable();

        ApplicationManager.getApplication().saveAll();

        super.doOKAction();
    }


    private void saveCurrentConfigurable() {
        final Configurable current = myEditor.getContext().getCurrentConfigurable();
        if (current == null) return;

        final PropertiesComponent props = PropertiesComponent.getInstance(myProject);

        if (current instanceof SearchableConfigurable) {
            props.setValue(LAST_SELECTED_CONFIGURABLE, ((SearchableConfigurable)current).getId());
        } else {
            props.setValue(LAST_SELECTED_CONFIGURABLE, current.getClass().getName());
        }
    }

    
    private static Configurable findLastSavedConfigurable(ConfigurableGroup[] groups, final Project project) {
        final String id = PropertiesComponent.getInstance(project).getValue(LAST_SELECTED_CONFIGURABLE);
        if (id == null) return null;

        return findConfigurableInGroups(id, groups);
    }

    
    private static Configurable findConfigurableInGroups(String id, Configurable.Composite... groups) {
        // avoid unnecessary group expand: check top-level configurables in all groups before looking at children
        for (Configurable.Composite group : groups) {
            final Configurable[] configurables = group.getConfigurables();
            for (Configurable c : configurables) {
                if (c instanceof SearchableConfigurable && id.equals(((SearchableConfigurable)c).getId())) {
                    return c;
                } else if (id.equals(c.getClass().getName())) {
                    return c;
                }
            }
        }
        for (Configurable.Composite group : groups) {
            final Configurable[] configurables = group.getConfigurables();
            for (Configurable c : configurables) {
                if (c instanceof Configurable.Composite) {
                    Configurable result = findConfigurableInGroups(id, (Configurable.Composite)c);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    
    private static Configurable findPreselectedByDisplayName(final String preselectedConfigurableDisplayName, ConfigurableGroup[] groups) {
        final List<Configurable> all = SearchUtil.expand(groups);
        for (Configurable each : all) {
            if (preselectedConfigurableDisplayName.equals(each.getDisplayName())) return each;
        }
        return null;
    }

    @Override
    public void doCancelAction(final AWTEvent source) {
        if (source instanceof KeyEvent || source instanceof ActionEvent) {
            if (myEditor.getContext().isHoldingFilter()) {
                myEditor.clearFilter();
                return;
            }
        }

        super.doCancelAction(source);
    }

    @Override
    public void doCancelAction() {
        saveCurrentConfigurable();
        super.doCancelAction();
    }

    
    @Override
    protected Action[] createActions() {
        myApplyAction = new ApplyAction();
        return new Action[] {getOKAction(), getCancelAction(), myApplyAction, getHelpAction()};
    }

    @Override
    protected void doHelpAction() {
        final String topic = myEditor.getHelpTopic();
        if (topic != null) {
            HelpManager.getInstance().invokeHelp(topic);
        }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myEditor.getPreferredFocusedComponent();
    }

    public Object getData( String dataId) {
        if (Settings.KEY.is(dataId)) {
            return myEditor.mySettings;
        }
        if (OptionsEditor.KEY.is(dataId)) {
            return myEditor;
        }
        return null;
    }

    private class ApplyAction extends AbstractAction {
        public ApplyAction() {
            super(CommonBundle.getApplyButtonText());
            setEnabled(false);
        }

        public void actionPerformed(final ActionEvent e) {
            myEditor.apply();
            myEditor.revalidate();
            myEditor.repaint();
        }
    }

}
