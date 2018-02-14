package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CustomShortcutSet;
import com.gome.maven.openapi.actionSystem.KeyboardShortcut;
import com.gome.maven.openapi.actionSystem.Shortcut;
import com.gome.maven.openapi.keymap.KeymapUtil;
import com.gome.maven.openapi.project.DumbAware;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class CloseOnESCAction extends EditorHeaderAction  implements DumbAware {
    public CloseOnESCAction(EditorSearchComponent editorSearchComponent, JComponent textField) {
        super(editorSearchComponent);

        ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
        if (KeymapUtil.isEmacsKeymap()) {
            shortcuts.add(new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK), null));
            textField.registerKeyboardAction(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    CloseOnESCAction.this.actionPerformed(null);
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
        } else {
            shortcuts.add(new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null));
        }

        registerCustomShortcutSet(new CustomShortcutSet(shortcuts.toArray(new Shortcut[shortcuts.size()])), textField);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        getEditorSearchComponent().close();
    }
}
