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
package com.gome.maven.diff.impl;

import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.codeInsight.hint.HintManagerImpl;
import com.gome.maven.codeInsight.hint.HintUtil;
import com.gome.maven.diff.DiffContext;
import com.gome.maven.diff.DiffManagerEx;
import com.gome.maven.diff.DiffTool;
import com.gome.maven.diff.FrameDiffTool;
import com.gome.maven.diff.FrameDiffTool.DiffViewer;
import com.gome.maven.diff.actions.impl.*;
import com.gome.maven.diff.impl.DiffSettingsHolder.DiffSettings;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.requests.ErrorDiffRequest;
import com.gome.maven.diff.requests.MessageDiffRequest;
import com.gome.maven.diff.requests.NoDiffRequest;
import com.gome.maven.diff.tools.ErrorDiffTool;
import com.gome.maven.diff.tools.external.ExternalDiffTool;
import com.gome.maven.diff.tools.util.DiffDataKeys;
import com.gome.maven.diff.tools.util.PrevNextDifferenceIterable;
import com.gome.maven.diff.util.DiffUserDataKeys;
import com.gome.maven.diff.util.DiffUserDataKeysEx;
import com.gome.maven.diff.util.DiffUserDataKeysEx.ScrollToPolicy;
import com.gome.maven.diff.util.DiffUtil;
import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.impl.DataManagerImpl;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.actionSystem.ex.ComboBoxAction;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.popup.Balloon;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.gome.maven.ui.HintHint;
import com.gome.maven.ui.LightweightHint;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.JBUI;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("InnerClassMayBeStatic")
public abstract class DiffRequestProcessor implements Disposable {
    private static final Logger LOG = Logger.getInstance(DiffRequestProcessor.class);

    private boolean myDisposed;

     private final Project myProject;
     private final DiffContext myContext;

     private final DiffSettings mySettings;
     private final List<DiffTool> myAvailableTools;
     private final LinkedList<DiffTool> myToolOrder;

     private final OpenInEditorAction myOpenInEditorAction;
     private DefaultActionGroup myPopupActionGroup;

     private final JPanel myPanel;
     private final MyPanel myMainPanel;
     private final ModifiablePanel myContentPanel;
     private final ModifiablePanel myToolbarPanel; // TODO: allow to call 'updateToolbar' from Viewer ?
     private final ModifiablePanel myToolbarStatusPanel;

     private DiffRequest myActiveRequest;

     private ViewerState myState;

    public DiffRequestProcessor( Project project) {
        this(project, new UserDataHolderBase());
    }

    public DiffRequestProcessor( Project project,  String place) {
        this(project, DiffUtil.createUserDataHolder(DiffUserDataKeysEx.PLACE, place));
    }

    public DiffRequestProcessor( Project project,  UserDataHolder context) {
        myProject = project;

        myAvailableTools = DiffManagerEx.getInstance().getDiffTools();
        myToolOrder = new LinkedList<DiffTool>();

        myContext = new MyDiffContext(context);
        myActiveRequest = NoDiffRequest.INSTANCE;

        mySettings = DiffSettingsHolder.getInstance().getSettings(myContext.getUserData(DiffUserDataKeysEx.PLACE));

        // UI

        myPanel = new JPanel(new BorderLayout());
        myMainPanel = new MyPanel();
        myContentPanel = new ModifiablePanel();
        myToolbarPanel = new ModifiablePanel();
        myToolbarStatusPanel = new ModifiablePanel();

        myPanel.add(myMainPanel, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(myToolbarPanel, BorderLayout.CENTER);
        topPanel.add(myToolbarStatusPanel, BorderLayout.EAST);


        myMainPanel.add(topPanel, BorderLayout.NORTH);
        myMainPanel.add(myContentPanel, BorderLayout.CENTER);

        myMainPanel.setFocusTraversalPolicyProvider(true);
        myMainPanel.setFocusTraversalPolicy(new MyFocusTraversalPolicy());

        JComponent bottomPanel = myContext.getUserData(DiffUserDataKeysEx.BOTTOM_PANEL);
        if (bottomPanel != null) myMainPanel.add(bottomPanel, BorderLayout.SOUTH);
        if (bottomPanel instanceof Disposable) Disposer.register(this, (Disposable)bottomPanel);


        myOpenInEditorAction = new OpenInEditorAction(new Runnable() {
            @Override
            public void run() {
                onAfterNavigate();
            }
        });
    }

    public void init() {
        myToolOrder.addAll(getToolOrderFromSettings(myAvailableTools));

        myActiveRequest.onAssigned(true);
        myState = new ErrorState((MessageDiffRequest)myActiveRequest);
        myState.init();
    }

    //
    // Update
    //

//    @CalledInAwt
    public void updateRequest() {
        updateRequest(false);
    }

//    @CalledInAwt
    public void updateRequest(boolean force) {
        updateRequest(force, null);
    }

//    @CalledInAwt
    public abstract void updateRequest(boolean force,  ScrollToPolicy scrollToChangePolicy);

    
    private FrameDiffTool getFittedTool() {
        List<FrameDiffTool> tools = new ArrayList<FrameDiffTool>();
        for (DiffTool tool : myToolOrder) {
            try {
                if (tool instanceof FrameDiffTool && tool.canShow(myContext, myActiveRequest)) {
                    tools.add((FrameDiffTool)tool);
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        tools = DiffUtil.filterSuppressedTools(tools);

        return tools.isEmpty() ? ErrorDiffTool.INSTANCE : tools.get(0);
    }

    
    private List<FrameDiffTool> getAvailableFittedTools() {
        List<FrameDiffTool> tools = new ArrayList<FrameDiffTool>();
        for (DiffTool tool : myAvailableTools) {
            try {
                if (tool instanceof FrameDiffTool && tool.canShow(myContext, myActiveRequest)) {
                    tools.add((FrameDiffTool)tool);
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        return DiffUtil.filterSuppressedTools(tools);
    }

    private void moveToolOnTop( DiffTool tool) {
        myToolOrder.remove(tool);

        FrameDiffTool toolToReplace = getFittedTool();

        int index;
        for (index = 0; index < myToolOrder.size(); index++) {
            if (myToolOrder.get(index) == toolToReplace) break;
        }
        myToolOrder.add(index, tool);

        updateToolOrderSettings(myToolOrder);
    }

    
    private ViewerState createState() {
        FrameDiffTool frameTool = getFittedTool();

        DiffViewer viewer = frameTool.createComponent(myContext, myActiveRequest);

        DiffViewerWrapper wrapper = myActiveRequest.getUserData(DiffViewerWrapper.KEY);
        if (wrapper == null) {
            return new DefaultState(viewer, frameTool);
        }
        else {
            return new WrapperState(viewer, frameTool, wrapper);
        }
    }

    //
    // Abstract
    //

//    @CalledInAwt
    protected void applyRequest( DiffRequest request, boolean force,  ScrollToPolicy scrollToChangePolicy) {
        myIterationState = IterationState.NONE;

        boolean hadFocus = isFocused();
        if (!force && request == myActiveRequest) return;

        request.putUserData(DiffUserDataKeysEx.SCROLL_TO_CHANGE, scrollToChangePolicy);

        myState.destroy();
        myToolbarStatusPanel.setContent(null);
        myToolbarPanel.setContent(null);
        myContentPanel.setContent(null);
        myMainPanel.putClientProperty(AnAction.ourClientProperty, null);

        myActiveRequest.onAssigned(false);
        myActiveRequest = request;
        myActiveRequest.onAssigned(true);

        try {
            myState = createState();
            myState.init();
        }
        catch (Throwable e) {
            LOG.error(e);
            myState = new ErrorState(new ErrorDiffRequest("Error: can't show diff"), getFittedTool());
            myState.init();
        }

        if (hadFocus) requestFocusInternal();
    }

    protected void setWindowTitle( String title) {
    }

    protected void onAfterNavigate() {
    }

    protected void onDispose() {
    }

    
    public <T> T getContextUserData( Key<T> key) {
        return myContext.getUserData(key);
    }

    public <T> void putContextUserData( Key<T> key,  T value) {
        myContext.putUserData(key, value);
    }

    
    protected List<AnAction> getNavigationActions() {
        return ContainerUtil.<AnAction>list(
                new MyPrevDifferenceAction(),
                new MyNextDifferenceAction(),
                new MyPrevChangeAction(),
                new MyNextChangeAction()
        );
    }

    //
    // Misc
    //

    public boolean isWindowFocused() {
        Window window = SwingUtilities.getWindowAncestor(myPanel);
        return window != null && window.isFocused();
    }

    public boolean isFocused() {
        return DiffUtil.isFocusedComponent(myProject, myPanel);
    }

    public void requestFocus() {
        DiffUtil.requestFocus(myProject, getPreferredFocusedComponent());
    }

    protected void requestFocusInternal() {
        JComponent component = getPreferredFocusedComponent();
        if (component != null) component.requestFocus();
    }

    
    protected List<DiffTool> getToolOrderFromSettings( List<DiffTool> availableTools) {
        List<DiffTool> result = new ArrayList<DiffTool>();
        List<String> savedOrder = getSettings().getDiffToolsOrder();

        for (final String clazz : savedOrder) {
            DiffTool tool = ContainerUtil.find(availableTools, new Condition<DiffTool>() {
                @Override
                public boolean value(DiffTool tool) {
                    return tool.getClass().getCanonicalName().equals(clazz);
                }
            });
            if (tool != null) result.add(tool);
        }

        for (DiffTool tool : availableTools) {
            if (!result.contains(tool)) result.add(tool);
        }

        return result;
    }

    protected void updateToolOrderSettings( List<DiffTool> toolOrder) {
        List<String> savedOrder = new ArrayList<String>();
        for (DiffTool tool : toolOrder) {
            savedOrder.add(tool.getClass().getCanonicalName());
        }
        getSettings().setDiffToolsOrder(savedOrder);
    }

    @Override
    public void dispose() {
        if (myDisposed) return;
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (myDisposed) return;
                myDisposed = true;

                onDispose();

                myState.destroy();
                myToolbarStatusPanel.setContent(null);
                myToolbarPanel.setContent(null);
                myContentPanel.setContent(null);

                myActiveRequest.onAssigned(false);
            }
        });
    }

    
    protected DefaultActionGroup collectToolbarActions( List<AnAction> viewerActions) {
        DefaultActionGroup group = new DefaultActionGroup();

        List<AnAction> navigationActions = new ArrayList<AnAction>();
        navigationActions.addAll(getNavigationActions());
        navigationActions.add(myOpenInEditorAction);
        navigationActions.add(new MyChangeDiffToolAction());
        DiffUtil.addActionBlock(group,
                navigationActions);

        DiffUtil.addActionBlock(group, viewerActions);

        List<AnAction> requestContextActions = myActiveRequest.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
        DiffUtil.addActionBlock(group, requestContextActions);

        List<AnAction> contextActions = myContext.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
        DiffUtil.addActionBlock(group, contextActions);

        DiffUtil.addActionBlock(group,
                new ShowInExternalToolAction(),
                new ShowOldDiffAction(),
                ActionManager.getInstance().getAction(IdeActions.ACTION_CONTEXT_HELP));

        return group;
    }

    
    protected DefaultActionGroup collectPopupActions( List<AnAction> viewerActions) {
        DefaultActionGroup group = new DefaultActionGroup();

        List<AnAction> selectToolActions = new ArrayList<AnAction>();
        for (DiffTool tool : getAvailableFittedTools()) {
            if (tool == myState.getActiveTool()) continue;
            selectToolActions.add(new DiffToolToggleAction(tool));
        }
        DiffUtil.addActionBlock(group, selectToolActions);

        DiffUtil.addActionBlock(group, viewerActions);

        return group;
    }

    protected void buildToolbar( List<AnAction> viewerActions) {
        ActionGroup group = collectToolbarActions(viewerActions);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DIFF_TOOLBAR, group, true);

        DataManager.registerDataProvider(toolbar.getComponent(), myMainPanel);
        toolbar.setTargetComponent(toolbar.getComponent());

        myToolbarPanel.setContent(toolbar.getComponent());
        for (AnAction action : group.getChildren(null)) {
            action.registerCustomShortcutSet(action.getShortcutSet(), myMainPanel);
        }
    }

    protected void buildActionPopup( List<AnAction> viewerActions) {
        ShowActionGroupPopupAction action = new ShowActionGroupPopupAction();
        action.registerCustomShortcutSet(action.getShortcutSet(), myMainPanel);

        myPopupActionGroup = collectPopupActions(viewerActions);
    }

    private void setTitle( String title) {
        if (title == null) title = "Diff";
        setWindowTitle(title);
    }

    //
    // Getters
    //

    
    public JComponent getComponent() {
        return myPanel;
    }

    
    public JComponent getPreferredFocusedComponent() {
        JComponent component = myState.getPreferredFocusedComponent();
        return component != null ? component : myToolbarPanel.getContent();
    }

    
    public Project getProject() {
        return myProject;
    }

    
    public DiffContext getContext() {
        return myContext;
    }

    
    protected DiffSettings getSettings() {
        return mySettings;
    }

    //
    // Actions
    //

    private class ShowInExternalToolAction extends DumbAwareAction {
        public ShowInExternalToolAction() {
            EmptyAction.setupAction(this, "Diff.ShowInExternalTool", null);
        }

        @Override
        public void update(AnActionEvent e) {
            if (!ExternalDiffTool.isEnabled()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            e.getPresentation().setEnabled(ExternalDiffTool.canShow(myActiveRequest));
            e.getPresentation().setVisible(true);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            try {
                ExternalDiffTool.showRequest(e.getProject(), myActiveRequest);
            }
            catch (Throwable ex) {
                Messages.showErrorDialog(e.getProject(), ex.getMessage(), "Can't Show Diff In External Tool");
            }
        }
    }

    private class MyChangeDiffToolAction extends ComboBoxAction implements DumbAware {
        public MyChangeDiffToolAction() {
            // TODO: add icons for diff tools, show only icon in toolbar - to reduce jumping on change ?
            setEnabledInModalContext(true);
        }

        @Override
        public void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();

            DiffTool activeTool = myState.getActiveTool();
            presentation.setText(activeTool.getName());

            if (activeTool == ErrorDiffTool.INSTANCE) {
                presentation.setEnabledAndVisible(false);
            }

            for (DiffTool tool : getAvailableFittedTools()) {
                if (tool != activeTool) {
                    presentation.setEnabledAndVisible(true);
                    return;
                }
            }

            presentation.setEnabledAndVisible(false);
        }

        
        @Override
        protected DefaultActionGroup createPopupActionGroup(JComponent button) {
            DefaultActionGroup group = new DefaultActionGroup();
            for (DiffTool tool : getAvailableFittedTools()) {
                group.add(new DiffToolToggleAction(tool));
            }

            return group;
        }
    }

    private class DiffToolToggleAction extends AnAction implements DumbAware {
         private final DiffTool myDiffTool;

        private DiffToolToggleAction( DiffTool tool) {
            super(tool.getName());
            setEnabledInModalContext(true);
            myDiffTool = tool;
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            if (myState.getActiveTool() == myDiffTool) return;

            moveToolOnTop(myDiffTool);

            updateRequest(true);
        }
    }

    private class ShowActionGroupPopupAction extends DumbAwareAction {
        public ShowActionGroupPopupAction() {
            EmptyAction.setupAction(this, "Diff.ShowSettingsPopup", null);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myPopupActionGroup != null && myPopupActionGroup.getChildrenCount() > 0);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            assert myPopupActionGroup != null;
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup("Diff Actions", myPopupActionGroup, e.getDataContext(),
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
            popup.showInCenterOf(myPanel);
        }
    }

    //
    // Navigation
    //

    private enum IterationState {NEXT, PREV, NONE}

     private IterationState myIterationState = IterationState.NONE;

//    @CalledInAwt
    protected boolean hasNextChange() {
        return false;
    }

//    @CalledInAwt
    protected boolean hasPrevChange() {
        return false;
    }

//    @CalledInAwt
    protected void goToNextChange(boolean fromDifferences) {
    }

//    @CalledInAwt
    protected void goToPrevChange(boolean fromDifferences) {
    }

//    @CalledInAwt
    protected boolean isNavigationEnabled() {
        return false;
    }

    protected class MyNextDifferenceAction extends NextDifferenceAction {
        @Override
        public void update( AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            PrevNextDifferenceIterable iterable = DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE.getData(e.getDataContext());
            if (iterable == null && !isNavigationEnabled()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            e.getPresentation().setVisible(true);
            if (iterable != null && iterable.canGoNext()) {
                e.getPresentation().setEnabled(true);
                return;
            }

            if (isNavigationEnabled() && hasNextChange() && getSettings().isGoToNextFileOnNextDifference()) {
                e.getPresentation().setEnabled(true);
                return;
            }

            e.getPresentation().setEnabled(false);
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            PrevNextDifferenceIterable iterable = DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE.getData(e.getDataContext());
            if (iterable != null && iterable.canGoNext()) {
                iterable.goNext();
                myIterationState = IterationState.NONE;
                return;
            }

            if (!isNavigationEnabled() || !hasNextChange() || !getSettings().isGoToNextFileOnNextDifference()) return;

            if (myIterationState != IterationState.NEXT) {
                // TODO: provide "change" word in chain UserData - for tests/etc
                notifyMessage(e.getData(DiffDataKeys.CURRENT_EDITOR), "Press again to go to the next file", true);
                myIterationState = IterationState.NEXT;
                return;
            }

            goToNextChange(true);
        }
    }

    protected class MyPrevDifferenceAction extends PrevDifferenceAction {
        @Override
        public void update( AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            PrevNextDifferenceIterable iterable = DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE.getData(e.getDataContext());
            if (iterable == null && !isNavigationEnabled()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            e.getPresentation().setVisible(true);
            if (iterable != null && iterable.canGoPrev()) {
                e.getPresentation().setEnabled(true);
                return;
            }

            if (isNavigationEnabled() && hasPrevChange() && getSettings().isGoToNextFileOnNextDifference()) {
                e.getPresentation().setEnabled(true);
                return;
            }

            e.getPresentation().setEnabled(false);
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            PrevNextDifferenceIterable iterable = DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE.getData(e.getDataContext());
            if (iterable != null && iterable.canGoPrev()) {
                iterable.goPrev();
                myIterationState = IterationState.NONE;
                return;
            }

            if (!isNavigationEnabled() || !hasPrevChange() || !getSettings().isGoToNextFileOnNextDifference()) return;

            if (myIterationState != IterationState.PREV) {
                notifyMessage(e.getData(DiffDataKeys.CURRENT_EDITOR), "Press again to go to the previous file", false);
                myIterationState = IterationState.PREV;
                return;
            }

            goToPrevChange(true);
        }
    }

    private void notifyMessage( Editor editor,  String message, boolean next) {
        final LightweightHint hint = new LightweightHint(HintUtil.createInformationLabel(message));
        Point point = new Point(myContentPanel.getWidth() / 2, next ? myContentPanel.getHeight() - JBUI.scale(40) : JBUI.scale(40));

        final HintHint hintHint = new HintHint(myContentPanel, point)
                .setPreferredPosition(next ? Balloon.Position.above : Balloon.Position.below)
                .setAwtTooltip(true)
                .setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD))
                .setTextBg(HintUtil.INFORMATION_COLOR)
                .setShowImmediately(true);

        if (editor == null) {
            final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            hint.show(myContentPanel, point.x, point.y, owner instanceof JComponent ? (JComponent)owner : null, hintHint);
        }
        else {
            Point editorPoint = SwingUtilities.convertPoint(myContentPanel, point, editor.getComponent());
            HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, editorPoint, HintManager.HIDE_BY_ANY_KEY |
                    HintManager.HIDE_BY_TEXT_CHANGE |
                    HintManager.HIDE_BY_SCROLLING, 0, false, hintHint);
        }
    }

    // Iterate requests

    protected class MyNextChangeAction extends NextChangeAction {
        @Override
        public void update( AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            if (!isNavigationEnabled()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(hasNextChange());
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            if (!isNavigationEnabled() || !hasNextChange()) return;

            goToNextChange(false);
        }
    }

    protected class MyPrevChangeAction extends PrevChangeAction {
        @Override
        public void update( AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            if (!isNavigationEnabled()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(hasPrevChange());
        }

        @Override
        public void actionPerformed( AnActionEvent e) {
            if (!isNavigationEnabled() || !hasPrevChange()) return;

            goToPrevChange(false);
        }
    }

    //
    // Helpers
    //

    private class MyPanel extends JPanel implements DataProvider {
        public MyPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension windowSize = DiffUtil.getDefaultDiffPanelSize();
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(windowSize.width, size.width), Math.max(windowSize.height, size.height));
        }

        
        @Override
        public Object getData( String dataId) {
            Object data;

            DataProvider contentProvider = DataManagerImpl.getDataProviderEx(myContentPanel.getContent());
            if (contentProvider != null) {
                data = contentProvider.getData(dataId);
                if (data != null) return data;
            }

            if (OpenInEditorAction.KEY.is(dataId)) {
                return myOpenInEditorAction;
            }
            else if (DiffDataKeys.DIFF_REQUEST.is(dataId)) {
                return myActiveRequest;
            }
            else if (CommonDataKeys.PROJECT.is(dataId)) {
                return myProject;
            }
            else if (PlatformDataKeys.HELP_ID.is(dataId)) {
                if (myActiveRequest.getUserData(DiffUserDataKeys.HELP_ID) != null) {
                    return myActiveRequest.getUserData(DiffUserDataKeys.HELP_ID);
                }
                else {
                    return "reference.dialogs.diff.file";
                }
            }
            else if (DiffDataKeys.DIFF_CONTEXT.is(dataId)) {
                return myContext;
            }

            data = myState.getData(dataId);
            if (data != null) return data;

            DataProvider requestProvider = myActiveRequest.getUserData(DiffUserDataKeys.DATA_PROVIDER);
            if (requestProvider != null) {
                data = requestProvider.getData(dataId);
                if (data != null) return data;
            }

            DataProvider contextProvider = myContext.getUserData(DiffUserDataKeys.DATA_PROVIDER);
            if (contextProvider != null) {
                data = contextProvider.getData(dataId);
                if (data != null) return data;
            }
            return null;
        }
    }

    private class MyFocusTraversalPolicy extends IdeFocusTraversalPolicy {
        @Override
        public final Component getDefaultComponentImpl(final Container focusCycleRoot) {
            JComponent component = DiffRequestProcessor.this.getPreferredFocusedComponent();
            if (component == null) return null;
            return IdeFocusTraversalPolicy.getPreferredFocusedComponent(component, this);
        }
    }

    private class MyDiffContext extends DiffContext {
         private final UserDataHolder myContext;

        public MyDiffContext( UserDataHolder context) {
            myContext = context;
        }

        
        @Override
        public Project getProject() {
            return DiffRequestProcessor.this.getProject();
        }

        @Override
        public boolean isFocused() {
            return DiffRequestProcessor.this.isFocused();
        }

        @Override
        public boolean isWindowFocused() {
            return DiffRequestProcessor.this.isWindowFocused();
        }

        @Override
        public void requestFocus() {
            DiffRequestProcessor.this.requestFocusInternal();
        }

        
        @Override
        public <T> T getUserData( Key<T> key) {
            return myContext.getUserData(key);
        }

        @Override
        public <T> void putUserData( Key<T> key,  T value) {
            myContext.putUserData(key, value);
        }
    }

    //
    // States
    //

    private interface ViewerState {
        void init();

        void destroy();

        
        JComponent getPreferredFocusedComponent();

        
        Object getData( String dataId);

        
        DiffTool getActiveTool();
    }

    private class ErrorState implements ViewerState {
         private final DiffTool myDiffTool;
         private final MessageDiffRequest myRequest;

         private final DiffViewer myViewer;

        public ErrorState( MessageDiffRequest request) {
            this(request, null);
        }

        public ErrorState( MessageDiffRequest request,  DiffTool diffTool) {
            myDiffTool = diffTool;
            myRequest = request;

            myViewer = ErrorDiffTool.INSTANCE.createComponent(myContext, myRequest);
        }

        @Override
        public void init() {
            myContentPanel.setContent(myViewer.getComponent());

            FrameDiffTool.ToolbarComponents init = myViewer.init();
            buildToolbar(init.toolbarActions);

            myPanel.validate();
        }

        @Override
        public void destroy() {
            Disposer.dispose(myViewer);
        }

        
        @Override
        public JComponent getPreferredFocusedComponent() {
            return null;
        }

        
        @Override
        public Object getData( String dataId) {
            return null;
        }

        
        @Override
        public DiffTool getActiveTool() {
            return myDiffTool != null ? myDiffTool : ErrorDiffTool.INSTANCE;
        }
    }

    private class DefaultState implements ViewerState {
         private final DiffViewer myViewer;
         private final FrameDiffTool myTool;

        public DefaultState( DiffViewer viewer,  FrameDiffTool tool) {
            myViewer = viewer;
            myTool = tool;
        }

        @Override
        public void init() {
            myContentPanel.setContent(myViewer.getComponent());
            setTitle(myActiveRequest.getTitle());

            myPanel.validate();

            FrameDiffTool.ToolbarComponents toolbarComponents = myViewer.init();

            buildToolbar(toolbarComponents.toolbarActions);
            buildActionPopup(toolbarComponents.popupActions);

            myToolbarStatusPanel.setContent(toolbarComponents.statusPanel);

            myPanel.validate();
        }

        @Override
        public void destroy() {
            Disposer.dispose(myViewer);
        }

        
        @Override
        public JComponent getPreferredFocusedComponent() {
            return myViewer.getPreferredFocusedComponent();
        }

        
        @Override
        public DiffTool getActiveTool() {
            return myTool;
        }

        
        @Override
        public Object getData( String dataId) {
            if (DiffDataKeys.DIFF_VIEWER.is(dataId)) {
                return myViewer;
            }
            return null;
        }
    }

    private class WrapperState implements ViewerState {
         private final DiffViewer myViewer;
         private final FrameDiffTool myTool;

         private DiffViewer myWrapperViewer;

        public WrapperState( DiffViewer viewer,  FrameDiffTool tool,  DiffViewerWrapper wrapper) {
            myViewer = viewer;
            myTool = tool;
            myWrapperViewer = wrapper.createComponent(myContext, myActiveRequest, myViewer);
        }

        @Override
        public void init() {
            myContentPanel.setContent(myWrapperViewer.getComponent());
            setTitle(myActiveRequest.getTitle());

            myPanel.validate();


            FrameDiffTool.ToolbarComponents toolbarComponents1 = myViewer.init();
            FrameDiffTool.ToolbarComponents toolbarComponents2 = myWrapperViewer.init();

            List<AnAction> toolbarActions = new ArrayList<AnAction>();
            if (toolbarComponents1.toolbarActions != null) toolbarActions.addAll(toolbarComponents1.toolbarActions);
            if (toolbarComponents2.toolbarActions != null) {
                if (!toolbarActions.isEmpty() && !toolbarComponents2.toolbarActions.isEmpty()) toolbarActions.add(Separator.getInstance());
                toolbarActions.addAll(toolbarComponents2.toolbarActions);
            }
            buildToolbar(toolbarActions);

            List<AnAction> popupActions = new ArrayList<AnAction>();
            if (toolbarComponents1.popupActions != null) popupActions.addAll(toolbarComponents1.popupActions);
            if (toolbarComponents2.popupActions != null) {
                if (!popupActions.isEmpty() && !toolbarComponents2.popupActions.isEmpty()) popupActions.add(Separator.getInstance());
                popupActions.addAll(toolbarComponents2.popupActions);
            }
            buildActionPopup(popupActions);


            myToolbarStatusPanel.setContent(toolbarComponents1.statusPanel); // TODO: combine both panels ?

            myPanel.validate();
        }

        @Override
        public void destroy() {
            Disposer.dispose(myViewer);
            Disposer.dispose(myWrapperViewer);
        }

        
        @Override
        public JComponent getPreferredFocusedComponent() {
            return myWrapperViewer.getPreferredFocusedComponent();
        }

        
        @Override
        public DiffTool getActiveTool() {
            return myTool;
        }

        
        @Override
        public Object getData( String dataId) {
            if (DiffDataKeys.DIFF_VIEWER.is(dataId)) {
                return myWrapperViewer;
            }
            return null;
        }
    }
}
