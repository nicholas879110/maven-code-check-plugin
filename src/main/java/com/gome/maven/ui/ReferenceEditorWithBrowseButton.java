/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.ui;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.event.DocumentListener;
import com.gome.maven.openapi.editor.impl.event.DocumentEventImpl;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.ComponentWithBrowseButton;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author ven
 */
public class ReferenceEditorWithBrowseButton extends ComponentWithBrowseButton<EditorTextField> implements TextAccessor {
    private final Function<String, Document> myFactory;
    private final List<DocumentListener> myDocumentListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    public ReferenceEditorWithBrowseButton(final ActionListener browseActionListener,
                                           final Project project,
                                           final Function<String, Document> factory,
                                           String text) {
        this(browseActionListener, new EditorTextField(factory.fun(text), project, StdFileTypes.JAVA), factory);
    }

    public ReferenceEditorWithBrowseButton(final ActionListener browseActionListener,
                                           final EditorTextField editorTextField,
                                           final Function<String, Document> factory) {
        super(editorTextField, browseActionListener);
        myFactory = factory;
    }

    public void addDocumentListener(DocumentListener listener) {
        myDocumentListeners.add(listener);
        getEditorTextField().getDocument().addDocumentListener(listener);
    }

    public void removeDocumentListener(DocumentListener listener) {
        myDocumentListeners.remove(listener);
        getEditorTextField().getDocument().removeDocumentListener(listener);
    }

    public EditorTextField getEditorTextField() {
        return getChildComponent();
    }

    public String getText() {
        return getEditorTextField().getText().trim();
    }

    public void setText(final String text) {
        Document oldDocument = getEditorTextField().getDocument();
        String oldText = oldDocument.getText();
        for (DocumentListener listener : myDocumentListeners) {
            oldDocument.removeDocumentListener(listener);
        }
        Document document = myFactory.fun(text);
        getEditorTextField().setDocument(document);
        for (DocumentListener listener : myDocumentListeners) {
            document.addDocumentListener(listener);
            listener.documentChanged(new DocumentEventImpl(document, 0, oldText, text, -1, false));
        }
    }

    public boolean isEditable() {
        return !getEditorTextField().getEditor().isViewer();
    }
}
