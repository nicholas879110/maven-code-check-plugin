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
package com.gome.maven.openapi.fileEditor.impl.text;

import com.gome.maven.codeHighlighting.BackgroundEditorHighlighter;
import com.gome.maven.ide.structureView.StructureViewBuilder;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.editor.ex.util.EditorUtil;
import com.gome.maven.openapi.fileEditor.*;
import com.gome.maven.openapi.fileEditor.ex.FileEditorManagerEx;
import com.gome.maven.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.SingleRootFileViewProvider;
import org.jdom.Element;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class TextEditorProvider implements FileEditorProvider, DumbAware {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.fileEditor.impl.text.TextEditorProvider");

    private static final Key<TextEditor> TEXT_EDITOR_KEY = Key.create("textEditor");

     private static final String TYPE_ID                         = "text-editor";
     private static final String LINE_ATTR                       = "line";
     private static final String COLUMN_ATTR                     = "column";
     private static final String SELECTION_START_LINE_ATTR       = "selection-start-line";
     private static final String SELECTION_START_COLUMN_ATTR     = "selection-start-column";
     private static final String SELECTION_END_LINE_ATTR         = "selection-end-line";
     private static final String SELECTION_END_COLUMN_ATTR       = "selection-end-column";
     private static final String VERTICAL_SCROLL_PROPORTION_ATTR = "vertical-scroll-proportion";
     private static final String CARET_ELEMENT                   = "caret";

    public static TextEditorProvider getInstance() {
        return ApplicationManager.getApplication().getComponent(TextEditorProvider.class);
    }

    @Override
    public boolean accept( Project project,  VirtualFile file) {
        if (file.isDirectory() || !file.isValid()) {
            return false;
        }
        if (SingleRootFileViewProvider.isTooLargeForContentLoading(file)) {
            return false;
        }

        final FileType ft = file.getFileType();
        return !ft.isBinary() || BinaryFileTypeDecompilers.INSTANCE.forFileType(ft) != null;
    }

    @Override
    
    public FileEditor createEditor( Project project,  final VirtualFile file) {
        LOG.assertTrue(accept(project, file));
        return new TextEditorImpl(project, file, this);
    }

    @Override
    public void disposeEditor( FileEditor editor) {
        Disposer.dispose(editor);
    }

    @Override
    
    public FileEditorState readState( Element element,  Project project,  VirtualFile file) {
        TextEditorState state = new TextEditorState();

        try {
            List<Element> caretElements = element.getChildren(CARET_ELEMENT);
            if (caretElements.isEmpty()) {
                state.CARETS = new TextEditorState.CaretState[] {readCaretInfo(element)};
            }
            else {
                state.CARETS = new TextEditorState.CaretState[caretElements.size()];
                for (int i = 0; i < caretElements.size(); i++) {
                    state.CARETS[i] = readCaretInfo(caretElements.get(i));
                }
            }

            String verticalScrollProportion = element.getAttributeValue(VERTICAL_SCROLL_PROPORTION_ATTR);
            state.VERTICAL_SCROLL_PROPORTION = verticalScrollProportion == null ? 0 : Float.parseFloat(verticalScrollProportion);
        }
        catch (NumberFormatException ignored) {
        }

        return state;
    }

    private static TextEditorState.CaretState readCaretInfo(Element element) {
        TextEditorState.CaretState caretState = new TextEditorState.CaretState();
        caretState.LINE = parseWithDefault(element, LINE_ATTR);
        caretState.COLUMN = parseWithDefault(element, COLUMN_ATTR);
        caretState.SELECTION_START_LINE = parseWithDefault(element, SELECTION_START_LINE_ATTR);
        caretState.SELECTION_START_COLUMN = parseWithDefault(element, SELECTION_START_COLUMN_ATTR);
        caretState.SELECTION_END_LINE = parseWithDefault(element, SELECTION_END_LINE_ATTR);
        caretState.SELECTION_END_COLUMN = parseWithDefault(element, SELECTION_END_COLUMN_ATTR);
        return caretState;
    }

    private static int parseWithDefault(Element element, String attributeName) {
        String value = element.getAttributeValue(attributeName);
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Override
    public void writeState( FileEditorState _state,  Project project,  Element element) {
        TextEditorState state = (TextEditorState)_state;

        element.setAttribute(VERTICAL_SCROLL_PROPORTION_ATTR, Float.toString(state.VERTICAL_SCROLL_PROPORTION));
        if (state.CARETS != null) {
            for (TextEditorState.CaretState caretState : state.CARETS) {
                Element e = new Element(CARET_ELEMENT);
                e.setAttribute(LINE_ATTR, Integer.toString(caretState.LINE));
                e.setAttribute(COLUMN_ATTR, Integer.toString(caretState.COLUMN));
                e.setAttribute(SELECTION_START_LINE_ATTR, Integer.toString(caretState.SELECTION_START_LINE));
                e.setAttribute(SELECTION_START_COLUMN_ATTR, Integer.toString(caretState.SELECTION_START_COLUMN));
                e.setAttribute(SELECTION_END_LINE_ATTR, Integer.toString(caretState.SELECTION_END_LINE));
                e.setAttribute(SELECTION_END_COLUMN_ATTR, Integer.toString(caretState.SELECTION_END_COLUMN));
                element.addContent(e);
            }
        }
    }

    @Override
    
    public String getEditorTypeId() {
        return TYPE_ID;
    }

    @Override
    
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.NONE;
    }

    
    public TextEditor getTextEditor( Editor editor) {
        TextEditor textEditor = editor.getUserData(TEXT_EDITOR_KEY);
        if (textEditor == null) {
            textEditor = createWrapperForEditor(editor);
            putTextEditor(editor, textEditor);
        }

        return textEditor;
    }

    
    protected EditorWrapper createWrapperForEditor( Editor editor) {
        return new EditorWrapper(editor);
    }

    
    public static Document[] getDocuments( FileEditor editor) {
        if (editor instanceof DocumentsEditor) {
            DocumentsEditor documentsEditor = (DocumentsEditor)editor;
            Document[] documents = documentsEditor.getDocuments();
            return documents.length > 0 ? documents : null;
        }

        if (editor instanceof TextEditor) {
            Document document = ((TextEditor)editor).getEditor().getDocument();
            return new Document[]{document};
        }

        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (int i = projects.length - 1; i >= 0; i--) {
            VirtualFile file = FileEditorManagerEx.getInstanceEx(projects[i]).getFile(editor);
            if (file != null) {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    return new Document[]{document};
                }
            }
        }

        return null;
    }

    static void putTextEditor(Editor editor, TextEditor textEditor) {
        editor.putUserData(TEXT_EDITOR_KEY, textEditor);
    }

    protected TextEditorState getStateImpl(final Project project,  Editor editor,  FileEditorStateLevel level){
        TextEditorState state = new TextEditorState();
        CaretModel caretModel = editor.getCaretModel();
        if (caretModel.supportsMultipleCarets()) {
            List<CaretState> caretsAndSelections = caretModel.getCaretsAndSelections();
            state.CARETS = new TextEditorState.CaretState[caretsAndSelections.size()];
            for (int i = 0; i < caretsAndSelections.size(); i++) {
                CaretState caretState = caretsAndSelections.get(i);
                LogicalPosition caretPosition = caretState.getCaretPosition();
                LogicalPosition selectionStartPosition = caretState.getSelectionStart();
                LogicalPosition selectionEndPosition = caretState.getSelectionEnd();
                state.CARETS[i] = createCaretState(caretPosition, selectionStartPosition, selectionEndPosition);
            }
        }
        else {
            LogicalPosition caretPosition = caretModel.getLogicalPosition();
            LogicalPosition selectionStartPosition = editor.offsetToLogicalPosition(editor.getSelectionModel().getSelectionStart());
            LogicalPosition selectionEndPosition = editor.offsetToLogicalPosition(editor.getSelectionModel().getSelectionEnd());
            state.CARETS = new TextEditorState.CaretState[1];
            state.CARETS[0] = createCaretState(caretPosition, selectionStartPosition, selectionEndPosition);
        }

        // Saving scrolling proportion on UNDO may cause undesirable results of undo action fails to perform since
        // scrolling proportion restored slightly differs from what have been saved.
        state.VERTICAL_SCROLL_PROPORTION = level == FileEditorStateLevel.UNDO ? -1 : EditorUtil.calcVerticalScrollProportion(editor);

        return state;
    }

    private static TextEditorState.CaretState createCaretState(LogicalPosition caretPosition, LogicalPosition selectionStartPosition, LogicalPosition selectionEndPosition) {
        TextEditorState.CaretState caretState = new TextEditorState.CaretState();
        caretState.LINE = getLine(caretPosition);
        caretState.COLUMN = getColumn(caretPosition);
        caretState.SELECTION_START_LINE = getLine(selectionStartPosition);
        caretState.SELECTION_START_COLUMN = getColumn(selectionStartPosition);
        caretState.SELECTION_END_LINE = getLine(selectionEndPosition);
        caretState.SELECTION_END_COLUMN = getColumn(selectionEndPosition);
        return caretState;
    }

    private static int getLine( LogicalPosition pos) {
        return pos == null ? 0 : pos.line;
    }

    private static int getColumn( LogicalPosition pos) {
        return pos == null ? 0 : pos.column;
    }

    protected void setStateImpl(final Project project, final Editor editor, final TextEditorState state){
        if (state.CARETS != null) {
            if (editor.getCaretModel().supportsMultipleCarets()) {
                CaretModel caretModel = editor.getCaretModel();
                List<CaretState> states = new ArrayList<CaretState>(state.CARETS.length);
                for (TextEditorState.CaretState caretState : state.CARETS) {
                    states.add(new CaretState(new LogicalPosition(caretState.LINE, caretState.COLUMN),
                            new LogicalPosition(caretState.SELECTION_START_LINE, caretState.SELECTION_START_COLUMN),
                            new LogicalPosition(caretState.SELECTION_END_LINE, caretState.SELECTION_END_COLUMN)));
                }
                caretModel.setCaretsAndSelections(states, false);
            }
            else {
                LogicalPosition pos = new LogicalPosition(state.CARETS[0].LINE, state.CARETS[0].COLUMN);
                editor.getCaretModel().moveToLogicalPosition(pos);
                editor.getSelectionModel().removeSelection();
            }
        }

        if (state.VERTICAL_SCROLL_PROPORTION != -1) {
            EditorUtil.setVerticalScrollProportion(editor, state.VERTICAL_SCROLL_PROPORTION);
        }

        if (!editor.getCaretModel().supportsMultipleCarets()) {
            if (state.CARETS[0].SELECTION_START_LINE == state.CARETS[0].SELECTION_END_LINE
                    && state.CARETS[0].SELECTION_START_COLUMN == state.CARETS[0].SELECTION_END_COLUMN) {
                editor.getSelectionModel().removeSelection();
            }
            else {
                int startOffset = editor.logicalPositionToOffset(new LogicalPosition(state.CARETS[0].SELECTION_START_LINE, state.CARETS[0].SELECTION_START_COLUMN));
                int endOffset = editor.logicalPositionToOffset(new LogicalPosition(state.CARETS[0].SELECTION_END_LINE, state.CARETS[0].SELECTION_END_COLUMN));
                editor.getSelectionModel().setSelection(startOffset, endOffset);
            }
        }
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }

    protected class EditorWrapper extends UserDataHolderBase implements TextEditor {
        private final Editor myEditor;

        public EditorWrapper( Editor editor) {
            myEditor = editor;
        }

        @Override
        
        public Editor getEditor() {
            return myEditor;
        }

        @Override
        
        public JComponent getComponent() {
            return myEditor.getComponent();
        }

        @Override
        public JComponent getPreferredFocusedComponent() {
            return myEditor.getContentComponent();
        }

        @Override
        
        public String getName() {
            return "Text";
        }

        @Override
        public StructureViewBuilder getStructureViewBuilder() {
            VirtualFile file = FileDocumentManager.getInstance().getFile(myEditor.getDocument());
            if (file == null) return null;

            final Project project = myEditor.getProject();
            LOG.assertTrue(project != null);
            return StructureViewBuilder.PROVIDER.getStructureViewBuilder(file.getFileType(), file, project);
        }

        @Override
        
        public FileEditorState getState( FileEditorStateLevel level) {
            return getStateImpl(null, myEditor, level);
        }

        @Override
        public void setState( FileEditorState state) {
            setStateImpl(null, myEditor, (TextEditorState)state);
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void dispose() { }

        @Override
        public void selectNotify() { }

        @Override
        public void deselectNotify() { }

        @Override
        public void addPropertyChangeListener( PropertyChangeListener listener) { }

        @Override
        public void removePropertyChangeListener( PropertyChangeListener listener) { }

        @Override
        public BackgroundEditorHighlighter getBackgroundHighlighter() {
            return null;
        }

        @Override
        public FileEditorLocation getCurrentLocation() {
            return null;
        }

        @Override
        public boolean canNavigateTo( final Navigatable navigatable) {
            return false;
        }

        @Override
        public void navigateTo( final Navigatable navigatable) {
        }
    }
}
