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
package com.gome.maven.openapi.fileEditor.ex;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.Caret;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.EditorDataProvider;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.FileEditorProvider;
import com.gome.maven.openapi.fileEditor.impl.EditorComposite;
import com.gome.maven.openapi.fileEditor.impl.EditorWindow;
import com.gome.maven.openapi.fileEditor.impl.EditorsSplitters;
import com.gome.maven.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class FileEditorManagerEx extends FileEditorManager implements BusyObject {
    protected final List<EditorDataProvider> myDataProviders = new ArrayList<EditorDataProvider>();

    public static FileEditorManagerEx getInstanceEx(Project project) {
        return (FileEditorManagerEx)getInstance(project);
    }

    /**
     * @return <code>JComponent</code> which represent the place where all editors are located
     */
    public abstract JComponent getComponent();

    /**
     * @return preferred focused component inside myEditor tabbed container.
     * This method does similar things like {@link FileEditor#getPreferredFocusedComponent()}
     * but it also tracks (and remember) focus movement inside tabbed container.
     *
     * @see EditorComposite#getPreferredFocusedComponent()
     */
    
    public abstract JComponent getPreferredFocusedComponent();

    
    public abstract Pair<FileEditor[], FileEditorProvider[]> getEditorsWithProviders( VirtualFile file);

    
    public abstract VirtualFile getFile( FileEditor editor);

    public abstract void updateFilePresentation( VirtualFile file);

    /**
     *
     * @return current window in splitters
     */
    public abstract EditorWindow getCurrentWindow();

    
    public abstract AsyncResult<EditorWindow> getActiveWindow();

    public abstract void setCurrentWindow(EditorWindow window);

    /**
     * Closes editors for the file opened in particular window.
     *
     * @param file file to be closed. Cannot be null.
     */
    public abstract void closeFile( VirtualFile file,  EditorWindow window);

    public abstract void unsplitWindow();

    public abstract void unsplitAllWindow();

    public abstract int getWindowSplitCount();

    public abstract boolean hasSplitOrUndockedWindows();

    
    public abstract EditorWindow[] getWindows();

    /**
     * @return arrays of all files (including <code>file</code> itself) that belong
     * to the same tabbed container. The method returns empty array if <code>file</code>
     * is not open. The returned files have the same order as they have in the
     * tabbed container.
     */
    
    public abstract VirtualFile[] getSiblings( VirtualFile file);

    public abstract void createSplitter(int orientation,  EditorWindow window);

    public abstract void changeSplitterOrientation();

    public abstract void flipTabs();
    public abstract boolean tabsMode();

    public abstract boolean isInSplitter();

    public abstract boolean hasOpenedFile ();

    
    public abstract VirtualFile getCurrentFile();

    
    public abstract Pair <FileEditor, FileEditorProvider> getSelectedEditorWithProvider( VirtualFile file);

    /**
     * Closes all files IN ACTIVE SPLITTER (window).
     *
     * @see com.gome.maven.ui.docking.DockManager#getContainers()
     * @see com.gome.maven.ui.docking.DockContainer#closeAll()
     */
    public abstract void closeAllFiles();

    
    public abstract EditorsSplitters getSplitters();

    @Override
    
    public FileEditor[] openFile( final VirtualFile file, final boolean focusEditor) {
        return openFileWithProviders(file, focusEditor, false).getFirst ();
    }

    
    @Override
    public FileEditor[] openFile( VirtualFile file, boolean focusEditor, boolean searchForOpen) {
        return openFileWithProviders(file, focusEditor, searchForOpen).getFirst();
    }

    
    public abstract Pair<FileEditor[],FileEditorProvider[]> openFileWithProviders( VirtualFile file,
                                                                                  boolean focusEditor,
                                                                                  boolean searchForSplitter);

    
    public abstract Pair<FileEditor[],FileEditorProvider[]> openFileWithProviders( VirtualFile file,
                                                                                  boolean focusEditor,
                                                                                   EditorWindow window);

    public abstract boolean isChanged( EditorComposite editor);

    public abstract EditorWindow getNextWindow( final EditorWindow window);

    public abstract EditorWindow getPrevWindow( final EditorWindow window);

    public abstract boolean isInsideChange();

    @Override
    
    public final Object getData( String dataId,  Editor editor,  Caret caret) {
        for (final EditorDataProvider dataProvider : myDataProviders) {
            final Object o = dataProvider.getData(dataId, editor, caret);
            if (o != null) return o;
        }
        return null;
    }

    @Override
    public void registerExtraEditorDataProvider( final EditorDataProvider provider, Disposable parentDisposable) {
        myDataProviders.add(provider);
        if (parentDisposable != null) {
            Disposer.register(parentDisposable, new Disposable() {
                @Override
                public void dispose() {
                    myDataProviders.remove(provider);
                }
            });
        }
    }

    public void refreshIcons() {
        if (this instanceof FileEditorManagerImpl) {
            final FileEditorManagerImpl mgr = (FileEditorManagerImpl)this;
            Set<EditorsSplitters> splitters = mgr.getAllSplitters();
            for (EditorsSplitters each : splitters) {
                for (VirtualFile file : mgr.getOpenFiles()) {
                    each.updateFileIcon(file);
                }
            }
        }
    }

    public abstract EditorsSplitters getSplittersFor(Component c);


    
    public abstract ActionCallback notifyPublisher( Runnable runnable);

}
