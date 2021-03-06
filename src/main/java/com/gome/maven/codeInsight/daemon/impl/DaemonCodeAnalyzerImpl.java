/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.codeHighlighting.BackgroundEditorHighlighter;
import com.gome.maven.codeHighlighting.HighlightingPass;
import com.gome.maven.codeHighlighting.Pass;
import com.gome.maven.codeHighlighting.TextEditorHighlightingPass;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzerSettingsImpl;
import com.gome.maven.codeInsight.daemon.LineMarkerInfo;
import com.gome.maven.codeInsight.daemon.ReferenceImporter;
import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.codeInsight.intention.impl.FileLevelIntentionComponent;
import com.gome.maven.codeInsight.intention.impl.IntentionHintComponent;
import com.gome.maven.ide.PowerSaveMode;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.editor.impl.DocumentMarkupModel;
import com.gome.maven.openapi.editor.markup.MarkupModel;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.TextEditor;
import com.gome.maven.openapi.fileEditor.ex.FileEditorManagerEx;
import com.gome.maven.openapi.fileEditor.impl.text.TextEditorProvider;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.fileTypes.impl.FileTypeManagerImpl;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.newvfs.RefreshQueueImpl;
import com.gome.maven.packageDependencies.DependencyValidationManager;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiDocumentManagerBase;
import com.gome.maven.psi.search.scope.packageSet.NamedScopeManager;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.*;
import com.gome.maven.util.io.storage.HeavyProcessLatch;
import com.gome.maven.util.ui.UIUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jdom.Element;

import java.util.*;

/**
 * This class also controls the auto-reparse and auto-hints.
 */
@State(
        name = "DaemonCodeAnalyzer",
        storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE)
)
public class DaemonCodeAnalyzerImpl extends DaemonCodeAnalyzerEx implements PersistentStateComponent<Element>, Disposable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl");

    private static final Key<List<LineMarkerInfo>> MARKERS_IN_EDITOR_DOCUMENT_KEY = Key.create("MARKERS_IN_EDITOR_DOCUMENT");
    private static final Key<List<HighlightInfo>> FILE_LEVEL_HIGHLIGHTS = Key.create("FILE_LEVEL_HIGHLIGHTS");
    private final Project myProject;
    private final DaemonCodeAnalyzerSettings mySettings;
     private final EditorTracker myEditorTracker;
     private final PsiDocumentManager myPsiDocumentManager;
    private DaemonProgressIndicator myUpdateProgress; //guarded by this

    private final Runnable myUpdateRunnable = createUpdateRunnable();

    private final Alarm myAlarm = new Alarm();
    private boolean myUpdateByTimerEnabled = true;
    private final Collection<VirtualFile> myDisabledHintsFiles = new THashSet<VirtualFile>();
    private final Collection<VirtualFile> myDisabledHighlightingFiles = new THashSet<VirtualFile>();

    private final FileStatusMap myFileStatusMap;
    private DaemonCodeAnalyzerSettings myLastSettings;

    private volatile IntentionHintComponent myLastIntentionHint;
    private volatile boolean myDisposed;     // the only possible transition: false -> true
    private volatile boolean myInitialized;  // the only possible transition: false -> true

     private static final String DISABLE_HINTS_TAG = "disable_hints";
     private static final String FILE_TAG = "file";
     private static final String URL_ATT = "url";
    private final PassExecutorService myPassExecutorService;

    private volatile boolean allowToInterrupt = true;

    public DaemonCodeAnalyzerImpl( Project project,
                                   DaemonCodeAnalyzerSettings daemonCodeAnalyzerSettings,
                                   EditorTracker editorTracker,
                                   PsiDocumentManager psiDocumentManager,
                                  @SuppressWarnings("UnusedParameters")  final NamedScopeManager namedScopeManager,
                                  @SuppressWarnings("UnusedParameters")  final DependencyValidationManager dependencyValidationManager) {
        myProject = project;
        mySettings = daemonCodeAnalyzerSettings;
        myEditorTracker = editorTracker;
        myPsiDocumentManager = psiDocumentManager;
        myLastSettings = ((DaemonCodeAnalyzerSettingsImpl)daemonCodeAnalyzerSettings).clone();

        myFileStatusMap = new FileStatusMap(project);
        myPassExecutorService = new PassExecutorService(project);
        Disposer.register(this, myPassExecutorService);
        Disposer.register(this, myFileStatusMap);
        DaemonProgressIndicator.setDebug(LOG.isDebugEnabled());

        assert !myInitialized : "Double Initializing";
        Disposer.register(this, new StatusBarUpdater(project));

        myInitialized = true;
        myDisposed = false;
        myFileStatusMap.markAllFilesDirty();
        Disposer.register(this, new Disposable() {
            @Override
            public void dispose() {
                assert myInitialized : "Disposing not initialized component";
                assert !myDisposed : "Double dispose";

                stopProcess(false, "Dispose");

                myDisposed = true;
                myLastSettings = null;
            }
        });
    }

    @Override
    public void dispose() {

    }

    
    
    public static List<HighlightInfo> getHighlights( Document document, HighlightSeverity minSeverity,  Project project) {
        List<HighlightInfo> infos = new ArrayList<HighlightInfo>();
        processHighlights(document, project, minSeverity, 0, document.getTextLength(),
                new CommonProcessors.CollectProcessor<HighlightInfo>(infos));
        return infos;
    }

    @Override
    
    
    public List<HighlightInfo> getFileLevelHighlights( Project project,  PsiFile file) {
        VirtualFile vFile = file.getViewProvider().getVirtualFile();
        final FileEditorManager manager = FileEditorManager.getInstance(project);
        List<HighlightInfo> result = new ArrayList<HighlightInfo>();
        for (FileEditor fileEditor : manager.getEditors(vFile)) {
            final List<HighlightInfo> infos = fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS);
            if (infos == null) continue;
            for (HighlightInfo info : infos) {
                result.add(info);
            }
        }
        return result;
    }

    @Override
    public void cleanFileLevelHighlights( Project project, final int group, PsiFile psiFile) {
        if (psiFile == null) return;
        FileViewProvider provider = psiFile.getViewProvider();
        VirtualFile vFile = provider.getVirtualFile();
        final FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor fileEditor : manager.getEditors(vFile)) {
            final List<HighlightInfo> infos = fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS);
            if (infos == null) continue;
            List<HighlightInfo> infosToRemove = new ArrayList<HighlightInfo>();
            for (HighlightInfo info : infos) {
                if (info.getGroup() == group) {
                    manager.removeTopComponent(fileEditor, info.fileLevelComponent);
                    infosToRemove.add(info);
                }
            }
            infos.removeAll(infosToRemove);
        }
    }

    @Override
    public void addFileLevelHighlight( final Project project,
                                      final int group,
                                       final HighlightInfo info,
                                       final PsiFile psiFile) {
        VirtualFile vFile = psiFile.getViewProvider().getVirtualFile();
        final FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor fileEditor : manager.getEditors(vFile)) {
            if (fileEditor instanceof TextEditor) {
                FileLevelIntentionComponent component = new FileLevelIntentionComponent(info.getDescription(), info.getSeverity(),
                        info.getGutterIconRenderer(), info.quickFixActionRanges,
                        project, psiFile, ((TextEditor)fileEditor).getEditor());
                manager.addTopComponent(fileEditor, component);
                List<HighlightInfo> fileLevelInfos = fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS);
                if (fileLevelInfos == null) {
                    fileLevelInfos = new ArrayList<HighlightInfo>();
                    fileEditor.putUserData(FILE_LEVEL_HIGHLIGHTS, fileLevelInfos);
                }
                info.fileLevelComponent = component;
                info.setGroup(group);
                fileLevelInfos.add(info);
            }
        }
    }

    @Override
    
    public List<HighlightInfo> runMainPasses( PsiFile psiFile,
                                              Document document,
                                              final ProgressIndicator progress) {
        setUpdateByTimerEnabled(true); // by default we disable daemon while in modal dialog, but here we need to re-enable it because otherwise the paused daemon will conflict with our started passes
        restart(); // clear status maps to run passes from scratch so that refCountHolder won't conflict and try to restart itself on partially filled maps

        final List<HighlightInfo> result = new ArrayList<HighlightInfo>();
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile != null && !virtualFile.getFileType().isBinary()) {
            List<TextEditorHighlightingPass> passes =
                    TextEditorHighlightingPassRegistrarEx.getInstanceEx(myProject).instantiateMainPasses(psiFile, document,
                            HighlightInfoProcessor.getEmpty());

            Collections.sort(passes, new Comparator<TextEditorHighlightingPass>() {
                @Override
                public int compare(TextEditorHighlightingPass o1, TextEditorHighlightingPass o2) {
                    if (o1 instanceof GeneralHighlightingPass) return -1;
                    if (o2 instanceof GeneralHighlightingPass) return 1;
                    return 0;
                }
            });

            LOG.debug("All passes for " + psiFile.getName()+ " started (" + passes+"). progress canceled: "+progress.isCanceled());
            try {
                for (TextEditorHighlightingPass pass : passes) {
                    pass.doCollectInformation(progress);
                    result.addAll(pass.getInfos());
                }
            }
            catch (ProcessCanceledException e) {
                LOG.debug("Canceled: " + progress);
                throw e;
            }
            LOG.debug("All passes for " + psiFile.getName()+ " run. progress canceled: "+progress.isCanceled()+"; infos: "+result);
        }

        return result;
    }

    
    
    public List<HighlightInfo> runPasses( PsiFile file,
                                          Document document,
                                          TextEditor textEditor,
                                          int[] toIgnore,
                                         boolean canChangeDocument,
                                          Runnable callbackWhileWaiting) throws ProcessCanceledException {
        return runPasses(file, document, Collections.singletonList(textEditor), toIgnore, canChangeDocument, callbackWhileWaiting);
    }

    
    
    List<HighlightInfo> runPasses( PsiFile file,
                                   Document document,
                                   List<TextEditor> textEditors,
                                   int[] toIgnore,
                                  boolean canChangeDocument,
                                   Runnable callbackWhileWaiting) throws ProcessCanceledException {
        assert myInitialized;
        assert !myDisposed;
        ApplicationEx application = ApplicationManagerEx.getApplicationEx();
        application.assertIsDispatchThread();
        if (application.isWriteAccessAllowed()) {
            throw new AssertionError("Must not start highlighting from within write action, or deadlock is imminent");
        }
        DaemonProgressIndicator.setDebug(true);
        ((FileTypeManagerImpl)FileTypeManager.getInstance()).drainReDetectQueue();
        // pump first so that queued event do not interfere
        UIUtil.dispatchAllInvocationEvents();

        // refresh will fire write actions interfering with highlighting
        while (RefreshQueueImpl.isRefreshInProgress() || HeavyProcessLatch.INSTANCE.isRunning()) {
            UIUtil.dispatchAllInvocationEvents();
        }

        UIUtil.dispatchAllInvocationEvents();

        Project project = file.getProject();
        FileStatusMap.getAndClearLog();
        FileStatusMap fileStatusMap = getFileStatusMap();
        fileStatusMap.allowDirt(canChangeDocument);

        Map<FileEditor, HighlightingPass[]> map = new HashMap<FileEditor, HighlightingPass[]>();
        for (TextEditor textEditor : textEditors) {
            TextEditorBackgroundHighlighter highlighter = (TextEditorBackgroundHighlighter)textEditor.getBackgroundHighlighter();
            final List<TextEditorHighlightingPass> passes = highlighter.getPasses(toIgnore);
            HighlightingPass[] array = passes.toArray(new HighlightingPass[passes.size()]);
            assert array.length != 0 : "Highlighting is disabled for the file " + file;
            map.put(textEditor, array);
        }
        for (int ignoreId : toIgnore) {
            fileStatusMap.markFileUpToDate(document, ignoreId);
        }

        final DaemonProgressIndicator progress = createUpdateProgress();
        myPassExecutorService.submitPasses(map, progress);
        try {
            while (progress.isRunning()) {
                try {
                    progress.checkCanceled();
                    if (callbackWhileWaiting != null) {
                        callbackWhileWaiting.run();
                    }
                    myPassExecutorService.waitFor(50);
                    UIUtil.dispatchAllInvocationEvents();
                    Throwable savedException = PassExecutorService.getSavedException(progress);
                    if (savedException != null) throw savedException;
                }
                catch (RuntimeException e) {
                    throw e;
                }
                catch (Error e) {
                    throw e;
                }
                catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            UIUtil.dispatchAllInvocationEvents();
            UIUtil.dispatchAllInvocationEvents();
            assert progress.isCanceled() && progress.isDisposed();

            return getHighlights(document, null, project);
        }
        finally {
            DaemonProgressIndicator.setDebug(false);
            String log = FileStatusMap.getAndClearLog();
            fileStatusMap.allowDirt(true);
            try {
                waitForTermination();
            }
            catch (Throwable e) {
                LOG.error(log, e);
            }
        }
    }

    
    public void prepareForTest() {
        setUpdateByTimerEnabled(false);
        waitForTermination();
    }

    
    public void cleanupAfterTest() {
        if (!myProject.isOpen()) return;
        setUpdateByTimerEnabled(false);
        waitForTermination();
    }

    void waitForTermination() {
        myPassExecutorService.cancelAll(true);
    }

    @Override
    public void settingsChanged() {
        DaemonCodeAnalyzerSettings settings = DaemonCodeAnalyzerSettings.getInstance();
        if (settings.isCodeHighlightingChanged(myLastSettings)) {
            restart();
        }
        myLastSettings = ((DaemonCodeAnalyzerSettingsImpl)settings).clone();
    }

    @Override
    public void updateVisibleHighlighters( Editor editor) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        // no need, will not work anyway
    }

    @Override
    public void setUpdateByTimerEnabled(boolean value) {
        myUpdateByTimerEnabled = value;
        stopProcess(value, "Update by timer change");
    }

    private int myDisableCount = 0;

    @Override
    public void disableUpdateByTimer( Disposable parentDisposable) {
        setUpdateByTimerEnabled(false);
        myDisableCount++;
        ApplicationManager.getApplication().assertIsDispatchThread();

        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                myDisableCount--;
                if (myDisableCount == 0) {
                    setUpdateByTimerEnabled(true);
                }
            }
        });
    }

    boolean isUpdateByTimerEnabled() {
        return myUpdateByTimerEnabled;
    }

    @Override
    public void setImportHintsEnabled( PsiFile file, boolean value) {
        VirtualFile vFile = file.getVirtualFile();
        if (value) {
            myDisabledHintsFiles.remove(vFile);
            stopProcess(true, "Import hints change");
        }
        else {
            myDisabledHintsFiles.add(vFile);
            HintManager.getInstance().hideAllHints();
        }
    }

    @Override
    public void resetImportHintsEnabledForProject() {
        myDisabledHintsFiles.clear();
    }

    @Override
    public void setHighlightingEnabled( PsiFile file, boolean value) {
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(file);
        if (value) {
            myDisabledHighlightingFiles.remove(virtualFile);
        }
        else {
            myDisabledHighlightingFiles.add(virtualFile);
        }
    }

    @Override
    public boolean isHighlightingAvailable( PsiFile file) {
        if (file == null || !file.isPhysical()) return false;
        if (myDisabledHighlightingFiles.contains(PsiUtilCore.getVirtualFile(file))) return false;

        if (file instanceof PsiCompiledElement) return false;
        final FileType fileType = file.getFileType();

        // To enable T.O.D.O. highlighting
        return !fileType.isBinary();
    }

    @Override
    public boolean isImportHintsEnabled( PsiFile file) {
        return isAutohintsAvailable(file) && !myDisabledHintsFiles.contains(file.getVirtualFile());
    }

    @Override
    public boolean isAutohintsAvailable(PsiFile file) {
        return isHighlightingAvailable(file) && !(file instanceof PsiCompiledElement);
    }

    @Override
    public void restart() {
        myFileStatusMap.markAllFilesDirty();
        stopProcess(true, "Global restart");
    }

    @Override
    public void restart( PsiFile file) {
        Document document = myPsiDocumentManager.getCachedDocument(file);
        if (document == null) return;
        myFileStatusMap.markFileScopeDirty(document, new TextRange(0, document.getTextLength()), file.getTextLength());
        stopProcess(true, "Psi file restart");
    }

    
    List<TextEditorHighlightingPass> getPassesToShowProgressFor(Document document) {
        List<TextEditorHighlightingPass> allPasses = myPassExecutorService.getAllSubmittedPasses();
        List<TextEditorHighlightingPass> result = new ArrayList<TextEditorHighlightingPass>(allPasses.size());
        for (TextEditorHighlightingPass pass : allPasses) {
            if (pass.getDocument() == document || pass.getDocument() == null) {
                result.add(pass);
            }
        }
        return result;
    }

    boolean isAllAnalysisFinished( PsiFile file) {
        if (myDisposed) return false;
        Document document = myPsiDocumentManager.getCachedDocument(file);
        return document != null &&
                document.getModificationStamp() == file.getViewProvider().getModificationStamp() &&
                myFileStatusMap.allDirtyScopesAreNull(document);
    }

    @Override
    public boolean isErrorAnalyzingFinished( PsiFile file) {
        if (myDisposed) return false;
        Document document = myPsiDocumentManager.getCachedDocument(file);
        return document != null &&
                document.getModificationStamp() == file.getViewProvider().getModificationStamp() &&
                myFileStatusMap.getFileDirtyScope(document, Pass.UPDATE_ALL) == null;
    }

    @Override
    
    public FileStatusMap getFileStatusMap() {
        return myFileStatusMap;
    }

    synchronized boolean isRunning() {
        return myUpdateProgress != null && !myUpdateProgress.isCanceled();
    }

    synchronized void stopProcess(boolean toRestartAlarm,  String reason) {
        if (!allowToInterrupt) throw new RuntimeException("Cannot interrupt daemon");

        cancelUpdateProgress(toRestartAlarm, reason);
        myAlarm.cancelAllRequests();
        boolean restart = toRestartAlarm && !myDisposed && myInitialized;
        if (restart) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    myAlarm.addRequest(myUpdateRunnable, mySettings.AUTOREPARSE_DELAY);
                }
            });
        }
    }

    private synchronized void cancelUpdateProgress(final boolean start,  String reason) {
        PassExecutorService.log(myUpdateProgress, null, "CancelX", reason, start);

        if (myUpdateProgress != null) {
            myUpdateProgress.cancel();
            myPassExecutorService.cancelAll(false);
            myUpdateProgress = null;
        }
    }


    static boolean processHighlightsNearOffset( Document document,
                                                Project project,
                                                final HighlightSeverity minSeverity,
                                               final int offset,
                                               final boolean includeFixRange,
                                                final Processor<HighlightInfo> processor) {
        return processHighlights(document, project, null, 0, document.getTextLength(), new Processor<HighlightInfo>() {
            @Override
            public boolean process( HighlightInfo info) {
                if (!isOffsetInsideHighlightInfo(offset, info, includeFixRange)) return true;

                int compare = info.getSeverity().compareTo(minSeverity);
                return compare < 0 || processor.process(info);
            }
        });
    }

    
    public HighlightInfo findHighlightByOffset( Document document, final int offset, final boolean includeFixRange) {
        return findHighlightByOffset(document, offset, includeFixRange, HighlightSeverity.INFORMATION);
    }

    
    HighlightInfo findHighlightByOffset( Document document,
                                        final int offset,
                                        final boolean includeFixRange,
                                         HighlightSeverity minSeverity) {
        final List<HighlightInfo> foundInfoList = new SmartList<HighlightInfo>();
        processHighlightsNearOffset(document, myProject, minSeverity, offset, includeFixRange,
                new Processor<HighlightInfo>() {
                    @Override
                    public boolean process( HighlightInfo info) {
                        if (info.getSeverity() == HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY) {
                            return true;
                        }
                        if (!foundInfoList.isEmpty()) {
                            HighlightInfo foundInfo = foundInfoList.get(0);
                            int compare = foundInfo.getSeverity().compareTo(info.getSeverity());
                            if (compare < 0) {
                                foundInfoList.clear();
                            }
                            else if (compare > 0) {
                                return true;
                            }
                        }
                        foundInfoList.add(info);
                        return true;
                    }
                });

        if (foundInfoList.isEmpty()) return null;
        if (foundInfoList.size() == 1) return foundInfoList.get(0);
        return new HighlightInfoComposite(foundInfoList);
    }

    private static boolean isOffsetInsideHighlightInfo(int offset,  HighlightInfo info, boolean includeFixRange) {
        RangeHighlighterEx highlighter = info.highlighter;
        if (highlighter == null || !highlighter.isValid()) return false;
        int startOffset = highlighter.getStartOffset();
        int endOffset = highlighter.getEndOffset();
        if (startOffset <= offset && offset <= endOffset) {
            return true;
        }
        if (!includeFixRange) return false;
        RangeMarker fixMarker = info.fixMarker;
        if (fixMarker != null) {  // null means its range is the same as highlighter
            if (!fixMarker.isValid()) return false;
            startOffset = fixMarker.getStartOffset();
            endOffset = fixMarker.getEndOffset();
            return startOffset <= offset && offset <= endOffset;
        }
        return false;
    }

    
    public static List<LineMarkerInfo> getLineMarkers( Document document, Project project) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
        return ObjectUtils.notNull(markup.getUserData(MARKERS_IN_EDITOR_DOCUMENT_KEY), Collections.<LineMarkerInfo>emptyList());
    }

    static void setLineMarkers( Document document, List<LineMarkerInfo> lineMarkers, Project project) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
        markup.putUserData(MARKERS_IN_EDITOR_DOCUMENT_KEY, lineMarkers);
    }

    void setLastIntentionHint( Project project,
                               PsiFile file,
                               Editor editor,
                               ShowIntentionsPass.IntentionsInfo intentions,
                              boolean hasToRecreate) {
        if (!editor.getSettings().isShowIntentionBulb()) {
            return;
        }
        ApplicationManager.getApplication().assertIsDispatchThread();
        hideLastIntentionHint();

        if (editor.getCaretModel().getCaretCount() > 1) return;

        IntentionHintComponent hintComponent = IntentionHintComponent.showIntentionHint(project, file, editor, intentions, false);
        if (hasToRecreate) {
            hintComponent.recreate();
        }
        myLastIntentionHint = hintComponent;
    }

    void hideLastIntentionHint() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        IntentionHintComponent hint = myLastIntentionHint;
        if (hint != null && hint.isVisible()) {
            hint.hide();
            myLastIntentionHint = null;
        }
    }

    
    IntentionHintComponent getLastIntentionHint() {
        return myLastIntentionHint;
    }

    
    @Override
    public Element getState() {
        Element state = new Element("state");
        if (myDisabledHintsFiles.isEmpty()) {
            return state;
        }

        List<String> array = new SmartList<String>();
        for (VirtualFile file : myDisabledHintsFiles) {
            if (file.isValid()) {
                array.add(file.getUrl());
            }
        }

        if (!array.isEmpty()) {
            Collections.sort(array);

            Element disableHintsElement = new Element(DISABLE_HINTS_TAG);
            state.addContent(disableHintsElement);
            for (String url : array) {
                disableHintsElement.addContent(new Element(FILE_TAG).setAttribute(URL_ATT, url));
            }
        }
        return state;
    }

    @Override
    public void loadState(Element state) {
        myDisabledHintsFiles.clear();

        Element element = state.getChild(DISABLE_HINTS_TAG);
        if (element != null) {
            for (Element e :(List<Element>) element.getChildren(FILE_TAG)) {
                String url = e.getAttributeValue(URL_ATT);
                if (url != null) {
                    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
                    if (file != null) {
                        myDisabledHintsFiles.add(file);
                    }
                }
            }
        }
    }

    private final Runnable submitPassesRunnable = new Runnable() {
        @Override
        public void run() {
            PassExecutorService.log(getUpdateProgress(), null, "Update Runnable. myUpdateByTimerEnabled:",
                    myUpdateByTimerEnabled, " something disposed:",
                    PowerSaveMode.isEnabled() || myDisposed || !myProject.isInitialized(), " activeEditors:",
                    myProject.isDisposed() ? null : getSelectedEditors());
            if (!myUpdateByTimerEnabled) return;
            if (myDisposed) return;
            ApplicationManager.getApplication().assertIsDispatchThread();

            final Collection<FileEditor> activeEditors = getSelectedEditors();
            if (activeEditors.isEmpty()) return;

            if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
                // makes no sense to start from within write action, will cancel anyway
                // we'll restart when the write action finish
                return;
            }
            final PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)myPsiDocumentManager;
            if (documentManager.hasUncommitedDocuments()) {
                documentManager.cancelAndRunWhenAllCommitted("restart daemon when all committed", this);
                return;
            }
            if (RefResolveService.ENABLED &&
                    !RefResolveService.getInstance(myProject).isUpToDate() &&
                    RefResolveService.getInstance(myProject).getQueueSize() == 1) {
                return; // if the user have just typed in something, wait until the file is re-resolved
                // (or else it will blink like crazy since unused symbols calculation depends on resolve service)
            }

            Map<FileEditor, HighlightingPass[]> passes = new THashMap<FileEditor, HighlightingPass[]>(activeEditors.size());
            for (FileEditor fileEditor : activeEditors) {
                BackgroundEditorHighlighter highlighter = fileEditor.getBackgroundHighlighter();
                if (highlighter != null) {
                    HighlightingPass[] highlightingPasses = highlighter.createPassesForEditor();
                    passes.put(fileEditor, highlightingPasses);
                }
            }
            // cancel all after calling createPasses() since there are perverts {@link com.gome.maven.util.xml.ui.DomUIFactoryImpl} who are changing PSI there
            cancelUpdateProgress(true, "Cancel by alarm");
            myAlarm.cancelAllRequests();
            DaemonProgressIndicator progress = createUpdateProgress();
            myPassExecutorService.submitPasses(passes, progress);
        }
    };

    
    private Runnable createUpdateRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().assertIsDispatchThread();

                if (myDisposed || !myProject.isInitialized() || PowerSaveMode.isEnabled()) {
                    return;
                }
                if (HeavyProcessLatch.INSTANCE.isRunning()) {
                    if (myAlarm.isEmpty()) {
                        myAlarm.addRequest(myUpdateRunnable, mySettings.AUTOREPARSE_DELAY);
                    }
                    return;
                }
                Editor activeEditor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();

                if (activeEditor == null) {
                    submitPassesRunnable.run();
                }
                else {
                    ((PsiDocumentManagerBase)myPsiDocumentManager).cancelAndRunWhenAllCommitted("start daemon when all committed",
                            submitPassesRunnable);
                }
            }
        };
    }

    
    private synchronized DaemonProgressIndicator createUpdateProgress() {
        DaemonProgressIndicator old = myUpdateProgress;
        if (old != null && !old.isCanceled()) {
            old.cancel();
        }
        DaemonProgressIndicator progress = new DaemonProgressIndicator() {
            @Override
            public void stopIfRunning() {
                super.stopIfRunning();
                myProject.getMessageBus().syncPublisher(DAEMON_EVENT_TOPIC).daemonFinished();
            }
        };
        progress.start();
        myUpdateProgress = progress;
        return progress;
    }

    @Override
    public void autoImportReferenceAtCursor( Editor editor,  PsiFile file) {
        for (ReferenceImporter importer : Extensions.getExtensions(ReferenceImporter.EP_NAME)) {
            if (importer.autoImportReferenceAtCursor(editor, file)) break;
        }
    }

    
    synchronized DaemonProgressIndicator getUpdateProgress() {
        return myUpdateProgress;
    }

    
    void allowToInterrupt(boolean can) {
        allowToInterrupt = can;
    }

    
    private Collection<FileEditor> getSelectedEditors() {
        // Editors in modal context
        List<Editor> editors = getActiveEditors();

        Collection<FileEditor> activeTextEditors = new THashSet<FileEditor>(editors.size());
        for (Editor editor : editors) {
            TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(editor);
            activeTextEditors.add(textEditor);
        }
        if (ApplicationManager.getApplication().getCurrentModalityState() != ModalityState.NON_MODAL) {
            return activeTextEditors;
        }

        // Editors in tabs.
        Collection<FileEditor> result = new THashSet<FileEditor>();
        Collection<VirtualFile> files = new THashSet<VirtualFile>(activeTextEditors.size());
        final FileEditor[] tabEditors = FileEditorManager.getInstance(myProject).getSelectedEditors();
        for (FileEditor tabEditor : tabEditors) {
            VirtualFile file = ((FileEditorManagerEx)FileEditorManager.getInstance(myProject)).getFile(tabEditor);
            if (file != null) {
                files.add(file);
            }
            result.add(tabEditor);
        }
        // do not duplicate documents
        for (FileEditor fileEditor : activeTextEditors) {
            VirtualFile file = ((FileEditorManagerEx)FileEditorManager.getInstance(myProject)).getFile(fileEditor);
            if (file != null && files.contains(file)) continue;
            result.add(fileEditor);
        }
        return result;
    }

    
    private List<Editor> getActiveEditors() {
        return myEditorTracker.getActiveEditors();
    }
}
