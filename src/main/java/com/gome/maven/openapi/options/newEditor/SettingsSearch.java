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
package com.gome.maven.openapi.options.newEditor;

import com.gome.maven.ide.ui.laf.darcula.ui.DarculaTextBorder;
import com.gome.maven.ide.ui.laf.darcula.ui.DarculaTextFieldUI;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.ui.SearchTextField;

import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.JTextField;
import javax.swing.JTree;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * @author Sergey.Malenkov
 */
abstract class SettingsSearch extends SearchTextField implements KeyListener {
    private boolean myDelegatingNow;

    SettingsSearch() {
        super(false);
        addKeyListener(new KeyAdapter() {
        });
        if (Registry.is("ide.new.settings.dialog")) {
            if (!SystemInfo.isMac) {
                JTextField editor = getTextEditor();
                editor.putClientProperty("JTextField.variant", "search");
                if (!(editor.getUI() instanceof DarculaTextFieldUI)) {
                    editor.setUI((DarculaTextFieldUI)DarculaTextFieldUI.createUI(editor));
                    editor.setBorder(new DarculaTextBorder());
                }
            }
        }
    }

    abstract void onTextKeyEvent(KeyEvent event);

    void delegateKeyEvent(KeyEvent event) {
        keyEventToTextField(event);
    }

    @Override
    protected boolean isSearchControlUISupported() {
        return true;
    }

    @Override
    protected boolean preprocessEventForTextField(KeyEvent event) {
        if (!myDelegatingNow) {
            KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(event);
            String strokeString = stroke.toString();
            if ("pressed ESCAPE".equals(strokeString) && getText().length() > 0) {
                setText(""); // reset filter on ESC
                return true;
            }
            if (getTextEditor().isFocusOwner()) {
                if ("pressed ENTER".equals(strokeString)) {
                    return true; // avoid closing dialog on ENTER
                }
                try {
                    myDelegatingNow = true;
                    int code = stroke.getKeyCode();
                    boolean treeNavigation = stroke.getModifiers() == 0 && (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN);
                    if (treeNavigation || !hasAction(stroke, getTextEditor().getInputMap())) {
                        onTextKeyEvent(event);
                        return true;
                    }
                }
                finally {
                    myDelegatingNow = false;
                }
            }
        }
        return false;
    }

    private static boolean hasAction(KeyStroke stroke, InputMap map) {
        return map != null && map.get(stroke) != null;
    }

    @Override
    public void keyPressed(KeyEvent event) {
        keyTyped(event);
    }

    @Override
    public void keyReleased(KeyEvent event) {
        keyTyped(event);
    }

    @Override
    public void keyTyped(KeyEvent event) {
        Object source = event.getSource();
        if (source instanceof JTree) {
            JTree tree = (JTree)source;
            if (!hasAction(KeyStroke.getKeyStrokeForEvent(event), tree.getInputMap())) {
                delegateKeyEvent(event);
            }
        }
    }
}