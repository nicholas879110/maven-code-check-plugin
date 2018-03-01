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

package com.gome.maven.util.xml.ui;

import com.gome.maven.codeHighlighting.BackgroundEditorHighlighter;
import com.gome.maven.ide.structureView.StructureViewBuilder;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ScrollType;
import com.gome.maven.openapi.fileEditor.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.ui.components.panels.Wrapper;
import com.gome.maven.util.xml.DomElement;
import com.gome.maven.util.xml.DomManager;
import com.gome.maven.util.xml.DomUtil;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * @author Sergey.Vasiliev
 */
abstract public class PerspectiveFileEditor extends UserDataHolderBase implements DocumentsEditor, Committable {
    private final Wrapper myWrapper = new Wrapper();
    private boolean myInitialised = false;
    /** createCustomComponent() is in progress */
    private boolean myInitializing;

    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);
    private final Project myProject;
    private final VirtualFile myFile;
    private final UndoHelper myUndoHelper;
    private boolean myInvalidated;

    protected PerspectiveFileEditor(final Project project, final VirtualFile file) {
        myProject = project;
        myUndoHelper = new UndoHelper(project, this);
        myFile = file;

        FileEditorManager.getInstance(myProject).addFileEditorManagerListener(new FileEditorManagerAdapter() {
            @Override
            public void selectionChanged( FileEditorManagerEvent event) {
                if (!isValid()) return;

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (myUndoHelper.isShowing() && !getComponent().isShowing()) {
                            deselectNotify();
                        } else if (!myUndoHelper.isShowing() && getComponent().isShowing()) {
                            selectNotify();
                        }
                    }
                });

                final FileEditor oldEditor = event.getOldEditor();
                final FileEditor newEditor = event.getNewEditor();
                if (oldEditor == null || newEditor == null) return;
                if (oldEditor.getComponent().isShowing() && newEditor.getComponent().isShowing()) return;

                if (PerspectiveFileEditor.this.equals(oldEditor)) {
                    if (newEditor instanceof TextEditor) {
                        ensureInitialized();
                        DomElement selectedDomElement = getSelectedDomElement();
                        if (selectedDomElement != null) {
                            setSelectionInTextEditor((TextEditor)newEditor, selectedDomElement);
                        }
                    }
                }
                else if (PerspectiveFileEditor.this.equals(newEditor)) {
                    if (oldEditor instanceof TextEditor) {
                        final DomElement element = getSelectedDomElementFromTextEditor((TextEditor)oldEditor);
                        if (element != null) {
                            ensureInitialized();
                            setSelectedDomElement(element);
                        }
                    }
                    else if (oldEditor instanceof PerspectiveFileEditor) {
                        ensureInitialized();
                        DomElement selectedDomElement = ((PerspectiveFileEditor)oldEditor).getSelectedDomElement();
                        if (selectedDomElement != null) {
                            setSelectedDomElement(selectedDomElement);
                        }
                    }
                }
            }
        }, this);

        myUndoHelper.startListeningDocuments();

        final PsiFile psiFile = getPsiFile();
        if (psiFile != null) {
            final Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);
            if (document != null) {
                addWatchedDocument(document);
            }
        }
    }

    
    abstract protected DomElement getSelectedDomElement();

    abstract protected void setSelectedDomElement(DomElement domElement);

    public final void addWatchedElement( final DomElement domElement) {
        addWatchedDocument(getDocumentManager().getDocument(DomUtil.getFile(domElement)));
    }

    public final void removeWatchedElement( final DomElement domElement) {
        removeWatchedDocument(getDocumentManager().getDocument(DomUtil.getFile(domElement)));
    }

    public final void addWatchedDocument(final Document document) {
        myUndoHelper.addWatchedDocument(document);
    }

    public final void removeWatchedDocument(final Document document) {
        myUndoHelper.removeWatchedDocument(document);
    }

    
    protected DomElement getSelectedDomElementFromTextEditor(final TextEditor textEditor) {
        final PsiFile psiFile = getPsiFile();
        if (psiFile == null) return null;
        final PsiElement psiElement = psiFile.findElementAt(textEditor.getEditor().getCaretModel().getOffset());

        if (psiElement == null) return null;

        final XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);

        return DomManager.getDomManager(myProject).getDomElement(xmlTag);
    }

    public void setSelectionInTextEditor(final TextEditor textEditor, final DomElement element) {
        if (element != null && element.isValid()) {
            final XmlTag tag = element.getXmlTag();
            if (tag == null) return;

            final PsiFile file = tag.getContainingFile();
            if (file == null) return;

            final Document document = getDocumentManager().getDocument(file);
            if (document == null || !document.equals(textEditor.getEditor().getDocument())) return;

            textEditor.getEditor().getCaretModel().moveToOffset(tag.getTextOffset());
            textEditor.getEditor().getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }

    protected final PsiDocumentManager getDocumentManager() {
        return PsiDocumentManager.getInstance(myProject);
    }

    
    public final PsiFile getPsiFile() {
        return PsiManager.getInstance(myProject).findFile(myFile);
    }

    @Override
    public final Document[] getDocuments() {
        return myUndoHelper.getDocuments();
    }

    public final Project getProject() {
        return myProject;
    }

    public final VirtualFile getVirtualFile() {
        return myFile;
    }

    @Override
    public void dispose() {
        if (myInvalidated) return;
        myInvalidated = true;
        myUndoHelper.stopListeningDocuments();
    }

    @Override
    public final boolean isModified() {
        return FileDocumentManager.getInstance().isFileModified(getVirtualFile());
    }

    @Override
    public boolean isValid() {
        return getVirtualFile().isValid();
    }

    @Override
    public void selectNotify() {
        if (!checkIsValid() || myInvalidated) return;
        ensureInitialized();
        setShowing(true);
        if (myInitialised) {
            reset();
        }
    }

    protected final void setShowing(final boolean b) {
        myUndoHelper.setShowing(b);
    }

    protected final synchronized void ensureInitialized() {
        if (!isInitialised() && !myInitializing) {
            myInitializing = true;
            JComponent component = createCustomComponent();
            myWrapper.setContent(component);
            myInitialised = true;
        }
    }

    @Override
    public void deselectNotify() {
        if (!checkIsValid() || myInvalidated) return;
        setShowing(false);
        commit();
    }

    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Override
    public FileEditorLocation getCurrentLocation() {
        return new FileEditorLocation() {
            @Override
            
            public FileEditor getEditor() {
                return PerspectiveFileEditor.this;
            }

            @Override
            public int compareTo( final FileEditorLocation fileEditorLocation) {
                return 0;
            }
        };
    }

    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    
    public FileEditorState getState( FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState( FileEditorState state) {
    }

    @Override
    public void addPropertyChangeListener( PropertyChangeListener listener) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener( PropertyChangeListener listener) {
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected boolean checkIsValid() {
        if (!myInvalidated && !isValid()) {
            myInvalidated = true;
            myPropertyChangeSupport.firePropertyChange(FileEditor.PROP_VALID, Boolean.TRUE, Boolean.FALSE);
        }
        return !myInvalidated;
    }

    @Override
    
    public JComponent getComponent() {
        return getWrapper();
    }

    
    protected abstract JComponent createCustomComponent();

    public Wrapper getWrapper() {
        return myWrapper;
    }

    protected final synchronized boolean isInitialised() {
        return myInitialised;
    }
}