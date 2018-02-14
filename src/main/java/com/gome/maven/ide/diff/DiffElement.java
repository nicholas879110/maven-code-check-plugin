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
package com.gome.maven.ide.diff;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.*;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.OpenFileDescriptor;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.encoding.EncodingManager;
import com.gome.maven.util.diff.FilesTooBigForDiffException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class DiffElement<T> /*implements Disposable */{
    public static final DiffElement[] EMPTY_ARRAY = new DiffElement[0];
    public static final DiffElement ERROR_NODE = new DiffErrorElement("Can't load elements", "An error has been occurred while getting children");
    private DiffPanel myDiffPanel;
    private Editor myEditor;
    private static final Logger LOG = Logger.getInstance(DiffElement.class.getName());

    public abstract String getPath();

    
    public abstract String getName();

    public String getPresentablePath() {
        return getName();
    }

    public abstract long getSize();

    public abstract long getTimeStamp();

    public FileType getFileType() {
        return FileTypeManager.getInstance().getFileTypeByFileName(getName());
    }

    public abstract boolean isContainer();

    public abstract DiffElement[] getChildren() throws IOException;

    
    public OpenFileDescriptor getOpenFileDescriptor( Project project) {
        return null;
    }

    /**
     * Returns content data as byte array. Can be null, if element for example is a container
     * @return content byte array
     * @throws java.io.IOException when reading
     */
    
    public abstract byte[] getContent() throws IOException;

    
    public Charset getCharset() {
        return EncodingManager.getInstance().getDefaultCharset();
    }

    
    public JComponent getViewComponent(Project project,  DiffElement target,  Disposable parentDisposable) {
        disposeViewComponent();
        try {
            FileType fileType = getFileType();
            if (fileType != null && fileType.isBinary()) {
                return getFromProviders(project, target);
            }
            final byte[] content = getContent();
            myEditor = createViewComponentEditor(project, content, fileType);
            return myEditor != null? myEditor.getComponent() : null;
        }
        catch (IOException e) {
            LOG.error(e);
            // TODO
        }
        return null;
    }

    
    protected Editor createViewComponentEditor(Project project,  byte[] content,  FileType fileType) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        if (editorFactory == null) return null;
        T value = getValue();
        Document document = value instanceof VirtualFile
                ? FileDocumentManager.getInstance().getDocument((VirtualFile)value)
                : editorFactory.createDocument(StringUtil.convertLineSeparators(new String(content)));

        if (document != null && fileType != null) {
            return editorFactory.createEditor(document, project, fileType, true);
        }
        return null;
    }

    
    protected JComponent getFromProviders(Project project, DiffElement target) {
        return null;
    }

    
    public JComponent getDiffComponent(DiffElement element, Project project, Window parentWindow, Disposable disposableParent) throws FilesTooBigForDiffException {
        disposeDiffComponent();

        DiffRequest request;
        try {
            request = createRequest(project, element);
        }
        catch (IOException e) {
            // TODO
            LOG.error(e);
            return null;
        }
        if (request != null) {
            myDiffPanel = DiffManager.getInstance().createDiffPanel(parentWindow, project, disposableParent, null);
            myDiffPanel.setRequestFocus(false);
            myDiffPanel.setDiffRequest(request);
            myDiffPanel.setTitle1(getName());
            myDiffPanel.setTitle2(element.getName());
            return myDiffPanel.getComponent();
        }

        return null;
    }

    
    protected DiffRequest createRequest(Project project, DiffElement element) throws IOException {
        final T src = getValue();
        final Object trg = element.getValue();
        if (src instanceof VirtualFile && trg instanceof VirtualFile
                && ((VirtualFile)src).getFileType().isBinary()
                && ((VirtualFile)trg).getFileType().isBinary()) {
            return createRequestForBinaries(project, ((VirtualFile)src), ((VirtualFile)trg));
        }
        if (src instanceof VirtualFile) {
            if (((VirtualFile)src).getFileType().isBinary()) return null;
            if (trg instanceof VirtualFile) {
                if (((VirtualFile)trg).getFileType().isBinary()) return null;
                final FileDocumentManager mgr = FileDocumentManager.getInstance();
                if (mgr.getDocument((VirtualFile)src) != null && mgr.getDocument((VirtualFile)trg) != null) {
                    return SimpleDiffRequest.compareFiles((VirtualFile)src, (VirtualFile)trg, project == null ? ProjectManager.getInstance().getDefaultProject() : project);
                }
            }
        }
        final DiffContent srcContent = createDiffContent();
        final DiffContent trgContent = element.createDiffContent();

        if (srcContent != null && trgContent != null) {
            final SimpleDiffRequest request = new SimpleDiffRequest(project, "");
            request.setContents(srcContent, trgContent);
            return request;
        }
        return null;
    }

    
    protected DiffRequest createRequestForBinaries(Project project,  VirtualFile src,  VirtualFile trg) {
        return SimpleDiffRequest.compareFiles(src, trg, project);
    }

    
    protected DiffContent createDiffContent() throws IOException {
        return new SimpleContent(new String(getContent(), getCharset()), getFileType());
    }

    public abstract T getValue();

    public void disposeViewComponent() {
        if (myEditor != null) {
            EditorFactory.getInstance().releaseEditor(myEditor);
            myEditor = null;
        }
    }

    public void disposeDiffComponent() {
        if (myDiffPanel != null) {
            Disposer.dispose(myDiffPanel);
            myDiffPanel = null;
        }
    }

    public String getSeparator() {
        return "/";
    }

    
    public Icon getIcon() {
        return null;
    }

    //@Override
    //public void dispose() {
    //}

    
    public Callable<DiffElement<T>> getElementChooser(Project project) {
        return null;
    }

    
    public DataProvider getDataProvider(Project project) {
        return null;
    }

    /**
     * Defines is it possible to perform such operations as copy or delete through Diff Panel
     *
     * @return <code>true</code> if copy, delete, etc operations are allowed,
     *        <code>false</code> otherwise
     */
    public boolean isOperationsEnabled() {
        return false;
    }

    /**
     * Copies element to the container.
     *
     * @param container file directory or other container
     * @param relativePath relative path from root
     * @return <code>true</code> if coping was completed successfully,
     *        <code>false</code> otherwise
     */
    
    public DiffElement<?> copyTo(DiffElement<T> container, String relativePath) {
        return null;
    }

    /**
     * Deletes element
     * @return <code>true</code> if deletion was completed successfully,
     *        <code>false</code> otherwise
     */
    public boolean delete() {
        return false;
    }

    public void refresh(boolean userInitiated) throws IOException{
    }
}
