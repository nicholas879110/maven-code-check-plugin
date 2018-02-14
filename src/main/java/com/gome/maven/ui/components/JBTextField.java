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
package com.gome.maven.ui.components;

import com.gome.maven.util.ui.ComponentWithEmptyText;
import com.gome.maven.util.ui.StatusText;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

public class JBTextField extends JTextField implements ComponentWithEmptyText {
    private TextComponentEmptyText myEmptyText;

    public JBTextField() {
        init();
    }

    public JBTextField(int i) {
        super(i);
        init();
    }

    public JBTextField(String s) {
        super(s);
        init();
    }

    public JBTextField(String s, int i) {
        super(s, i);
        init();
    }

    private void init() {
        UIUtil.addUndoRedoActions(this);
        myEmptyText = new TextComponentEmptyText(this);
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        UIUtil.resetUndoRedoActions(this);
    }

    @Override
    public StatusText getEmptyText() {
        return myEmptyText;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        myEmptyText.paintStatusText(g);
    }
}
