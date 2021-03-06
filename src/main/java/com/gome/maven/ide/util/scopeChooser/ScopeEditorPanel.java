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
package com.gome.maven.ide.util.scopeChooser;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.actionSystem.ex.ComboBoxAction;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.packageDependencies.DependencyUISettings;
import com.gome.maven.packageDependencies.ui.*;
import com.gome.maven.psi.search.scope.packageSet.*;
import com.gome.maven.ui.*;
import com.gome.maven.ui.components.panels.VerticalLayout;
import com.gome.maven.ui.treeStructure.Tree;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.ui.ColorIcon;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.tree.TreeUtil;
import com.gome.maven.util.ui.update.Activatable;
import com.gome.maven.util.ui.update.UiNotifyConnector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

public class ScopeEditorPanel {

    private JPanel myButtonsPanel;
    private RawCommandLineEditor myPatternField;
    private JPanel myTreeToolbar;
    private final Tree myPackageTree;
    private JPanel myPanel;
    private JPanel myTreePanel;
    private JLabel myMatchingCountLabel;
    private JPanel myLegendPanel;

    private final Project myProject;
    private final TreeExpansionMonitor myTreeExpansionMonitor;
    private final Marker myTreeMarker;
    private PackageSet myCurrentScope = null;
    private boolean myIsInUpdate = false;
    private String myErrorMessage;
    private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SHARED_THREAD);

    private JLabel myCaretPositionLabel;
    private int myCaretPosition = 0;
    private JPanel myMatchingCountPanel;
    private JPanel myPositionPanel;
    private JLabel myRecursivelyIncluded;
    private JLabel myPartiallyIncluded;
    private PanelProgressIndicator myCurrentProgress;
    private NamedScopesHolder myHolder;

    public ScopeEditorPanel( final Project project, final NamedScopesHolder holder) {
        myProject = project;
        myHolder = holder;

        myPackageTree = new Tree(new RootNode(project));

        myButtonsPanel.add(createActionsPanel());

        myTreePanel.setLayout(new BorderLayout());
        myTreePanel.add(ScrollPaneFactory.createScrollPane(myPackageTree), BorderLayout.CENTER);

        myTreeToolbar.setLayout(new BorderLayout());
        myTreeToolbar.add(createTreeToolbar(), BorderLayout.WEST);

        myTreeExpansionMonitor = PackageTreeExpansionMonitor.install(myPackageTree, myProject);

        myTreeMarker = new Marker() {
            @Override
            public boolean isMarked(VirtualFile file) {
                return myCurrentScope != null && (myCurrentScope instanceof PackageSetBase ? ((PackageSetBase)myCurrentScope).contains(file, project, myHolder)
                        : myCurrentScope.contains(PackageSetBase.getPsiFile(file, myProject), myHolder));
            }
        };

        myPatternField.setDialogCaption("Pattern");
        myPatternField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void textChanged(DocumentEvent event) {
                onTextChange();
            }
        });

        myPatternField.getTextField().addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                myCaretPosition = e.getDot();
                updateCaretPositionText();
            }
        });

        myPatternField.getTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (myErrorMessage != null) {
                    myPositionPanel.setVisible(true);
                    myPanel.revalidate();
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                myPositionPanel.setVisible(false);
                myPanel.revalidate();
            }
        });

        initTree(myPackageTree);
        new UiNotifyConnector(myPanel, new Activatable() {
            @Override
            public void showNotify() {
            }

            @Override
            public void hideNotify() {
                cancelCurrentProgress();
            }
        });
        myPartiallyIncluded.setIcon(new ColorIcon(10, MyTreeCellRenderer.PARTIAL_INCLUDED));
        myRecursivelyIncluded.setIcon(new ColorIcon(10, MyTreeCellRenderer.WHOLE_INCLUDED));
    }

    private void updateCaretPositionText() {
        if (myErrorMessage != null) {
            myCaretPositionLabel.setText(IdeBundle.message("label.scope.editor.caret.position", myCaretPosition + 1));
        }
        else {
            myCaretPositionLabel.setText("");
        }
        myPositionPanel.setVisible(myErrorMessage != null);
        myCaretPositionLabel.setVisible(myErrorMessage != null);
        myPanel.revalidate();
    }

    public JPanel getPanel() {
        return myPanel;
    }

    public JPanel getTreePanel(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(myTreePanel, BorderLayout.CENTER);
        panel.add(myLegendPanel, BorderLayout.SOUTH);
        return panel;
    }

    public JPanel getTreeToolbar() {
        return myTreeToolbar;
    }

    private void onTextChange() {
        if (!myIsInUpdate) {
            myUpdateAlarm.cancelAllRequests();
            cancelCurrentProgress();
            final String text = myPatternField.getText();
            myCurrentScope = new InvalidPackageSet(text);
            try {
                if (!StringUtil.isEmpty(text)) {
                    myCurrentScope = PackageSetFactory.getInstance().compile(text);
                }
                myErrorMessage = null;
            }
            catch (Exception e) {
                myErrorMessage = e.getMessage();
                showErrorMessage();
            }
            rebuild(false);
        }
        else if (!invalidScopeInside(myCurrentScope)){
            myErrorMessage = null;
        }
    }

    private static boolean invalidScopeInside(PackageSet currentScope) {
        if (currentScope instanceof InvalidPackageSet) return true;
        if (currentScope instanceof UnionPackageSet) {
            if (invalidScopeInside(((UnionPackageSet)currentScope).getFirstSet())) return true;
            if (invalidScopeInside(((UnionPackageSet)currentScope).getSecondSet())) return true;
        }
        if (currentScope instanceof IntersectionPackageSet) {
            if (invalidScopeInside(((IntersectionPackageSet)currentScope).getFirstSet())) return true;
            if (invalidScopeInside(((IntersectionPackageSet)currentScope).getSecondSet())) return true;
        }
        if (currentScope instanceof ComplementPackageSet) {
            return invalidScopeInside(((ComplementPackageSet)currentScope).getComplementarySet());
        }
        return false;
    }

    private void showErrorMessage() {
        myMatchingCountLabel.setText(StringUtil.capitalize(myErrorMessage));
        myMatchingCountLabel.setForeground(JBColor.red);
        myMatchingCountLabel.setToolTipText(myErrorMessage);
    }

    private JComponent createActionsPanel() {
        final JButton include = new JButton(IdeBundle.message("button.include"));
        final JButton includeRec = new JButton(IdeBundle.message("button.include.recursively"));
        final JButton exclude = new JButton(IdeBundle.message("button.exclude"));
        final JButton excludeRec = new JButton(IdeBundle.message("button.exclude.recursively"));
        myPackageTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                final boolean recursiveEnabled = isButtonEnabled(true);
                includeRec.setEnabled(recursiveEnabled);
                excludeRec.setEnabled(recursiveEnabled);

                final boolean nonRecursiveEnabled = isButtonEnabled(false);
                include.setEnabled(nonRecursiveEnabled);
                exclude.setEnabled(nonRecursiveEnabled);
            }
        });

        JPanel buttonsPanel = new JPanel(new VerticalLayout(5));
        buttonsPanel.add(include);
        buttonsPanel.add(includeRec);
        buttonsPanel.add(exclude);
        buttonsPanel.add(excludeRec);

        include.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                includeSelected(false);
            }
        });
        includeRec.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                includeSelected(true);
            }
        });
        exclude.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                excludeSelected(false);
            }
        });
        excludeRec.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                excludeSelected(true);
            }
        });

        return buttonsPanel;
    }

    boolean isButtonEnabled(boolean rec) {
        final TreePath[] paths = myPackageTree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                final PackageDependenciesNode node = (PackageDependenciesNode)path.getLastPathComponent();
                if (PatternDialectProvider.getInstance(DependencyUISettings.getInstance().SCOPE_TYPE).createPackageSet(node, rec) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void excludeSelected(boolean recurse) {
        final ArrayList<PackageSet> selected = getSelectedSets(recurse);
        if (selected == null || selected.isEmpty()) return;
        for (PackageSet set : selected) {
            if (myCurrentScope == null) {
                myCurrentScope = new ComplementPackageSet(set);
            } else if (myCurrentScope instanceof InvalidPackageSet) {
                myCurrentScope = StringUtil.isEmpty(myCurrentScope.getText()) ? new ComplementPackageSet(set) : new IntersectionPackageSet(myCurrentScope, new ComplementPackageSet(set));
            }
            else {
                final boolean[] append = {true};
                final PackageSet simplifiedScope = processComplementaryScope(myCurrentScope, set, false, append);
                if (!append[0]) {
                    myCurrentScope = simplifiedScope;
                }
                else {
                    myCurrentScope = simplifiedScope != null ? new IntersectionPackageSet(simplifiedScope, new ComplementPackageSet(set)) : new ComplementPackageSet(set);
                }
            }
        }
        rebuild(true);
    }

    private void includeSelected(boolean recurse) {
        final ArrayList<PackageSet> selected = getSelectedSets(recurse);
        if (selected == null || selected.isEmpty()) return;
        for (PackageSet set : selected) {
            if (myCurrentScope == null) {
                myCurrentScope = set;
            }
            else if (myCurrentScope instanceof InvalidPackageSet) {
                myCurrentScope = StringUtil.isEmpty(myCurrentScope.getText()) ? set : new UnionPackageSet(myCurrentScope, set);
            }
            else {
                final boolean[] append = {true};
                final PackageSet simplifiedScope = processComplementaryScope(myCurrentScope, set, true, append);
                if (!append[0]) {
                    myCurrentScope = simplifiedScope;
                } else {
                    myCurrentScope = simplifiedScope != null ? new UnionPackageSet(simplifiedScope, set) : set;
                }
            }
        }
        rebuild(true);
    }

    
    static PackageSet processComplementaryScope( PackageSet current, PackageSet added, boolean checkComplementSet, boolean[] append) {
        final String text = added.getText();
        if (current instanceof ComplementPackageSet &&
                Comparing.strEqual(((ComplementPackageSet)current).getComplementarySet().getText(), text)) {
            if (checkComplementSet) {
                append[0] = false;
            }
            return null;
        }
        if (Comparing.strEqual(current.getText(), text)) {
            if (!checkComplementSet) {
                append[0] = false;
            }
            return null;
        }

        if (current instanceof UnionPackageSet) {
            final PackageSet left = processComplementaryScope(((UnionPackageSet)current).getFirstSet(), added, checkComplementSet, append);
            final PackageSet right = processComplementaryScope(((UnionPackageSet)current).getSecondSet(), added, checkComplementSet, append);
            if (left == null) return right;
            if (right == null) return left;
            return new UnionPackageSet(left, right);
        }

        if (current instanceof IntersectionPackageSet) {
            final PackageSet left = processComplementaryScope(((IntersectionPackageSet)current).getFirstSet(), added, checkComplementSet, append);
            final PackageSet right = processComplementaryScope(((IntersectionPackageSet)current).getSecondSet(), added, checkComplementSet, append);
            if (left == null) return right;
            if (right == null) return left;
            return new IntersectionPackageSet(left, right);
        }

        return current;
    }

    
    private ArrayList<PackageSet> getSelectedSets(boolean recursively) {
        int[] rows = myPackageTree.getSelectionRows();
        if (rows == null) return null;
        final ArrayList<PackageSet> result = new ArrayList<PackageSet>();
        for (int row : rows) {
            final PackageDependenciesNode node = (PackageDependenciesNode)myPackageTree.getPathForRow(row).getLastPathComponent();
            final PackageSet set = PatternDialectProvider.getInstance(DependencyUISettings.getInstance().SCOPE_TYPE).createPackageSet(node, recursively);
            if (set != null) {
                result.add(set);
            }
        }
        return result;
    }


    private JComponent createTreeToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();
        final Runnable update = new Runnable() {
            @Override
            public void run() {
                rebuild(true);
            }
        };
        if (ProjectViewDirectoryHelper.getInstance(myProject).supportsFlattenPackages()) {
            group.add(new FlattenPackagesAction(update));
        }
        final PatternDialectProvider[] dialectProviders = Extensions.getExtensions(PatternDialectProvider.EP_NAME);
        for (PatternDialectProvider provider : dialectProviders) {
            for (AnAction action : provider.createActions(myProject, update)) {
                group.add(action);
            }
        }
        group.add(new ShowFilesAction(update));
        final Module[] modules = ModuleManager.getInstance(myProject).getModules();
        if (modules.length > 1) {
            group.add(new ShowModulesAction(update));
            group.add(new ShowModuleGroupsAction(update));
        }
        group.add(new FilterLegalsAction(update));

        if (dialectProviders.length > 1) {
            group.add(new ChooseScopeTypeAction(update));
        }

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        return toolbar.getComponent();
    }

    private void rebuild(final boolean updateText,  final Runnable runnable, final boolean requestFocus, final int delayMillis){
        myUpdateAlarm.cancelAllRequests();
        final Runnable request = new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        if (updateText) {
                            final String text = myCurrentScope != null ? myCurrentScope.getText() : null;
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        myIsInUpdate = true;
                                        myPatternField.setText(text);
                                    }
                                    finally {
                                        myIsInUpdate = false;
                                    }
                                }
                            });
                        }

                        try {
                            if (!myProject.isDisposed()) {
                                updateTreeModel(requestFocus);
                            }
                        }
                        catch (ProcessCanceledException e) {
                            return;
                        }
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                });
            }
        };
        myUpdateAlarm.addRequest(request, delayMillis);
    }

    private void rebuild(final boolean updateText) {
        rebuild(updateText, null, true, 300);
    }

    public void setHolder(NamedScopesHolder holder) {
        myHolder = holder;
    }

    private void initTree(Tree tree) {
        tree.setCellRenderer(new MyTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setLineStyleAngled();

        TreeUtil.installActions(tree);
        SmartExpander.installOn(tree);
        new TreeSpeedSearch(tree);
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                ((PackageDependenciesNode)event.getPath().getLastPathComponent()).sortChildren();
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });

        PopupHandler.installUnknownPopupHandler(tree, createTreePopupActions(), ActionManager.getInstance());
    }

    private ActionGroup createTreePopupActions() {
        final DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction(IdeBundle.message("button.include")) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                includeSelected(false);
            }
        });
        actionGroup.add(new AnAction(IdeBundle.message("button.include.recursively")) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                includeSelected(true);
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(isButtonEnabled(true));
            }
        });

        actionGroup.add(new AnAction(IdeBundle.message("button.exclude")) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                excludeSelected(false);
            }
        });
        actionGroup.add(new AnAction(IdeBundle.message("button.exclude.recursively")) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                excludeSelected(true);
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(isButtonEnabled(true));
            }
        });

        return actionGroup;
    }

    private void updateTreeModel(final boolean requestFocus) throws ProcessCanceledException {
        PanelProgressIndicator progress = createProgressIndicator(requestFocus);
        progress.setBordersVisible(false);
        myCurrentProgress = progress;
        Runnable updateModel = new Runnable() {
            @Override
            public void run() {
                final ProcessCanceledException [] ex = new ProcessCanceledException[1];
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                    @Override
                    public void run() {
                        if (myProject.isDisposed()) return;
                        try {
                            myTreeExpansionMonitor.freeze();
                            final TreeModel model = PatternDialectProvider.getInstance(DependencyUISettings.getInstance().SCOPE_TYPE).createTreeModel(myProject, myTreeMarker);
                            ((PackageDependenciesNode)model.getRoot()).sortChildren();
                            if (myErrorMessage == null) {
                                myMatchingCountLabel
                                        .setText(IdeBundle.message("label.scope.contains.files", model.getMarkedFileCount(), model.getTotalFileCount()));
                                myMatchingCountLabel.setForeground(new JLabel().getForeground());
                            }
                            else {
                                showErrorMessage();
                            }

                            SwingUtilities.invokeLater(new Runnable(){
                                @Override
                                public void run() { //not under progress
                                    myPackageTree.setModel(model);
                                    myTreeExpansionMonitor.restore();
                                }
                            });
                        } catch (ProcessCanceledException e) {
                            ex[0] = e;
                        }
                        finally {
                            myCurrentProgress = null;
                            //update label
                            setToComponent(myMatchingCountLabel, requestFocus);
                        }
                    }
                });
                if (ex[0] != null) {
                    throw ex[0];
                }
            }
        };
        ProgressManager.getInstance().runProcess(updateModel, progress);
    }

    private PanelProgressIndicator createProgressIndicator(final boolean requestFocus) {
        return new MyPanelProgressIndicator(requestFocus);
    }

    public void cancelCurrentProgress(){
        if (myCurrentProgress != null){
            myCurrentProgress.cancel();
        }
    }

    public void apply() throws ConfigurationException {
    }

    public PackageSet getCurrentScope() {
        return myCurrentScope;
    }

    public String getPatternText() {
        return myPatternField.getText();
    }

    public void reset(PackageSet packageSet,  Runnable runnable) {
        myCurrentScope = packageSet;
        myPatternField.setText(myCurrentScope == null ? "" : myCurrentScope.getText());
        rebuild(false, runnable, false, 0);
    }

    private void setToComponent(final JComponent cmp, final boolean requestFocus) {
        myMatchingCountPanel.removeAll();
        myMatchingCountPanel.add(cmp, BorderLayout.CENTER);
        myMatchingCountPanel.revalidate();
        myMatchingCountPanel.repaint();
        if (requestFocus) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    myPatternField.getTextField().requestFocusInWindow();
                }
            });
        }
    }

    public void restoreCanceledProgress() {
        if (myIsInUpdate) {
            rebuild(false);
        }
    }

    public void clearCaches() {
        FileTreeModelBuilder.clearCaches(myProject);
    }

    private static class MyTreeCellRenderer extends ColoredTreeCellRenderer {
        private static final Color WHOLE_INCLUDED = new JBColor(new Color(10, 119, 0), new Color(0xA5C25C));
        private static final Color PARTIAL_INCLUDED = new JBColor(new Color(0, 50, 160), DarculaColors.BLUE);

        @Override
        public void customizeCellRenderer(JTree tree,
                                          Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean leaf,
                                          int row,
                                          boolean hasFocus) {
            if (value instanceof PackageDependenciesNode) {
                PackageDependenciesNode node = (PackageDependenciesNode)value;
                setIcon(node.getIcon());

                setForeground(selected && hasFocus ? UIUtil.getTreeSelectionForeground() : UIUtil.getTreeForeground());
                if (!(selected && hasFocus) && node.hasMarked() && !DependencyUISettings.getInstance().UI_FILTER_LEGALS) {
                    setForeground(node.hasUnmarked() ? PARTIAL_INCLUDED : WHOLE_INCLUDED);
                }
                append(node.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                final String locationString = node.getComment();
                if (!StringUtil.isEmpty(locationString)) {
                    append(" (" + locationString + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
                }
            }
        }
    }

    private final class ChooseScopeTypeAction extends ComboBoxAction{
        private final Runnable myUpdate;

        public ChooseScopeTypeAction(final Runnable update) {
            myUpdate = update;
        }

        @Override
        
        protected DefaultActionGroup createPopupActionGroup(final JComponent button) {
            final DefaultActionGroup group = new DefaultActionGroup();
            for (final PatternDialectProvider provider : Extensions.getExtensions(PatternDialectProvider.EP_NAME)) {
                group.add(new AnAction(provider.getDisplayName()) {
                    @Override
                    public void actionPerformed(final AnActionEvent e) {
                        DependencyUISettings.getInstance().SCOPE_TYPE = provider.getShortName();
                        myUpdate.run();
                    }
                });
            }
            return group;
        }

        @Override
        public void update(final AnActionEvent e) {
            super.update(e);
            final PatternDialectProvider provider = PatternDialectProvider.getInstance(DependencyUISettings.getInstance().SCOPE_TYPE);
            e.getPresentation().setText(provider.getDisplayName());
            e.getPresentation().setIcon(provider.getIcon());
        }
    }

    private final class FilterLegalsAction extends ToggleAction {
        private final Runnable myUpdate;

        public FilterLegalsAction(final Runnable update) {
            super(IdeBundle.message("action.show.included.only"),
                    IdeBundle.message("action.description.show.included.only"), AllIcons.General.Filter);
            myUpdate = update;
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return DependencyUISettings.getInstance().UI_FILTER_LEGALS;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            DependencyUISettings.getInstance().UI_FILTER_LEGALS = flag;
            UIUtil.setEnabled(myLegendPanel, !flag, true);
            myUpdate.run();
        }
    }

    protected class MyPanelProgressIndicator extends PanelProgressIndicator {
        private final boolean myRequestFocus;

        public MyPanelProgressIndicator(final boolean requestFocus) {
            super(new Consumer<JComponent>() {
                @Override
                public void consume(final JComponent component) {
                    setToComponent(component, requestFocus);
                }
            });
            myRequestFocus = requestFocus;
        }

        @Override
        public void stop() {
            super.stop();
            setToComponent(myMatchingCountLabel, myRequestFocus);
        }

        @Override
        public String getText() { //just show non-blocking progress
            return null;
        }

        @Override
        public String getText2() {
            return null;
        }
    }
}
