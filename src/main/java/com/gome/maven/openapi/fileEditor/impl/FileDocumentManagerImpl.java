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
package com.gome.maven.openapi.fileEditor.impl;

import com.gome.maven.AppTopics;
import com.gome.maven.CommonBundle;
import com.gome.maven.codeStyle.CodeStyleFacade;
import com.gome.maven.diff.DiffContentFactory;
import com.gome.maven.diff.DiffManager;
import com.gome.maven.diff.DiffRequestPanel;
import com.gome.maven.diff.contents.DocumentContent;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.requests.SimpleDiffRequest;
import com.gome.maven.diff.util.DiffUserDataKeys;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.command.UndoConfirmationPolicy;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.ex.DocumentEx;
import com.gome.maven.openapi.editor.ex.EditorSettingsExternalizable;
import com.gome.maven.openapi.editor.impl.EditorFactoryImpl;
import com.gome.maven.openapi.editor.impl.TrailingSpacesStripper;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.*;
import com.gome.maven.openapi.fileEditor.impl.text.TextEditorImpl;
import com.gome.maven.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.UnknownFileType;
import com.gome.maven.openapi.project.*;
import com.gome.maven.openapi.project.ex.ProjectEx;
import com.gome.maven.openapi.ui.DialogBuilder;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.io.FileUtilRt;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFileSystem;
import com.gome.maven.pom.core.impl.PomModelImpl;
import com.gome.maven.psi.ExternalChangeAction;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.SingleRootFileViewProvider;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.ui.UIBundle;
import com.gome.maven.ui.components.JBScrollPane;
import com.gome.maven.util.Function;
import com.gome.maven.util.PairProcessor;
import com.gome.maven.util.ThrowableRunnable;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.List;

public class FileDocumentManagerImpl extends FileDocumentManager implements VirtualFileListener,
        ProjectManagerListener, SafeWriteRequestor {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.fileEditor.impl.FileDocumentManagerImpl");

    private static final Key<String> LINE_SEPARATOR_KEY = Key.create("LINE_SEPARATOR_KEY");
    public static final Key<Document> HARD_REF_TO_DOCUMENT_KEY = Key.create("HARD_REF_TO_DOCUMENT_KEY");
    private static final Key<VirtualFile> FILE_KEY = Key.create("FILE_KEY");
    private static final Key<Boolean> MUST_RECOMPUTE_FILE_TYPE = Key.create("Must recompute file type");

    private final Set<Document> myUnsavedDocuments = ContainerUtil.newConcurrentSet();
    private final Map<VirtualFile, Document> myDocuments = ContainerUtil.createConcurrentWeakValueMap();

    private final MessageBus myBus;

    private static final Object lock = new Object();
    private final FileDocumentManagerListener myMultiCaster;
    private final TrailingSpacesStripper myTrailingSpacesStripper = new TrailingSpacesStripper();

    private boolean myOnClose = false;

    public FileDocumentManagerImpl( VirtualFileManager virtualFileManager,  ProjectManager projectManager) {
        virtualFileManager.addVirtualFileListener(this);
        projectManager.addProjectManagerListener(this);

        myBus = ApplicationManager.getApplication().getMessageBus();
        InvocationHandler handler = new InvocationHandler() {
            
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                multiCast(method, args);
                return null;
            }
        };

        final ClassLoader loader = FileDocumentManagerListener.class.getClassLoader();
        myMultiCaster = (FileDocumentManagerListener)Proxy.newProxyInstance(loader, new Class[]{FileDocumentManagerListener.class}, handler);
    }

    private static void unwrapAndRethrow(Exception e) {
        Throwable unwrapped = e;
        if (e instanceof InvocationTargetException) {
            unwrapped = e.getCause() == null ? e : e.getCause();
        }
        if (unwrapped instanceof Error) throw (Error)unwrapped;
        if (unwrapped instanceof RuntimeException) throw (RuntimeException)unwrapped;
        LOG.error(unwrapped);
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    private void multiCast( Method method, Object[] args) {
        try {
            method.invoke(myBus.syncPublisher(AppTopics.FILE_DOCUMENT_SYNC), args);
        }
        catch (ClassCastException e) {
            LOG.error("Arguments: "+ Arrays.toString(args), e);
        }
        catch (Exception e) {
            unwrapAndRethrow(e);
        }

        // Allows pre-save document modification
        for (FileDocumentManagerListener listener : getListeners()) {
            try {
                method.invoke(listener, args);
            }
            catch (Exception e) {
                unwrapAndRethrow(e);
            }
        }

        // stripping trailing spaces
        try {
            method.invoke(myTrailingSpacesStripper, args);
        }
        catch (Exception e) {
            unwrapAndRethrow(e);
        }
    }

    @Override
    
    public Document getDocument( final VirtualFile file) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        DocumentEx document = (DocumentEx)getCachedDocument(file);
        if (document == null) {
            if (!file.isValid() || file.isDirectory() ||
                    SingleRootFileViewProvider.isTooLargeForContentLoading(file) ||
                    isBinaryWithoutDecompiler(file)) {
                return null;
            }

            final CharSequence text = LoadTextUtil.loadText(file);
            synchronized (lock) {
                document = (DocumentEx)getCachedDocument(file);
                if (document != null) return document; // Double checking

                document = (DocumentEx)createDocument(text, file);
                document.setModificationStamp(file.getModificationStamp());
                final FileType fileType = file.getFileType();
                document.setReadOnly(!file.isWritable() || fileType.isBinary());
                if (file instanceof LightVirtualFile) {
                    registerDocument(document, file);
                } else {
                    myDocuments.put(file, document);
                    document.putUserData(FILE_KEY, file);
                }

                if (!(file instanceof LightVirtualFile || file.getFileSystem() instanceof NonPhysicalFileSystem)) {
                    document.addDocumentListener(
                            new DocumentAdapter() {
                                @Override
                                public void documentChanged(DocumentEvent e) {
                                    final Document document = e.getDocument();
                                    myUnsavedDocuments.add(document);
                                    final Runnable currentCommand = CommandProcessor.getInstance().getCurrentCommand();
                                    Project project = currentCommand == null ? null : CommandProcessor.getInstance().getCurrentCommandProject();
                                    String lineSeparator = CodeStyleFacade.getInstance(project).getLineSeparator();
                                    document.putUserData(LINE_SEPARATOR_KEY, lineSeparator);

                                    // avoid documents piling up during batch processing
                                    if (areTooManyDocumentsInTheQueue(myUnsavedDocuments)) {
                                        saveAllDocumentsLater();
                                    }
                                }
                            }
                    );
                }
            }

            myMultiCaster.fileContentLoaded(file, document);
        }

        return document;
    }

    public static boolean areTooManyDocumentsInTheQueue(Collection<Document> documents) {
        if (documents.size() > 100) return true;
        int totalSize = 0;
        for (Document document : documents) {
            totalSize += document.getTextLength();
            if (totalSize > 10 * FileUtilRt.MEGABYTE) return true;
        }
        return false;
    }

    private static Document createDocument(final CharSequence text, VirtualFile file) {
        boolean acceptSlashR = file instanceof LightVirtualFile && StringUtil.indexOf(text, '\r') >= 0;
        return ((EditorFactoryImpl)EditorFactory.getInstance()).createDocument(text, acceptSlashR, false);
    }

    @Override
    
    public Document getCachedDocument( VirtualFile file) {
        Document hard = file.getUserData(HARD_REF_TO_DOCUMENT_KEY);
        return hard != null ? hard : myDocuments.get(file);
    }

    public static void registerDocument( final Document document,  VirtualFile virtualFile) {
        synchronized (lock) {
            virtualFile.putUserData(HARD_REF_TO_DOCUMENT_KEY, document);
            document.putUserData(FILE_KEY, virtualFile);
        }
    }

    @Override
    
    public VirtualFile getFile( Document document) {
        return document.getUserData(FILE_KEY);
    }

    
    public void dropAllUnsavedDocuments() {
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            throw new RuntimeException("This method is only for test mode!");
        }
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        if (!myUnsavedDocuments.isEmpty()) {
            myUnsavedDocuments.clear();
            fireUnsavedDocumentsDropped();
        }
    }

    private void saveAllDocumentsLater() {
        // later because some document might have been blocked by PSI right now
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (ApplicationManager.getApplication().isDisposed()) {
                    return;
                }
                final Document[] unsavedDocuments = getUnsavedDocuments();
                for (Document document : unsavedDocuments) {
                    VirtualFile file = getFile(document);
                    if (file == null) continue;
                    Project project = ProjectUtil.guessProjectForFile(file);
                    if (project == null) continue;
                    if (PsiDocumentManager.getInstance(project).isDocumentBlockedByPsi(document)) continue;

                    saveDocument(document);
                }
            }
        });
    }

    @Override
    public void saveAllDocuments() {
        saveAllDocuments(true);
    }

    /**
     * @param isExplicit caused by user directly (Save action) or indirectly (e.g. Compile)
     */
    public void saveAllDocuments(boolean isExplicit) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        myMultiCaster.beforeAllDocumentsSaving();
        if (myUnsavedDocuments.isEmpty()) return;

        final Map<Document, IOException> failedToSave = new HashMap<Document, IOException>();
        final Set<Document> vetoed = new HashSet<Document>();
        while (true) {
            int count = 0;

            for (Document document : myUnsavedDocuments) {
                if (failedToSave.containsKey(document)) continue;
                if (vetoed.contains(document)) continue;
                try {
                    doSaveDocument(document, isExplicit);
                }
                catch (IOException e) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    failedToSave.put(document, e);
                }
                catch (SaveVetoException e) {
                    vetoed.add(document);
                }
                count++;
            }

            if (count == 0) break;
        }

        if (!failedToSave.isEmpty()) {
            handleErrorsOnSave(failedToSave);
        }
    }

    @Override
    public void saveDocument( final Document document) {
        saveDocument(document, true);
    }

    public void saveDocument( final Document document, final boolean explicit) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (!myUnsavedDocuments.contains(document)) return;

        try {
            doSaveDocument(document, explicit);
        }
        catch (IOException e) {
            handleErrorsOnSave(Collections.singletonMap(document, e));
        }
        catch (SaveVetoException ignored) {
        }
    }

    @Override
    public void saveDocumentAsIs( Document document) {
        EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();

        String trailer = editorSettings.getStripTrailingSpaces();
        boolean ensureEOLonEOF = editorSettings.isEnsureNewLineAtEOF();
        editorSettings.setStripTrailingSpaces(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE);
        editorSettings.setEnsureNewLineAtEOF(false);
        try {
            saveDocument(document);
        }
        finally {
            editorSettings.setStripTrailingSpaces(trailer);
            editorSettings.setEnsureNewLineAtEOF(ensureEOLonEOF);
        }
    }

    private static class SaveVetoException extends Exception {}

    private void doSaveDocument( final Document document, boolean isExplicit) throws IOException, SaveVetoException {
        VirtualFile file = getFile(document);

        if (file == null || file instanceof LightVirtualFile || file.isValid() && !isFileModified(file)) {
            removeFromUnsaved(document);
            return;
        }

        if (file.isValid() && needsRefresh(file)) {
            file.refresh(false, false);
            if (!myUnsavedDocuments.contains(document)) return;
        }

        for (FileDocumentSynchronizationVetoer vetoer : Extensions.getExtensions(FileDocumentSynchronizationVetoer.EP_NAME)) {
            if (!vetoer.maySaveDocument(document, isExplicit)) {
                throw new SaveVetoException();
            }
        }

        final AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(getClass());
        try {
            doSaveDocumentInWriteAction(document, file);
        }
        finally {
            token.finish();
        }
    }

    private void doSaveDocumentInWriteAction( final Document document,  final VirtualFile file) throws IOException {
        if (!file.isValid()) {
            removeFromUnsaved(document);
            return;
        }

        if (!file.equals(getFile(document))) {
            registerDocument(document, file);
        }

        if (!isSaveNeeded(document, file)) {
            if (document instanceof DocumentEx) {
                ((DocumentEx)document).setModificationStamp(file.getModificationStamp());
            }
            removeFromUnsaved(document);
            updateModifiedProperty(file);
            return;
        }

        PomModelImpl.guardPsiModificationsIn(new ThrowableRunnable<IOException>() {
            @Override
            public void run() throws IOException {
                myMultiCaster.beforeDocumentSaving(document);
                LOG.assertTrue(file.isValid());

                String text = document.getText();
                String lineSeparator = getLineSeparator(document, file);
                if (!lineSeparator.equals("\n")) {
                    text = StringUtil.convertLineSeparators(text, lineSeparator);
                }

                Project project = ProjectLocator.getInstance().guessProjectForFile(file);
                LoadTextUtil.write(project, file, FileDocumentManagerImpl.this, text, document.getModificationStamp());

                myUnsavedDocuments.remove(document);
                LOG.assertTrue(!myUnsavedDocuments.contains(document));
                myTrailingSpacesStripper.clearLineModificationFlags(document);
            }
        });
    }

    private static void updateModifiedProperty( VirtualFile file) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            for (FileEditor editor : fileEditorManager.getAllEditors(file)) {
                if (editor instanceof TextEditorImpl) {
                    ((TextEditorImpl)editor).updateModifiedProperty();
                }
            }
        }
    }

    private void removeFromUnsaved( Document document) {
        myUnsavedDocuments.remove(document);
        fireUnsavedDocumentsDropped();
        LOG.assertTrue(!myUnsavedDocuments.contains(document));
    }

    private static boolean isSaveNeeded( Document document,  VirtualFile file) throws IOException {
        if (file.getFileType().isBinary() || document.getTextLength() > 1000 * 1000) {    // don't compare if the file is too big
            return true;
        }

        byte[] bytes = file.contentsToByteArray();
        CharSequence loaded = LoadTextUtil.getTextByBinaryPresentation(bytes, file, false, false);

        return !Comparing.equal(document.getCharsSequence(), loaded);
    }

    private static boolean needsRefresh(final VirtualFile file) {
        final VirtualFileSystem fs = file.getFileSystem();
        return fs instanceof NewVirtualFileSystem && file.getTimeStamp() != ((NewVirtualFileSystem)fs).getTimeStamp(file);
    }

    
    public static String getLineSeparator( Document document,  VirtualFile file) {
        String lineSeparator = LoadTextUtil.getDetectedLineSeparator(file);
        if (lineSeparator == null) {
            lineSeparator = document.getUserData(LINE_SEPARATOR_KEY);
            assert lineSeparator != null : document;
        }
        return lineSeparator;
    }

    @Override
    
    public String getLineSeparator( VirtualFile file,  Project project) {
        String lineSeparator = file == null ? null : LoadTextUtil.getDetectedLineSeparator(file);
        if (lineSeparator == null) {
            CodeStyleFacade settingsManager = project == null
                    ? CodeStyleFacade.getInstance()
                    : CodeStyleFacade.getInstance(project);
            lineSeparator = settingsManager.getLineSeparator();
        }
        return lineSeparator;
    }

    @Override
    public boolean requestWriting( Document document, Project project) {
        final VirtualFile file = getInstance().getFile(document);
        if (project != null && file != null && file.isValid()) {
            return !file.getFileType().isBinary() && ReadonlyStatusHandler.ensureFilesWritable(project, file);
        }
        if (document.isWritable()) {
            return true;
        }
        document.fireReadOnlyModificationAttempt();
        return false;
    }

    @Override
    public void reloadFiles( final VirtualFile... files) {
        for (VirtualFile file : files) {
            if (file.exists()) {
                final Document doc = getCachedDocument(file);
                if (doc != null) {
                    reloadFromDisk(doc);
                }
            }
        }
    }

    @Override
    
    public Document[] getUnsavedDocuments() {
        if (myUnsavedDocuments.isEmpty()) {
            return Document.EMPTY_ARRAY;
        }

        List<Document> list = new ArrayList<Document>(myUnsavedDocuments);
        return list.toArray(new Document[list.size()]);
    }

    @Override
    public boolean isDocumentUnsaved( Document document) {
        return myUnsavedDocuments.contains(document);
    }

    @Override
    public boolean isFileModified( VirtualFile file) {
        final Document doc = getCachedDocument(file);
        return doc != null && isDocumentUnsaved(doc) && doc.getModificationStamp() != file.getModificationStamp();
    }

    @Override
    public void propertyChanged( VirtualFilePropertyEvent event) {
        final VirtualFile file = event.getFile();
        if (VirtualFile.PROP_WRITABLE.equals(event.getPropertyName())) {
            final Document document = getCachedDocument(file);
            if (document != null) {
                ApplicationManager.getApplication().runWriteAction(new ExternalChangeAction() {
                    @Override
                    public void run() {
                        document.setReadOnly(!file.isWritable());
                    }
                });
            }
        }
        else if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
            Document document = getCachedDocument(file);
            if (document != null) {
                // a file is linked to a document - chances are it is an "unknown text file" now
                if (isBinaryWithoutDecompiler(file)) {
                    unbindFileFromDocument(file, document);
                }
            }
        }
    }

    private void unbindFileFromDocument( VirtualFile file,  Document document) {
        myDocuments.remove(file);
        file.putUserData(HARD_REF_TO_DOCUMENT_KEY, null);
        document.putUserData(FILE_KEY, null);
    }

    private static boolean isBinaryWithDecompiler( VirtualFile file) {
        final FileType ft = file.getFileType();
        return ft.isBinary() && BinaryFileTypeDecompilers.INSTANCE.forFileType(ft) != null;
    }

    private static boolean isBinaryWithoutDecompiler( VirtualFile file) {
        final FileType fileType = file.getFileType();
        return fileType.isBinary() && BinaryFileTypeDecompilers.INSTANCE.forFileType(fileType) == null;
    }

    @Override
    public void contentsChanged( VirtualFileEvent event) {
        if (event.isFromSave()) return;
        final VirtualFile file = event.getFile();
        final Document document = getCachedDocument(file);
        if (document == null) {
            myMultiCaster.fileWithNoDocumentChanged(file);
            return;
        }

        if (isBinaryWithDecompiler(file)) {
            myMultiCaster.fileWithNoDocumentChanged(file); // This will generate PSI event at FileManagerImpl
        }

        long documentStamp = document.getModificationStamp();
        long oldFileStamp = event.getOldModificationStamp();
        if (documentStamp != oldFileStamp) {
            LOG.info("reload " + file.getName() + " from disk?");
            LOG.info("  documentStamp:" + documentStamp);
            LOG.info("  oldFileStamp:" + oldFileStamp);

            if (file.isValid() && askReloadFromDisk(file, document)) {
                reloadFromDisk(document);
            }
        }
        else {
            reloadFromDisk(document);
        }
    }

    @Override
    public void reloadFromDisk( final Document document) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        final VirtualFile file = getFile(document);
        assert file != null;

        if (!fireBeforeFileContentReload(file, document)) {
            return;
        }

        if (file.getLength() > FileUtilRt.LARGE_FOR_CONTENT_LOADING) {
            unbindFileFromDocument(file, document);
            myUnsavedDocuments.remove(document);
            myMultiCaster.fileWithNoDocumentChanged(file);
            return;
        }

        final Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(
                        new ExternalChangeAction.ExternalDocumentChange(document, project) {
                            @Override
                            public void run() {
                                boolean wasWritable = document.isWritable();
                                DocumentEx documentEx = (DocumentEx)document;
                                documentEx.setReadOnly(false);
                                LoadTextUtil.setCharsetWasDetectedFromBytes(file, null);
                                file.setBOM(null); // reset BOM in case we had one and the external change stripped it away
                                if (!isBinaryWithoutDecompiler(file)) {
                                    documentEx.replaceText(LoadTextUtil.loadText(file), file.getModificationStamp());
                                    documentEx.setReadOnly(!wasWritable);
                                }
                            }
                        }
                );
            }
        }, UIBundle.message("file.cache.conflict.action"), null, UndoConfirmationPolicy.REQUEST_CONFIRMATION);

        myUnsavedDocuments.remove(document);

        myMultiCaster.fileContentReloaded(file, document);
    }

    private PairProcessor<VirtualFile, Document> askReloadFromDisk = new PairProcessor<VirtualFile, Document>() {
        @Override
        public boolean process(final VirtualFile file, final Document document) {
            String message = UIBundle.message("file.cache.conflict.message.text", file.getPresentableUrl());

            final DialogBuilder builder = new DialogBuilder();
            builder.setCenterPanel(new JLabel(message, Messages.getQuestionIcon(), SwingConstants.CENTER));
            builder.addOkAction().setText(UIBundle.message("file.cache.conflict.load.fs.changes.button"));
            builder.addCancelAction().setText(UIBundle.message("file.cache.conflict.keep.memory.changes.button"));
            builder.addAction(new AbstractAction(UIBundle.message("file.cache.conflict.show.difference.button")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final ProjectEx project = (ProjectEx)ProjectLocator.getInstance().guessProjectForFile(file);

                    FileType fileType = file.getFileType();
                    String fsContent = LoadTextUtil.loadText(file).toString();
                    DocumentContent content1 = DiffContentFactory.getInstance().create(fsContent, fileType);
                    DocumentContent content2 = DiffContentFactory.getInstance().create(project, document, file);
                    String title = UIBundle.message("file.cache.conflict.for.file.dialog.title", file.getPresentableUrl());
                    String title1 = UIBundle.message("file.cache.conflict.diff.content.file.system.content");
                    String title2 = UIBundle.message("file.cache.conflict.diff.content.memory.content");
                    DiffRequest request = new SimpleDiffRequest(title, content1, content2, title1, title2);
                    request.putUserData(DiffUserDataKeys.GO_TO_SOURCE_DISABLE, true);
                    DialogBuilder diffBuilder = new DialogBuilder(project);
                    DiffRequestPanel diffPanel = DiffManager.getInstance().createRequestPanel(project, diffBuilder, diffBuilder.getWindow());
                    diffPanel.setRequest(request);
                    diffBuilder.setCenterPanel(diffPanel.getComponent());
                    diffBuilder.setDimensionServiceKey("FileDocumentManager.FileCacheConflict");
                    diffBuilder.addOkAction().setText(UIBundle.message("file.cache.conflict.save.changes.button"));
                    diffBuilder.addCancelAction();
                    diffBuilder.setTitle(title);
                    if (diffBuilder.show() == DialogWrapper.OK_EXIT_CODE) {
                        builder.getDialogWrapper().close(DialogWrapper.CANCEL_EXIT_CODE);
                    }
                }
            });
            builder.setTitle(UIBundle.message("file.cache.conflict.dialog.title"));
            builder.setButtonsAlignment(SwingConstants.CENTER);
            builder.setHelpId("reference.dialogs.fileCacheConflict");
            return builder.show() == 0;
        }
    };

    
    public void setAskReloadFromDisk( Disposable disposable,
                                      PairProcessor<VirtualFile, Document> newProcessor) {
        final PairProcessor<VirtualFile, Document> old = askReloadFromDisk;
        askReloadFromDisk = newProcessor;
        Disposer.register(disposable, new Disposable() {
            @Override
            public void dispose() {
                askReloadFromDisk = old;
            }
        });
    }

    private boolean askReloadFromDisk(final VirtualFile file, final Document document) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (!isDocumentUnsaved(document)) return true;

        return askReloadFromDisk.process(file, document);
    }

    @Override
    public void fileCreated( VirtualFileEvent event) {
    }

    @Override
    public void fileDeleted( VirtualFileEvent event) {
        Document doc = getCachedDocument(event.getFile());
        if (doc != null) {
            myTrailingSpacesStripper.documentDeleted(doc);
        }
    }

    @Override
    public void fileMoved( VirtualFileMoveEvent event) {
    }

    @Override
    public void fileCopied( VirtualFileCopyEvent event) {
        fileCreated(event);
    }

    @Override
    public void beforePropertyChange( VirtualFilePropertyEvent event) {
    }

    @Override
    public void beforeContentsChange( VirtualFileEvent event) {
        VirtualFile virtualFile = event.getFile();
        // check file type in second order to avoid content detection running
        if (virtualFile.getLength() == 0 && virtualFile.getFileType() == UnknownFileType.INSTANCE) {
            virtualFile.putUserData(MUST_RECOMPUTE_FILE_TYPE, Boolean.TRUE);
        }
    }

    public static boolean recomputeFileTypeIfNecessary( VirtualFile virtualFile) {
        if (virtualFile.getUserData(MUST_RECOMPUTE_FILE_TYPE) != null) {
            virtualFile.getFileType();
            virtualFile.putUserData(MUST_RECOMPUTE_FILE_TYPE, null);
            return true;
        }
        return false;
    }

    @Override
    public void beforeFileDeletion( VirtualFileEvent event) {
    }

    @Override
    public void beforeFileMovement( VirtualFileMoveEvent event) {
    }

    @Override
    public void projectOpened(Project project) {
    }

    @Override
    public boolean canCloseProject(Project project) {
        if (!myUnsavedDocuments.isEmpty()) {
            myOnClose = true;
            try {
                saveAllDocuments();
            }
            finally {
                myOnClose = false;
            }
        }
        return myUnsavedDocuments.isEmpty();
    }

    @Override
    public void projectClosed(Project project) {
    }

    @Override
    public void projectClosing(Project project) {
    }

    private void fireUnsavedDocumentsDropped() {
        myMultiCaster.unsavedDocumentsDropped();
    }

    private boolean fireBeforeFileContentReload(final VirtualFile file,  Document document) {
        for (FileDocumentSynchronizationVetoer vetoer : Extensions.getExtensions(FileDocumentSynchronizationVetoer.EP_NAME)) {
            try {
                if (!vetoer.mayReloadFileContent(file, document)) {
                    return false;
                }
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }

        myMultiCaster.beforeFileContentReload(file, document);
        return true;
    }

    
    private static FileDocumentManagerListener[] getListeners() {
        return FileDocumentManagerListener.EP_NAME.getExtensions();
    }

    private void handleErrorsOnSave( Map<Document, IOException> failures) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            IOException ioException = failures.isEmpty() ? null : failures.values().iterator().next();
            if (ioException != null) {
                throw new RuntimeException(ioException);
            }
            return;
        }
        for (IOException exception : failures.values()) {
            LOG.warn(exception);
        }

        final String text = StringUtil.join(failures.values(), new Function<IOException, String>() {
            @Override
            public String fun(IOException e) {
                return e.getMessage();
            }
        }, "\n");

        final DialogWrapper dialog = new DialogWrapper(null) {
            {
                init();
                setTitle(UIBundle.message("cannot.save.files.dialog.title"));
            }

            @Override
            protected void createDefaultActions() {
                super.createDefaultActions();
                myOKAction.putValue(Action.NAME, UIBundle
                        .message(myOnClose ? "cannot.save.files.dialog.ignore.changes" : "cannot.save.files.dialog.revert.changes"));
                myOKAction.putValue(DEFAULT_ACTION, null);

                if (!myOnClose) {
                    myCancelAction.putValue(Action.NAME, CommonBundle.getCloseButtonText());
                }
            }

            @Override
            protected JComponent createCenterPanel() {
                final JPanel panel = new JPanel(new BorderLayout(0, 5));

                panel.add(new JLabel(UIBundle.message("cannot.save.files.dialog.message")), BorderLayout.NORTH);

                final JTextPane area = new JTextPane();
                area.setText(text);
                area.setEditable(false);
                area.setMinimumSize(new Dimension(area.getMinimumSize().width, 50));
                panel.add(new JBScrollPane(area, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                        BorderLayout.CENTER);

                return panel;
            }
        };

        if (dialog.showAndGet()) {
            for (Document document : failures.keySet()) {
                reloadFromDisk(document);
            }
        }
    }
}
