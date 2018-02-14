package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.execution.impl.ConsoleViewUtil;
import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.find.FindModel;
import com.gome.maven.find.FindUtil;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.editor.Editor;

/**
 * Created by IntelliJ IDEA.
 * User: zajac
 * Date: 05.03.11
 * Time: 10:57
 * To change this template use File | Settings | File Templates.
 */
public class SwitchToReplace extends EditorHeaderAction {
    public SwitchToReplace(EditorSearchComponent editorSearchComponent) {
        super(editorSearchComponent);
        AnAction replaceAction = ActionManager.getInstance().getAction("Replace");
        if (replaceAction != null) {
            registerCustomShortcutSet(replaceAction.getShortcutSet(), editorSearchComponent);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final Editor editor = getEditorSearchComponent().getEditor();
        e.getPresentation().setEnabled(!ConsoleViewUtil.isConsoleViewEditor(editor));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        EditorSearchComponent component = getEditorSearchComponent();
        final FindModel findModel = component.getFindModel();
        FindUtil.configureFindModel(true, null, findModel, false);
        component.selectAllText();
    }
}
