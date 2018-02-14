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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Jul 17, 2007
 * Time: 3:20:51 PM
 */
package com.gome.maven.openapi.vfs.encoding;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.impl.TransferToPooledThreadQueue;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.event.EditorFactoryAdapter;
import com.gome.maven.openapi.editor.event.EditorFactoryEvent;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectLocator;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.ObjectUtils;
import com.gome.maven.util.Processor;
import com.gome.maven.util.xmlb.annotations.Attribute;
import gnu.trove.Equality;
import gnu.trove.THashSet;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;


@State(
        name = "Encoding",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/encoding.xml")
        }
)
public class EncodingManagerImpl extends EncodingManager implements PersistentStateComponent<EncodingManagerImpl.State>, Disposable {
    private static final Equality<Reference<Document>> REFERENCE_EQUALITY = new Equality<Reference<Document>>() {
        @Override
        public boolean equals(Reference<Document> o1, Reference<Document> o2) {
            Object v1 = o1 == null ? REFERENCE_EQUALITY : o1.get();
            Object v2 = o2 == null ? REFERENCE_EQUALITY : o2.get();
            return v1 == v2;
        }
    };
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    static class State {
        
        private Charset myDefaultEncoding = CharsetToolkit.UTF8_CHARSET;

        @Attribute("default_encoding")
        
        public String getDefaultCharsetName() {
            return myDefaultEncoding == ChooseFileEncodingAction.NO_ENCODING ? "" : myDefaultEncoding.name();
        }

        public void setDefaultCharsetName( String name) {
            myDefaultEncoding = name.isEmpty()
                    ? ChooseFileEncodingAction.NO_ENCODING
                    : ObjectUtils.notNull(CharsetToolkit.forName(name), CharsetToolkit.getDefaultSystemCharset());
        }
    }

    private State myState = new State();

    private final Alarm updateEncodingFromContent = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    private static final Key<Charset> CACHED_CHARSET_FROM_CONTENT = Key.create("CACHED_CHARSET_FROM_CONTENT");

    private final TransferToPooledThreadQueue<Reference<Document>> myChangedDocuments = new TransferToPooledThreadQueue<Reference<Document>>(
            "Encoding detection thread",
            ApplicationManager.getApplication().getDisposed(),
            -1, // drain the whole queue, do not reschedule
            new Processor<Reference<Document>>() {
                @Override
                public boolean process(Reference<Document> ref) {
                    Document document = ref.get();
                    if (document == null) return true; // document gced, don't bother
                    handleDocument(document);
                    return true;
                }
            });

    public EncodingManagerImpl( EditorFactory editorFactory) {
        editorFactory.getEventMulticaster().addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent e) {
                queueUpdateEncodingFromContent(e.getDocument());
            }
        }, this);
        editorFactory.addEditorFactoryListener(new EditorFactoryAdapter() {
            @Override
            public void editorCreated( EditorFactoryEvent event) {
                queueUpdateEncodingFromContent(event.getEditor().getDocument());
            }
        }, this);
    }

     public static final String PROP_CACHED_ENCODING_CHANGED = "cachedEncoding";

    private void handleDocument( final Document document) {
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) return;
        Project project = guessProject(virtualFile);
        if (project != null && project.isDisposed()) return;
        Charset charset = LoadTextUtil.charsetFromContentOrNull(project, virtualFile, document.getImmutableCharSequence());
        Charset oldCached = getCachedCharsetFromContent(document);
        if (!Comparing.equal(charset, oldCached)) {
            setCachedCharsetFromContent(charset, oldCached, document);
        }
    }

    private void setCachedCharsetFromContent(Charset charset, Charset oldCached,  Document document) {
        document.putUserData(CACHED_CHARSET_FROM_CONTENT, charset);
        firePropertyChange(document, PROP_CACHED_ENCODING_CHANGED, oldCached, charset);
    }

    public Charset computeCharsetFromContent( final VirtualFile virtualFile) {
        final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return null;
        }
        Charset cached = EncodingManager.getInstance().getCachedCharsetFromContent(document);
        if (cached != null) {
            return cached;
        }

        final Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        return ApplicationManager.getApplication().runReadAction(new Computable<Charset>() {
            @Override
            public Charset compute() {
                Charset charsetFromContent = LoadTextUtil.charsetFromContentOrNull(project, virtualFile, document.getImmutableCharSequence());
                if (charsetFromContent != null) {
                    setCachedCharsetFromContent(charsetFromContent, null, document);
                }
                return charsetFromContent;
            }
        });
    }

    @Override
    public void dispose() {
        updateEncodingFromContent.cancelAllRequests();
        clearDocumentQueue();
    }

    public void queueUpdateEncodingFromContent( Document document) {
        myChangedDocuments.offerIfAbsent(new WeakReference<Document>(document), REFERENCE_EQUALITY);
    }

    @Override
    
    public Charset getCachedCharsetFromContent( Document document) {
        return document.getUserData(CACHED_CHARSET_FROM_CONTENT);
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }

    @Override
    
    public Collection<Charset> getFavorites() {
        Set<Charset> result = new THashSet<Charset>();
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            result.addAll(EncodingProjectManager.getInstance(project).getFavorites());
        }
        return result;
    }

    @Override
    
    public Charset getEncoding( VirtualFile virtualFile, boolean useParentDefaults) {
        Project project = guessProject(virtualFile);
        if (project == null) return null;
        EncodingProjectManager encodingManager = EncodingProjectManager.getInstance(project);
        if (encodingManager == null) return null; //tests
        return encodingManager.getEncoding(virtualFile, useParentDefaults);
    }

    public void clearDocumentQueue() {
        myChangedDocuments.stop();
    }

    
    private static Project guessProject(final VirtualFile virtualFile) {
        return ProjectLocator.getInstance().guessProjectForFile(virtualFile);
    }

    @Override
    public void setEncoding( VirtualFile virtualFileOrDir,  Charset charset) {
        Project project = guessProject(virtualFileOrDir);
        EncodingProjectManager.getInstance(project).setEncoding(virtualFileOrDir, charset);
    }

    @Override
    public boolean isUseUTFGuessing(final VirtualFile virtualFile) {
        return true;
    }

    @Override
    public boolean isNative2Ascii( final VirtualFile virtualFile) {
        Project project = guessProject(virtualFile);
        return project != null && EncodingProjectManager.getInstance(project).isNative2Ascii(virtualFile);
    }

    @Override
    public boolean isNative2AsciiForPropertiesFiles() {
        Project project = guessProject(null);
        return project != null && EncodingProjectManager.getInstance(project).isNative2AsciiForPropertiesFiles();
    }

    @Override
    public void setNative2AsciiForPropertiesFiles(final VirtualFile virtualFile, final boolean native2Ascii) {
        Project project = guessProject(virtualFile);
        if (project == null) return;
        EncodingProjectManager.getInstance(project).setNative2AsciiForPropertiesFiles(virtualFile, native2Ascii);
    }

    @Override
    
    public Charset getDefaultCharset() {
        return myState.myDefaultEncoding == ChooseFileEncodingAction.NO_ENCODING ? CharsetToolkit.getDefaultSystemCharset() : myState.myDefaultEncoding;
    }

    @Override
    
    public String getDefaultCharsetName() {
        return myState.getDefaultCharsetName();
    }

    @Override
    public void setDefaultCharsetName( String name) {
        myState.setDefaultCharsetName(name);
    }

    @Override
    
    public Charset getDefaultCharsetForPropertiesFiles( final VirtualFile virtualFile) {
        Project project = guessProject(virtualFile);
        if (project == null) return null;
        return EncodingProjectManager.getInstance(project).getDefaultCharsetForPropertiesFiles(virtualFile);
    }

    @Override
    public void setDefaultCharsetForPropertiesFiles( final VirtualFile virtualFile, final Charset charset) {
        Project project = guessProject(virtualFile);
        if (project == null) return;
        EncodingProjectManager.getInstance(project).setDefaultCharsetForPropertiesFiles(virtualFile, charset);
    }

    @Override
    public void addPropertyChangeListener( final PropertyChangeListener listener,  Disposable parentDisposable) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removePropertyChangeListener(listener);
            }
        });
    }

    private void removePropertyChangeListener( PropertyChangeListener listener){
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    void firePropertyChange( Document document,  String propertyName, final Object oldValue, final Object newValue) {
        Object source = document == null ? this : document;
        myPropertyChangeSupport.firePropertyChange(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
    }
}
