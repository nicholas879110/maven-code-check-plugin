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
package com.gome.maven.openapi.wm.impl.status;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.notification.impl.IdeNotificationArea;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.TaskInfo;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.popup.BalloonHandler;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.wm.*;
import com.gome.maven.openapi.wm.ex.ProgressIndicatorEx;
import com.gome.maven.openapi.wm.ex.StatusBarEx;
import com.gome.maven.ui.ClickListener;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.ui.popup.NotificationPopup;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.ui.JBUI;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * User: spLeaner
 */
public class IdeStatusBarImpl extends JComponent implements StatusBarEx {
    private static final int MIN_ICON_HEIGHT = 18 + 1 + 1;
    private final InfoAndProgressPanel myInfoAndProgressPanel;
    private IdeFrame myFrame;

    private enum Position {LEFT, RIGHT, CENTER}

    private static final String uiClassID = "IdeStatusBarUI";

    private final Map<String, WidgetBean> myWidgetMap = new HashMap<String, WidgetBean>();
    private final List<String> myOrderedWidgets = new ArrayList<String>();

    private JPanel myLeftPanel;
    private JPanel myRightPanel;
    private JPanel myCenterPanel;

    private String myInfo;
    private String myRequestor;

    private final List<String> myCustomComponentIds = new ArrayList<String>();

    private final Set<IdeStatusBarImpl> myChildren = new HashSet<IdeStatusBarImpl>();
    //private ToolWindowsWidget myToolWindowWidget;

    private static class WidgetBean {
        JComponent component;
        Position position;
        StatusBarWidget widget;
        String anchor;

        static WidgetBean create( final StatusBarWidget widget,
                                  final Position position,
                                  final JComponent component,
                                  String anchor) {
            final WidgetBean bean = new WidgetBean();
            bean.widget = widget;
            bean.position = position;
            bean.component = component;
            bean.anchor = anchor;
            return bean;
        }
    }

    @Override
    public StatusBar findChild(Component c) {
        Component eachParent = c;
        IdeFrame frame = null;
        while (eachParent != null) {
            if (eachParent instanceof IdeFrame) {
                frame = (IdeFrame)eachParent;
            }
            eachParent = eachParent.getParent();
        }

        return frame != null ? frame.getStatusBar() : this;
    }

    @Override
    public void install(IdeFrame frame) {
        myFrame = frame;
    }

    private void updateChildren(ChildAction action) {
        for (IdeStatusBarImpl child : myChildren) {
            action.update(child);
        }
    }

    interface ChildAction {
        void update(IdeStatusBarImpl child);
    }

    @Override
    public StatusBar createChild() {
        final IdeStatusBarImpl bar = new IdeStatusBarImpl(this);
        myChildren.add(bar);
        Disposer.register(bar, new Disposable() {
            @Override
            public void dispose() {
                myChildren.remove(bar);
            }
        });

        for (String eachId : myOrderedWidgets) {
            WidgetBean eachBean = myWidgetMap.get(eachId);
            if (eachBean.widget instanceof StatusBarWidget.Multiframe) {
                StatusBarWidget copy = ((StatusBarWidget.Multiframe)eachBean.widget).copy();
                bar.addWidget(copy, eachBean.position);
            }
        }

        return bar;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    IdeStatusBarImpl( IdeStatusBarImpl master) {
        setLayout(new BorderLayout(JBUI.scale(2), 0));
        setBorder(JBUI.Borders.empty(1, 4, 0, SystemInfo.isMac ? 2 : 0));

        myInfoAndProgressPanel = new InfoAndProgressPanel();
        addWidget(myInfoAndProgressPanel, Position.CENTER);

        setOpaque(true);
        updateUI();

        if (master == null) {
            Disposer.register(Disposer.get("ui"), this);
        }

        if (master == null) {
            addWidget(new ToolWindowsWidget(this), Position.LEFT);
        }

        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }


    public IdeStatusBarImpl() {
        this(null);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (size == null) return null;

        Insets insets = getInsets();
        int minHeight = insets.top + insets.bottom + MIN_ICON_HEIGHT;
        return new Dimension(size.width, Math.max(size.height, minHeight));
    }

    @Override
    public void addWidget( final StatusBarWidget widget) {
        addWidget(widget, Position.RIGHT, "__AUTODETECT__");
    }

    @Override
    public void addWidget( final StatusBarWidget widget,  String anchor) {
        addWidget(widget, Position.RIGHT, anchor);
    }

    private void addWidget( final StatusBarWidget widget,  final Position pos) {
        addWidget(widget, pos, "__IGNORED__");
    }

    @Override
    public void addWidget( final StatusBarWidget widget,  final Disposable parentDisposable) {
        addWidget(widget);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removeWidget(widget.ID());
            }
        });
    }

    @Override
    public void addWidget( final StatusBarWidget widget,  String anchor,  final Disposable parentDisposable) {
        addWidget(widget, anchor);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removeWidget(widget.ID());
            }
        });
    }

    @Override
    public void removeCustomIndicationComponents() {
        for (final String id : myCustomComponentIds) {
            removeWidget(id);
        }

        myCustomComponentIds.clear();
    }

    @Override
    public void addCustomIndicationComponent( final JComponent c) {
        final String customId = c.getClass().getName() + new Random().nextLong();
        addWidget(new CustomStatusBarWidget() {
            @Override
            
            public String ID() {
                return customId;
            }

            @Override
            
            public WidgetPresentation getPresentation( PlatformType type) {
                return null;
            }

            @Override
            public void install( StatusBar statusBar) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public JComponent getComponent() {
                return c;
            }
        });

        myCustomComponentIds.add(customId);
    }

    //@Override
    //protected void processMouseMotionEvent(MouseEvent e) {
    //  final Point point = e.getPoint();
    //  if (myToolWindowWidget != null) {
    //    if(point.x < 42 && 0 <= point.y && point.y <= getHeight()) {
    //      myToolWindowWidget.mouseEntered();
    //    } else {
    //      myToolWindowWidget.mouseExited();
    //    }
    //  }
    //  super.processMouseMotionEvent(e);
    //}

    //@Override
    //protected void processMouseEvent(MouseEvent e) {
    //  if (e.getID() == MouseEvent.MOUSE_EXITED && myToolWindowWidget != null) {
    //    if (!new Rectangle(0,0,22, getHeight()).contains(e.getPoint())) {
    //      myToolWindowWidget.mouseExited();
    //    }
    //  }
    //  super.processMouseEvent(e);
    //}

    @Override
    public void removeCustomIndicationComponent( final JComponent c) {
        final Set<String> keySet = myWidgetMap.keySet();
        final String[] keys = ArrayUtil.toStringArray(keySet);
        for (final String key : keys) {
            final WidgetBean value = myWidgetMap.get(key);
            if (value.widget instanceof CustomStatusBarWidget && value.component == c) {
                removeWidget(key);
                myCustomComponentIds.remove(key);
            }
        }
    }

    @Override
    public void dispose() {
        myWidgetMap.clear();
        myChildren.clear();

        if (myLeftPanel != null) myLeftPanel.removeAll();
        if (myRightPanel != null) myRightPanel.removeAll();
        if (myCenterPanel != null) myCenterPanel.removeAll();
    }

    private void addWidget( final StatusBarWidget widget,  final Position pos,  final String anchor) {
        myOrderedWidgets.add(widget.ID());

        JPanel panel;
        if (pos == Position.RIGHT) {
            if (myRightPanel == null) {
                myRightPanel = new JPanel();
                myRightPanel.setLayout(new BoxLayout(myRightPanel, BoxLayout.X_AXIS));
                myRightPanel.setOpaque(false);
                add(myRightPanel, BorderLayout.EAST);
            }

            panel = myRightPanel;
        }
        else if (pos == Position.LEFT) {
            if (myLeftPanel == null) {
                myLeftPanel = new JPanel();
                myLeftPanel.setLayout(new BoxLayout(myLeftPanel, BoxLayout.X_AXIS));
                myLeftPanel.setOpaque(false);
                add(myLeftPanel, BorderLayout.WEST);
            }

            panel = myLeftPanel;
        }
        else {
            if (myCenterPanel == null) {
                myCenterPanel = new JPanel(new BorderLayout());
                myCenterPanel.setOpaque(false);
                add(myCenterPanel, BorderLayout.CENTER);
            }

            panel = myCenterPanel;
        }

        final JComponent c = wrap(widget);
        if (Position.RIGHT == pos && panel.getComponentCount() > 0) {
            String wid;
            boolean before;
            if (!anchor.equals("__AUTODETECT__")) {
                final List<String> parts = StringUtil.split(anchor, " ");
                if (parts.size() < 2 || !myWidgetMap.keySet().contains(parts.get(1))) {
                    wid = IdeNotificationArea.WIDGET_ID;
                    before = true;
                }
                else {
                    wid = parts.get(1);
                    before = "before".equalsIgnoreCase(parts.get(0));
                }
            }
            else {
                wid = IdeNotificationArea.WIDGET_ID;
                before = true;
            }

            for (final String id : myWidgetMap.keySet()) {
                if (id.equalsIgnoreCase(wid)) {
                    final WidgetBean bean = myWidgetMap.get(id);
                    int i = 0;
                    for (final Component component : myRightPanel.getComponents()) {
                        if (component == bean.component) {
                            if (before) {
                                panel.add(c, i);
                                updateBorder(i);
                            }
                            else {
                                final int ndx = i + 1;
                                panel.add(c, i + 1);
                                updateBorder(ndx);
                            }

                            installWidget(widget, pos, c, anchor);
                            return;
                        }

                        i++;
                    }
                }
            }
        }

        if (Position.LEFT == pos && panel.getComponentCount() == 0) {
            c.setBorder(SystemInfo.isMac ? JBUI.Borders.empty(2, 0, 2, 4) : JBUI.Borders.empty());
        }

        panel.add(c);
        installWidget(widget, pos, c, anchor);

        if (widget instanceof StatusBarWidget.Multiframe) {
            final StatusBarWidget.Multiframe mfw = (StatusBarWidget.Multiframe)widget;
            updateChildren(new ChildAction() {
                @Override
                public void update(IdeStatusBarImpl child) {
                    StatusBarWidget widgetCopy = mfw.copy();
                    child.addWidget(widgetCopy, pos, anchor);
                }
            });
        }
    }

    private void updateBorder(int ndx) {
        if (myRightPanel.getComponentCount() == 0) return;
        if (ndx >= myRightPanel.getComponentCount()) ndx--;
        if (ndx < 0) ndx++;
        final JComponent self = (JComponent)myRightPanel.getComponent(ndx);
        if (self instanceof IconPresentationWrapper || self instanceof IconLikeCustomStatusBarWidget) {
            final int prev = ndx - 1;
            final int next = ndx + 1;

            final JComponent p = prev >= 0 ? (JComponent)myRightPanel.getComponent(prev) : null;
            final JComponent n = next < myRightPanel.getComponentCount() ? (JComponent)myRightPanel.getComponent(next) : null;

            final boolean prevIcon = p instanceof IconPresentationWrapper || p instanceof IconLikeCustomStatusBarWidget;
            final boolean nextIcon = n instanceof IconPresentationWrapper || n instanceof IconLikeCustomStatusBarWidget;

            // 2peter: please do not touch it anymore :)
            self.setBorder(prevIcon ? JBUI.Borders.empty(2, 2, 2, 2) : StatusBarWidget.WidgetBorder.INSTANCE);
            if (nextIcon) n.setBorder(JBUI.Borders.empty(2, 2, 2, 2));
        }
    }

    @Override
    public void setInfo( final String s) {
        setInfo(s, null);
    }

    @Override
    public void setInfo( final String s,  final String requestor) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (myInfoAndProgressPanel != null) {
                    Couple<String> pair = myInfoAndProgressPanel.setText(s, requestor);
                    myInfo = pair.first;
                    myRequestor = pair.second;
                }
            }
        });
    }

    @Override
    public String getInfo() {
        return myInfo;
    }

    @Override
    public String getInfoRequestor() {
        return myRequestor;
    }

    @Override
    public void addProgress( ProgressIndicatorEx indicator,  TaskInfo info) {
        myInfoAndProgressPanel.addProgress(indicator, info);
    }

    @Override
    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
        return myInfoAndProgressPanel.getBackgroundProcesses();
    }

    @Override
    public void setProcessWindowOpen(final boolean open) {
        myInfoAndProgressPanel.setProcessWindowOpen(open);
    }

    @Override
    public boolean isProcessWindowOpen() {
        return myInfoAndProgressPanel.isProcessWindowOpen();
    }

    @Override
    public void startRefreshIndication(final String tooltipText) {
        myInfoAndProgressPanel.setRefreshToolTipText(tooltipText);
        myInfoAndProgressPanel.setRefreshVisible(true);

        updateChildren(new ChildAction() {
            @Override
            public void update(IdeStatusBarImpl child) {
                child.startRefreshIndication(tooltipText);
            }
        });
    }

    @Override
    public void stopRefreshIndication() {
        myInfoAndProgressPanel.setRefreshVisible(false);

        updateChildren(new ChildAction() {
            @Override
            public void update(IdeStatusBarImpl child) {
                child.stopRefreshIndication();
            }
        });
    }

    @Override
    public BalloonHandler notifyProgressByBalloon( MessageType type,  String htmlBody) {
        return notifyProgressByBalloon(type, htmlBody, null, null);
    }

    @Override
    public BalloonHandler notifyProgressByBalloon( MessageType type,
                                                   String htmlBody,
                                                   Icon icon,
                                                   HyperlinkListener listener) {
        return myInfoAndProgressPanel.notifyByBalloon(type, htmlBody, icon, listener);
    }

    @Override
    public void fireNotificationPopup( JComponent content, Color backgroundColor) {
        new NotificationPopup(this, content, backgroundColor);
    }

    private void installWidget( final StatusBarWidget widget,
                                final Position pos,
                                final JComponent c,
                               String anchor) {
        myWidgetMap.put(widget.ID(), WidgetBean.create(widget, pos, c, anchor));
        widget.install(this);
        Disposer.register(this, widget);
    }

    private static JComponent wrap( final StatusBarWidget widget) {
        if (widget instanceof CustomStatusBarWidget) return ((CustomStatusBarWidget)widget).getComponent();
        final StatusBarWidget.WidgetPresentation presentation =
                widget.getPresentation(SystemInfo.isMac ? StatusBarWidget.PlatformType.MAC : StatusBarWidget.PlatformType.DEFAULT);
        assert presentation != null : "Presentation should not be null!";

        JComponent wrapper;
        if (presentation instanceof StatusBarWidget.IconPresentation) {
            wrapper = new IconPresentationWrapper((StatusBarWidget.IconPresentation)presentation);
        }
        else if (presentation instanceof StatusBarWidget.TextPresentation) {
            wrapper = new TextPresentationWrapper((StatusBarWidget.TextPresentation)presentation);
            wrapper.setBorder(StatusBarWidget.WidgetBorder.INSTANCE);
        }
        else if (presentation instanceof StatusBarWidget.MultipleTextValuesPresentation) {
            wrapper = new MultipleTextValuesPresentationWrapper((StatusBarWidget.MultipleTextValuesPresentation)presentation);
            wrapper.setBorder(StatusBarWidget.WidgetBorder.INSTANCE);
        }
        else {
            throw new IllegalArgumentException("Unable to find a wrapper for presentation: " + presentation.getClass().getSimpleName());
        }

        return wrapper;
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    @SuppressWarnings({"MethodOverloadsMethodOfSuperclass"})
    protected void setUI(StatusBarUI ui) {
        super.setUI(ui);
    }

    @Override
    public void updateUI() {
        if (UIManager.get(getUIClassID()) != null) {
            setUI((StatusBarUI)UIManager.getUI(this));
        }
        else {
            setUI(/*SystemInfo.isMac && !UIUtil.isUnderDarcula() ? new MacStatusBarUI() :*/ new StatusBarUI());
        }
    }

    //@Override
    //protected void paintChildren(final Graphics g) {
    //  if (getUI() instanceof MacStatusBarUI && !MacStatusBarUI.isActive(this)) {
    //    final Graphics2D g2d = (Graphics2D)g.create();
    //    //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.4f));
    //    super.paintChildren(g2d);
    //    g2d.dispose();
    //  }
    //  else {
    //    super.paintChildren(g);
    //  }
    //}

    public StatusBarUI getUI() {
        return (StatusBarUI)ui;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (UIUtil.isUnderDarcula()) {
            //IDEA-112093
            g.setColor(UIUtil.getPanelBackground());
            g.drawLine(0, getHeight(), getWidth(), getHeight());
        }
    }

    @Override
    public void removeWidget( final String id) {
        final WidgetBean bean = myWidgetMap.get(id);
        if (bean != null) {
            if (Position.LEFT == bean.position) {
                myLeftPanel.remove(bean.component);
            }
            else if (Position.RIGHT == bean.position) {
                final Component[] components = myRightPanel.getComponents();
                int i = 0;
                for (final Component c : components) {
                    if (c == bean.component) break;
                    i++;
                }

                myRightPanel.remove(bean.component);
                updateBorder(i);
            }
            else {
                myCenterPanel.remove(bean.component);
            }

            myWidgetMap.remove(bean.widget.ID());
            Disposer.dispose(bean.widget);
        }

        updateChildren(new ChildAction() {
            @Override
            public void update(IdeStatusBarImpl child) {
                child.removeWidget(id);
            }
        });

        myOrderedWidgets.remove(id);
    }

    @Override
    public void updateWidgets() {
        for (final String s : myWidgetMap.keySet()) {
            updateWidget(s);
        }

        updateChildren(new ChildAction() {
            @Override
            public void update(IdeStatusBarImpl child) {
                child.updateWidgets();
            }
        });
    }

    @Override
    public void updateWidget( final String id) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                final WidgetBean bean = myWidgetMap.get(id);
                if (bean != null) {
                    if (bean.component instanceof StatusBarWrapper) {
                        ((StatusBarWrapper)bean.component).beforeUpdate();
                    }

                    bean.component.repaint();
                }

                updateChildren(new ChildAction() {
                    @Override
                    public void update(IdeStatusBarImpl child) {
                        child.updateWidget(id);
                    }
                });
            }
        });
    }

    
    public StatusBarWidget getWidget(String id) {
        WidgetBean bean = myWidgetMap.get(id);
        return bean == null ? null : bean.widget;
    }

    private interface StatusBarWrapper {
        void beforeUpdate();
    }

    private static final class MultipleTextValuesPresentationWrapper extends TextPanel implements StatusBarWrapper {
        private final StatusBarWidget.MultipleTextValuesPresentation myPresentation;

        private MultipleTextValuesPresentationWrapper( final StatusBarWidget.MultipleTextValuesPresentation presentation) {
            super();
            myPresentation = presentation;

            putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);
            setToolTipText(presentation.getTooltipText());
            new ClickListener() {
                @Override
                public boolean onClick( MouseEvent e, int clickCount) {
                    final ListPopup popup = myPresentation.getPopupStep();
                    if (popup == null) return false;
                    final Dimension dimension = popup.getContent().getPreferredSize();
                    final Point at = new Point(0, -dimension.height);
                    popup.show(new RelativePoint(e.getComponent(), at));
                    return true;
                }
            }.installOn(this);


            setOpaque(false);
        }

        @Override
        public void beforeUpdate() {
            setText(myPresentation.getSelectedValue());
        }

        @Override
        
        public String getToolTipText() {
            return myPresentation.getTooltipText();
        }

        @Override
        protected void paintComponent( final Graphics g) {
            super.paintComponent(g);

            if (getText() != null) {
                final Rectangle r = getBounds();
                final Insets insets = getInsets();
                Icon icon = AllIcons.Ide.Statusbar_arrows;
                icon.paintIcon(this, g,
                        r.width - insets.right - icon.getIconWidth() - 2,
                        r.height / 2 - icon.getIconHeight() / 2);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            final Dimension preferredSize = super.getPreferredSize();
            return new Dimension(preferredSize.width + AllIcons.Ide.Statusbar_arrows.getIconWidth() + 4, preferredSize.height);
        }
    }

    private static final class TextPresentationWrapper extends TextPanel implements StatusBarWrapper {
        private final StatusBarWidget.TextPresentation myPresentation;
        private final Consumer<MouseEvent> myClickConsumer;
        private boolean myMouseOver;

        private TextPresentationWrapper( final StatusBarWidget.TextPresentation presentation) {
            super();
            myPresentation = presentation;
            myClickConsumer = myPresentation.getClickConsumer();

            setTextAlignment(presentation.getAlignment());

            putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);
            setToolTipText(presentation.getTooltipText());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (myClickConsumer != null && !e.isPopupTrigger() && MouseEvent.BUTTON1 == e.getButton()) {
                        myClickConsumer.consume(e);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    myMouseOver = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    myMouseOver = false;
                }
            });

            setOpaque(false);
        }

        @Override
        
        public String getToolTipText() {
            return myPresentation.getTooltipText();
        }

        @Override
        public void beforeUpdate() {
            setText(myPresentation.getText());
        }
    }

    private static final class IconPresentationWrapper extends JComponent implements StatusBarWrapper {
        private final StatusBarWidget.IconPresentation myPresentation;
        private final Consumer<MouseEvent> myClickConsumer;
        private Icon myIcon;

        private IconPresentationWrapper( final StatusBarWidget.IconPresentation presentation) {
            myPresentation = presentation;
            myClickConsumer = myPresentation.getClickConsumer();
            myIcon = myPresentation.getIcon();

            putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);
            setToolTipText(presentation.getTooltipText());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (myClickConsumer != null && !e.isPopupTrigger() && MouseEvent.BUTTON1 == e.getButton()) {
                        myClickConsumer.consume(e);
                    }
                }
            });

            setOpaque(false);
        }

        @Override
        public void beforeUpdate() {
            myIcon = myPresentation.getIcon();
        }

        private StatusBarWidget.IconPresentation getPresentation() {
            return myPresentation;
        }

        @Override
        
        public String getToolTipText() {
            return myPresentation.getTooltipText();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final Rectangle bounds = getBounds();
            final Insets insets = JBUI.insets(getInsets());

            if (myIcon != null) {
                final int iconWidth = myIcon.getIconWidth();
                final int iconHeight = myIcon.getIconHeight();

                myIcon.paintIcon(this, g, insets.left + (bounds.width - insets.left - insets.right - iconWidth) / 2,
                        insets.top + (bounds.height - insets.top - insets.bottom - iconHeight) / 2);
            }
        }

        @Override
        public Dimension getMinimumSize() {
            return JBUI.size(24, MIN_ICON_HEIGHT);
        }

        @Override
        public Dimension getPreferredSize() {
            return getMinimumSize();
        }
    }

    @Override
    public IdeFrame getFrame() {
        return myFrame;
    }
}