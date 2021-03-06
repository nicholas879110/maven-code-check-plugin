/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.gome.maven.openapi.roots.ui.configuration;

import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.roots.*;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.EventDispatcher;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 * @since Oct 8, 2003
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class ContentEntryEditor implements ContentRootPanel.ActionCallback {
    private boolean myIsSelected;
    private ContentRootPanel myContentRootPanel;
    private JPanel myMainPanel;
    protected EventDispatcher<ContentEntryEditorListener> myEventDispatcher;
    private final String myContentEntryUrl;
    private final List<ModuleSourceRootEditHandler<?>> myEditHandlers;

    public interface ContentEntryEditorListener extends EventListener{

        void editingStarted( ContentEntryEditor editor);
        void beforeEntryDeleted( ContentEntryEditor editor);
        void sourceFolderAdded( ContentEntryEditor editor, SourceFolder folder);
        void sourceFolderRemoved( ContentEntryEditor editor, VirtualFile file);
        void folderExcluded( ContentEntryEditor editor, VirtualFile file);
        void folderIncluded( ContentEntryEditor editor, String fileUrl);
        void navigationRequested( ContentEntryEditor editor, VirtualFile file);
        void sourceRootPropertiesChanged( ContentEntryEditor editor,  SourceFolder folder);
    }

    public ContentEntryEditor(String url, List<ModuleSourceRootEditHandler<?>> editHandlers) {
        myContentEntryUrl = url;
        myEditHandlers = editHandlers;
    }

    protected final List<ModuleSourceRootEditHandler<?>> getEditHandlers() {
        return myEditHandlers;
    }

    public String getContentEntryUrl() {
        return myContentEntryUrl;
    }

    public void initUI() {
        myMainPanel = new JPanel(new BorderLayout());
        myMainPanel.setOpaque(false);
        myMainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                myEventDispatcher.getMulticaster().editingStarted(ContentEntryEditor.this);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!myIsSelected) {
                    highlight(true);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!myIsSelected) {
                    highlight(false);
                }
            }
        });
        myEventDispatcher = EventDispatcher.create(ContentEntryEditorListener.class);
        setSelected(false);
        update();
    }

    
    protected ContentEntry getContentEntry() {
        final ModifiableRootModel model = getModel();
        if (model != null) {
            final ContentEntry[] entries = model.getContentEntries();
            for (ContentEntry entry : entries) {
                if (entry.getUrl().equals(myContentEntryUrl)) return entry;
            }
        }

        return null;
    }

    protected abstract ModifiableRootModel getModel();

    @Override
    public void deleteContentEntry() {
        final String path = FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(myContentEntryUrl));
        final int answer = Messages.showYesNoDialog(ProjectBundle.message("module.paths.remove.content.prompt", path),
                ProjectBundle.message("module.paths.remove.content.title"), Messages.getQuestionIcon());
        if (answer != Messages.YES) { // no
            return;
        }
        myEventDispatcher.getMulticaster().beforeEntryDeleted(this);
        final ContentEntry entry = getContentEntry();
        if (entry != null) {
            getModel().removeContentEntry(entry);
        }
    }

    @Override
    public void deleteContentFolder(ContentEntry contentEntry, ContentFolder folder) {
        if (folder instanceof SourceFolder) {
            removeSourceFolder((SourceFolder)folder);
            update();
        }
        else if (folder instanceof ExcludeFolder) {
            removeExcludeFolder(folder.getUrl());
            update();
        }

    }

    @Override
    public void navigateFolder(ContentEntry contentEntry, ContentFolder contentFolder) {
        final VirtualFile file = contentFolder.getFile();
        if (file != null) { // file can be deleted externally
            myEventDispatcher.getMulticaster().navigationRequested(this, file);
        }
    }

    @Override
    public void onSourceRootPropertiesChanged( SourceFolder folder) {
        update();
        myEventDispatcher.getMulticaster().sourceRootPropertiesChanged(this, folder);
    }

    public void addContentEntryEditorListener(ContentEntryEditorListener listener) {
        myEventDispatcher.addListener(listener);
    }

    public void removeContentEntryEditorListener(ContentEntryEditorListener listener) {
        myEventDispatcher.removeListener(listener);
    }

    public void setSelected(boolean isSelected) {
        if (myIsSelected != isSelected) {
            highlight(isSelected);
            myIsSelected = isSelected;
        }
    }

    private void highlight(boolean selected) {
        if (myContentRootPanel != null) {
            myContentRootPanel.setSelected(selected);
        }
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    public void update() {
        if (myContentRootPanel != null) {
            myMainPanel.remove(myContentRootPanel);
        }
        myContentRootPanel = createContentRootPane();
        myContentRootPanel.initUI();
        myContentRootPanel.setSelected(myIsSelected);
        myMainPanel.add(myContentRootPanel, BorderLayout.CENTER);
        myMainPanel.revalidate();
    }

    protected ContentRootPanel createContentRootPane() {
        return new ContentRootPanel(this, myEditHandlers) {
            @Override
            protected ContentEntry getContentEntry() {
                return ContentEntryEditor.this.getContentEntry();
            }
        };
    }

    
    public SourceFolder addSourceFolder( final VirtualFile file, boolean isTestSource, String packagePrefix) {
        return addSourceFolder(file, isTestSource ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE,
                JpsJavaExtensionService.getInstance().createSourceRootProperties(packagePrefix));
    }

    
    public <P extends JpsElement> SourceFolder addSourceFolder( final VirtualFile file, final JpsModuleSourceRootType<P> rootType,
                                                               final P properties) {
        final ContentEntry contentEntry = getContentEntry();
        if (contentEntry != null) {
            final SourceFolder sourceFolder = contentEntry.addSourceFolder(file, rootType, properties);
            myEventDispatcher.getMulticaster().sourceFolderAdded(this, sourceFolder);
            update();
            return sourceFolder;
        }

        return null;
    }

    
    protected SourceFolder doAddSourceFolder( final VirtualFile file, final boolean isTestSource) {
        final ContentEntry contentEntry = getContentEntry();
        return contentEntry != null ? contentEntry.addSourceFolder(file, isTestSource) : null;
    }

    public void removeSourceFolder( final SourceFolder sourceFolder) {
        try {
            doRemoveSourceFolder(sourceFolder);
        }
        finally {
            myEventDispatcher.getMulticaster().sourceFolderRemoved(this, sourceFolder.getFile());
            update();
        }
    }

    protected void doRemoveSourceFolder( final SourceFolder sourceFolder) {
        final ContentEntry contentEntry = getContentEntry();
        if (contentEntry != null) contentEntry.removeSourceFolder(sourceFolder);
    }

    
    public ExcludeFolder addExcludeFolder( final VirtualFile file) {
        try {
            return doAddExcludeFolder(file);
        }
        finally {
            myEventDispatcher.getMulticaster().folderExcluded(this, file);
            update();
        }
    }

    
    protected ExcludeFolder doAddExcludeFolder( final VirtualFile file) {
        final ContentEntry contentEntry = getContentEntry();
        return contentEntry != null ? contentEntry.addExcludeFolder(file) : null;
    }

    public void removeExcludeFolder( final String excludeRootUrl) {
        try {
            doRemoveExcludeFolder(excludeRootUrl);
        }
        finally {
            myEventDispatcher.getMulticaster().folderIncluded(this, excludeRootUrl);
            update();
        }
    }

    protected void doRemoveExcludeFolder( final String excludeRootUrl) {
        final ContentEntry contentEntry = getContentEntry();
        if (contentEntry != null) {
            contentEntry.removeExcludeFolder(excludeRootUrl);
        }
    }

    
    public JpsModuleSourceRootType<?> getRootType( VirtualFile file) {
        SourceFolder folder = getSourceFolder(file);
        return folder != null ? folder.getRootType() : null;
    }

    public boolean isExcludedOrUnderExcludedDirectory( final VirtualFile file) {
        final ContentEntry contentEntry = getContentEntry();
        if (contentEntry == null) {
            return false;
        }
        for (VirtualFile excludedDir : contentEntry.getExcludeFolderFiles()) {
            if (VfsUtilCore.isAncestor(excludedDir, file, false)) {
                return true;
            }
        }
        return false;
    }

    
    public SourceFolder getSourceFolder( final VirtualFile file) {
        final ContentEntry contentEntry = getContentEntry();
        if (contentEntry == null) {
            return null;
        }
        for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
            final VirtualFile f = sourceFolder.getFile();
            if (f != null && f.equals(file)) {
                return sourceFolder;
            }
        }
        return null;
    }
}
