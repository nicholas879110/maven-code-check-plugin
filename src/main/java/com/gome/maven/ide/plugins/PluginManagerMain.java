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
package com.gome.maven.ide.plugins;

import com.gome.maven.CommonBundle;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.BrowserUtil;
import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.plugins.sorters.SortByStatusAction;
import com.gome.maven.ide.ui.search.SearchUtil;
import com.gome.maven.ide.ui.search.SearchableOptionsRegistrar;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.application.ex.ApplicationInfoEx;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.PluginId;
import com.gome.maven.openapi.progress.EmptyProgressIndicator;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.updateSettings.impl.UpdateChecker;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.*;
import com.gome.maven.ui.border.CustomLineBorder;
import com.gome.maven.ui.components.JBLabel;
import com.gome.maven.ui.components.JBScrollPane;
import com.gome.maven.ui.speedSearch.SpeedSearchSupply;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.JBUI;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.update.UiNotifyConnector;
import com.gome.maven.xml.util.XmlStringUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.plaf.BorderUIResource;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.gome.maven.openapi.util.text.StringUtil.isEmptyOrSpaces;

/**
 * @author stathik
 * @author Konstantin Bulenkov
 */
public abstract class PluginManagerMain implements Disposable {
    public static final String JETBRAINS_VENDOR = "JetBrains";

    public static Logger LOG = Logger.getInstance("#com.gome.maven.ide.plugins.PluginManagerMain");

     private static final String TEXT_SUFFIX = "</body></html>";

     private static final String HTML_PREFIX = "<a href=\"";
     private static final String HTML_SUFFIX = "</a>";

    private boolean requireShutdown = false;

    private JPanel myToolbarPanel;
    private JPanel main;

    private JEditorPane myDescriptionTextArea;

    private JPanel myTablePanel;
    protected JPanel myActionsPanel;
    private JPanel myHeader;
    private PluginHeaderPanel myPluginHeaderPanel;
    private JPanel myInfoPanel;
    protected JBLabel myPanelDescription;
    private JBScrollPane myDescriptionScrollPane;


    protected PluginTableModel pluginsModel;
    protected PluginTable pluginTable;

    private ActionToolbar myActionToolbar;

    protected final MyPluginsFilter myFilter = new MyPluginsFilter();
    protected PluginManagerUISettings myUISettings;
    private boolean myDisposed = false;
    private boolean myBusy = false;

    public PluginManagerMain(
            PluginManagerUISettings uiSettings) {
        myUISettings = uiSettings;
    }

    public static boolean isJetBrainsPlugin( IdeaPluginDescriptor plugin) {
        return JETBRAINS_VENDOR.equals(plugin.getVendor());
    }

    protected void init() {
        GuiUtils.replaceJSplitPaneWithIDEASplitter(main);
        myDescriptionTextArea.setEditorKit(new HTMLEditorKit());
        myDescriptionTextArea.setEditable(false);
        myDescriptionTextArea.addHyperlinkListener(new MyHyperlinkListener());

        JScrollPane installedScrollPane = createTable();
        myPluginHeaderPanel = new PluginHeaderPanel(this);
        myHeader.setBackground(UIUtil.getTextFieldBackground());
        myPluginHeaderPanel.getPanel().setBackground(UIUtil.getTextFieldBackground());
        myPluginHeaderPanel.getPanel().setOpaque(true);

        myHeader.add(myPluginHeaderPanel.getPanel(), BorderLayout.CENTER);
        installTableActions();

        myTablePanel.add(installedScrollPane, BorderLayout.CENTER);
        UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myPanelDescription);
        myPanelDescription.setBorder(JBUI.Borders.emptyLeft(7));

        final JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                final Color bg = UIUtil.getTableBackground(false);
                ((Graphics2D)g).setPaint(new GradientPaint(0, 0, ColorUtil.shift(bg, 1.4), 0, getHeight(), ColorUtil.shift(bg, 0.9)));
                g.fillRect(0,0, getWidth(), getHeight());
            }
        };
        header.setBorder(new CustomLineBorder(1, 1, 0, 1));
        final JLabel mySortLabel = new JLabel();
        mySortLabel.setForeground(UIUtil.getLabelDisabledForeground());
        mySortLabel.setBorder(JBUI.Borders.empty(1, 1, 1, 5));
        mySortLabel.setIcon(AllIcons.General.SplitDown);
        mySortLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        header.add(mySortLabel, BorderLayout.EAST);
        myTablePanel.add(header, BorderLayout.NORTH);
        myToolbarPanel.setLayout(new BorderLayout());
        myActionToolbar = ActionManager.getInstance().createActionToolbar("PluginManager", getActionGroup(true), true);
        final JComponent component = myActionToolbar.getComponent();
        myToolbarPanel.add(component, BorderLayout.CENTER);
        myToolbarPanel.add(myFilter, BorderLayout.WEST);
        new ClickListener() {
            @Override
            public boolean onClick( MouseEvent event, int clickCount) {
                JBPopupFactory.getInstance().createActionGroupPopup("Sort by:", createSortersGroup(), DataManager.getInstance().getDataContext(pluginTable),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true).showUnderneathOf(mySortLabel);
                return true;
            }
        }.installOn(mySortLabel);
        final TableModelListener modelListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                String text = "Sort by:";
                if (pluginsModel.isSortByStatus()) {
                    text += " status,";
                }
                if (pluginsModel.isSortByRating()) {
                    text += " rating,";
                }
                if (pluginsModel.isSortByDownloads()) {
                    text += " downloads,";
                }
                if (pluginsModel.isSortByUpdated()) {
                    text += " updated,";
                }
                text += " name";
                mySortLabel.setText(text);
            }
        };
        pluginTable.getModel().addTableModelListener(modelListener);
        modelListener.tableChanged(null);

        myDescriptionScrollPane.setBackground(UIUtil.getTextFieldBackground());
        Border border = new BorderUIResource.LineBorderUIResource(new JBColor(Gray._220, Gray._55), 1);
        myInfoPanel.setBorder(border);
    }

    protected abstract JScrollPane createTable();

    @Override
    public void dispose() {
        myDisposed = true;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    public void filter(String filter) {
        myFilter.setSelectedItem(filter);
    }

    public void reset() {
        UiNotifyConnector.doWhenFirstShown(getPluginTable(), new Runnable() {
            @Override
            public void run() {
                requireShutdown = false;
                TableUtil.ensureSelectionExists(getPluginTable());
            }
        });
    }

    public PluginTable getPluginTable() {
        return pluginTable;
    }

    private static String getTextPrefix() {
        final int fontSize = JBUI.scale(12);
        final int m1 = JBUI.scale(2);
        final int m2 = JBUI.scale(5);
        return String.format(
                "<html><head>" +
                        "    <style type=\"text/css\">" +
                        "        p {" +
                        "            font-family: Arial,serif; font-size: %dpt; margin: %dpx %dpx" +
                        "        }" +
                        "    </style>" +
                        "</head><body style=\"font-family: Arial,serif; font-size: %dpt; margin: %dpx %dpx;\">",
                fontSize, m1, m1, fontSize, m2, m2);
    }

    public PluginTableModel getPluginsModel() {
        return pluginsModel;
    }

    protected void installTableActions() {
        pluginTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                refresh();
            }
        });

        PopupHandler.installUnknownPopupHandler(pluginTable, getActionGroup(false), ActionManager.getInstance());

        new MySpeedSearchBar(pluginTable);
    }

    public void refresh() {
        IdeaPluginDescriptor[] descriptors = pluginTable.getSelectedObjects();
        IdeaPluginDescriptor plugin = descriptors != null && descriptors.length == 1 ? descriptors[0] : null;
        pluginInfoUpdate(plugin, myFilter.getFilter(), myDescriptionTextArea, myPluginHeaderPanel);
        myActionToolbar.updateActionsImmediately();
        final JComponent parent = (JComponent)myHeader.getParent();
        parent.revalidate();
        parent.repaint();
    }

    public void setRequireShutdown(boolean val) {
        requireShutdown |= val;
    }

    public List<IdeaPluginDescriptorImpl> getDependentList(IdeaPluginDescriptorImpl pluginDescriptor) {
        return pluginsModel.dependent(pluginDescriptor);
    }

    protected void modifyPluginsList(List<IdeaPluginDescriptor> list) {
        IdeaPluginDescriptor[] selected = pluginTable.getSelectedObjects();
        pluginsModel.updatePluginsList(list);
        pluginsModel.filter(myFilter.getFilter().toLowerCase());
        if (selected != null) {
            select(selected);
        }
    }

    protected abstract ActionGroup getActionGroup(boolean inToolbar);

    protected abstract PluginManagerMain getAvailable();
    protected abstract PluginManagerMain getInstalled();

    public JPanel getMainPanel() {
        return main;
    }

    protected boolean acceptHost(String host) {
        return true;
    }

    /**
     * Start a new thread which downloads new list of plugins from the site in
     * the background and updates a list of plugins in the table.
     */
    protected void loadPluginsFromHostInBackground() {
        setDownloadStatus(true);

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                final List<IdeaPluginDescriptor> list = ContainerUtil.newArrayList();
                final List<String> errors = ContainerUtil.newSmartList();
                ProgressIndicator indicator = new EmptyProgressIndicator();

                List<String> hosts = RepositoryHelper.getPluginHosts();
                Set<PluginId> unique = ContainerUtil.newHashSet();
                for (String host : hosts) {
                    try {
                        if (host == null || acceptHost(host)) {
                            List<IdeaPluginDescriptor> plugins = RepositoryHelper.loadPlugins(host, null, indicator);
                            for (IdeaPluginDescriptor plugin : plugins) {
                                if (unique.add(plugin.getPluginId())) {
                                    list.add(plugin);
                                }
                            }
                        }
                    }
                    catch (FileNotFoundException e) {
                        LOG.info(host, e);
                    }
                    catch (IOException e) {
                        LOG.info(host, e);
                        if (host != ApplicationInfoEx.getInstanceEx().getBuiltinPluginsUrl()) {
                            errors.add(e.getMessage());
                        }
                    }
                }

                UIUtil.invokeLaterIfNeeded(new Runnable() {
                    @Override
                    public void run() {
                        setDownloadStatus(false);

                        if (!list.isEmpty()) {
                            InstalledPluginsState state = InstalledPluginsState.getInstance();
                            for (IdeaPluginDescriptor descriptor : list) {
                                state.onDescriptorDownload(descriptor);
                            }

                            modifyPluginsList(list);
                            propagateUpdates(list);
                        }

                        if (!errors.isEmpty()) {
                            String message = IdeBundle.message("error.list.of.plugins.was.not.loaded", StringUtil.join(errors, ", "));
                            String title = IdeBundle.message("title.plugins");
                            String ok = CommonBundle.message("button.retry"), cancel = CommonBundle.getCancelButtonText();
                            if (Messages.showOkCancelDialog(message, title, ok, cancel, Messages.getErrorIcon()) == Messages.OK) {
                                loadPluginsFromHostInBackground();
                            }
                        }
                    }
                });
            }
        });
    }

    protected abstract void propagateUpdates(List<IdeaPluginDescriptor> list);

    protected void setDownloadStatus(boolean status) {
        pluginTable.setPaintBusy(status);
        myBusy = status;
    }

    protected void loadAvailablePlugins() {
        try {
            //  If we already have a file with downloaded plugins from the last time,
            //  then read it, load into the list and start the updating process.
            //  Otherwise just start the process of loading the list and save it
            //  into the persistent config file for later reading.
            List<IdeaPluginDescriptor> list = RepositoryHelper.loadCachedPlugins();
            if (list != null) {
                modifyPluginsList(list);
            }
        }
        catch (Exception ex) {
            //  Nothing to do, just ignore - if nothing can be read from the local
            //  file just start downloading of plugins' list from the site.
        }
        loadPluginsFromHostInBackground();
    }

    public static boolean downloadPlugins(final List<PluginNode> plugins,
                                          final List<IdeaPluginDescriptor> allPlugins,
                                          final Runnable onSuccess,
                                           final Runnable cleanup) throws IOException {
        final boolean[] result = new boolean[1];
        try {
            ProgressManager.getInstance().run(new Task.Backgroundable(null, IdeBundle.message("progress.download.plugins"), true, PluginManagerUISettings.getInstance()) {
                @Override
                public void run( ProgressIndicator indicator) {
                    try {
                        if (PluginInstaller.prepareToInstall(plugins, allPlugins, indicator)) {
                            ApplicationManager.getApplication().invokeLater(onSuccess);
                            result[0] = true;
                        }
                    }
                    finally {
                        if (cleanup != null) cleanup.run();
                    }
                }
            });
        }
        catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof IOException) {
                throw (IOException)e.getCause();
            }
            else {
                throw e;
            }
        }
        return result[0];
    }

    public boolean isRequireShutdown() {
        return requireShutdown;
    }

    public void ignoreChanges() {
        requireShutdown = false;
    }

    public static void pluginInfoUpdate(IdeaPluginDescriptor plugin,
                                         String filter,
                                         JEditorPane descriptionTextArea,
                                         PluginHeaderPanel header) {
        if (plugin == null) {
            setTextValue(null, filter, descriptionTextArea);
            header.getPanel().setVisible(false);
            return;
        }
        StringBuilder sb = new StringBuilder();
        header.setPlugin(plugin);
        String description = plugin.getDescription();
        if (!isEmptyOrSpaces(description)) {
            sb.append(description);
        }

        String changeNotes = plugin.getChangeNotes();
        if (!isEmptyOrSpaces(changeNotes)) {
            sb.append("<h4>Change Notes</h4>");
            sb.append(changeNotes);
        }

        if (!plugin.isBundled()) {
            String vendor = plugin.getVendor();
            String vendorEmail = plugin.getVendorEmail();
            String vendorUrl = plugin.getVendorUrl();
            if (!isEmptyOrSpaces(vendor) || !isEmptyOrSpaces(vendorEmail) || !isEmptyOrSpaces(vendorUrl)) {
                sb.append("<h4>Vendor</h4>");

                if (!isEmptyOrSpaces(vendor)) {
                    sb.append(vendor);
                }
                if (!isEmptyOrSpaces(vendorUrl)) {
                    sb.append("<br>").append(composeHref(vendorUrl));
                }
                if (!isEmptyOrSpaces(vendorEmail)) {
                    sb.append("<br>")
                            .append(HTML_PREFIX)
                            .append("mailto:").append(vendorEmail)
                            .append("\">").append(vendorEmail).append(HTML_SUFFIX);
                }
            }

            String pluginDescriptorUrl = plugin.getUrl();
            if (!isEmptyOrSpaces(pluginDescriptorUrl)) {
                sb.append("<h4>Plugin homepage</h4>").append(composeHref(pluginDescriptorUrl));
            }

            String size = plugin instanceof PluginNode ? ((PluginNode)plugin).getSize() : null;
            if (!isEmptyOrSpaces(size)) {
                sb.append("<h4>Size</h4>").append(PluginManagerColumnInfo.getFormattedSize(size));
            }
        }

        setTextValue(sb, filter, descriptionTextArea);
    }

    private static void setTextValue( StringBuilder text,  String filter, JEditorPane pane) {
        if (text != null) {
            text.insert(0, getTextPrefix());
            text.append(TEXT_SUFFIX);
            pane.setText(SearchUtil.markup(text.toString(), filter).trim());
            pane.setCaretPosition(0);
        }
        else {
            pane.setText(getTextPrefix() + TEXT_SUFFIX);
        }
    }

    private static String composeHref(String vendorUrl) {
        return HTML_PREFIX + vendorUrl + "\">" + vendorUrl + HTML_SUFFIX;
    }

    public boolean isModified() {
        if (requireShutdown) return true;
        return false;
    }

    public String apply() {
        final String applyMessage = canApply();
        if (applyMessage != null) return applyMessage;
        setRequireShutdown(true);
        return null;
    }

    
    protected String canApply() {
        return null;
    }

    private void createUIComponents() {
        myHeader = new JPanel(new BorderLayout()) {
            @Override
            public Color getBackground() {
                return UIUtil.getTextFieldBackground();
            }
        };
    }

    public static class MyHyperlinkListener implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                JEditorPane pane = (JEditorPane)e.getSource();
                if (e instanceof HTMLFrameHyperlinkEvent) {
                    HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)e;
                    HTMLDocument doc = (HTMLDocument)pane.getDocument();
                    doc.processHTMLFrameHyperlinkEvent(evt);
                }
                else {
                    URL url = e.getURL();
                    if (url != null) {
                        BrowserUtil.browse(url);
                    }
                }
            }
        }
    }

    private static class MySpeedSearchBar extends SpeedSearchBase<PluginTable> {
        public MySpeedSearchBar(PluginTable cmp) {
            super(cmp);
        }

        @Override
        protected int convertIndexToModel(int viewIndex) {
            return getComponent().convertRowIndexToModel(viewIndex);
        }

        @Override
        public int getSelectedIndex() {
            return myComponent.getSelectedRow();
        }

        @Override
        public Object[] getAllElements() {
            return myComponent.getElements();
        }

        @Override
        public String getElementText(Object element) {
            return ((IdeaPluginDescriptor)element).getName();
        }

        @Override
        public void selectElement(Object element, String selectedText) {
            for (int i = 0; i < myComponent.getRowCount(); i++) {
                if (myComponent.getObjectAt(i).getName().equals(((IdeaPluginDescriptor)element).getName())) {
                    myComponent.setRowSelectionInterval(i, i);
                    TableUtil.scrollSelectionToVisible(myComponent);
                    break;
                }
            }
        }
    }

    public void select(IdeaPluginDescriptor... descriptors) {
        pluginTable.select(descriptors);
    }

    protected static boolean isAccepted(String filter,
                                        Set<String> search,
                                        IdeaPluginDescriptor descriptor) {
        if (StringUtil.isEmpty(filter)) return true;
        if (isAccepted(search, filter, descriptor.getName())) {
            return true;
        }
        else {
            final String description = descriptor.getDescription();
            if (description != null && isAccepted(search, filter, description)) {
                return true;
            }
            final String category = descriptor.getCategory();
            if (category != null && isAccepted(search, filter, category)) {
                return true;
            }
            final String changeNotes = descriptor.getChangeNotes();
            if (changeNotes != null && isAccepted(search, filter, changeNotes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAccepted(Set<String> search,  String filter,  String description) {
        if (StringUtil.containsIgnoreCase(description, filter)) return true;
        Set<String> descriptionSet = new HashSet<String>(search);
        descriptionSet.removeAll(SearchableOptionsRegistrar.getInstance().getProcessedWords(description));
        return descriptionSet.isEmpty();
    }

    public static void notifyPluginsUpdated( Project project) {
        final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        String title = IdeBundle.message("update.notifications.title");
        String action = IdeBundle.message(app.isRestartCapable() ? "ide.restart.action" : "ide.shutdown.action");
        String message = IdeBundle.message("ide.restart.required.notification", action, ApplicationNamesInfo.getInstance().getFullProductName());
        NotificationListener listener = new NotificationListener() {
            @Override
            public void hyperlinkUpdate( Notification notification,  HyperlinkEvent event) {
                notification.expire();
                app.restart(true);
            }
        };
        UpdateChecker.NOTIFICATIONS.createNotification(title, XmlStringUtil.wrapInHtml(message), NotificationType.INFORMATION, listener).notify(project);
    }

    public class MyPluginsFilter extends FilterComponent {
        public MyPluginsFilter() {
            super("PLUGIN_FILTER", 5);
        }

        @Override
        public void filter() {
            getPluginTable().putClientProperty(SpeedSearchSupply.SEARCH_QUERY_KEY, getFilter());
            pluginsModel.filter(getFilter().toLowerCase());
            TableUtil.ensureSelectionExists(getPluginTable());
        }
    }

    protected class RefreshAction extends DumbAwareAction {
        public RefreshAction() {
            super("Reload List of Plugins", "Reload list of plugins", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            loadAvailablePlugins();
            myFilter.setFilter("");
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!myBusy);
        }
    }

    protected DefaultActionGroup createSortersGroup() {
        final DefaultActionGroup group = new DefaultActionGroup("Sort by", true);
        group.addAction(new SortByStatusAction(pluginTable, pluginsModel));
        return group;
    }
}
