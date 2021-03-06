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
package com.gome.maven.ui;

import com.gome.maven.util.ui.AbstractLayoutManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ExpandedItemRendererComponentWrapper extends JComponent {
    public ExpandedItemRendererComponentWrapper( final Component rendererComponent) {
        add(rendererComponent);
        setOpaque(false);
        setLayout(new AbstractLayoutManager() {
            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return rendererComponent.getPreferredSize();
            }

            @Override
            public void layoutContainer(Container parent) {
                Dimension size = parent.getSize();
                Insets i = parent.getInsets();
                Dimension pref = rendererComponent.getPreferredSize();
                rendererComponent.setBounds(i.left, i.top, Math.max(pref.width, size.width - i.left - i.right), size.height - i.top - i.bottom);
            }
        });
    }

    @Override
    public void setBorder(Border border) {
        if (getComponentCount() == 1) {
            Component component = getComponent(0);
            if (component instanceof JComponent) {
                ((JComponent)component).setBorder(border);
                return;
            }
        }
        super.setBorder(border);
    }
}
