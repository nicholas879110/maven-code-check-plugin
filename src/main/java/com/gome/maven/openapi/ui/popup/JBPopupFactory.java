/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package com.gome.maven.openapi.ui.popup;

import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.ui.awt.RelativePoint;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;

/**
 * Factory class for creating popup chooser windows (similar to the Code | Generate... popup) and various notifications/confirmations.
 *
 * @author mike
 * @since 6.0
 */
public abstract class JBPopupFactory {
    /**
     * Returns the popup factory instance.
     *
     * @return the popup factory instance.
     */
    public static JBPopupFactory getInstance() {
        return ServiceManager.getService(JBPopupFactory.class);
    }

    
    public PopupChooserBuilder createListPopupBuilder( JList list) {
        return new PopupChooserBuilder(list);
    }

    /**
     * Creates a popup with the specified title and two options, Yes and No.
     *
     * @param title the title of the popup.
     * @param onYes the runnable which is executed when the Yes option is selected.
     * @param defaultOptionIndex the index of the option which is selected by default.
     * @return the popup instance.
     */
    
    public abstract ListPopup createConfirmation(String title, Runnable onYes, int defaultOptionIndex);

    /**
     * Creates a popup allowing to choose one of two specified options and execute code when one of them is selected.
     *
     * @param title the title of the popup.
     * @param yesText the title for the Yes option.
     * @param noText the title for the No option.
     * @param onYes the runnable which is executed when the Yes option is selected.
     * @param defaultOptionIndex the index of the option which is selected by default.
     * @return the popup instance.
     */
    
    public abstract ListPopup createConfirmation(String title, String yesText, String noText, Runnable onYes, int defaultOptionIndex);

    /**
     * Creates a popup allowing to choose one of two specified options and execute code when either of them is selected.
     *
     * @param title the title of the popup.
     * @param yesText the title for the Yes option.
     * @param noText the title for the No option.
     * @param onYes the runnable which is executed when the Yes option is selected.
     * @param onNo the runnable which is executed when the No option is selected.
     * @param defaultOptionIndex the index of the option which is selected by default.
     * @return the popup instance.
     */
    
    public abstract ListPopup createConfirmation(String title,
                                                 String yesText,
                                                 String noText,
                                                 Runnable onYes,
                                                 Runnable onNo,
                                                 int defaultOptionIndex);

    
    public abstract ListPopupStep createActionsStep( ActionGroup actionGroup,
                                                     DataContext dataContext,
                                                    boolean showNumbers,
                                                    boolean showDisabledActions,
                                                    String title,
                                                    Component component,
                                                    boolean honorActionMnemonics);

    
    public abstract ListPopupStep createActionsStep( ActionGroup actionGroup,
                                                     DataContext dataContext,
                                                    boolean showNumbers,
                                                    boolean showDisabledActions,
                                                    String title,
                                                    Component component,
                                                    boolean honorActionMnemonics,
                                                    int defaultOptionIndex, final boolean autoSelectionEnabled);

    
    public abstract RelativePoint guessBestPopupLocation( JComponent component);

    public boolean isChildPopupFocused( Component parent) {
        return getChildFocusedPopup(parent) != null;
    }

    public JBPopup getChildFocusedPopup( Component parent) {
        if (parent == null) return null;
        List<JBPopup> popups = getChildPopups(parent);
        for (JBPopup each : popups) {
            if (each.isFocused()) return each;
            JBPopup childFocusedPopup = getChildFocusedPopup(each.getContent());
            if (childFocusedPopup != null) {
                return childFocusedPopup;
            }
        }
        return null;
    }

    /**
     * Possible ways to select actions in a popup from keyboard.
     */
    public enum ActionSelectionAid {
        /**
         * The actions in a popup are prefixed by numbers (indexes in the list).
         */
        NUMBERING,

        /**
         * Same as numbering, but will allow A-Z 'numbers' when out of 0-9 range.
         */
        ALPHA_NUMBERING,

        /**
         * The actions in a popup can be selected by typing part of the action's text.
         */
        SPEEDSEARCH,

        /**
         * The actions in a popup can be selected by pressing the character from the action's text prefixed with
         * an &amp; character.
         */
        MNEMONICS
    }

    /**
     * Creates a popup allowing to choose one of the actions from the specified action group.
     *
     * @param title the title of the popup.
     * @param actionGroup the action group from which the popup is built.
     * @param dataContext the data context which provides the data for the selected action
     * @param selectionAidMethod keyboard selection mode for actions in the popup.
     * @param showDisabledActions if true, disabled actions are shown as disabled; if false, disabled actions are not shown
     * @return the popup instance.
     */
    
    public abstract ListPopup createActionGroupPopup(  String title,
                                                      ActionGroup actionGroup,
                                                      DataContext dataContext,
                                                     ActionSelectionAid selectionAidMethod,
                                                     boolean showDisabledActions);

    /**
     * Creates a popup allowing to choose one of the actions from the specified action group.
     *
     * @param title the title of the popup.
     * @param actionGroup the action group from which the popup is built.
     * @param dataContext the data context which provides the data for the selected action
     * @param selectionAidMethod keyboard selection mode for actions in the popup.
     * @param showDisabledActions if true, disabled actions are shown as disabled; if false, disabled actions are not shown
     * @param actionPlace action place for ActionManager to use when creating the popup
     * @return the popup instance.
     */
    
    public abstract ListPopup createActionGroupPopup(String title,
                                                      ActionGroup actionGroup,
                                                      DataContext dataContext,
                                                     ActionSelectionAid selectionAidMethod,
                                                     boolean showDisabledActions,  final String actionPlace);

    /**
     * Creates a popup allowing to choose one of the actions from the specified action group.
     *
     * @param title the title of the popup.
     * @param actionGroup the action group from which the popup is built.
     * @param dataContext the data context which provides the data for the selected action
     * @param selectionAidMethod keyboard selection mode for actions in the popup.
     * @param showDisabledActions if true, disabled actions are shown as disabled; if false, disabled actions are not shown
     * @param disposeCallback method which is called when the popup is closed (either by selecting an action or by canceling)
     * @param maxRowCount maximum number of popup rows visible at once (if there are more actions in the action group, a scrollbar
     *                    is displayed)
     * @return the popup instance.
     */
    
    public abstract ListPopup createActionGroupPopup( String title,
                                                      ActionGroup actionGroup,
                                                      DataContext dataContext,
                                                     ActionSelectionAid selectionAidMethod,
                                                     boolean showDisabledActions,
                                                      Runnable disposeCallback,
                                                     int maxRowCount);

    
    public abstract ListPopup createActionGroupPopup( String title,
                                                      ActionGroup actionGroup,
                                                      DataContext dataContext,
                                                     boolean showNumbers,
                                                     boolean showDisabledActions,
                                                     boolean honorActionMnemonics,
                                                      Runnable disposeCallback,
                                                     int maxRowCount,
                                                      Condition<AnAction> preselectActionCondition);

    /**
     * @deprecated use {@link #createListPopup(ListPopupStep)} instead (<code>step</code> must be a ListPopupStep in any case)
     */
    
    public abstract ListPopup createWizardStep( PopupStep step);

    /**
     * Creates a custom list popup with the specified step.
     *
     * @param step the custom step for the list popup.
     * @return the popup instance.
     */
    
    public abstract ListPopup createListPopup( ListPopupStep step);

    /**
     * Creates a custom list popup with the specified step.
     *
     * @param step        the custom step for the list popup.
     * @param maxRowCount the number of visible rows to show in the popup (if the popup has more items,
     *                    a scrollbar will be displayed).
     * @return the popup instance.
     * @since 14.1
     */
    
    public abstract ListPopup createListPopup( ListPopupStep step, int maxRowCount);

    
    public abstract TreePopup createTree(JBPopup parent,  TreePopupStep step, Object parentValue);
    
    public abstract TreePopup createTree( TreePopupStep step);

    
    public abstract ComponentPopupBuilder createComponentPopupBuilder( JComponent content,  JComponent preferableFocusComponent);

    /**
     * Returns the location where a popup with the specified data context is displayed.
     *
     * @param dataContext the data context from which the location is determined.
     * @return location as close as possible to the action origin. Method has special handling of
     *         the following components:<br>
     *         - caret offset for editor<br>
     *         - current selected node for tree<br>
     *         - current selected row for list<br>
     */
    
    public abstract RelativePoint guessBestPopupLocation( DataContext dataContext);

    /**
     * Returns the location where a popup invoked from the specified editor should be displayed.
     *
     * @param editor the editor over which the popup is shown.
     * @return location as close as possible to the action origin.
     */
    
    public abstract RelativePoint guessBestPopupLocation( Editor editor);

    /**
     * @param editor the editor over which the popup is shown.
     * @return true if popup location is located in visible area
     *         false if center would be suggested instead
     */
    public abstract boolean isBestPopupLocationVisible( Editor editor);

    public abstract Point getCenterOf(JComponent container, JComponent content);

    
    public abstract List<JBPopup> getChildPopups( Component parent);

    public abstract boolean isPopupActive();

    
    public abstract BalloonBuilder createBalloonBuilder( JComponent content);

    
    public abstract BalloonBuilder createDialogBalloonBuilder( JComponent content, String title);

    
    public abstract BalloonBuilder createHtmlTextBalloonBuilder( String htmlContent,  Icon icon, Color fillColor,  HyperlinkListener listener);

    
    public abstract BalloonBuilder createHtmlTextBalloonBuilder( String htmlContent, MessageType messageType,  HyperlinkListener listener);

    
    public abstract JBPopup createMessage(String text);

    
    public abstract Balloon getParentBalloonFor( Component c);

}
