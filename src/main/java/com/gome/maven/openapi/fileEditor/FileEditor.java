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
package com.gome.maven.openapi.fileEditor;

import com.gome.maven.codeHighlighting.BackgroundEditorHighlighter;
import com.gome.maven.ide.structureView.StructureViewBuilder;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.UserDataHolder;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 *
 * @see TextEditor
 */
public interface FileEditor extends UserDataHolder, Disposable {
    /**
     * @see #isModified()
     */
     String PROP_MODIFIED = "modified";
    /**
     * @see #isValid()
     */
     String PROP_VALID = "valid";

    /**
     * @return component which represents editor in the UI.
     * The method should never return <code>null</code>.
     */
    
    JComponent getComponent();

    /**
     * Returns component to be focused when editor is opened.
     */
    
    JComponent getPreferredFocusedComponent();

    /**
     * @return editor's name, a string that identifies editor among
     * other editors. For example, UI form might have two editor: "GUI Designer"
     * and "Text". So "GUI Designer" can be a name of one editor and "Text"
     * can be a name of other editor. The method should never return <code>null</code>.
     */
     
    String getName();

    /**
     * @return editor's internal state. Method should never return <code>null</code>.
     */
    
    FileEditorState getState( FileEditorStateLevel level);

    /**
     * Applies given state to the editor.
     * @param state cannot be null
     */
    void setState( FileEditorState state);

    /**
     * @return whether the editor's content is modified in comparison with its file.
     */
    boolean isModified();

    /**
     * @return whether the editor is valid or not. An editor is valid if the contents displayed in it still exists. For example, an editor
     * displaying the contents of a file stops being valid if the file is deleted.
     */
    boolean isValid();

    /**
     * This method is invoked each time when the editor is selected.
     * This can happen in two cases: editor is selected because the selected file
     * has been changed or editor for the selected file has been changed.
     */
    void selectNotify();

    /**
     * This method is invoked each time when the editor is deselected.
     */
    void deselectNotify();

    /**
     * Removes specified listener
     *
     * @param listener to be added
     */
    void addPropertyChangeListener( PropertyChangeListener listener);

    /**
     * Adds specified listener
     *
     * @param listener to be removed
     */
    void removePropertyChangeListener( PropertyChangeListener listener);

    /**
     * @return highlighter object to perform background analysis and highlighting activities.
     * Return <code>null</code> if no background highlighting activity necessary for this file editor.
     */
    
    BackgroundEditorHighlighter getBackgroundHighlighter();

    /**
     * The method is optional. Currently is used only by find usages subsystem
     * @return the location of user focus. Typically it's a caret or any other form of selection start.
     */
    
    FileEditorLocation getCurrentLocation();

    
    StructureViewBuilder getStructureViewBuilder();
}
