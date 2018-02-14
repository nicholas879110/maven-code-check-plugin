package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.ToggleAction;


public class ToggleMultiline extends ToggleAction {
    private final EditorSearchComponent myEditorSearchComponent;

    public ToggleMultiline(EditorSearchComponent editorSearchComponent) {
        super("Multiline", "Toggle Multiline Mode", AllIcons.Actions.ShowViewer);
        myEditorSearchComponent = editorSearchComponent;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myEditorSearchComponent.getFindModel().isMultiline();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        myEditorSearchComponent.getFindModel().setMultiline(state);
    }
}
