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
package com.gome.maven.util.xml.ui;

import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.application.WriteAction;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.event.DocumentListener;
import com.gome.maven.openapi.editor.markup.EffectType;
import com.gome.maven.openapi.editor.markup.MarkupModel;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.ui.EditorTextField;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.util.xml.DomElement;
import com.gome.maven.util.xml.highlighting.DomElementAnnotationsManager;
import com.gome.maven.util.xml.highlighting.DomElementProblemDescriptor;
import com.gome.maven.util.xml.highlighting.DomElementsProblemsHolder;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public abstract class EditorTextFieldControl<T extends JComponent> extends BaseModifiableControl<T, String> {
    private static final JTextField J_TEXT_FIELD = new JTextField() {
        @Override
        public void addNotify() {
            throw new UnsupportedOperationException("Shouldn't be shown");
        }

        @Override
        public void setVisible(boolean aFlag) {
            throw new UnsupportedOperationException("Shouldn't be shown");
        }
    };
    private final boolean myCommitOnEveryChange;
    private final DocumentListener myListener = new DocumentAdapter() {
        @Override
        public void documentChanged(DocumentEvent e) {
            setModified();
            if (myCommitOnEveryChange) {
                commit();
            }
        }
    };

    protected EditorTextFieldControl(final DomWrapper<String> domWrapper, final boolean commitOnEveryChange) {
        super(domWrapper);
        myCommitOnEveryChange = commitOnEveryChange;
    }


    protected EditorTextFieldControl(final DomWrapper<String> domWrapper) {
        this(domWrapper, false);
    }

    protected abstract EditorTextField getEditorTextField( T component);

    @Override
    protected void doReset() {
        final EditorTextField textField = getEditorTextField(getComponent());
        textField.getDocument().removeDocumentListener(myListener);
        super.doReset();
        textField.getDocument().addDocumentListener(myListener);
    }

    @Override
    protected JComponent getComponentToListenFocusLost(final T component) {
        return getEditorTextField(getComponent());
    }

    @Override
    protected JComponent getHighlightedComponent(final T component) {
        return J_TEXT_FIELD;
    }

    @Override
    protected T createMainComponent(T boundedComponent) {
        final Project project = getProject();
        boundedComponent = createMainComponent(boundedComponent, project);

        final EditorTextField editorTextField = getEditorTextField(boundedComponent);
        editorTextField.setSupplementary(true);
        editorTextField.getDocument().addDocumentListener(myListener);
        return boundedComponent;
    }

    protected abstract T createMainComponent(T boundedComponent, Project project);

    @Override
    
    protected String getValue() {
        return getEditorTextField(getComponent()).getText();
    }

    @Override
    protected void setValue(final String value) {
        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                new WriteAction() {
                    @Override
                    protected void run(Result result) throws Throwable {
                        final T component = getComponent();
                        final Document document = getEditorTextField(component).getDocument();
                        document.replaceString(0, document.getTextLength(), value == null ? "" : value);
                    }
                }.execute();
            }
        });
    }

    @Override
    protected void updateComponent() {
        final DomElement domElement = getDomElement();
        if (domElement == null || !domElement.isValid()) return;

        final EditorTextField textField = getEditorTextField(getComponent());
        final Project project = getProject();
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!project.isOpen()) return;
                if (!getDomWrapper().isValid()) return;

                final DomElement domElement = getDomElement();
                if (domElement == null || !domElement.isValid()) return;

                final DomElementAnnotationsManager manager = DomElementAnnotationsManager.getInstance(project);
                final DomElementsProblemsHolder holder = manager.getCachedProblemHolder(domElement);
                final List<DomElementProblemDescriptor> errorProblems = holder.getProblems(domElement);
                final List<DomElementProblemDescriptor> warningProblems = new ArrayList<DomElementProblemDescriptor>(holder.getProblems(domElement, true, HighlightSeverity.WARNING));
                warningProblems.removeAll(errorProblems);

                Color background = getDefaultBackground();
                if (errorProblems.size() > 0 && textField.getText().trim().length() == 0) {
                    background = getErrorBackground();
                }
                else if (warningProblems.size() > 0) {
                    background = getWarningBackground();
                }

                final Editor editor = textField.getEditor();
                if (editor != null) {
                    final MarkupModel markupModel = editor.getMarkupModel();
                    markupModel.removeAllHighlighters();
                    if (!errorProblems.isEmpty() && editor.getDocument().getLineCount() > 0) {
                        final TextAttributes attributes = SimpleTextAttributes.ERROR_ATTRIBUTES.toTextAttributes();
                        attributes.setEffectType(EffectType.WAVE_UNDERSCORE);
                        attributes.setEffectColor(attributes.getForegroundColor());
                        markupModel.addLineHighlighter(0, 0, attributes);
                        editor.getContentComponent().setToolTipText(errorProblems.get(0).getDescriptionTemplate());
                    }
                }

                textField.setBackground(background);
            }
        });

    }

    @Override
    public boolean canNavigate(final DomElement element) {
        return getDomElement().equals(element);
    }

    @Override
    public void navigate(final DomElement element) {
        final EditorTextField field = getEditorTextField(getComponent());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                field.requestFocus();
                field.selectAll();
            }
        });
    }
}