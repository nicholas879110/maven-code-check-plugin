package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.util.containers.ContainerUtil;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zajac
 * Date: 05.03.11
 * Time: 10:44
 * To change this template use File | Settings | File Templates.
 */
public class ShowHistoryAction extends EditorHeaderAction implements DumbAware {
    private final Getter<JTextComponent> myTextField;

    public JTextComponent getTextField() {
        return myTextField.get();
    }

    public ShowHistoryAction(final Getter<JTextComponent> textField, EditorSearchComponent editorSearchComponent) {
        super(editorSearchComponent);
        myTextField = textField;
        getTemplatePresentation().setIcon(AllIcons.Actions.Search);
        final String s = getTextField() == getEditorSearchComponent().getSearchField() ? "Search" : "Replace";
        getTemplatePresentation().setDescription(s + " history");
        getTemplatePresentation().setText(s + " History");

        ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
        if (getTextField() == getEditorSearchComponent().getSearchField()) {
            //ContainerUtil.addAll(shortcuts, ActionManager.getInstance().getAction(IdeActions.ACTION_FIND).getShortcutSet().getShortcuts());
            ContainerUtil.addAll(shortcuts, ActionManager.getInstance().getAction("IncrementalSearch").getShortcutSet().getShortcuts());
        }
        shortcuts.add(new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK), null));

        registerCustomShortcutSet(new CustomShortcutSet(shortcuts.toArray(new Shortcut[shortcuts.size()])), getTextField());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setVisible(getEditorSearchComponent().getFindModel().isMultiline());
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        getEditorSearchComponent().showHistory(e.getInputEvent() instanceof MouseEvent, getTextField());
    }


}
