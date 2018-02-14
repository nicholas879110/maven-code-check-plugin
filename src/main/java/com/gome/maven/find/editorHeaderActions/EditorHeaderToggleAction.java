package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.Presentation;
import com.gome.maven.openapi.actionSystem.ex.CheckboxAction;
import com.gome.maven.openapi.project.DumbAware;

import javax.swing.*;

public abstract class EditorHeaderToggleAction extends CheckboxAction implements DumbAware {

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    public EditorSearchComponent getEditorSearchComponent() {
        return myEditorSearchComponent;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation) {
        final JComponent customComponent = super.createCustomComponent(presentation);
        customComponent.setFocusable(false);
        customComponent.setOpaque(false);
        return customComponent;
    }

    private final EditorSearchComponent myEditorSearchComponent;

    protected EditorHeaderToggleAction(EditorSearchComponent editorSearchComponent, String text) {
        super(text);
        myEditorSearchComponent = editorSearchComponent;
    }
}
