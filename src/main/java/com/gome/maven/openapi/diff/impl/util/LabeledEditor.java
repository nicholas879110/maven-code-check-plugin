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
package com.gome.maven.openapi.diff.impl.util;


import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class LabeledEditor extends JPanel {

    private final Border myEditorBorder;
    private JComponent myMainComponent;

    public LabeledEditor( Border editorBorder) {
        super(new BorderLayout());
        myEditorBorder = editorBorder;
    }

    public LabeledEditor() {
        this(null);
    }

    public void setComponent( JComponent component,  JComponent titleComponent) {
        myMainComponent = component;
        removeAll();

        JPanel title = new JPanel(new BorderLayout());
        title.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        title.add(titleComponent);
        revalidate();

        final JPanel p = new JPanel(new BorderLayout());
        if (myEditorBorder != null) {
            p.setBorder(myEditorBorder);
        }
        p.add(component, BorderLayout.CENTER);
        add(p, BorderLayout.CENTER);
        add(title, BorderLayout.NORTH);
    }

    public void updateTitle(JComponent title) {
        setComponent(myMainComponent, title);
    }
}
