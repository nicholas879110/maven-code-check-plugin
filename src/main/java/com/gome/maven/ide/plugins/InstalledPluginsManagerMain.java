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

import com.gome.maven.CommonBundle;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.actions.ShowSettingsUtilImpl;
import com.gome.maven.ide.startup.StartupActionScriptManager;
import com.gome.maven.ide.ui.search.ActionFromOptionDescriptorProvider;
import com.gome.maven.ide.ui.search.OptionDescription;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.actionSystem.ex.ComboBoxAction;
import com.gome.maven.openapi.extensions.PluginId;
import com.gome.maven.openapi.fileChooser.FileChooser;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.options.ex.SingleConfigurableEditor;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.ex.MessagesEx;
import com.gome.maven.openapi.updateSettings.impl.PluginDownloader;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.IdeBorderFactory;
import com.gome.maven.ui.ScrollPaneFactory;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.util.ArrayUtilRt;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Function;
import com.gome.maven.util.ui.StatusText;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.gome.maven.openapi.util.Pair.pair;

/**
 * @author anna
 */
public class InstalledPluginsManagerMain extends PluginManagerMain {
    private static final String PLUGINS_PRESELECTION_PATH = "plugins.preselection.path";

    private static final InstalledPluginsState ourState = InstalledPluginsState.getInstance();

    public InstalledPluginsManagerMain(PluginManagerUISettings uiSettings) {
        super(uiSettings);
        init();

        myActionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton installJB = new JButton("Install JetBrains plugin...");
        installJB.setMnemonic('j');
        installJB.addActionListener(new BrowseRepoListener(JETBRAINS_VENDOR));
        myActionsPanel.add(installJB);

        JButton browse = new JButton("Browse repositories...");
        browse.setMnemonic('b');
        browse.addActionListener(new BrowseRepoListener(null));
        myActionsPanel.add(browse);

        JButton installFromDisk = new JButton("Install plugin from disk...");
        installFromDisk.setMnemonic('d');
        installFromDisk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final InstalledPluginsTableModel model = (InstalledPluginsTableModel)pluginsModel;
                chooseAndInstall(model, new Consumer<Pair<File, IdeaPluginDescriptor>>() {
                    @Override
                    public void consume(Pair<File, IdeaPluginDescriptor> pair) {
                        model.appendOrUpdateDescriptor(pair.second);
                        setRequireShutdown(true);
                        select(pair.second);
                    }
                }, myActionsPanel);
            }
        });
        myActionsPanel.add(installFromDisk);

        StatusText emptyText = pluginTable.getEmptyText();
        emptyText.setText("Nothing to show.");
        emptyText.appendText(" Click ");
        emptyText.appendText("Browse", SimpleTextAttributes.LINK_ATTRIBUTES, new BrowseRepoListener(null));
        emptyText.appendText(" to search for non-bundled plugins.");
    }

    private static void chooseAndInstall( final InstalledPluginsTableModel model,
                                          final Consumer<Pair<File, IdeaPluginDescriptor>> callback,
                                          final Component parent) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, false, true, true, false, false) {
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                final String extension = file.getExtension();
                return Comparing.strEqual(extension, "jar") || Comparing.strEqual(extension, "zip");
            }
        };
        descriptor.setTitle("Choose Plugin File");
        descriptor.setDescription("JAR and ZIP archives are accepted");
        final String oldPath = PropertiesComponent.getInstance().getValue(PLUGINS_PRESELECTION_PATH);
        final VirtualFile toSelect = oldPath == null ? null : VfsUtil.findFileByIoFile(new File(FileUtil.toSystemDependentName(oldPath)), false);
        FileChooser.chooseFile(descriptor, null, parent, toSelect, new Consumer<VirtualFile>() {
            @Override
            public void consume( VirtualFile virtualFile) {
                File file = VfsUtilCore.virtualToIoFile(virtualFile);
                PropertiesComponent.getInstance().setValue(PLUGINS_PRESELECTION_PATH, FileUtil.toSystemIndependentName(file.getParent()));

                try {
                    IdeaPluginDescriptorImpl pluginDescriptor = PluginDownloader.loadDescriptionFromJar(file);
                    if (pluginDescriptor == null) {
                        MessagesEx.showErrorDialog(parent, "Fail to load plugin descriptor from file " + file.getName(), CommonBundle.getErrorTitle());
                        return;
                    }

                    if (ourState.wasInstalled(pluginDescriptor.getPluginId())) {
                        String message = "Plugin '" + pluginDescriptor.getName() + "' was already installed";
                        MessagesEx.showWarningDialog(parent, message, CommonBundle.getWarningTitle());
                        return;
                    }

                    if (PluginManagerCore.isIncompatible(pluginDescriptor)) {
                        String message = "Plugin '" + pluginDescriptor.getName() + "' is incompatible with this installation";
                        MessagesEx.showErrorDialog(parent, message, CommonBundle.getErrorTitle());
                        return;
                    }

                    IdeaPluginDescriptor installedPlugin = PluginManager.getPlugin(pluginDescriptor.getPluginId());
                    if (installedPlugin != null) {
                        File oldFile = installedPlugin.getPath();
                        if (oldFile != null) {
                            StartupActionScriptManager.addActionCommand(new StartupActionScriptManager.DeleteCommand(oldFile));
                        }
                    }

                    PluginInstaller.install(file, file.getName(), false);
                    ourState.onPluginInstall(pluginDescriptor);
                    checkInstalledPluginDependencies(model, pluginDescriptor, parent);
                    callback.consume(pair(file, (IdeaPluginDescriptor)pluginDescriptor));
                }
                catch (IOException ex) {
                    MessagesEx.showErrorDialog(parent, ex.getMessage(), CommonBundle.getErrorTitle());
                }
            }
        });
    }

    private static void checkInstalledPluginDependencies( InstalledPluginsTableModel model,
                                                          IdeaPluginDescriptorImpl pluginDescriptor,
                                                          Component parent) {
        final Set<PluginId> notInstalled = new HashSet<PluginId>();
        final Set<PluginId> disabledIds = new HashSet<PluginId>();
        final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
        final PluginId[] optionalDependentPluginIds = pluginDescriptor.getOptionalDependentPluginIds();
        for (PluginId id : dependentPluginIds) {
            if (ArrayUtilRt.find(optionalDependentPluginIds, id) > -1) continue;
            final boolean disabled = model.isDisabled(id);
            final boolean enabled = model.isEnabled(id);
            if (!enabled && !disabled && !PluginManagerCore.isModuleDependency(id)) {
                notInstalled.add(id);
            }
            else if (disabled) {
                disabledIds.add(id);
            }
        }
        if (!notInstalled.isEmpty()) {
            String deps = StringUtil.join(notInstalled, new Function<PluginId, String>() {
                @Override
                public String fun(PluginId id) {
                    return id.toString();
                }
            }, ", ");
            String message = "Plugin " + pluginDescriptor.getName() + " depends on unknown plugin" + (notInstalled.size() > 1 ? "s " : " ") + deps;
            MessagesEx.showWarningDialog(parent, message, CommonBundle.getWarningTitle());
        }
        if (!disabledIds.isEmpty()) {
            final Set<IdeaPluginDescriptor> dependencies = new HashSet<IdeaPluginDescriptor>();
            for (IdeaPluginDescriptor ideaPluginDescriptor : model.getAllPlugins()) {
                if (disabledIds.contains(ideaPluginDescriptor.getPluginId())) {
                    dependencies.add(ideaPluginDescriptor);
                }
            }
            String part = "disabled plugin" + (dependencies.size() > 1 ? "s " : " ");
            String deps = StringUtil.join(dependencies, new Function<IdeaPluginDescriptor, String>() {
                @Override
                public String fun(IdeaPluginDescriptor descriptor) {
                    return descriptor.getName();
                }
            }, ", ");
            String message = "Plugin " + pluginDescriptor.getName() + " depends on " + part + deps + ". Enable " + part.trim() + "?";
            if (MessagesEx.showOkCancelDialog(parent, message, CommonBundle.getWarningTitle(), Messages.getWarningIcon()) == Messages.OK) {
                model.enableRows(dependencies.toArray(new IdeaPluginDescriptor[dependencies.size()]), Boolean.TRUE);
            }
        }
    }

    @Override
    protected void propagateUpdates(List<IdeaPluginDescriptor> list) {
    }

    private PluginManagerConfigurable createAvailableConfigurable(final String vendorFilter) {
        return new PluginManagerConfigurable(PluginManagerUISettings.getInstance(), true) {
            @Override
            protected PluginManagerMain createPanel() {
                return new AvailablePluginsManagerMain(InstalledPluginsManagerMain.this, myUISettings, vendorFilter);
            }

            @Override
            public String getDisplayName() {
                return vendorFilter != null ? "Browse " + vendorFilter + " Plugins " : "Browse Repositories";
            }
        };
    }

    @Override
    protected JScrollPane createTable() {
        pluginsModel = new InstalledPluginsTableModel();
        if (PluginManagerUISettings.getInstance().installedSortByStatus) {
            pluginsModel.setSortByStatus(true);
        }

        pluginTable = new PluginTable(pluginsModel);
        pluginTable.setTableHeader(null);

        JScrollPane installedScrollPane = ScrollPaneFactory.createScrollPane(pluginTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pluginTable.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int column = InstalledPluginsTableModel.getCheckboxColumn();
                final int[] selectedRows = pluginTable.getSelectedRows();
                boolean currentlyMarked = true;
                for (final int selectedRow : selectedRows) {
                    if (selectedRow < 0 || !pluginTable.isCellEditable(selectedRow, column)) {
                        return;
                    }
                    final Boolean enabled = (Boolean)pluginTable.getValueAt(selectedRow, column);
                    currentlyMarked &= enabled == null || enabled.booleanValue();
                }
                final IdeaPluginDescriptor[] selected = new IdeaPluginDescriptor[selectedRows.length];
                for (int i = 0, selectedLength = selected.length; i < selectedLength; i++) {
                    selected[i] = pluginsModel.getObjectAt(pluginTable.convertRowIndexToModel(selectedRows[i]));
                }
                ((InstalledPluginsTableModel)pluginsModel).enableRows(selected, currentlyMarked ? Boolean.FALSE : Boolean.TRUE);
                pluginTable.repaint();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);
        pluginTable.setExpandableItemsEnabled(false);
        return installedScrollPane;
    }

    @Override
    protected PluginManagerMain getAvailable() {
        return this;
    }

    @Override
    protected PluginManagerMain getInstalled() {
        return this;
    }

    @Override
    protected ActionGroup getActionGroup(boolean inToolbar) {
        final DefaultActionGroup actionGroup = new DefaultActionGroup();
        if (inToolbar) {
            //actionGroup.add(new SortByStatusAction("Sort by Status"));
            actionGroup.add(new MyFilterEnabledAction());
            //actionGroup.add(new MyFilterBundleAction());
        }
        else {
            actionGroup.add(new RefreshAction());
            actionGroup.addAction(createSortersGroup());
            actionGroup.add(Separator.getInstance());
            actionGroup.add(new InstallPluginAction(getAvailable(), getInstalled()));
            actionGroup.add(new UninstallPluginAction(this, pluginTable));
        }
        return actionGroup;
    }

    @Override
    public boolean isModified() {
        final boolean modified = super.isModified();
        if (modified) return true;
        final List<String> disabledPlugins = PluginManagerCore.getDisabledPlugins();
        for (int i = 0; i < pluginsModel.getRowCount(); i++) {
            if (isPluginStateChanged(pluginsModel.getObjectAt(i), disabledPlugins)) {
                return true;
            }
        }
        for (IdeaPluginDescriptor descriptor : pluginsModel.filtered) {
            if (isPluginStateChanged(descriptor, disabledPlugins)) {
                return true;
            }
        }
        for (Map.Entry<PluginId, Boolean> entry : ((InstalledPluginsTableModel)pluginsModel).getEnabledMap().entrySet()) {
            final Boolean enabled = entry.getValue();
            if (enabled != null && !enabled.booleanValue() && !disabledPlugins.contains(entry.getKey().toString())) {
                return true;
            }
        }

        return false;
    }

    private boolean isPluginStateChanged(final IdeaPluginDescriptor pluginDescriptor,
                                         final List<String> disabledPlugins) {
        final PluginId pluginId = pluginDescriptor.getPluginId();
        final boolean enabledInTable = ((InstalledPluginsTableModel)pluginsModel).isEnabled(pluginId);
        if (pluginDescriptor.isEnabled() != enabledInTable) {
            if (enabledInTable && !disabledPlugins.contains(pluginId.getIdString())) {
                return false; //was disabled automatically on startup
            }
            return true;
        }
        return false;
    }

    @Override
    public String apply() {
        final String apply = super.apply();
        if (apply != null) return apply;
        for (int i = 0; i < pluginTable.getRowCount(); i++) {
            final IdeaPluginDescriptor pluginDescriptor = pluginsModel.getObjectAt(i);
            final Boolean enabled = (Boolean)pluginsModel.getValueAt(i, InstalledPluginsTableModel.getCheckboxColumn());
            pluginDescriptor.setEnabled(enabled != null && enabled.booleanValue());
        }
        for (IdeaPluginDescriptor descriptor : pluginsModel.filtered) {
            descriptor.setEnabled(((InstalledPluginsTableModel)pluginsModel).isEnabled(descriptor.getPluginId()));
        }
        try {
            final ArrayList<String> ids = new ArrayList<String>();
            for (Map.Entry<PluginId, Boolean> entry : ((InstalledPluginsTableModel)pluginsModel).getEnabledMap().entrySet()) {
                final Boolean value = entry.getValue();
                if (value != null && !value.booleanValue()) {
                    ids.add(entry.getKey().getIdString());
                }
            }
            PluginManagerCore.saveDisabledPlugins(ids, false);
        }
        catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }

    @Override
    protected String canApply() {
        final Map<PluginId, Set<PluginId>> dependentToRequiredListMap =
                new HashMap<PluginId, Set<PluginId>>(((InstalledPluginsTableModel)pluginsModel).getDependentToRequiredListMap());
        for (Iterator<PluginId> iterator = dependentToRequiredListMap.keySet().iterator(); iterator.hasNext(); ) {
            final PluginId id = iterator.next();
            boolean hasNonModuleDeps = false;
            for (PluginId pluginId : dependentToRequiredListMap.get(id)) {
                if (!PluginManagerCore.isModuleDependency(pluginId)) {
                    hasNonModuleDeps = true;
                    break;
                }
            }
            if (!hasNonModuleDeps) {
                iterator.remove();
            }
        }
        if (!dependentToRequiredListMap.isEmpty()) {
            return "<html><body style=\"padding: 5px;\">Unable to apply changes: plugin" +
                    (dependentToRequiredListMap.size() == 1 ? " " : "s ") +
                    StringUtil.join(dependentToRequiredListMap.keySet(), new Function<PluginId, String>() {
                        @Override
                        public String fun(final PluginId pluginId) {
                            final IdeaPluginDescriptor ideaPluginDescriptor = PluginManager.getPlugin(pluginId);
                            return "\"" + (ideaPluginDescriptor != null ? ideaPluginDescriptor.getName() : pluginId.getIdString()) + "\"";
                        }
                    }, ", ") +
                    " won't be able to load.</body></html>";
        }
        return super.canApply();
    }

    private class MyFilterEnabledAction extends ComboBoxAction implements DumbAware {

        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            e.getPresentation().setText(((InstalledPluginsTableModel)pluginsModel).getEnabledFilter());
        }

        
        @Override
        protected DefaultActionGroup createPopupActionGroup(JComponent button) {
            final DefaultActionGroup gr = new DefaultActionGroup();
            for (final String enabledValue : InstalledPluginsTableModel.ENABLED_VALUES) {
                gr.add(new DumbAwareAction(enabledValue) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        final IdeaPluginDescriptor[] selection = pluginTable.getSelectedObjects();
                        final String filter = myFilter.getFilter().toLowerCase();
                        ((InstalledPluginsTableModel)pluginsModel).setEnabledFilter(enabledValue, filter);
                        if (selection != null) {
                            select(selection);
                        }
                    }
                });
            }
            return gr;
        }

        @Override
        public JComponent createCustomComponent(Presentation presentation) {
            final JComponent component = super.createCustomComponent(presentation);
            final JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);
            panel.add(component, BorderLayout.CENTER);
            final JLabel comp = new JLabel("Show:");
            comp.setIconTextGap(0);
            comp.setHorizontalTextPosition(SwingConstants.RIGHT);
            comp.setVerticalTextPosition(SwingConstants.CENTER);
            comp.setAlignmentX(Component.RIGHT_ALIGNMENT);
            panel.add(comp, BorderLayout.WEST);
            panel.setBorder(IdeBorderFactory.createEmptyBorder(0, 2, 0, 0));
            return panel;
        }
    }

    private class BrowseRepoListener implements ActionListener {

        private final String myVendor;

        public BrowseRepoListener(String vendor) {
            myVendor = vendor;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final PluginManagerConfigurable configurable = createAvailableConfigurable(myVendor);
            final SingleConfigurableEditor configurableEditor =
                    new SingleConfigurableEditor(myActionsPanel, configurable, ShowSettingsUtilImpl.createDimensionKey(configurable), false) {
                        {
                            setOKButtonText(CommonBundle.message("close.action.name"));
                            setOKButtonMnemonic('C');
                            final String filter = myFilter.getFilter();
                            if (!StringUtil.isEmptyOrSpaces(filter)) {
                                final Runnable searchRunnable = configurable.enableSearch(filter);
                                LOG.assertTrue(searchRunnable != null);
                                searchRunnable.run();
                            }
                        }

                        
                        @Override
                        protected Action[] createActions() {
                            return new Action[]{getOKAction()};
                        }
                    };
            configurableEditor.show();
        }
    }

    public static class InstallFromDiskAction extends DumbAwareAction {
        public InstallFromDiskAction( String text) {
            super(text, "", AllIcons.Nodes.Plugin);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            chooseAndInstall(new InstalledPluginsTableModel(), new Consumer<Pair<File, IdeaPluginDescriptor>>() {
                @Override
                public void consume(Pair<File, IdeaPluginDescriptor> pair) {
                    PluginManagerConfigurable.shutdownOrRestartApp();
                }
            }, null);
        }
    }

    public static class PluginsActionFromOptionDescriptorProvider extends ActionFromOptionDescriptorProvider {
        
        @Override
        public AnAction provide( OptionDescription description) {
            String name = "Install plugin from disk...";
            if (name.equals(description.getHit()) && "preferences.pluginManager".equals(description.getConfigurableId())) {
                return new InstalledPluginsManagerMain.InstallFromDiskAction(name);
            }
            return null;
        }
    }
}
