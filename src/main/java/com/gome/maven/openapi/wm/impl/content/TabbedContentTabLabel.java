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
package com.gome.maven.openapi.wm.impl.content;

import com.gome.maven.ide.IdeEventQueue;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.ui.popup.JBPopup;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.ui.ClickListener;
import com.gome.maven.ui.components.JBList;
import com.gome.maven.ui.content.TabbedContent;
import com.gome.maven.util.NotNullFunction;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * @author Konstantin Bulenkov
 */
public class TabbedContentTabLabel extends ContentTabLabel implements Disposable {
    private final ComboIcon myComboIcon = new ComboIcon() {
        @Override
        public Rectangle getIconRec() {
            return new Rectangle(getWidth() - getIconWidth() - 3, 0, getIconWidth(), getHeight());
        }

        @Override
        public boolean isActive() {
            return true;
        }
    };
    private final TabbedContent myContent;

    public TabbedContentTabLabel(TabbedContent content, TabContentLayout layout) {
        super(content, layout);
        myContent = content;
        new ClickListener() {
            @Override
            public boolean onClick( MouseEvent event, int clickCount) {
                showPopup();
                return true;
            }
        }.installOn(this);
    }

    private void showPopup() {
        IdeEventQueue.getInstance().getPopupManager().closeAllPopups();
        ArrayList<String> names = new ArrayList<String>();
        for (Pair<String, JComponent> tab : myContent.getTabs()) {
            names.add(tab.first);
        }
        final JBList list = new JBList(names);
        list.installCellRenderer(new NotNullFunction<Object, JComponent>() {
            final JLabel label = new JLabel();
            {
                label.setBorder(new EmptyBorder(UIUtil.getListCellPadding()));
            }
            
            @Override
            public JComponent fun(Object dom) {
                label.setText(dom.toString());
                return label;
            }
        });
        final JBPopup popup = JBPopupFactory.getInstance().createListPopupBuilder(list)
                .setItemChoosenCallback(new Runnable() {
                    @Override
                    public void run() {
                        int index = list.getSelectedIndex();
                        if (index != -1) {
                            myContent.selectContent(index);
                        }
                    }
                }).createPopup();
        Disposer.register(this, popup);
        popup.showUnderneathOf(this);
    }

    @Override
    public void update() {
        super.update();
        if (myContent != null) {
            setText(myContent.getTabName());
        }
        setHorizontalAlignment(LEFT);
    }

    @Override
    public Dimension getPreferredSize() {
        final Dimension size = super.getPreferredSize();
        return new Dimension(size.width + 12, size.height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        myComboIcon.paintIcon(this, g);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        Disposer.dispose(this);
    }
}
