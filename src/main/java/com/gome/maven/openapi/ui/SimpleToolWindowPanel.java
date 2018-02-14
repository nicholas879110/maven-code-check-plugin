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
package com.gome.maven.openapi.ui;

import com.gome.maven.openapi.actionSystem.ActionToolbar;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.ui.switcher.QuickActionProvider;
import com.gome.maven.util.ui.AwtVisitor;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.List;

public class SimpleToolWindowPanel extends JPanel implements QuickActionProvider, DataProvider {

    private JComponent myToolbar;
    private JComponent myContent;

    private boolean myBorderless;
    protected boolean myVertical;
    private boolean myProvideQuickActions;

    public SimpleToolWindowPanel(boolean vertical) {
        this(vertical, false);
    }

    public SimpleToolWindowPanel(boolean vertical, boolean borderless) {
        setLayout(new BorderLayout(vertical ? 0 : 1, vertical ? 1 : 0));
        myBorderless = borderless;
        myVertical = vertical;
        setProvideQuickActions(true);

        addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                Component child = e.getChild();

                if (child instanceof Container) {
                    ((Container)child).addContainerListener(this);
                }
                if (myBorderless) {
                    UIUtil.removeScrollBorder(SimpleToolWindowPanel.this);
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                Component child = e.getChild();

                if (child instanceof Container) {
                    ((Container)child).removeContainerListener(this);
                }
            }
        });
    }

    public boolean isToolbarVisible() {
        return myToolbar != null && myToolbar.isVisible();
    }

    public void setToolbar( JComponent c) {
        if (c == null) {
            remove(myToolbar);
        }
        myToolbar = c;

        if (c != null) {
            if (myVertical) {
                add(c, BorderLayout.NORTH);
            } else {
                add(c, BorderLayout.WEST);
            }
        }

        revalidate();
        repaint();
    }

    
    public Object getData( String dataId) {
        return QuickActionProvider.KEY.is(dataId) && myProvideQuickActions ? this : null;
    }

    public SimpleToolWindowPanel setProvideQuickActions(boolean provide) {
        myProvideQuickActions = provide;
        return this;
    }

    public List<AnAction> getActions(boolean originalProvider) {
        final Ref<ActionToolbar> toolbar = new Ref<ActionToolbar>();
        if (myToolbar != null) {
            new AwtVisitor(myToolbar) {
                @Override
                public boolean visit(Component component) {
                    if (component instanceof ActionToolbar) {
                        toolbar.set((ActionToolbar)component);
                        return true;
                    }
                    return false;
                }
            };
        }

        if (toolbar.get() != null) {
            return toolbar.get().getActions(originalProvider);
        }

        return null;
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean isCycleRoot() {
        return false;
    }

    public void setContent(JComponent c) {
        myContent = c;
        add(c, BorderLayout.CENTER);

        if (myBorderless) {
            UIUtil.removeScrollBorder(c);
        }

        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (myToolbar != null && myToolbar.getParent() == this && myContent != null && myContent.getParent() == this) {
            g.setColor(UIUtil.getBorderColor());
            if (myVertical) {
                final int y = (int)myToolbar.getBounds().getMaxY();
                g.drawLine(0, y, getWidth(), y);
            } else {
                int x = (int)myToolbar.getBounds().getMaxX();
                g.drawLine(x, 0, x, getHeight());
            }
        }
    }
}