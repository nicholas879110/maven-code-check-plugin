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

package com.gome.maven.codeInsight.intention.impl;

import com.gome.maven.codeInsight.CodeInsightBundle;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.daemon.impl.ShowIntentionsPass;
import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.codeInsight.hint.HintManagerImpl;
import com.gome.maven.codeInsight.hint.PriorityQuestionAction;
import com.gome.maven.codeInsight.hint.ScrollAwareHint;
import com.gome.maven.codeInsight.intention.HighPriorityAction;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInsight.intention.impl.config.IntentionActionWrapper;
import com.gome.maven.codeInsight.intention.impl.config.IntentionManagerSettings;
import com.gome.maven.codeInsight.intention.impl.config.IntentionSettingsConfigurable;
import com.gome.maven.codeInsight.unwrap.ScopeHighlighter;
import com.gome.maven.codeInspection.SuppressIntentionActionFromFix;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.VisualPosition;
import com.gome.maven.openapi.editor.actions.EditorActionUtil;
import com.gome.maven.openapi.editor.colors.EditorColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.event.EditorFactoryAdapter;
import com.gome.maven.openapi.editor.event.EditorFactoryEvent;
import com.gome.maven.openapi.keymap.KeymapUtil;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.JBPopupListener;
import com.gome.maven.openapi.ui.popup.LightweightWindowEvent;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.refactoring.BaseRefactoringIntentionAction;
import com.gome.maven.ui.HintHint;
import com.gome.maven.ui.LightweightHint;
import com.gome.maven.ui.PopupMenuListenerAdapter;
import com.gome.maven.ui.RowIcon;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.ThreeState;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.EmptyIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 * @author Mike
 * @author Valentin
 * @author Eugene Belyaev
 * @author Konstantin Bulenkov
 * @author and me too (Chinee?)
 */
public class IntentionHintComponent extends JPanel implements Disposable, ScrollAwareHint {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.intention.impl.IntentionHintComponent.ListPopupRunnable");

    static final Icon ourInactiveArrowIcon = new EmptyIcon(AllIcons.General.ArrowDown.getIconWidth(), AllIcons.General.ArrowDown.getIconHeight());

    private static final int NORMAL_BORDER_SIZE = 6;
    private static final int SMALL_BORDER_SIZE = 4;

    private static final Border INACTIVE_BORDER = BorderFactory.createEmptyBorder(NORMAL_BORDER_SIZE, NORMAL_BORDER_SIZE, NORMAL_BORDER_SIZE, NORMAL_BORDER_SIZE);
    private static final Border INACTIVE_BORDER_SMALL = BorderFactory.createEmptyBorder(SMALL_BORDER_SIZE, SMALL_BORDER_SIZE, SMALL_BORDER_SIZE, SMALL_BORDER_SIZE);

    private static Border createActiveBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getBorderColor(), 1), BorderFactory.createEmptyBorder(NORMAL_BORDER_SIZE - 1, NORMAL_BORDER_SIZE-1, NORMAL_BORDER_SIZE-1, NORMAL_BORDER_SIZE-1));
    }

    private static  Border createActiveBorderSmall() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getBorderColor(), 1), BorderFactory.createEmptyBorder(SMALL_BORDER_SIZE-1, SMALL_BORDER_SIZE-1, SMALL_BORDER_SIZE-1, SMALL_BORDER_SIZE-1));
    }

    private static Color getBorderColor() {
        return EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.SELECTED_TEARLINE_COLOR);
    }

    private final Editor myEditor;

    private static final Alarm myAlarm = new Alarm();

    private final RowIcon myHighlightedIcon;
    private final JLabel myIconLabel;

    private final RowIcon myInactiveIcon;

    private static final int DELAY = 500;
    private final MyComponentHint myComponentHint;
    private volatile boolean myPopupShown = false;
    private boolean myDisposed = false;
    private volatile ListPopup myPopup;
    private final PsiFile myFile;

    private PopupMenuListener myOuterComboboxPopupListener;

    
    public static IntentionHintComponent showIntentionHint( Project project,
                                                            PsiFile file,
                                                            Editor editor,
                                                            ShowIntentionsPass.IntentionsInfo intentions,
                                                           boolean showExpanded) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        final Point position = getHintPosition(editor);
        return showIntentionHint(project, file, editor, intentions, showExpanded, position);
    }

    
    public static IntentionHintComponent showIntentionHint( final Project project,
                                                            PsiFile file,
                                                            final Editor editor,
                                                            ShowIntentionsPass.IntentionsInfo intentions,
                                                           boolean showExpanded,
                                                            Point position) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        final IntentionHintComponent component = new IntentionHintComponent(project, file, editor, intentions);

        component.showIntentionHintImpl(!showExpanded, position);
        Disposer.register(project, component);
        if (showExpanded) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!editor.isDisposed() && editor.getComponent().isShowing()) {
                        component.showPopup(false);
                    }
                }
            }, project.getDisposed());
        }

        return component;
    }

    
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public void dispose() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        myDisposed = true;
        myComponentHint.hide();
        super.hide();

        if (myOuterComboboxPopupListener != null) {
            final Container ancestor = SwingUtilities.getAncestorOfClass(JComboBox.class, myEditor.getContentComponent());
            if (ancestor != null) {
                ((JComboBox)ancestor).removePopupMenuListener(myOuterComboboxPopupListener);
            }

            myOuterComboboxPopupListener = null;
        }
    }

    @Override
    public void editorScrolled() {
        closePopup();
    }

    //true if actions updated, there is nothing to do
    //false if has to recreate popup, no need to reshow
    //null if has to reshow
    public Boolean updateActions( ShowIntentionsPass.IntentionsInfo intentions) {
        if (myPopup.isDisposed()) return null;
        if (!myFile.isValid()) return null;
        IntentionListStep step = (IntentionListStep)myPopup.getListStep();
        if (!step.updateActions(intentions)) {
            return Boolean.TRUE;
        }
        if (!myPopupShown) {
            return Boolean.FALSE;
        }
        return null;
    }

    // for using in tests !

    public IntentionAction getAction(int index) {
        if (myPopup == null || myPopup.isDisposed()) {
            return null;
        }
        IntentionListStep listStep = (IntentionListStep)myPopup.getListStep();
        List<IntentionActionWithTextCaching> values = listStep.getValues();
        if (values.size() <= index) {
            return null;
        }
        return values.get(index).getAction();
    }

    public void recreate() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        IntentionListStep step = (IntentionListStep)myPopup.getListStep();
        recreateMyPopup(step);
    }

    private void showIntentionHintImpl(final boolean delay, final Point position) {
        final int offset = myEditor.getCaretModel().getOffset();

        myComponentHint.setShouldDelay(delay);

        HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();

        PriorityQuestionAction action = new PriorityQuestionAction() {
            @Override
            public boolean execute() {
                showPopup(false);
                return true;
            }

            @Override
            public int getPriority() {
                return -10;
            }
        };
        if (hintManager.canShowQuestionAction(action)) {
            hintManager.showQuestionHint(myEditor, position, offset, offset, myComponentHint, action, HintManager.ABOVE);
        }
    }

    
    private static Point getHintPosition(Editor editor) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return new Point();
        final int offset = editor.getCaretModel().getOffset();
        final VisualPosition pos = editor.offsetToVisualPosition(offset);
        int line = pos.line;

        final Point position = editor.visualPositionToXY(new VisualPosition(line, 0));
        LOG.assertTrue(editor.getComponent().isDisplayable());

        JComponent convertComponent = editor.getContentComponent();

        Point realPoint;
        final boolean oneLineEditor = editor.isOneLineMode();
        if (oneLineEditor) {
            // place bulb at the corner of the surrounding component
            final JComponent contentComponent = editor.getContentComponent();
            Container ancestorOfClass = SwingUtilities.getAncestorOfClass(JComboBox.class, contentComponent);

            if (ancestorOfClass != null) {
                convertComponent = (JComponent) ancestorOfClass;
            } else {
                ancestorOfClass = SwingUtilities.getAncestorOfClass(JTextField.class, contentComponent);
                if (ancestorOfClass != null) {
                    convertComponent = (JComponent) ancestorOfClass;
                }
            }

            realPoint = new Point(- (AllIcons.Actions.RealIntentionBulb.getIconWidth() / 2) - 4, - (AllIcons.Actions.RealIntentionBulb
                    .getIconHeight() / 2));
        } else {
            // try to place bulb on the same line
            final int borderHeight = NORMAL_BORDER_SIZE;

            int yShift = -(NORMAL_BORDER_SIZE + AllIcons.Actions.RealIntentionBulb.getIconHeight());
            if (canPlaceBulbOnTheSameLine(editor)) {
                yShift = -(borderHeight + (AllIcons.Actions.RealIntentionBulb.getIconHeight() - editor.getLineHeight()) /2 + 3);
            }

            final int xShift = AllIcons.Actions.RealIntentionBulb.getIconWidth();

            Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
            realPoint = new Point(Math.max(0,visibleArea.x - xShift), position.y + yShift);
        }

        Point location = SwingUtilities.convertPoint(convertComponent, realPoint, editor.getComponent().getRootPane().getLayeredPane());
        return new Point(location.x, location.y);
    }

    private static boolean canPlaceBulbOnTheSameLine(Editor editor) {
        if (ApplicationManager.getApplication().isUnitTestMode() || editor.isOneLineMode()) return false;
        final int offset = editor.getCaretModel().getOffset();
        final VisualPosition pos = editor.offsetToVisualPosition(offset);
        int line = pos.line;

        final int firstNonSpaceColumnOnTheLine = EditorActionUtil.findFirstNonSpaceColumnOnTheLine(editor, line);
        if (firstNonSpaceColumnOnTheLine == -1) return false;
        final Point point = editor.visualPositionToXY(new VisualPosition(line, firstNonSpaceColumnOnTheLine));
        return point.x > AllIcons.Actions.RealIntentionBulb.getIconWidth() + (editor.isOneLineMode() ? SMALL_BORDER_SIZE : NORMAL_BORDER_SIZE) * 2;
    }

    private IntentionHintComponent( Project project,
                                    PsiFile file,
                                    final Editor editor,
                                    ShowIntentionsPass.IntentionsInfo intentions) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        myFile = file;
        myEditor = editor;

        setLayout(new BorderLayout());
        setOpaque(false);

        boolean showRefactoringsBulb = ContainerUtil.exists(intentions.inspectionFixesToShow, new Condition<HighlightInfo.IntentionActionDescriptor>() {
            @Override
            public boolean value(HighlightInfo.IntentionActionDescriptor descriptor) {
                return descriptor.getAction() instanceof BaseRefactoringIntentionAction;
            }
        });
        boolean showFix = !showRefactoringsBulb && ContainerUtil.exists(intentions.errorFixesToShow, new Condition<HighlightInfo.IntentionActionDescriptor>() {
            @Override
            public boolean value(HighlightInfo.IntentionActionDescriptor descriptor) {
                return IntentionManagerSettings.getInstance().isShowLightBulb(descriptor.getAction());
            }
        });

        Icon smartTagIcon = showRefactoringsBulb ? AllIcons.Actions.RefactoringBulb : showFix ? AllIcons.Actions.QuickfixBulb : AllIcons.Actions.IntentionBulb;

        myHighlightedIcon = new RowIcon(2);
        myHighlightedIcon.setIcon(smartTagIcon, 0);
        myHighlightedIcon.setIcon(AllIcons.General.ArrowDown, 1);

        myInactiveIcon = new RowIcon(2);
        myInactiveIcon.setIcon(smartTagIcon, 0);
        myInactiveIcon.setIcon(ourInactiveArrowIcon, 1);

        myIconLabel = new JLabel(myInactiveIcon);
        myIconLabel.setOpaque(false);

        add(myIconLabel, BorderLayout.CENTER);

        setBorder(editor.isOneLineMode() ? INACTIVE_BORDER_SMALL : INACTIVE_BORDER);

        myIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON1) {
                    showPopup(true);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                onMouseEnter(editor.isOneLineMode());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                onMouseExit(editor.isOneLineMode());
            }
        });

        myComponentHint = new MyComponentHint(this);
        IntentionListStep step = new IntentionListStep(this, intentions, myEditor, myFile, project);
        recreateMyPopup(step);
        // dispose myself when editor closed
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
            @Override
            public void editorReleased( EditorFactoryEvent event) {
                if (event.getEditor() == myEditor) {
                    hide();
                }
            }
        }, this);
    }

    @Override
    public void hide() {
        Disposer.dispose(this);
    }

    private void onMouseExit(final boolean small) {
        Window ancestor = SwingUtilities.getWindowAncestor(myPopup.getContent());
        if (ancestor == null) {
            myIconLabel.setIcon(myInactiveIcon);
            setBorder(small ? INACTIVE_BORDER_SMALL : INACTIVE_BORDER);
        }
    }

    private void onMouseEnter(final boolean small) {
        myIconLabel.setIcon(myHighlightedIcon);
        setBorder(small ? createActiveBorderSmall() : createActiveBorder());

        String acceleratorsText = KeymapUtil.getFirstKeyboardShortcutText(
                ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
        if (!acceleratorsText.isEmpty()) {
            myIconLabel.setToolTipText(CodeInsightBundle.message("lightbulb.tooltip", acceleratorsText));
        }
    }

    
    public LightweightHint getComponentHint() {
        return myComponentHint;
    }

    private void closePopup() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        myPopup.cancel();
        myPopupShown = false;
    }

    private void showPopup(boolean mouseClick) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (myPopup == null || myPopup.isDisposed()) return;

        if (mouseClick && isShowing()) {
            final RelativePoint swCorner = RelativePoint.getSouthWestOf(this);
            final int yOffset = canPlaceBulbOnTheSameLine(myEditor) ? 0 : myEditor.getLineHeight() - (myEditor.isOneLineMode() ? SMALL_BORDER_SIZE : NORMAL_BORDER_SIZE);
            myPopup.show(new RelativePoint(swCorner.getComponent(), new Point(swCorner.getPoint().x, swCorner.getPoint().y + yOffset)));
        }
        else {
            myPopup.showInBestPositionFor(myEditor);
        }

        myPopupShown = true;
    }

    private void recreateMyPopup( IntentionListStep step) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (myPopup != null) {
            Disposer.dispose(myPopup);
        }
        myPopup = JBPopupFactory.getInstance().createListPopup(step);

        boolean committed = PsiDocumentManager.getInstance(myFile.getProject()).isCommitted(myEditor.getDocument());
        final PsiFile injectedFile = committed ? InjectedLanguageUtil.findInjectedPsiNoCommit(myFile, myEditor.getCaretModel().getOffset()) : null;
        final Editor injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(myEditor, injectedFile);

        final ScopeHighlighter highlighter = new ScopeHighlighter(myEditor);
        final ScopeHighlighter injectionHighlighter = new ScopeHighlighter(injectedEditor);

        myPopup.addListener(new JBPopupListener.Adapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                highlighter.dropHighlight();
                injectionHighlighter.dropHighlight();
                myPopupShown = false;
            }
        });
        myPopup.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final Object source = e.getSource();
                highlighter.dropHighlight();
                injectionHighlighter.dropHighlight();

                if (source instanceof DataProvider) {
                    final Object selectedItem = PlatformDataKeys.SELECTED_ITEM.getData((DataProvider)source);
                    if (selectedItem instanceof IntentionActionWithTextCaching) {
                        final IntentionAction action = ((IntentionActionWithTextCaching)selectedItem).getAction();
                        if (action instanceof SuppressIntentionActionFromFix) {
                            if (injectedFile != null && ((SuppressIntentionActionFromFix)action).isShouldBeAppliedToInjectionHost() == ThreeState.NO) {
                                final PsiElement at = injectedFile.findElementAt(injectedEditor.getCaretModel().getOffset());
                                final PsiElement container = ((SuppressIntentionActionFromFix)action).getContainer(at);
                                if (container != null) {
                                    injectionHighlighter.highlight(container, Collections.singletonList(container));
                                }
                            }
                            else {
                                final PsiElement at = myFile.findElementAt(myEditor.getCaretModel().getOffset());
                                final PsiElement container = ((SuppressIntentionActionFromFix)action).getContainer(at);
                                if (container != null) {
                                    highlighter.highlight(container, Collections.singletonList(container));
                                }
                            }
                        }
                    }
                }
            }
        });

        if (myEditor.isOneLineMode()) {
            // hide popup on combobox popup show
            final Container ancestor = SwingUtilities.getAncestorOfClass(JComboBox.class, myEditor.getContentComponent());
            if (ancestor != null) {
                final JComboBox comboBox = (JComboBox)ancestor;
                myOuterComboboxPopupListener = new PopupMenuListenerAdapter() {
                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                        hide();
                    }
                };

                comboBox.addPopupMenuListener(myOuterComboboxPopupListener);
            }
        }

        Disposer.register(this, myPopup);
        Disposer.register(myPopup, new Disposable() {
            @Override
            public void dispose() {
                ApplicationManager.getApplication().assertIsDispatchThread();
            }
        });
    }

    void canceled( IntentionListStep intentionListStep) {
        if (myPopup.getListStep() != intentionListStep || myDisposed) {
            return;
        }
        // Root canceled. Create new popup. This one cannot be reused.
        recreateMyPopup(intentionListStep);
    }

    private static class MyComponentHint extends LightweightHint {
        private boolean myVisible = false;
        private boolean myShouldDelay;

        private MyComponentHint(JComponent component) {
            super(component);
        }

        @Override
        public void show( final JComponent parentComponent,
                         final int x,
                         final int y,
                         final JComponent focusBackComponent,
                          HintHint hintHint) {
            myVisible = true;
            if (myShouldDelay) {
                myAlarm.cancelAllRequests();
                myAlarm.addRequest(new Runnable() {
                    @Override
                    public void run() {
                        showImpl(parentComponent, x, y, focusBackComponent);
                    }
                }, DELAY);
            }
            else {
                showImpl(parentComponent, x, y, focusBackComponent);
            }
        }

        private void showImpl(JComponent parentComponent, int x, int y, JComponent focusBackComponent) {
            if (!parentComponent.isShowing()) return;
            super.show(parentComponent, x, y, focusBackComponent, new HintHint(parentComponent, new Point(x, y)));
        }

        @Override
        public void hide() {
            super.hide();
            myVisible = false;
            myAlarm.cancelAllRequests();
        }

        @Override
        public boolean isVisible() {
            return myVisible || super.isVisible();
        }

        public void setShouldDelay(boolean shouldDelay) {
            myShouldDelay = shouldDelay;
        }
    }

    public static class EnableDisableIntentionAction extends AbstractEditIntentionSettingsAction {
        private final IntentionManagerSettings mySettings = IntentionManagerSettings.getInstance();
        private final IntentionAction myAction;

        public EnableDisableIntentionAction(IntentionAction action) {
            super(action);
            myAction = action;
            // needed for checking errors in user written actions
            //noinspection ConstantConditions
            LOG.assertTrue(myFamilyName != null, "action "+action.getClass()+" family returned null");
        }

        @Override
        
        public String getText() {
            return mySettings.isEnabled(myAction) ?
                    CodeInsightBundle.message("disable.intention.action", myFamilyName) :
                    CodeInsightBundle.message("enable.intention.action", myFamilyName);
        }

        @Override
        public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            mySettings.setEnabled(myAction, !mySettings.isEnabled(myAction));
        }

        @Override
        public String toString() {
            return getText();
        }
    }

    public static class EditIntentionSettingsAction extends AbstractEditIntentionSettingsAction implements HighPriorityAction {
        public EditIntentionSettingsAction(IntentionAction action) {
            super(action);
        }

        
        @Override
        public String getText() {
            return "Edit intention settings";
        }

        @Override
        public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            final IntentionSettingsConfigurable configurable = new IntentionSettingsConfigurable();
            ShowSettingsUtil.getInstance().editConfigurable(project, configurable, new Runnable() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            configurable.selectIntention(myFamilyName);
                        }
                    });
                }
            });
        }
    }

    private static abstract class AbstractEditIntentionSettingsAction implements IntentionAction {
        protected final String myFamilyName;
        private final boolean myDisabled;

        public AbstractEditIntentionSettingsAction(IntentionAction action) {
            myFamilyName = action.getFamilyName();
            myDisabled = action instanceof IntentionActionWrapper &&
                    Comparing.equal(action.getFamilyName(), ((IntentionActionWrapper)action).getFullFamilyName());
        }

        
        @Override
        public String getFamilyName() {
            return getText();
        }

        @Override
        public boolean isAvailable( Project project, Editor editor, PsiFile file) {
            return !myDisabled;
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
