package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.find.FindSettings;
import com.gome.maven.openapi.actionSystem.AnActionEvent;

public class ToggleWholeWordsOnlyAction extends EditorHeaderToggleAction {
    private static final String WHOLE_WORDS_ONLY = "Wo&rds";

    public ToggleWholeWordsOnlyAction(EditorSearchComponent editorSearchComponent) {
        super(editorSearchComponent, WHOLE_WORDS_ONLY);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return getEditorSearchComponent().getFindModel().isWholeWordsOnly();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(!getEditorSearchComponent().getFindModel().isRegularExpressions());
        e.getPresentation().setVisible(!getEditorSearchComponent().getFindModel().isMultiline());
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        FindSettings.getInstance().setLocalWholeWordsOnly(state);
        getEditorSearchComponent().getFindModel().setWholeWordsOnly(state);
    }
}
