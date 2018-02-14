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

package com.gome.maven.psi.impl;

import com.gome.maven.AppTopics;
import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.injected.editor.EditorWindowImpl;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.SettingsSavingComponent;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.ex.DocumentBulkUpdateListener;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.FileDocumentManagerAdapter;
import com.gome.maven.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectLocator;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.SmartPointerManager;
import com.gome.maven.psi.impl.source.PostprocessReformattingAspect;
import com.gome.maven.util.FileContentUtil;
import com.gome.maven.util.Processor;
import com.gome.maven.util.messages.MessageBus;
import com.gome.maven.util.ui.UIUtil;

import java.util.Collection;
import java.util.List;

//todo listen & notifyListeners readonly events?
public class PsiDocumentManagerImpl extends PsiDocumentManagerBase implements SettingsSavingComponent {
    private final DocumentCommitThread myDocumentCommitThread;
    private final boolean myUnitTestMode = ApplicationManager.getApplication().isUnitTestMode();

    public PsiDocumentManagerImpl( final Project project,
                                   PsiManager psiManager,
                                   SmartPointerManager smartPointerManager,
                                   EditorFactory editorFactory,
                                   MessageBus bus,
                                    final DocumentCommitThread documentCommitThread) {
        super(project, psiManager, smartPointerManager, bus, documentCommitThread);
        myDocumentCommitThread = documentCommitThread;
        editorFactory.getEventMulticaster().addDocumentListener(this, project);
        bus.connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerAdapter() {
            @Override
            public void fileContentLoaded( final VirtualFile virtualFile,  Document document) {
                PsiFile psiFile = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
                    @Override
                    public PsiFile compute() {
                        return myProject.isDisposed() || !virtualFile.isValid() ? null : getCachedPsiFile(virtualFile);
                    }
                });
                fireDocumentCreated(document, psiFile);
            }
        });
        bus.connect().subscribe(DocumentBulkUpdateListener.TOPIC, new DocumentBulkUpdateListener.Adapter() {
            @Override
            public void updateFinished( Document doc) {
                documentCommitThread.queueCommit(project, doc, "Bulk update finished");
            }
        });
    }

    
    @Override
    public PsiFile getPsiFile( Document document) {
        final PsiFile psiFile = super.getPsiFile(document);
        if (myUnitTestMode) {
            final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            if (virtualFile != null && virtualFile.isValid()) {
                Collection<Project> projects = ProjectLocator.getInstance().getProjectsForFile(virtualFile);
                if (!projects.isEmpty() && !projects.contains(myProject)) {
                    LOG.error("Trying to get PSI for an alien project. VirtualFile=" + virtualFile +
                            ";\n myProject=" + myProject +
                            ";\n projects returned: " + projects);
                }
            }
        }
        return psiFile;
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        super.documentChanged(event);
        // avoid documents piling up during batch processing
        if (FileDocumentManagerImpl.areTooManyDocumentsInTheQueue(myUncommittedDocuments)) {
            if (myUnitTestMode) {
                myStopTrackingDocuments = true;
                try {
                    LOG.error("Too many uncommitted documents for " + myProject + ":\n" + myUncommittedDocuments);
                }
                finally {
                    clearUncommittedDocuments();
                }
            }
            commitAllDocuments();
        }
    }

    @Override
    protected void beforeDocumentChangeOnUnlockedDocument( final FileViewProvider viewProvider) {
        PostprocessReformattingAspect.getInstance(myProject).beforeDocumentChanged(viewProvider);
        super.beforeDocumentChangeOnUnlockedDocument(viewProvider);
    }

    @Override
    protected boolean finishCommitInWriteAction( Document document,
                                                 List<Processor<Document>> finishProcessors,
                                                boolean synchronously) {
        EditorWindowImpl.disposeInvalidEditors();  // in write action
        return super.finishCommitInWriteAction(document, finishProcessors, synchronously);
    }

    @Override
    public boolean isDocumentBlockedByPsi( Document doc) {
        final FileViewProvider viewProvider = getCachedViewProvider(doc);
        return viewProvider != null && PostprocessReformattingAspect.getInstance(myProject).isViewProviderLocked(viewProvider);
    }

    @Override
    public void doPostponedOperationsAndUnblockDocument( Document doc) {
        if (doc instanceof DocumentWindow) doc = ((DocumentWindow)doc).getDelegate();
        final PostprocessReformattingAspect component = myProject.getComponent(PostprocessReformattingAspect.class);
        final FileViewProvider viewProvider = getCachedViewProvider(doc);
        if (viewProvider != null) component.doPostponedFormatting(viewProvider);
    }

    @Override
    public void save() {
        // Ensure all documents are committed on save so file content dependent indices, that use PSI to build have consistent content.
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                try {
                    commitAllDocuments();
                }
                catch (Exception e) {
                    LOG.error(e);
                }
            }
        });
    }

    @Override
    
    public void clearUncommittedDocuments() {
        super.clearUncommittedDocuments();
        myDocumentCommitThread.clearQueue();
    }

    
    @Override
    public String toString() {
        return super.toString() + " for the project " + myProject + ".";
    }

    @Override
    public void reparseFiles( Collection<VirtualFile> files, boolean includeOpenFiles) {
        FileContentUtil.reparseFiles(myProject, files, includeOpenFiles);
    }
}
