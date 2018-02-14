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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeInsight.daemon.ChangeLocalityDetector;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.ex.EditorMarkupModel;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiDocumentManagerBase;
import com.gome.maven.psi.impl.PsiDocumentManagerImpl;
import com.gome.maven.psi.impl.PsiDocumentTransactionListener;
import com.gome.maven.psi.impl.PsiTreeChangeEventImpl;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.messages.MessageBusConnection;
import gnu.trove.THashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class PsiChangeHandler extends PsiTreeChangeAdapter implements Disposable {
    private static final ExtensionPointName<ChangeLocalityDetector> EP_NAME = ExtensionPointName.create("com.intellij.daemon.changeLocalityDetector");
    private /*NOT STATIC!!!*/ final Key<Boolean> UPDATE_ON_COMMIT_ENGAGED = Key.create("UPDATE_ON_COMMIT_ENGAGED");

    private final Project myProject;
    private final Map<Document, List<Pair<PsiElement, Boolean>>> changedElements = new THashMap<Document, List<Pair<PsiElement, Boolean>>>();
    private final FileStatusMap myFileStatusMap;

    PsiChangeHandler( Project project,
                      final PsiDocumentManagerImpl documentManager,
                      EditorFactory editorFactory,
                      MessageBusConnection connection,
                      FileStatusMap fileStatusMap) {
        myProject = project;
        myFileStatusMap = fileStatusMap;
        editorFactory.getEventMulticaster().addDocumentListener(new DocumentAdapter() {
            @Override
            public void beforeDocumentChange(DocumentEvent e) {
                final Document document = e.getDocument();
                if (documentManager.getSynchronizer().isInSynchronization(document)) return;
                if (documentManager.getCachedPsiFile(document) == null) return;
                if (document.getUserData(UPDATE_ON_COMMIT_ENGAGED) == null) {
                    document.putUserData(UPDATE_ON_COMMIT_ENGAGED, Boolean.TRUE);
                    PsiDocumentManagerBase.addRunOnCommit(document, new Runnable() {
                        @Override
                        public void run() {
                            if (document.getUserData(UPDATE_ON_COMMIT_ENGAGED) != null) {
                                updateChangesForDocument(document);
                                document.putUserData(UPDATE_ON_COMMIT_ENGAGED, null);
                            }
                        }
                    });
                }
            }
        }, this);

        connection.subscribe(PsiDocumentTransactionListener.TOPIC, new PsiDocumentTransactionListener() {
            @Override
            public void transactionStarted( final Document doc,  final PsiFile file) {
            }

            @Override
            public void transactionCompleted( final Document document,  final PsiFile file) {
                updateChangesForDocument(document);
                document.putUserData(UPDATE_ON_COMMIT_ENGAGED, null); // ensure we don't call updateChangesForDocument() twice which can lead to whole file re-highlight
            }
        });
    }

    @Override
    public void dispose() {
    }

    private void updateChangesForDocument( final Document document) {
        if (DaemonListeners.isUnderIgnoredAction(null) || myProject.isDisposed()) return;
        List<Pair<PsiElement, Boolean>> toUpdate = changedElements.get(document);
        if (toUpdate == null) {
            // The document has been changed, but psi hasn't
            // We may still need to rehighlight the file if there were changes inside highlighted ranges.
            if (UpdateHighlightersUtil.isWhitespaceOptimizationAllowed(document)) return;

            // don't create PSI for files in other projects
            PsiElement file = PsiDocumentManager.getInstance(myProject).getCachedPsiFile(document);
            if (file == null) return;

            toUpdate = Collections.singletonList(Pair.create(file, true));
        }
        Application application = ApplicationManager.getApplication();
        final Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        if (editor != null && !application.isUnitTestMode()) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!editor.isDisposed()) {
                        EditorMarkupModel markupModel = (EditorMarkupModel)editor.getMarkupModel();
                        PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
                        TrafficLightRenderer.setOrRefreshErrorStripeRenderer(markupModel, myProject, editor.getDocument(), file);
                    }
                }
            }, ModalityState.stateForComponent(editor.getComponent()), myProject.getDisposed());
        }

        for (Pair<PsiElement, Boolean> changedElement : toUpdate) {
            PsiElement element = changedElement.getFirst();
            Boolean whiteSpaceOptimizationAllowed = changedElement.getSecond();
            updateByChange(element, document, whiteSpaceOptimizationAllowed);
        }
        changedElements.remove(document);
    }

    @Override
    public void childAdded( PsiTreeChangeEvent event) {
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void childRemoved( PsiTreeChangeEvent event) {
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void childReplaced( PsiTreeChangeEvent event) {
        queueElement(event.getNewChild(), typesEqual(event.getNewChild(), event.getOldChild()), event);
    }

    private static boolean typesEqual(final PsiElement newChild, final PsiElement oldChild) {
        return newChild != null && oldChild != null && newChild.getClass() == oldChild.getClass();
    }

    @Override
    public void childrenChanged( PsiTreeChangeEvent event) {
        if (((PsiTreeChangeEventImpl)event).isGenericChange()) {
            return;
        }
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void beforeChildMovement( PsiTreeChangeEvent event) {
        queueElement(event.getOldParent(), true, event);
        queueElement(event.getNewParent(), true, event);
    }

    @Override
    public void beforeChildrenChange( PsiTreeChangeEvent event) {
        // this event sent always before every PSI change, even not significant one (like after quick typing/backspacing char)
        // mark file dirty just in case
        PsiFile psiFile = event.getFile();
        if (psiFile != null) {
            myFileStatusMap.markFileScopeDirtyDefensively(psiFile);
        }
    }

    @Override
    public void propertyChanged( PsiTreeChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (!propertyName.equals(PsiTreeChangeEvent.PROP_WRITABLE)) {
            myFileStatusMap.markAllFilesDirty();
        }
    }

    private void queueElement(PsiElement child, final boolean whitespaceOptimizationAllowed, PsiTreeChangeEvent event) {
        PsiFile file = event.getFile();
        if (file == null) file = child.getContainingFile();
        if (file == null) {
            myFileStatusMap.markAllFilesDirty();
            return;
        }

        if (!child.isValid()) return;
        Document document = PsiDocumentManager.getInstance(myProject).getCachedDocument(file);
        if (document != null) {
            List<Pair<PsiElement, Boolean>> toUpdate = changedElements.get(document);
            if (toUpdate == null) {
                toUpdate = new SmartList<Pair<PsiElement, Boolean>>();
                changedElements.put(document, toUpdate);
            }
            toUpdate.add(Pair.create(child, whitespaceOptimizationAllowed));
        }
    }

    private void updateByChange( PsiElement child,  final Document document, final boolean whitespaceOptimizationAllowed) {
        final PsiFile file;
        try {
            file = child.getContainingFile();
        }
        catch (PsiInvalidElementAccessException e) {
            myFileStatusMap.markAllFilesDirty();
            return;
        }
        if (file == null || file instanceof PsiCompiledElement) {
            myFileStatusMap.markAllFilesDirty();
            return;
        }

        int fileLength = file.getTextLength();
        if (!file.getViewProvider().isPhysical()) {
            myFileStatusMap.markFileScopeDirty(document, new TextRange(0, fileLength), fileLength);
            return;
        }

        PsiElement element = whitespaceOptimizationAllowed && UpdateHighlightersUtil.isWhitespaceOptimizationAllowed(document) ? child : child.getParent();
        while (true) {
            if (element == null || element instanceof PsiFile || element instanceof PsiDirectory) {
                myFileStatusMap.markAllFilesDirty();
                return;
            }

            final PsiElement scope = getChangeHighlightingScope(element);
            if (scope != null) {
                myFileStatusMap.markFileScopeDirty(document, scope.getTextRange(), fileLength);
                return;
            }

            element = element.getParent();
        }
    }

    
    private static PsiElement getChangeHighlightingScope(PsiElement element) {
        DefaultChangeLocalityDetector defaultDetector = null;
        for (ChangeLocalityDetector detector : Extensions.getExtensions(EP_NAME)) {
            if (detector instanceof DefaultChangeLocalityDetector) {
                // run default detector last
                assert defaultDetector == null : defaultDetector;
                defaultDetector = (DefaultChangeLocalityDetector)detector;
                continue;
            }
            final PsiElement scope = detector.getChangeHighlightingDirtyScopeFor(element);
            if (scope != null) return scope;
        }
        assert defaultDetector != null : "com.intellij.codeInsight.daemon.impl.DefaultChangeLocalityDetector is unregistered";
        return defaultDetector.getChangeHighlightingDirtyScopeFor(element);
    }
}
