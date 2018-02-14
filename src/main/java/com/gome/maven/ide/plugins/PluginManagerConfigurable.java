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
package com.gome.maven.ide.plugins;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.options.BaseConfigurable;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.options.SearchableConfigurable;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.SplitterProportionsData;
import com.gome.maven.openapi.util.Disposer;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.List;

public class PluginManagerConfigurable extends BaseConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    public static final String ID = "preferences.pluginManager";
    public static final String DISPLAY_NAME = IdeBundle.message("title.plugins");

    protected final PluginManagerUISettings myUISettings;

    private PluginManagerMain myPluginManagerMain;
    private boolean myAvailable;

    public PluginManagerConfigurable(final PluginManagerUISettings UISettings) {
        myUISettings = UISettings;
    }

    public PluginManagerConfigurable(final PluginManagerUISettings UISettings, boolean available) {
        myUISettings = UISettings;
        myAvailable = available;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myPluginManagerMain == null ? null : myPluginManagerMain.getPluginTable();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public void reset() {
        myPluginManagerMain.reset();
        myPluginManagerMain.pluginsModel.sort();
        getSplitterProportions().restoreSplitterProportions(myPluginManagerMain.getMainPanel());
    }

    @Override
    
    public String getHelpTopic() {
        return ID;
    }

    @Override
    public void disposeUIResources() {
        if (myPluginManagerMain != null) {
            getSplitterProportions().saveSplitterProportions(myPluginManagerMain.getMainPanel());

            if (myAvailable) {
                final RowSorter<? extends TableModel> rowSorter = myPluginManagerMain.pluginTable.getRowSorter();
                if (rowSorter != null) {
                    final List<? extends RowSorter.SortKey> sortKeys = rowSorter.getSortKeys();
                    if (sortKeys.size() > 0) {
                        final RowSorter.SortKey sortKey = sortKeys.get(0);
                        myUISettings.AVAILABLE_SORT_COLUMN_ORDER = sortKey.getSortOrder().ordinal();
                    }
                }
                myUISettings.availableSortByStatus = myPluginManagerMain.pluginsModel.isSortByStatus();
            }
            else {
                myUISettings.installedSortByStatus = myPluginManagerMain.pluginsModel.isSortByStatus();
            }

            Disposer.dispose(myPluginManagerMain);
            myPluginManagerMain = null;
        }
    }

    private SplitterProportionsData getSplitterProportions() {
        return myAvailable ? myUISettings.availableProportions : myUISettings.installedProportions;
    }

    @Override
    public JComponent createComponent() {
        return getOrCreatePanel().getMainPanel();
    }

    protected PluginManagerMain createPanel() {
        return new InstalledPluginsManagerMain(myUISettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        final String applyMessage = myPluginManagerMain.apply();
        if (applyMessage != null) {
            throw new ConfigurationException(applyMessage);
        }

        if (myPluginManagerMain.isRequireShutdown()) {
            if (showRestartDialog() == Messages.YES) {
                ApplicationManagerEx.getApplicationEx().restart(true);
            }
            else {
                myPluginManagerMain.ignoreChanges();
            }
        }
    }

    public PluginManagerMain getOrCreatePanel() {
        if (myPluginManagerMain == null) {
            myPluginManagerMain = createPanel();
        }
        return myPluginManagerMain;
    }

    @Messages.YesNoResult
    public static int showRestartDialog() {
        return showRestartDialog(IdeBundle.message("update.notifications.title"));
    }

    @Messages.YesNoResult
    public static int showRestartDialog( String title) {
        String action = IdeBundle.message(ApplicationManagerEx.getApplicationEx().isRestartCapable() ? "ide.restart.action" : "ide.shutdown.action");
        String message = IdeBundle.message("ide.restart.required.message", action, ApplicationNamesInfo.getInstance().getFullProductName());
        return Messages.showYesNoDialog(message, title, action, IdeBundle.message("ide.postpone.action"), Messages.getQuestionIcon());
    }

    public static void shutdownOrRestartApp() {
        shutdownOrRestartApp(IdeBundle.message("update.notifications.title"));
    }

    public static void shutdownOrRestartApp( String title) {
        if (showRestartDialog(title) == Messages.YES) {
            ApplicationManagerEx.getApplicationEx().restart(true);
        }
    }

    @Override
    public boolean isModified() {
        return myPluginManagerMain != null && myPluginManagerMain.isModified();
    }

    @Override
    
    public String getId() {
        return getHelpTopic();
    }

    @Override
    
    public Runnable enableSearch(final String option) {
        return new Runnable() {
            @Override
            public void run() {
                if (myPluginManagerMain != null) {
                    myPluginManagerMain.filter(option);
                }
            }
        };
    }

    public void select(IdeaPluginDescriptor... descriptors) {
        myPluginManagerMain.select(descriptors);
    }
}
