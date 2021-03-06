/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.ui.popup;

import com.gome.maven.CommonBundle;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.DataManager;
import com.gome.maven.ide.IdeEventQueue;
import com.gome.maven.ide.IdeTooltipManager;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.actionSystem.ex.ActionUtil;
import com.gome.maven.openapi.actionSystem.impl.ActionMenu;
import com.gome.maven.openapi.actionSystem.impl.Utils;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.CaretModel;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.VisualPosition;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.popup.*;
import com.gome.maven.openapi.ui.popup.util.BaseListPopupStep;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.EmptyRunnable;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.openapi.wm.ex.WindowManagerEx;
import com.gome.maven.openapi.wm.impl.IdeFrameImpl;
import com.gome.maven.ui.ColorUtil;
import com.gome.maven.ui.FocusTrackback;
import com.gome.maven.ui.HintHint;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.ui.components.panels.NonOpaquePanel;
import com.gome.maven.ui.popup.list.ListPopupImpl;
import com.gome.maven.ui.popup.mock.MockConfirmation;
import com.gome.maven.ui.popup.tree.TreePopupImpl;
import com.gome.maven.util.PlatformIcons;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.containers.WeakHashMap;
import com.gome.maven.util.ui.EmptyIcon;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PopupFactoryImpl extends JBPopupFactory {

    /**
     * Allows to get an editor position for which a popup with auxiliary information might be shown.
     * <p/>
     * Primary intention for this key is to hint popup position for the non-caret location.
     */
    public static final Key<VisualPosition> ANCHOR_POPUP_POSITION = Key.create("popup.anchor.position");

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ui.popup.PopupFactoryImpl");

    private final Map<Disposable, List<Balloon>> myStorage = new WeakHashMap<Disposable, List<Balloon>>();

    
    @Override
    public ListPopup createConfirmation(String title, final Runnable onYes, int defaultOptionIndex) {
        return createConfirmation(title, CommonBundle.getYesButtonText(), CommonBundle.getNoButtonText(), onYes, defaultOptionIndex);
    }

    
    @Override
    public ListPopup createConfirmation(String title, final String yesText, String noText, final Runnable onYes, int defaultOptionIndex) {
        return createConfirmation(title, yesText, noText, onYes, EmptyRunnable.getInstance(), defaultOptionIndex);
    }

    
    @Override
    public JBPopup createMessage(String text) {
        return createListPopup(new BaseListPopupStep<String>(null, new String[]{text}));
    }

    @Override
    public Balloon getParentBalloonFor( Component c) {
        if (c == null) return null;
        Component eachParent = c;
        while (eachParent != null) {
            if (eachParent instanceof JComponent) {
                Object balloon = ((JComponent)eachParent).getClientProperty(Balloon.KEY);
                if (balloon instanceof Balloon) {
                    return (Balloon)balloon;
                }
            }
            eachParent = eachParent.getParent();
        }

        return null;
    }

    
    @Override
    public ListPopup createConfirmation(String title,
                                        final String yesText,
                                        String noText,
                                        final Runnable onYes,
                                        final Runnable onNo,
                                        int defaultOptionIndex)
    {

        final BaseListPopupStep<String> step = new BaseListPopupStep<String>(title, new String[]{yesText, noText}) {
            @Override
            public PopupStep onChosen(String selectedValue, final boolean finalChoice) {
                if (selectedValue.equals(yesText)) {
                    onYes.run();
                }
                else {
                    onNo.run();
                }
                return FINAL_CHOICE;
            }

            @Override
            public void canceled() {
                onNo.run();
            }

            @Override
            public boolean isMnemonicsNavigationEnabled() {
                return true;
            }
        };
        step.setDefaultOptionIndex(defaultOptionIndex);

        final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        return app == null || !app.isUnitTestMode() ? new ListPopupImpl(step) : new MockConfirmation(step, yesText);
    }


    private static ListPopup createActionGroupPopup(final String title,
                                                     ActionGroup actionGroup,
                                                     DataContext dataContext,
                                                    boolean showNumbers,
                                                    boolean useAlphaAsNumbers,
                                                    boolean showDisabledActions,
                                                    boolean honorActionMnemonics,
                                                    final Runnable disposeCallback,
                                                    final int maxRowCount) {
        return createActionGroupPopup(title, actionGroup, dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics, disposeCallback,
                maxRowCount, null, null);
    }

    public ListPopup createActionGroupPopup(final String title,
                                            final ActionGroup actionGroup,
                                             DataContext dataContext,
                                            boolean showNumbers,
                                            boolean showDisabledActions,
                                            boolean honorActionMnemonics,
                                            final Runnable disposeCallback,
                                            final int maxRowCount) {
        return createActionGroupPopup(title, actionGroup, dataContext, showNumbers, showDisabledActions, honorActionMnemonics, disposeCallback,
                maxRowCount, null);
    }

    private static ListPopup createActionGroupPopup(final String title,
                                                     ActionGroup actionGroup,
                                                     DataContext dataContext,
                                                    boolean showNumbers,
                                                    boolean useAlphaAsNumbers,
                                                    boolean showDisabledActions,
                                                    boolean honorActionMnemonics,
                                                    final Runnable disposeCallback,
                                                    final int maxRowCount,
                                                    final Condition<AnAction> preselectActionCondition,  final String actionPlace) {
        return new ActionGroupPopup(title, actionGroup, dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics,
                disposeCallback, maxRowCount, preselectActionCondition, actionPlace);
    }

    public static class ActionGroupPopup extends ListPopupImpl {

        private final Runnable myDisposeCallback;
        private final Component myComponent;

        public ActionGroupPopup(final String title,
                                 ActionGroup actionGroup,
                                 DataContext dataContext,
                                boolean showNumbers,
                                boolean useAlphaAsNumbers,
                                boolean showDisabledActions,
                                boolean honorActionMnemonics,
                                final Runnable disposeCallback,
                                final int maxRowCount,
                                final Condition<AnAction> preselectActionCondition,
                                 final String actionPlace) {
            super(createStep(title, actionGroup, dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics,
                    preselectActionCondition, actionPlace),
                    maxRowCount);
            myDisposeCallback = disposeCallback;
            myComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);

            addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    final JList list = (JList)e.getSource();
                    final ActionItem actionItem = (ActionItem)list.getSelectedValue();
                    if (actionItem == null) return;
                    AnAction action = actionItem.getAction();
                    Presentation presentation = new Presentation();
                    presentation.setDescription(action.getTemplatePresentation().getDescription());
                    final String actualActionPlace = actionPlace == null ? ActionPlaces.UNKNOWN : actionPlace;
                    final AnActionEvent actionEvent =
                            new AnActionEvent(null, DataManager.getInstance().getDataContext(myComponent), actualActionPlace, presentation,
                                    ActionManager.getInstance(), 0);
                    actionEvent.setInjectedContext(action.isInInjectedContext());
                    ActionUtil.performDumbAwareUpdate(action, actionEvent, false);
                    ActionMenu.showDescriptionInStatusBar(true, myComponent, presentation.getDescription());
                }
            });
        }

        private static ListPopupStep createStep(String title,
                                                 ActionGroup actionGroup,
                                                 DataContext dataContext,
                                                boolean showNumbers,
                                                boolean useAlphaAsNumbers,
                                                boolean showDisabledActions,
                                                boolean honorActionMnemonics,
                                                Condition<AnAction> preselectActionCondition,
                                                 String actionPlace) {
            final Component component = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
            LOG.assertTrue(component != null, "dataContext has no component for new ListPopupStep");

            final ActionStepBuilder builder =
                    new ActionStepBuilder(dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics);
            if (actionPlace != null) {
                builder.setActionPlace(actionPlace);
            }
            builder.buildGroup(actionGroup);
            final List<ActionItem> items = builder.getItems();

            return new ActionPopupStep(items, title, component, showNumbers || honorActionMnemonics && itemsHaveMnemonics(items),
                    preselectActionCondition, false, showDisabledActions);
        }

        @Override
        public void dispose() {
            if (myDisposeCallback != null) {
                myDisposeCallback.run();
            }
            ActionMenu.showDescriptionInStatusBar(true, myComponent, null);
            super.dispose();
        }
    }

    
    @Override
    public ListPopup createActionGroupPopup(final String title,
                                             final ActionGroup actionGroup,
                                             DataContext dataContext,
                                            boolean showNumbers,
                                            boolean showDisabledActions,
                                            boolean honorActionMnemonics,
                                            final Runnable disposeCallback,
                                            final int maxRowCount,
                                            final Condition<AnAction> preselectActionCondition) {
        return createActionGroupPopup(title, actionGroup, dataContext, showNumbers, true, showDisabledActions, honorActionMnemonics,
                disposeCallback, maxRowCount, preselectActionCondition, null);
    }

    
    @Override
    public ListPopup createActionGroupPopup(String title,
                                             ActionGroup actionGroup,
                                             DataContext dataContext,
                                            ActionSelectionAid selectionAidMethod,
                                            boolean showDisabledActions) {
        return createActionGroupPopup(title, actionGroup, dataContext,
                selectionAidMethod == ActionSelectionAid.NUMBERING || selectionAidMethod == ActionSelectionAid.ALPHA_NUMBERING,
                selectionAidMethod == ActionSelectionAid.ALPHA_NUMBERING,
                showDisabledActions,
                selectionAidMethod == ActionSelectionAid.MNEMONICS,
                null, -1);
    }

    
    @Override
    public ListPopup createActionGroupPopup(String title,
                                             ActionGroup actionGroup,
                                             DataContext dataContext,
                                            ActionSelectionAid selectionAidMethod,
                                            boolean showDisabledActions,
                                             String actionPlace) {
        return createActionGroupPopup(title, actionGroup, dataContext,
                selectionAidMethod == ActionSelectionAid.NUMBERING || selectionAidMethod == ActionSelectionAid.ALPHA_NUMBERING,
                selectionAidMethod == ActionSelectionAid.ALPHA_NUMBERING,
                showDisabledActions,
                selectionAidMethod == ActionSelectionAid.MNEMONICS,
                null, -1, null, actionPlace);
    }

    
    @Override
    public ListPopup createActionGroupPopup(String title,
                                             ActionGroup actionGroup,
                                             DataContext dataContext,
                                            ActionSelectionAid selectionAidMethod,
                                            boolean showDisabledActions,
                                            Runnable disposeCallback,
                                            int maxRowCount) {
        return createActionGroupPopup(title, actionGroup, dataContext,
                selectionAidMethod == ActionSelectionAid.NUMBERING || selectionAidMethod == ActionSelectionAid.ALPHA_NUMBERING,
                selectionAidMethod == ActionSelectionAid.ALPHA_NUMBERING,
                showDisabledActions,
                selectionAidMethod == ActionSelectionAid.MNEMONICS,
                disposeCallback,
                maxRowCount);
    }

    
    @Override
    public ListPopupStep createActionsStep( final ActionGroup actionGroup,
                                            DataContext dataContext,
                                           final boolean showNumbers,
                                           final boolean showDisabledActions,
                                           final String title,
                                           final Component component,
                                           final boolean honorActionMnemonics) {
        return createActionsStep(actionGroup, dataContext, showNumbers, showDisabledActions, title, component, honorActionMnemonics, 0, false);
    }

    private static ListPopupStep createActionsStep( ActionGroup actionGroup,  DataContext dataContext,
                                                   boolean showNumbers, boolean useAlphaAsNumbers, boolean showDisabledActions,
                                                   String title, Component component, boolean honorActionMnemonics,
                                                   final int defaultOptionIndex, final boolean autoSelectionEnabled) {
        final List<ActionItem> items = makeActionItemsFromActionGroup(actionGroup, dataContext, showNumbers, useAlphaAsNumbers,
                showDisabledActions, honorActionMnemonics);
        return new ActionPopupStep(items, title, component, showNumbers || honorActionMnemonics && itemsHaveMnemonics(items),
                new Condition<AnAction>() {
                    @Override
                    public boolean value(AnAction action) {
                        return defaultOptionIndex >= 0 &&
                                defaultOptionIndex < items.size() &&
                                items.get(defaultOptionIndex).getAction().equals(action);
                    }
                }, autoSelectionEnabled, showDisabledActions);
    }

    
    private static List<ActionItem> makeActionItemsFromActionGroup( ActionGroup actionGroup,
                                                                    DataContext dataContext,
                                                                   boolean showNumbers,
                                                                   boolean useAlphaAsNumbers,
                                                                   boolean showDisabledActions,
                                                                   boolean honorActionMnemonics) {
        final ActionStepBuilder builder = new ActionStepBuilder(dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions,
                honorActionMnemonics);
        builder.buildGroup(actionGroup);
        return builder.getItems();
    }

    
    private static ListPopupStep createActionsStep( ActionGroup actionGroup,  DataContext dataContext,
                                                   boolean showNumbers, boolean useAlphaAsNumbers, boolean showDisabledActions,
                                                   String title, Component component, boolean honorActionMnemonics,
                                                   Condition<AnAction> preselectActionCondition, boolean autoSelectionEnabled) {
        final List<ActionItem> items = makeActionItemsFromActionGroup(actionGroup, dataContext, showNumbers, useAlphaAsNumbers,
                showDisabledActions, honorActionMnemonics);
        return new ActionPopupStep(items, title, component, showNumbers || honorActionMnemonics && itemsHaveMnemonics(items), preselectActionCondition,
                autoSelectionEnabled, showDisabledActions);
    }

    
    @Override
    public ListPopupStep createActionsStep( ActionGroup actionGroup,  DataContext dataContext, boolean showNumbers, boolean showDisabledActions,
                                           String title, Component component, boolean honorActionMnemonics, int defaultOptionIndex,
                                           final boolean autoSelectionEnabled) {
        return createActionsStep(actionGroup, dataContext, showNumbers, true, showDisabledActions, title, component, honorActionMnemonics,
                defaultOptionIndex, autoSelectionEnabled);
    }

    private static boolean itemsHaveMnemonics(final List<ActionItem> items) {
        for (ActionItem item : items) {
            if (item.getAction().getTemplatePresentation().getMnemonic() != 0) return true;
        }

        return false;
    }

    
    @Override
    public ListPopup createWizardStep( PopupStep step) {
        return new ListPopupImpl((ListPopupStep) step);
    }

    
    @Override
    public ListPopup createListPopup( ListPopupStep step) {
        return new ListPopupImpl(step);
    }

    
    @Override
    public ListPopup createListPopup( ListPopupStep step, int maxRowCount) {
        return new ListPopupImpl(step, maxRowCount);
    }

    
    @Override
    public TreePopup createTree(JBPopup parent,  TreePopupStep aStep, Object parentValue) {
        return new TreePopupImpl(parent, aStep, parentValue);
    }

    
    @Override
    public TreePopup createTree( TreePopupStep aStep) {
        return new TreePopupImpl(aStep);
    }

    
    @Override
    public ComponentPopupBuilder createComponentPopupBuilder( JComponent content, JComponent prefferableFocusComponent) {
        return new ComponentPopupBuilderImpl(content, prefferableFocusComponent);
    }


    
    @Override
    public RelativePoint guessBestPopupLocation( DataContext dataContext) {
        Component component = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
        JComponent focusOwner = component instanceof JComponent ? (JComponent)component : null;

        if (focusOwner == null) {
            Project project = CommonDataKeys.PROJECT.getData(dataContext);
            IdeFrameImpl frame = project == null ? null : ((WindowManagerEx)WindowManager.getInstance()).getFrame(project);
            focusOwner = frame == null ? null : frame.getRootPane();
            if (focusOwner == null) {
                throw new IllegalArgumentException("focusOwner cannot be null");
            }
        }

        final Point point = PlatformDataKeys.CONTEXT_MENU_POINT.getData(dataContext);
        if (point != null) {
            return new RelativePoint(focusOwner, point);
        }

        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null && focusOwner == editor.getContentComponent()) {
            return guessBestPopupLocation(editor);
        }
        else {
            return guessBestPopupLocation(focusOwner);
        }
    }

    
    @Override
    public RelativePoint guessBestPopupLocation( final JComponent component) {
        Point popupMenuPoint = null;
        final Rectangle visibleRect = component.getVisibleRect();
        if (component instanceof JList) { // JList
            JList list = (JList)component;
            int firstVisibleIndex = list.getFirstVisibleIndex();
            int lastVisibleIndex = list.getLastVisibleIndex();
            int[] selectedIndices = list.getSelectedIndices();
            for (int index : selectedIndices) {
                if (firstVisibleIndex <= index && index <= lastVisibleIndex) {
                    Rectangle cellBounds = list.getCellBounds(index, index);
                    popupMenuPoint = new Point(visibleRect.x + visibleRect.width / 4, cellBounds.y + cellBounds.height);
                    break;
                }
            }
        }
        else if (component instanceof JTree) { // JTree
            JTree tree = (JTree)component;
            int[] selectionRows = tree.getSelectionRows();
            if (selectionRows != null) {
                Arrays.sort(selectionRows);
                for (int i = 0; i < selectionRows.length; i++) {
                    int row = selectionRows[i];
                    Rectangle rowBounds = tree.getRowBounds(row);
                    if (visibleRect.contains(rowBounds)) {
                        popupMenuPoint = new Point(rowBounds.x + 2, rowBounds.y + rowBounds.height - 1);
                        break;
                    }
                }
                if (popupMenuPoint == null) {//All selected rows are out of visible rect
                    Point visibleCenter = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
                    double minDistance = Double.POSITIVE_INFINITY;
                    int bestRow = -1;
                    Point rowCenter;
                    double distance;
                    for (int i = 0; i < selectionRows.length; i++) {
                        int row = selectionRows[i];
                        Rectangle rowBounds = tree.getRowBounds(row);
                        rowCenter = new Point(rowBounds.x + rowBounds.width / 2, rowBounds.y + rowBounds.height / 2);
                        distance = visibleCenter.distance(rowCenter);
                        if (minDistance > distance) {
                            minDistance = distance;
                            bestRow = row;
                        }
                    }

                    if (bestRow != -1) {
                        Rectangle rowBounds = tree.getRowBounds(bestRow);
                        tree.scrollRectToVisible(new Rectangle(rowBounds.x, rowBounds.y, Math.min(visibleRect.width, rowBounds.width), rowBounds.height));
                        popupMenuPoint = new Point(rowBounds.x + 2, rowBounds.y + rowBounds.height - 1);
                    }
                }
            }
        }
        else if (component instanceof JTable) {
            JTable table = (JTable)component;
            int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            int row = Math.max(table.getSelectionModel().getLeadSelectionIndex(), table.getSelectionModel().getAnchorSelectionIndex());
            Rectangle rect = table.getCellRect(row, column, false);
            if (!visibleRect.intersects(rect)) {
                table.scrollRectToVisible(rect);
            }
            popupMenuPoint = new Point(rect.x, rect.y + rect.height);
        }
        else if (component instanceof PopupOwner) {
            popupMenuPoint = ((PopupOwner)component).getBestPopupPosition();
        }
        if (popupMenuPoint == null) {
            popupMenuPoint = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
        }

        return new RelativePoint(component, popupMenuPoint);
    }

    @Override
    public boolean isBestPopupLocationVisible( Editor editor) {
        return getVisibleBestPopupLocation(editor) != null;
    }

    
    @Override
    public RelativePoint guessBestPopupLocation( Editor editor) {
        Point p = getVisibleBestPopupLocation(editor);
        if (p == null) {
            final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
            p = new Point((visibleArea.x + visibleArea.width) / 2, (visibleArea.y + visibleArea.height) / 2);
        }
        return new RelativePoint(editor.getContentComponent(), p);
    }

    
    private static Point getVisibleBestPopupLocation( Editor editor) {
        VisualPosition visualPosition = editor.getUserData(ANCHOR_POPUP_POSITION);

        if (visualPosition == null) {
            CaretModel caretModel = editor.getCaretModel();
            if (caretModel.isUpToDate()) {
                visualPosition = caretModel.getVisualPosition();
            }
            else {
                visualPosition = editor.offsetToVisualPosition(caretModel.getOffset());
            }
        }

        Point p = editor.visualPositionToXY(new VisualPosition(visualPosition.line + 1, visualPosition.column));

        final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        return visibleArea.contains(p) ? p : null;
    }

    @Override
    public Point getCenterOf(JComponent container, JComponent content) {
        return AbstractPopup.getCenterOf(container, content);
    }

    public static class ActionItem {
        private final AnAction myAction;
        private final String myText;
        private final boolean myIsEnabled;
        private final Icon myIcon;
        private final boolean myPrependWithSeparator;
        private final String mySeparatorText;

        private ActionItem( AnAction action,  String text, boolean enabled, Icon icon, final boolean prependWithSeparator, String separatorText) {
            myAction = action;
            myText = text;
            myIsEnabled = enabled;
            myIcon = icon;
            myPrependWithSeparator = prependWithSeparator;
            mySeparatorText = separatorText;
        }

        
        public AnAction getAction() {
            return myAction;
        }

        
        public String getText() {
            return myText;
        }

        public Icon getIcon() {
            return myIcon;
        }

        public boolean isPrependWithSeparator() {
            return myPrependWithSeparator;
        }

        public String getSeparatorText() {
            return mySeparatorText;
        }

        public boolean isEnabled() { return myIsEnabled; }
    }

    private static class ActionPopupStep implements ListPopupStepEx<ActionItem>, MnemonicNavigationFilter<ActionItem>, SpeedSearchFilter<ActionItem> {
        private final List<ActionItem> myItems;
        private final String myTitle;
        private final Component myContext;
        private final boolean myEnableMnemonics;
        private final int myDefaultOptionIndex;
        private final boolean myAutoSelectionEnabled;
        private final boolean myShowDisabledActions;
        private Runnable myFinalRunnable;
         private final Condition<AnAction> myPreselectActionCondition;

        private ActionPopupStep( final List<ActionItem> items, final String title, Component context, boolean enableMnemonics,
                                 Condition<AnAction> preselectActionCondition, final boolean autoSelection, boolean showDisabledActions) {
            myItems = items;
            myTitle = title;
            myContext = context;
            myEnableMnemonics = enableMnemonics;
            myDefaultOptionIndex = getDefaultOptionIndexFromSelectCondition(preselectActionCondition, items);
            myPreselectActionCondition = preselectActionCondition;
            myAutoSelectionEnabled = autoSelection;
            myShowDisabledActions = showDisabledActions;
        }

        private static int getDefaultOptionIndexFromSelectCondition( Condition<AnAction> preselectActionCondition,
                                                                     List<ActionItem> items) {
            int defaultOptionIndex = 0;
            if (preselectActionCondition != null) {
                for (int i = 0; i < items.size(); i++) {
                    final AnAction action = items.get(i).getAction();
                    if (preselectActionCondition.value(action)) {
                        defaultOptionIndex = i;
                        break;
                    }
                }
            }
            return defaultOptionIndex;
        }

        @Override
        
        public List<ActionItem> getValues() {
            return myItems;
        }

        @Override
        public boolean isSelectable(final ActionItem value) {
            return value.isEnabled();
        }

        @Override
        public int getMnemonicPos(final ActionItem value) {
            final String text = getTextFor(value);
            int i = text.indexOf(UIUtil.MNEMONIC);
            if (i < 0) {
                i = text.indexOf('&');
            }
            if (i < 0) {
                i = text.indexOf('_');
            }
            return i;
        }

        @Override
        public Icon getIconFor(final ActionItem aValue) {
            return aValue.getIcon();
        }

        @Override
        
        public String getTextFor(final ActionItem value) {
            return value.getText();
        }

        @Override
        public ListSeparator getSeparatorAbove(final ActionItem value) {
            return value.isPrependWithSeparator() ? new ListSeparator(value.getSeparatorText()) : null;
        }

        @Override
        public int getDefaultOptionIndex() {
            return myDefaultOptionIndex;
        }

        @Override
        public String getTitle() {
            return myTitle;
        }

        @Override
        public PopupStep onChosen(final ActionItem actionChoice, final boolean finalChoice) {
            return onChosen(actionChoice, finalChoice, 0);
        }

        @Override
        public PopupStep onChosen(ActionItem actionChoice, boolean finalChoice, final int eventModifiers) {
            if (!actionChoice.isEnabled()) return FINAL_CHOICE;
            final AnAction action = actionChoice.getAction();
            DataManager mgr = DataManager.getInstance();

            final DataContext dataContext = myContext != null ? mgr.getDataContext(myContext) : mgr.getDataContext();

            if (action instanceof ActionGroup && (!finalChoice || !((ActionGroup)action).canBePerformed(dataContext))) {
                return createActionsStep((ActionGroup)action, dataContext, myEnableMnemonics, true, myShowDisabledActions, null, myContext, false,
                        myPreselectActionCondition, false);
            }
            else {
                myFinalRunnable = new Runnable() {
                    @Override
                    public void run() {
                        final AnActionEvent event = new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, action.getTemplatePresentation().clone(),
                                ActionManager.getInstance(), eventModifiers);
                        event.setInjectedContext(action.isInInjectedContext());
                        if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
                            action.actionPerformed(event);
                        }
                    }
                };
                return FINAL_CHOICE;
            }
        }

        @Override
        public Runnable getFinalRunnable() {
            return myFinalRunnable;
        }

        @Override
        public boolean hasSubstep(final ActionItem selectedValue) {
            return selectedValue != null && selectedValue.isEnabled() && selectedValue.getAction() instanceof ActionGroup;
        }

        @Override
        public void canceled() {
        }

        @Override
        public boolean isMnemonicsNavigationEnabled() {
            return myEnableMnemonics;
        }

        @Override
        public MnemonicNavigationFilter<ActionItem> getMnemonicNavigationFilter() {
            return this;
        }

        @Override
        public boolean canBeHidden(final ActionItem value) {
            return true;
        }

        @Override
        public String getIndexedString(final ActionItem value) {
            return getTextFor(value);
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return true;
        }

        @Override
        public boolean isAutoSelectionEnabled() {
            return myAutoSelectionEnabled;
        }

        @Override
        public SpeedSearchFilter<ActionItem> getSpeedSearchFilter() {
            return this;
        }
    }

    @Override
    
    public List<JBPopup> getChildPopups( final Component component) {
        return FocusTrackback.getChildPopups(component);
    }

    @Override
    public boolean isPopupActive() {
        return IdeEventQueue.getInstance().isPopupActive();
    }

    private static class ActionStepBuilder {
        private final List<ActionItem>                myListModel;
        private final DataContext                     myDataContext;
        private final boolean                         myShowNumbers;
        private final boolean                         myUseAlphaAsNumbers;
        private final boolean                         myShowDisabled;
        private final HashMap<AnAction, Presentation> myAction2presentation;
        private       int                             myCurrentNumber;
        private       boolean                         myPrependWithSeparator;
        private       String                          mySeparatorText;
        private final boolean                         myHonorActionMnemonics;
        private       Icon                            myEmptyIcon;
        private int myMaxIconWidth  = -1;
        private int myMaxIconHeight = -1;
         private String myActionPlace;

        private ActionStepBuilder( DataContext dataContext, final boolean showNumbers, final boolean useAlphaAsNumbers,
                                  final boolean showDisabled, final boolean honorActionMnemonics)
        {
            myUseAlphaAsNumbers = useAlphaAsNumbers;
            myListModel = new ArrayList<ActionItem>();
            myDataContext = dataContext;
            myShowNumbers = showNumbers;
            myShowDisabled = showDisabled;
            myAction2presentation = new HashMap<AnAction, Presentation>();
            myCurrentNumber = 0;
            myPrependWithSeparator = false;
            mySeparatorText = null;
            myHonorActionMnemonics = honorActionMnemonics;
            myActionPlace = ActionPlaces.UNKNOWN;
        }

        public void setActionPlace( String actionPlace) {
            myActionPlace = actionPlace;
        }

        
        public List<ActionItem> getItems() {
            return myListModel;
        }

        public void buildGroup( ActionGroup actionGroup) {
            calcMaxIconSize(actionGroup);
            myEmptyIcon = myMaxIconHeight != -1 && myMaxIconWidth != -1 ? new EmptyIcon(myMaxIconWidth, myMaxIconHeight) : null;

            appendActionsFromGroup(actionGroup);

            if (myListModel.isEmpty()) {
                myListModel.add(new ActionItem(Utils.EMPTY_MENU_FILLER, Utils.NOTHING_HERE, false, null, false, null));
            }
        }

        private void calcMaxIconSize(final ActionGroup actionGroup) {
            AnAction[] actions = actionGroup.getChildren(createActionEvent(actionGroup));
            for (AnAction action : actions) {
                if (action == null) continue;
                if (action instanceof ActionGroup) {
                    final ActionGroup group = (ActionGroup)action;
                    if (!group.isPopup()) {
                        calcMaxIconSize(group);
                        continue;
                    }
                }

                Icon icon = action.getTemplatePresentation().getIcon();
                if (icon == null && action instanceof Toggleable) icon = PlatformIcons.CHECK_ICON;
                if (icon != null) {
                    final int width = icon.getIconWidth();
                    final int height = icon.getIconHeight();
                    if (myMaxIconWidth < width) {
                        myMaxIconWidth = width;
                    }
                    if (myMaxIconHeight < height) {
                        myMaxIconHeight = height;
                    }
                }
            }
        }

        
        private AnActionEvent createActionEvent( AnAction actionGroup) {
            final AnActionEvent actionEvent =
                    new AnActionEvent(null, myDataContext, myActionPlace, getPresentation(actionGroup), ActionManager.getInstance(), 0);
            actionEvent.setInjectedContext(actionGroup.isInInjectedContext());
            return actionEvent;
        }

        private void appendActionsFromGroup( ActionGroup actionGroup) {
            AnAction[] actions = actionGroup.getChildren(createActionEvent(actionGroup));
            for (AnAction action : actions) {
                if (action == null) {
                    LOG.error("null action in group " + actionGroup);
                    continue;
                }
                if (action instanceof Separator) {
                    myPrependWithSeparator = true;
                    mySeparatorText = ((Separator)action).getText();
                }
                else {
                    if (action instanceof ActionGroup) {
                        ActionGroup group = (ActionGroup)action;
                        if (group.isPopup()) {
                            appendAction(group);
                        }
                        else {
                            appendActionsFromGroup(group);
                        }
                    }
                    else {
                        appendAction(action);
                    }
                }
            }
        }

        private void appendAction( AnAction action) {
            Presentation presentation = getPresentation(action);
            AnActionEvent event = createActionEvent(action);

            ActionUtil.performDumbAwareUpdate(action, event, true);
            if ((myShowDisabled || presentation.isEnabled()) && presentation.isVisible()) {
                String text = presentation.getText();
                if (myShowNumbers) {
                    if (myCurrentNumber < 9) {
                        text = "&" + (myCurrentNumber + 1) + ". " + text;
                    }
                    else if (myCurrentNumber == 9) {
                        text = "&" + 0 + ". " + text;
                    }
                    else if (myUseAlphaAsNumbers) {
                        text = "&" + (char)('A' + myCurrentNumber - 10) + ". " + text;
                    }
                    myCurrentNumber++;
                }
                else if (myHonorActionMnemonics) {
                    text = Presentation.restoreTextWithMnemonic(text, action.getTemplatePresentation().getMnemonic());
                }

                Icon icon = presentation.getIcon();
                if (icon == null) {
                     final String actionId = ActionManager.getInstance().getId(action);
                    if (actionId != null && actionId.startsWith("QuickList.")) {
                        icon = AllIcons.Actions.QuickList;
                    }
                    else if (action instanceof Toggleable) {
                        boolean toggled = Boolean.TRUE.equals(presentation.getClientProperty(Toggleable.SELECTED_PROPERTY));
                        icon = toggled? new IconWrapper(PlatformIcons.CHECK_ICON) : myEmptyIcon;
                    }
                    else {
                        icon = myEmptyIcon;
                    }
                }
                else {
                    icon = new IconWrapper(icon);
                }
                boolean prependSeparator = (!myListModel.isEmpty() || mySeparatorText != null) && myPrependWithSeparator;
                assert text != null : action + " has no presentation";
                myListModel.add(new ActionItem(action, text, presentation.isEnabled(), icon, prependSeparator, mySeparatorText));
                myPrependWithSeparator = false;
                mySeparatorText = null;
            }
        }

        /**
         * Adjusts icon size to maximum, so that icons with different sizes were aligned correctly.
         */
        private class IconWrapper implements Icon {

            private Icon myIcon;

            IconWrapper(Icon icon) {
                myIcon = icon;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                myIcon.paintIcon(c, g, x, y);
            }

            @Override
            public int getIconWidth() {
                return myMaxIconWidth;
            }

            @Override
            public int getIconHeight() {
                return myMaxIconHeight;
            }
        }

        private Presentation getPresentation( AnAction action) {
            Presentation presentation = myAction2presentation.get(action);
            if (presentation == null) {
                presentation = action.getTemplatePresentation().clone();
                myAction2presentation.put(action, presentation);
            }
            return presentation;
        }
    }

    
    @Override
    public BalloonBuilder createBalloonBuilder( final JComponent content) {
        return new BalloonPopupBuilderImpl(myStorage, content);
    }

    
    @Override
    public BalloonBuilder createDialogBalloonBuilder( JComponent content, String title) {
        final BalloonPopupBuilderImpl builder = new BalloonPopupBuilderImpl(myStorage, content);
        final Color bg = UIManager.getColor("Panel.background");
        final Color borderOriginal = Color.darkGray;
        final Color border = ColorUtil.toAlpha(borderOriginal, 75);
        builder
                .setDialogMode(true)
                .setTitle(title)
                .setAnimationCycle(200)
                .setFillColor(bg)
                .setBorderColor(border)
                .setHideOnClickOutside(false)
                .setHideOnKeyOutside(false)
                .setHideOnAction(false)
                .setCloseButtonEnabled(true)
                .setShadow(true);

        return builder;
    }

    
    @Override
    public BalloonBuilder createHtmlTextBalloonBuilder( final String htmlContent,  final Icon icon, final Color fillColor,
                                                        final HyperlinkListener listener)
    {


        JEditorPane text = IdeTooltipManager.initPane(htmlContent, new HintHint().setAwtTooltip(true), null);

        if (listener != null) {
            text.addHyperlinkListener(listener);
        }
        text.setEditable(false);
        NonOpaquePanel.setTransparent(text);
        text.setBorder(null);


        JLabel label = new JLabel();
        final JPanel content = new NonOpaquePanel(new BorderLayout((int)(label.getIconTextGap() * 1.5), (int)(label.getIconTextGap() * 1.5)));

        final NonOpaquePanel textWrapper = new NonOpaquePanel(new GridBagLayout());
        JScrollPane scrolledText = new JScrollPane(text);
        scrolledText.setBackground(fillColor);
        scrolledText.getViewport().setBackground(fillColor);
        scrolledText.getViewport().setBorder(null);
        scrolledText.setBorder(null);
        textWrapper.add(scrolledText);
        content.add(textWrapper, BorderLayout.CENTER);

        final NonOpaquePanel north = new NonOpaquePanel(new BorderLayout());
        north.add(new JLabel(icon), BorderLayout.NORTH);
        content.add(north, BorderLayout.WEST);

        content.setBorder(new EmptyBorder(2, 4, 2, 4));

        final BalloonBuilder builder = createBalloonBuilder(content);

        builder.setFillColor(fillColor);

        return builder;
    }

    
    @Override
    public BalloonBuilder createHtmlTextBalloonBuilder( String htmlContent,
                                                       MessageType messageType,
                                                        HyperlinkListener listener)
    {
        return createHtmlTextBalloonBuilder(htmlContent, messageType.getDefaultIcon(), messageType.getPopupBackground(), listener);
    }
}
