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

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.Caret;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.List;

public abstract class FileEditorManager {

    public static final Key<Boolean> USE_CURRENT_WINDOW = Key.create("OpenFile.searchForOpen");

    public static FileEditorManager getInstance( Project project) {
        return project.getComponent(FileEditorManager.class);
    }

    /**
     * @param file file to open. Parameter cannot be null. File should be valid.
     *
     * @return array of opened editors
     */
    
    public abstract FileEditor[] openFile( VirtualFile file, boolean focusEditor);


    /**
     * Opens a file
     *
     *
     * @param file file to open
     * @param focusEditor <code>true</code> if need to focus
     * @return array of opened editors
     */
    
    public FileEditor[] openFile( VirtualFile file, boolean focusEditor, boolean searchForOpen) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Closes all editors opened for the file.
     *
     * @param file file to be closed. Cannot be null.
     */
    public abstract void closeFile( VirtualFile file);

    /**
     * Works as {@link #openFile(VirtualFile, boolean)} but forces opening of text editor.
     * This method ignores {@link FileEditorPolicy#HIDE_DEFAULT_EDITOR} policy.
     *
     * @return opened text editor. The method returns <code>null</code> in case if text editor wasn't opened.
     */
    
    public abstract Editor openTextEditor( OpenFileDescriptor descriptor, boolean focusEditor);

    /**
     * @return currently selected text editor. The method returns <code>null</code> in case
     * there is no selected editor at all or selected editor is not a text one.
     */
    
    public abstract Editor getSelectedTextEditor();

    /**
     * @return <code>true</code> if <code>file</code> is opened, <code>false</code> otherwise
     */
    public abstract boolean isFileOpen( VirtualFile file);

    /**
     * @return all opened files. Order of files in the array corresponds to the order of editor tabs.
     */
    
    public abstract VirtualFile[] getOpenFiles();

    /**
     * @return files currently selected. The method returns empty array if there are no selected files.
     * If more than one file is selected (split), the file with most recent focused editor is returned first.
     */
    
    public abstract VirtualFile[] getSelectedFiles();

    /**
     * @return editors currently selected. The method returns empty array if no editors are open.
     */
    
    public abstract FileEditor[] getSelectedEditors();

    /**
     * @param file cannot be null
     *
     * @return editor which is currently selected in the currently selected file.
     * The method returns <code>null</code> if <code>file</code> is not opened.
     */
    
    public abstract FileEditor getSelectedEditor( VirtualFile file);

    /**
     * @param file cannot be null
     *
     * @return current editors for the specified <code>file</code>
     */
    
    public abstract FileEditor[] getEditors( VirtualFile file);

    /**
     * @param file cannot be null
     *
     * @return all editors for the specified <code>file</code>
     */
    
    public abstract FileEditor[] getAllEditors( VirtualFile file);

    /**
     * @return all open editors
     */
    
    public abstract FileEditor[] getAllEditors();

    /**
     * @deprecated use addTopComponent
     */
    public abstract void showEditorAnnotation( FileEditor editor,  JComponent annotationComponent);
    /**
     * @deprecated use removeTopComponent
     */
    public abstract void removeEditorAnnotation( FileEditor editor,  JComponent annotationComponent);

    public abstract void addTopComponent( final FileEditor editor,  final JComponent component);
    public abstract void removeTopComponent( final FileEditor editor,  final JComponent component);
    public abstract void addBottomComponent( final FileEditor editor,  final JComponent component);
    public abstract void removeBottomComponent( final FileEditor editor,  final JComponent component);


    /**
     * Adds specified <code>listener</code>
     * @param listener listener to be added
     * @deprecated Use MessageBus instead: see {@link FileEditorManagerListener#FILE_EDITOR_MANAGER}
     */
    public abstract void addFileEditorManagerListener( FileEditorManagerListener listener);

    /**
     * @deprecated Use {@link FileEditorManagerListener#FILE_EDITOR_MANAGER} instead
     */
    public abstract void addFileEditorManagerListener( FileEditorManagerListener listener,  Disposable parentDisposable);

    /**
     * Removes specified <code>listener</code>
     *
     * @param listener listener to be removed
     * @deprecated Use {@link FileEditorManagerListener#FILE_EDITOR_MANAGER} instead
     */
    public abstract void removeFileEditorManagerListener( FileEditorManagerListener listener);

    
    public abstract List<FileEditor> openEditor( OpenFileDescriptor descriptor, boolean focusEditor);

    /**
     * Returns the project with which the file editor manager is associated.
     *
     * @return the project instance.
     * @since 5.0.1
     */
    
    public abstract Project getProject();

    public abstract void registerExtraEditorDataProvider( EditorDataProvider provider, Disposable parentDisposable);

    /**
     * Returns data associated with given editor/caret context. Data providers are registered via
     * {@link #registerExtraEditorDataProvider(EditorDataProvider, com.gome.maven.openapi.Disposable)} method.
     */
    
    public abstract Object getData( String dataId,  Editor editor,  Caret caret);

    /**
     * Selects a specified file editor tab for the specified editor.
     * @param file a file to switch the file editor tab for. The function does nothing if the file is not currently open in the editor.
     * @param fileEditorProviderId the ID of the file editor to open; matches the return value of
     * {@link com.gome.maven.openapi.fileEditor.FileEditorProvider#getEditorTypeId()}
     */
    public abstract void setSelectedEditor( VirtualFile file,  String fileEditorProviderId);
}
