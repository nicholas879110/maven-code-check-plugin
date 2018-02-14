package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.find.FindModel;
import com.gome.maven.openapi.actionSystem.AnActionEvent;

public class ToggleInCommentsAction extends EditorHeaderToggleAction implements SecondaryHeaderAction {
    private static final String TEXT = "In &Comments Only";

    public ToggleInCommentsAction(EditorSearchComponent editorSearchComponent) {
        super(editorSearchComponent, TEXT);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return getEditorSearchComponent().getFindModel().isInCommentsOnly();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        getEditorSearchComponent().getFindModel().setSearchContext(state ? FindModel.SearchContext.IN_COMMENTS : FindModel.SearchContext.ANY);
    }
}
