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
package com.gome.maven.ide.actions;

import com.gome.maven.ide.ui.search.SearchUtil;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurableGroup;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.options.TabbedConfigurable;
import com.gome.maven.openapi.options.ex.*;
import com.gome.maven.openapi.options.newEditor.IdeSettingsDialog;
import com.gome.maven.openapi.options.newEditor.OptionsEditor;
import com.gome.maven.openapi.options.newEditor.OptionsEditorDialog;
import com.gome.maven.openapi.options.newEditor.SettingsDialog;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.navigation.Place;
import com.gome.maven.util.ui.update.Activatable;
import com.gome.maven.util.ui.update.UiNotifyConnector;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author max
 */
public class ShowSettingsUtilImpl extends ShowSettingsUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.actions.ShowSettingsUtilImpl");
    private final AtomicBoolean myShown = new AtomicBoolean(false);

    
    private static Project getProject( Project project) {
        return project != null ? project : ProjectManager.getInstance().getDefaultProject();
    }

    
    public static DialogWrapper getDialog( Project project,  ConfigurableGroup[] groups,  Configurable toSelect) {
        project = getProject(project);
        final ConfigurableGroup[] filteredGroups = filterEmptyGroups(groups);
        if (Registry.is("ide.new.settings.dialog")) {
            return Registry.is("ide.new.settings.view")
                    ? new SettingsDialog(project, filteredGroups, toSelect, null)
                    : new IdeSettingsDialog(project, filteredGroups, toSelect);
        }
        //noinspection deprecation
        return Registry.is("ide.perProjectModality")
                ? new OptionsEditorDialog(project, filteredGroups, toSelect, true)
                : new OptionsEditorDialog(project, filteredGroups, toSelect);
    }

    
    public static ConfigurableGroup[] getConfigurableGroups( Project project, boolean withIdeSettings) {
        ConfigurableGroup[] groups = !withIdeSettings
                ? new ConfigurableGroup[]{new ProjectConfigurablesGroup(getProject(project))}
                : (project == null)
                ? new ConfigurableGroup[]{new IdeConfigurablesGroup()}
                : new ConfigurableGroup[]{
                new ProjectConfigurablesGroup(project),
                new IdeConfigurablesGroup()};

        return Registry.is("ide.new.settings.dialog")
                ? new ConfigurableGroup[]{new SortedConfigurableGroup(project, getConfigurables(groups, true))}
                : groups;
    }

    
    public static Configurable[] getConfigurables( Project project, boolean withGroupReverseOrder) {
        return getConfigurables(getConfigurableGroups(project, true), withGroupReverseOrder);
    }

    
    private static Configurable[] getConfigurables( ConfigurableGroup[] groups, boolean withGroupReverseOrder) {
        Configurable[][] arrays = new Configurable[groups.length][];
        int length = 0;
        for (int i = 0; i < groups.length; i++) {
            arrays[i] = groups[withGroupReverseOrder ? groups.length - 1 - i : i].getConfigurables();
            length += arrays[i].length;
        }
        Configurable[] configurables = new Configurable[length];
        int offset = 0;
        for (Configurable[] array : arrays) {
            System.arraycopy(array, 0, configurables, offset, array.length);
            offset += array.length;
        }
        return configurables;
    }

    @Override
    public void showSettingsDialog( Project project,  ConfigurableGroup[] group) {
        try {
            myShown.set(true);
            getDialog(project, group, null).show();
        }
        catch (Exception e) {
            LOG.error(e);
        }
        finally {
            myShown.set(false);
        }
    }

    @Override
    public void showSettingsDialog( final Project project, final Class configurableClass) {
        assert Configurable.class.isAssignableFrom(configurableClass) : "Not a configurable: " + configurableClass.getName();

        ConfigurableGroup[] groups = getConfigurableGroups(project, true);

        Configurable config = new ConfigurableVisitor.ByType(configurableClass).find(groups);

        assert config != null : "Cannot find configurable: " + configurableClass.getName();

        getDialog(project, groups, config).show();
    }

    @Override
    public void showSettingsDialog( final Project project,  final String nameToSelect) {
        ConfigurableGroup[] groups = getConfigurableGroups(project, true);
        Project actualProject = getProject(project);

        groups = filterEmptyGroups(groups);
        getDialog(actualProject, groups, findPreselectedByDisplayName(nameToSelect, groups)).show();
    }

    
    private static Configurable findPreselectedByDisplayName(final String preselectedConfigurableDisplayName, ConfigurableGroup[] groups) {
        final List<Configurable> all = SearchUtil.expand(groups);
        for (Configurable each : all) {
            if (preselectedConfigurableDisplayName.equals(each.getDisplayName())) return each;
        }
        return null;
    }

    public static void showSettingsDialog( Project project, final String id2Select, final String filter) {
        ConfigurableGroup[] group = getConfigurableGroups(project, true);

        group = filterEmptyGroups(group);
        final Configurable configurable2Select = id2Select == null ? null : new ConfigurableVisitor.ByID(id2Select).find(group);

        if (Registry.is("ide.new.settings.view")) {
            new SettingsDialog(getProject(project), group, configurable2Select, filter).show();
            return;
        }
        final DialogWrapper dialog = getDialog(project, group, configurable2Select);

        new UiNotifyConnector.Once(dialog.getContentPane(), new Activatable.Adapter() {
            @Override
            public void showNotify() {
                final OptionsEditor editor = (OptionsEditor)((DataProvider)dialog).getData(OptionsEditor.KEY.getName());
                LOG.assertTrue(editor != null);
                editor.select(configurable2Select, filter);
            }
        });
        dialog.show();
    }

    @Override
    public void showSettingsDialog( final Project project, final Configurable toSelect) {
        getDialog(project, getConfigurableGroups(project, true), toSelect).show();
    }

    
    private static ConfigurableGroup[] filterEmptyGroups( final ConfigurableGroup[] group) {
        List<ConfigurableGroup> groups = new ArrayList<ConfigurableGroup>();
        for (ConfigurableGroup g : group) {
            if (g.getConfigurables().length > 0) {
                groups.add(g);
            }
        }
        return groups.toArray(new ConfigurableGroup[groups.size()]);
    }

    @Override
    public boolean editConfigurable(Project project, Configurable configurable) {
        return editConfigurable(project, createDimensionKey(configurable), configurable);
    }

    @Override
    public <T extends Configurable> T findApplicationConfigurable(final Class<T> confClass) {
        return ConfigurableExtensionPointUtil.findApplicationConfigurable(confClass);
    }

    @Override
    public <T extends Configurable> T findProjectConfigurable(final Project project, final Class<T> confClass) {
        //noinspection deprecation
        return ConfigurableExtensionPointUtil.findProjectConfigurable(project, confClass);
    }

    @Override
    public boolean editConfigurable(Project project, String dimensionServiceKey,  Configurable configurable) {
        return editConfigurable(project, dimensionServiceKey, configurable, isWorthToShowApplyButton(configurable));
    }

    private static boolean isWorthToShowApplyButton( Configurable configurable) {
        return configurable instanceof Place.Navigator ||
                configurable instanceof Composite ||
                configurable instanceof TabbedConfigurable;
    }

    @Override
    public boolean editConfigurable(Project project, String dimensionServiceKey,  Configurable configurable, boolean showApplyButton) {
        return editConfigurable(null, project, configurable, dimensionServiceKey, null, showApplyButton);
    }

    @Override
    public boolean editConfigurable(Project project, Configurable configurable, Runnable advancedInitialization) {
        return editConfigurable(null, project, configurable, createDimensionKey(configurable), advancedInitialization, isWorthToShowApplyButton(configurable));
    }

    @Override
    public boolean editConfigurable( Component parent,  Configurable configurable) {
        return editConfigurable(parent, configurable, null);
    }

    @Override
    public boolean editConfigurable( Component parent,  Configurable configurable,  Runnable advancedInitialization) {
        return editConfigurable(parent, null, configurable, createDimensionKey(configurable), advancedInitialization, isWorthToShowApplyButton(configurable));
    }

    private static boolean editConfigurable( Component parent,
                                             Project project,
                                             Configurable configurable,
                                            String dimensionKey,
                                             final Runnable advancedInitialization,
                                            boolean showApplyButton) {
        final DialogWrapper editor;
        if (parent == null) {
            editor = Registry.is("ide.new.settings.view")
                    ? new SettingsDialog(project, dimensionKey, configurable, showApplyButton, false)
                    : new SingleConfigurableEditor(project, configurable, dimensionKey, showApplyButton);
        }
        else {
            editor = Registry.is("ide.new.settings.view")
                    ? new SettingsDialog(parent, dimensionKey, configurable, showApplyButton, false)
                    : new SingleConfigurableEditor(parent, configurable, dimensionKey, showApplyButton);
        }
        if (advancedInitialization != null) {
            new UiNotifyConnector.Once(editor.getContentPane(), new Activatable.Adapter() {
                @Override
                public void showNotify() {
                    advancedInitialization.run();
                }
            });
        }
        return editor.showAndGet();
    }

    
    public static String createDimensionKey( Configurable configurable) {
        return '#' + StringUtil.replaceChar(StringUtil.replaceChar(configurable.getDisplayName(), '\n', '_'), ' ', '_');
    }

    @Override
    public boolean editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable) {
        return editConfigurable(parent, null, configurable, dimensionServiceKey, null, isWorthToShowApplyButton(configurable));
    }

    public boolean isAlreadyShown() {
        return myShown.get();
    }
}
