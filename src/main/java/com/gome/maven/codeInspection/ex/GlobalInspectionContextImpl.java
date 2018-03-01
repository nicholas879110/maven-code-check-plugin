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

package com.gome.maven.codeInspection.ex;

import com.gome.maven.analysis.AnalysisScope;
import com.gome.maven.analysis.AnalysisUIOptions;
import com.gome.maven.analysis.PerformAnalysisInBackgroundOption;
import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfoProcessor;
import com.gome.maven.codeInsight.daemon.impl.LocalInspectionsPass;
import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.lang.GlobalInspectionContextExtension;
import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.reference.RefManagerImpl;
import com.gome.maven.codeInspection.reference.RefVisitor;
import com.gome.maven.codeInspection.ui.DefaultInspectionToolPresentation;
import com.gome.maven.codeInspection.ui.InspectionResultsView;
import com.gome.maven.codeInspection.ui.InspectionToolPresentation;
import com.gome.maven.concurrency.JobLauncher;
import com.gome.maven.concurrency.JobLauncherImpl;
import com.gome.maven.concurrency.SensitiveProgressWrapper;
import com.gome.maven.lang.annotation.ProblemGroup;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.notification.NotificationGroup;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.ToggleAction;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.components.PathMacroManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.progress.*;
import com.gome.maven.openapi.progress.util.ProgressIndicatorUtils;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectCoreUtil;
import com.gome.maven.openapi.project.ProjectUtilCore;
import com.gome.maven.openapi.roots.FileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.ToolWindowId;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.LocalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.ui.content.*;
import com.gome.maven.util.*;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.ui.UIUtil;
import gnu.trove.THashSet;
import org.jdom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class GlobalInspectionContextImpl extends GlobalInspectionContextBase implements GlobalInspectionContext {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.ex.GlobalInspectionContextImpl");
    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup("Inspection Results", ToolWindowId.INSPECTION);
    private final NotNullLazyValue<ContentManager> myContentManager;
    private InspectionResultsView myView;
    private Content myContent;

    
    private AnalysisUIOptions myUIOptions;

    public GlobalInspectionContextImpl( Project project,  NotNullLazyValue<ContentManager> contentManager) {
        super(project);

        myUIOptions = AnalysisUIOptions.getInstance(project).copy();
        myContentManager = contentManager;
    }

    
    private ContentManager getContentManager() {
        return myContentManager.getValue();
    }

    public synchronized void addView( InspectionResultsView view,  String title) {
        if (myContent != null) return;
        myContentManager.getValue().addContentManagerListener(new ContentManagerAdapter() {
            @Override
            public void contentRemoved(ContentManagerEvent event) {
                if (event.getContent() == myContent) {
                    if (myView != null) {
                        close(false);
                    }
                    myContent = null;
                }
            }
        });

        myView = view;
        myContent = ContentFactory.SERVICE.getInstance().createContent(view, title, false);

        myContent.setDisposer(myView);

        ContentManager contentManager = getContentManager();
        contentManager.addContent(myContent);
        contentManager.setSelectedContent(myContent);

        ToolWindowManager.getInstance(getProject()).getToolWindow(ToolWindowId.INSPECTION).activate(null);
    }

    public void addView( InspectionResultsView view) {
        addView(view, view.getCurrentProfileName() == null
                ? InspectionsBundle.message("inspection.results.title")
                : InspectionsBundle.message("inspection.results.for.profile.toolwindow.title", view.getCurrentProfileName()));

    }

    @Override
    public void doInspections( final AnalysisScope scope) {
        if (myContent != null) {
            getContentManager().removeContent(myContent, true);
        }
        super.doInspections(scope);
    }

    public void launchInspectionsOffline( final AnalysisScope scope,
                                          final String outputPath,
                                         final boolean runGlobalToolsOnly,
                                          final List<File> inspectionsResults) {
        performInspectionsWithProgressAndExportResults(scope, runGlobalToolsOnly, true, outputPath, inspectionsResults);
    }

    public void performInspectionsWithProgressAndExportResults( final AnalysisScope scope,
                                                               final boolean runGlobalToolsOnly,
                                                               final boolean isOfflineInspections,
                                                                final String outputPath,
                                                                final List<File> inspectionsResults) {
        cleanupTools();
        setCurrentScope(scope);

        final Runnable action = new Runnable() {
            @Override
            public void run() {
                DefaultInspectionToolPresentation.setOutputPath(outputPath);
                try {
                    performInspectionsWithProgress(scope, runGlobalToolsOnly, isOfflineInspections);
                    exportResults(inspectionsResults, outputPath);
                }
                finally {
                    DefaultInspectionToolPresentation.setOutputPath(null);
                }
            }
        };
        if (isOfflineInspections) {
            ApplicationManager.getApplication().runReadAction(action);
        }
        else {
            action.run();
        }
    }

    private void exportResults( List<File> inspectionsResults,  String outputPath) {
         final String ext = ".xml";
        final Map<Element, Tools> globalTools = new HashMap<Element, Tools>();
        for (Map.Entry<String,Tools> entry : myTools.entrySet()) {
            final Tools sameTools = entry.getValue();
            boolean hasProblems = false;
            String toolName = entry.getKey();
            if (sameTools != null) {
                for (ScopeToolState toolDescr : sameTools.getTools()) {
                    InspectionToolWrapper toolWrapper = toolDescr.getTool();
                    if (toolWrapper instanceof LocalInspectionToolWrapper) {
                        hasProblems = new File(outputPath, toolName + ext).exists();
                    }
                    else {
                        InspectionToolPresentation presentation = getPresentation(toolWrapper);
                        presentation.updateContent();
                        if (presentation.hasReportedProblems()) {
                            final Element root = new Element(InspectionsBundle.message("inspection.problems"));
                            globalTools.put(root, sameTools);
                            LOG.assertTrue(!hasProblems, toolName);
                            break;
                        }
                    }
                }
            }
            if (hasProblems) {
                try {
                    new File(outputPath).mkdirs();
                    final File file = new File(outputPath, toolName + ext);
                    inspectionsResults.add(file);
                    FileUtil
                            .writeToFile(file, ("</" + InspectionsBundle.message("inspection.problems") + ">").getBytes(CharsetToolkit.UTF8_CHARSET), true);
                }
                catch (IOException e) {
                    LOG.error(e);
                }
            }
        }

        getRefManager().iterate(new RefVisitor() {
            @Override
            public void visitElement( final RefEntity refEntity) {
                for (Map.Entry<Element, Tools> entry : globalTools.entrySet()) {
                    Tools tools = entry.getValue();
                    Element element = entry.getKey();
                    for (ScopeToolState state : tools.getTools()) {
                        try {
                            InspectionToolWrapper toolWrapper = state.getTool();
                            InspectionToolPresentation presentation = getPresentation(toolWrapper);
                            presentation.exportResults(element, refEntity);
                        }
                        catch (Throwable e) {
                            LOG.error("Problem when exporting: " + refEntity.getExternalName(), e);
                        }
                    }
                }
            }
        });

        for (Map.Entry<Element, Tools> entry : globalTools.entrySet()) {
            final String toolName = entry.getValue().getShortName();
            Element element = entry.getKey();
            element.setAttribute(LOCAL_TOOL_ATTRIBUTE, Boolean.toString(false));
            final org.jdom.Document doc = new org.jdom.Document(element);
            PathMacroManager.getInstance(getProject()).collapsePaths(doc.getRootElement());
            try {
                new File(outputPath).mkdirs();
                final File file = new File(outputPath, toolName + ext);
                inspectionsResults.add(file);

                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), CharsetToolkit.UTF8_CHARSET);
                try {
                    JDOMUtil.writeDocument(doc, writer, "\n");
                }
                finally {
                    writer.close();
                }
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public void ignoreElement( InspectionProfileEntry tool,  PsiElement element) {
        final RefElement refElement = getRefManager().getReference(element);
        final Tools tools = myTools.get(tool.getShortName());
        if (tools != null){
            for (ScopeToolState state : tools.getTools()) {
                InspectionToolWrapper toolWrapper = state.getTool();
                ignoreElementRecursively(toolWrapper, refElement);
            }
        }
    }

    public InspectionResultsView getView() {
        return myView;
    }

    private void ignoreElementRecursively( InspectionToolWrapper toolWrapper, final RefEntity refElement) {
        if (refElement != null) {
            InspectionToolPresentation presentation = getPresentation(toolWrapper);
            presentation.ignoreCurrentElement(refElement);
            final List<RefEntity> children = refElement.getChildren();
            if (children != null) {
                for (RefEntity child : children) {
                    ignoreElementRecursively(toolWrapper, child);
                }
            }
        }
    }

    
    public AnalysisUIOptions getUIOptions() {
        return myUIOptions;
    }

    public void setSplitterProportion(final float proportion) {
        myUIOptions.SPLITTER_PROPORTION = proportion;
    }

    
    public ToggleAction createToggleAutoscrollAction() {
        return myUIOptions.getAutoScrollToSourceHandler().createToggleAction();
    }

    @Override
    protected void launchInspections( final AnalysisScope scope) {
        myUIOptions = AnalysisUIOptions.getInstance(getProject()).copy();
        myView = new InspectionResultsView(getProject(), getCurrentProfile(), scope, this, new InspectionRVContentProviderImpl(getProject()));
        super.launchInspections(scope);
    }

    
    @Override
    protected PerformInBackgroundOption createOption() {
        return new PerformAnalysisInBackgroundOption(getProject());
    }

    @Override
    protected void notifyInspectionsFinished() {
        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                LOG.info("Code inspection finished");

                if (myView != null) {
                    if (!myView.update() && !getUIOptions().SHOW_ONLY_DIFF) {
                        NOTIFICATION_GROUP.createNotification(InspectionsBundle.message("inspection.no.problems.message"), MessageType.INFO).notify(getProject());
                        close(true);
                    }
                    else {
                        addView(myView);
                    }
                }
            }
        });
    }

    @Override
    protected void runTools( final AnalysisScope scope, boolean runGlobalToolsOnly, boolean isOfflineInspections) {
        final ProgressIndicator progressIndicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        if (progressIndicator == null) {
            throw new IncorrectOperationException("Must be run under progress");
        }
        if (!isOfflineInspections && ApplicationManager.getApplication().isDispatchThread()) {
            throw new IncorrectOperationException("Must not start inspections from within EDT");
        }
        if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
            throw new IncorrectOperationException("Must not start inspections from within write action");
        }
        // in offline inspection application we don't care about global read action
        if (!isOfflineInspections && ApplicationManager.getApplication().isReadAccessAllowed()) {
            throw new IncorrectOperationException("Must not start inspections from within global read action");
        }
        final InspectionManager inspectionManager = InspectionManager.getInstance(getProject());
        final List<Tools> globalTools = new ArrayList<Tools>();
        final List<Tools> localTools = new ArrayList<Tools>();
        final List<Tools> globalSimpleTools = new ArrayList<Tools>();
        initializeTools(globalTools, localTools, globalSimpleTools);
        appendPairedInspectionsForUnfairTools(globalTools, globalSimpleTools, localTools);

        ((RefManagerImpl)getRefManager()).initializeAnnotators();
        runGlobalTools(scope, inspectionManager, globalTools, isOfflineInspections);

        if (runGlobalToolsOnly) return;

        final Set<VirtualFile> localScopeFiles = scope.toSearchScope() instanceof LocalSearchScope ? new THashSet<VirtualFile>() : null;
        for (Tools tools : globalSimpleTools) {
            GlobalInspectionToolWrapper toolWrapper = (GlobalInspectionToolWrapper)tools.getTool();
            GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
            tool.inspectionStarted(inspectionManager, this, getPresentation(toolWrapper));
        }

        final boolean headlessEnvironment = ApplicationManager.getApplication().isHeadlessEnvironment();
        final Map<String, InspectionToolWrapper> map = getInspectionWrappersMap(localTools);

        final BlockingQueue<PsiFile> filesToInspect = new ArrayBlockingQueue<PsiFile>(1000);
        final Queue<PsiFile> filesFailedToInspect = new LinkedBlockingQueue<PsiFile>();
        // use original progress indicator here since we don't want it to cancel on write action start
        startIterateScope(scope, localScopeFiles, headlessEnvironment, filesToInspect, progressIndicator);

        Processor<PsiFile> processor = new Processor<PsiFile>() {
            @Override
            public boolean process(final PsiFile file) {
                ProgressManager.checkCanceled();
                if (!ApplicationManagerEx.getApplicationEx().tryRunReadAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!file.isValid()) {
                            return;
                        }
                        inspectFile(file, inspectionManager, localTools, globalSimpleTools, map);
                    }
                })) {
                    throw new ProcessCanceledException();
                }

                return true;
            }
        };
        while (true) {
            Disposable disposable = Disposer.newDisposable();
            ProgressIndicator wrapper = new SensitiveProgressWrapper(progressIndicator);
            wrapper.start();
            ProgressIndicatorUtils.forceWriteActionPriority(wrapper, disposable);

            try {
                // use wrapper here to cancel early when write action start but do not affect the original indicator
                ((JobLauncherImpl)JobLauncher.getInstance()).processQueue(filesToInspect, filesFailedToInspect, wrapper, TOMBSTONE, processor);
                break;
            }
            catch (ProcessCanceledException ignored) {
                progressIndicator.checkCanceled();
                // PCE may be thrown from inside wrapper when write action started
                // go on with the write and then resume processing the rest of the queue
                assert !ApplicationManager.getApplication().isReadAccessAllowed();
                assert !ApplicationManager.getApplication().isDispatchThread();

                // wait for write action to complete
                ApplicationManager.getApplication().runReadAction(EmptyRunnable.getInstance());
            }
            finally {
                Disposer.dispose(disposable);
            }
        }
        progressIndicator.checkCanceled();

        for (Tools tools : globalSimpleTools) {
            GlobalInspectionToolWrapper toolWrapper = (GlobalInspectionToolWrapper)tools.getTool();
            GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
            ProblemDescriptionsProcessor problemDescriptionProcessor = getProblemDescriptionProcessor(toolWrapper, map);
            tool.inspectionFinished(inspectionManager, this, problemDescriptionProcessor);
        }
    }

    private boolean inspectFile( final PsiFile file,
                                 final InspectionManager inspectionManager,
                                 List<Tools> localTools,
                                 List<Tools> globalSimpleTools,
                                 final Map<String, InspectionToolWrapper> wrappersMap) {
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(file);
        if (document == null) return true;

        VirtualFile virtualFile = file.getVirtualFile();
        String url = ProjectUtilCore.displayUrlRelativeToProject(virtualFile, virtualFile.getPresentableUrl(), getProject(), true, false);
        incrementJobDoneAmount(getStdJobDescriptors().LOCAL_ANALYSIS, url);

        final LocalInspectionsPass pass = new LocalInspectionsPass(file, document, 0,
                file.getTextLength(), LocalInspectionsPass.EMPTY_PRIORITY_RANGE, true,
                HighlightInfoProcessor.getEmpty());
        try {
            final List<LocalInspectionToolWrapper> lTools = getWrappersFromTools(localTools, file);
            pass.doInspectInBatch(this, inspectionManager, lTools);

            final List<GlobalInspectionToolWrapper> tools = getWrappersFromTools(globalSimpleTools, file);
            JobLauncher.getInstance().invokeConcurrentlyUnderProgress(tools, myProgressIndicator, false, new Processor<GlobalInspectionToolWrapper>() {
                @Override
                public boolean process(GlobalInspectionToolWrapper toolWrapper) {
                    GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
                    ProblemsHolder holder = new ProblemsHolder(inspectionManager, file, false);
                    ProblemDescriptionsProcessor problemDescriptionProcessor = getProblemDescriptionProcessor(toolWrapper, wrappersMap);
                    tool.checkFile(file, inspectionManager, holder, GlobalInspectionContextImpl.this, problemDescriptionProcessor);
                    InspectionToolPresentation toolPresentation = getPresentation(toolWrapper);
                    LocalDescriptorsUtil.addProblemDescriptors(holder.getResults(), false, GlobalInspectionContextImpl.this, null,
                            CONVERT, toolPresentation);
                    return true;
                }
            });
        }
        catch (ProcessCanceledException e) {
            final Throwable cause = e.getCause();
            if (cause == null) {
                throw e;
            }
            LOG.error("In file: " + file, cause);
        }
        catch (IndexNotReadyException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error("In file: " + file.getName(), e);
        }
        finally {
            InjectedLanguageManager.getInstance(getProject()).dropFileCaches(file);
        }
        return true;
    }

    private static final PsiFile TOMBSTONE = PsiUtilCore.NULL_PSI_FILE;

    private void startIterateScope( final AnalysisScope scope,
                                    final Collection<VirtualFile> localScopeFiles,
                                   final boolean headlessEnvironment,
                                    final BlockingQueue<PsiFile> outFilesToInspect,
                                    final ProgressIndicator progressIndicator) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final FileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
                    scope.accept(new Processor<VirtualFile>() {
                        @Override
                        public boolean process(final VirtualFile file) {
                            progressIndicator.checkCanceled();
                            if (ProjectCoreUtil.isProjectOrWorkspaceFile(file) || !fileIndex.isInContent(file)) return true;
                            final PsiFile[] psiFile = new PsiFile[1];

                            Document document = ApplicationManager.getApplication().runReadAction(new Computable<Document>() {
                                @Override
                                public Document compute() {
                                    if (getProject().isDisposed()) throw new ProcessCanceledException();
                                    PsiFile psi = PsiManager.getInstance(getProject()).findFile(file);
                                    Document document = psi == null ? null : shouldProcess(psi, headlessEnvironment, localScopeFiles);
                                    if (document != null) {
                                        psiFile[0] = psi;
                                    }
                                    return document;
                                }
                            });
                            //do not inspect binary files
                            if (document != null && psiFile[0] != null) {
                                try {
                                    LOG.assertTrue(!ApplicationManager.getApplication().isReadAccessAllowed());
                                    outFilesToInspect.put(psiFile[0]);
                                }
                                catch (InterruptedException e) {
                                    LOG.error(e);
                                }
                            }
                            return true;
                        }
                    });
                }
                catch (ProcessCanceledException e) {
                    // ignore, but put tombstone
                }
                finally {
                    try {
                        outFilesToInspect.put(TOMBSTONE);
                    }
                    catch (InterruptedException e) {
                        LOG.error(e);
                    }
                }
            }
        });
    }

    private Document shouldProcess( PsiFile file, boolean headlessEnvironment,  Collection<VirtualFile> localScopeFiles) {
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return null;
        if (isBinary(file)) return null; //do not inspect binary files

        if (myView == null && !headlessEnvironment) {
            throw new ProcessCanceledException();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Running local inspections on " + virtualFile.getPath());
        }

        if (SingleRootFileViewProvider.isTooLargeForIntelligence(virtualFile)) return null;
        if (localScopeFiles != null && !localScopeFiles.add(virtualFile)) return null;

        return PsiDocumentManager.getInstance(getProject()).getDocument(file);
    }

    private void runGlobalTools( final AnalysisScope scope,
                                 final InspectionManager inspectionManager,
                                 List<Tools> globalTools,
                                boolean isOfflineInspections) {
        LOG.assertTrue(!ApplicationManager.getApplication().isReadAccessAllowed() || isOfflineInspections, "Must not run under read action, too unresponsive");
        final List<InspectionToolWrapper> needRepeatSearchRequest = new ArrayList<InspectionToolWrapper>();

        final boolean canBeExternalUsages = scope.getScopeType() != AnalysisScope.PROJECT;
        for (Tools tools : globalTools) {
            for (ScopeToolState state : tools.getTools()) {
                final InspectionToolWrapper toolWrapper = state.getTool();
                final GlobalInspectionTool tool = (GlobalInspectionTool)toolWrapper.getTool();
                final InspectionToolPresentation toolPresentation = getPresentation(toolWrapper);
                try {
                    if (tool.isGraphNeeded()) {
                        try {
                            ((RefManagerImpl)getRefManager()).findAllDeclarations();
                        }
                        catch (Throwable e) {
                            getStdJobDescriptors().BUILD_GRAPH.setDoneAmount(0);
                            throw e;
                        }
                    }
                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            tool.runInspection(scope, inspectionManager, GlobalInspectionContextImpl.this, toolPresentation);
                            //skip phase when we are sure that scope already contains everything
                            if (canBeExternalUsages &&
                                    tool.queryExternalUsagesRequests(inspectionManager, GlobalInspectionContextImpl.this, toolPresentation)) {
                                needRepeatSearchRequest.add(toolWrapper);
                            }
                        }
                    });
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (IndexNotReadyException e) {
                    throw e;
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }
        }
        for (GlobalInspectionContextExtension extension : myExtensions.values()) {
            try {
                extension.performPostRunActivities(needRepeatSearchRequest, this);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (IndexNotReadyException e) {
                throw e;
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
    }

    private void appendPairedInspectionsForUnfairTools( List<Tools> globalTools,
                                                        List<Tools> globalSimpleTools,
                                                        List<Tools> localTools) {
        Tools[] larray = localTools.toArray(new Tools[localTools.size()]);
        for (Tools tool : larray) {
            LocalInspectionToolWrapper toolWrapper = (LocalInspectionToolWrapper)tool.getTool();
            LocalInspectionTool localTool = toolWrapper.getTool();
            if (localTool instanceof PairedUnfairLocalInspectionTool) {
                String batchShortName = ((PairedUnfairLocalInspectionTool)localTool).getInspectionForBatchShortName();
                InspectionProfile currentProfile = getCurrentProfile();
                InspectionToolWrapper batchInspection;
                if (currentProfile == null) {
                    batchInspection = null;
                }
                else {
                    final InspectionToolWrapper pairedWrapper = currentProfile.getInspectionTool(batchShortName, getProject());
                    batchInspection = pairedWrapper != null ? pairedWrapper.createCopy() : null;
                }
                if (batchInspection != null && !myTools.containsKey(batchShortName)) {
                    // add to existing inspections to run
                    InspectionProfileEntry batchTool = batchInspection.getTool();
                    Tools newTool = new ToolsImpl(batchInspection, batchInspection.getDefaultLevel(), true);
                    if (batchTool instanceof LocalInspectionTool) localTools.add(newTool);
                    else if (batchTool instanceof GlobalSimpleInspectionTool) globalSimpleTools.add(newTool);
                    else if (batchTool instanceof GlobalInspectionTool) globalTools.add(newTool);
                    else throw new AssertionError(batchTool);
                    myTools.put(batchShortName, newTool);
                    batchInspection.initialize(this);
                }
            }
        }
    }

    
    private static <T extends InspectionToolWrapper> List<T> getWrappersFromTools( List<Tools> localTools,  PsiFile file) {
        final List<T> lTools = new ArrayList<T>();
        for (Tools tool : localTools) {
            //noinspection unchecked
            final T enabledTool = (T)tool.getEnabledTool(file);
            if (enabledTool != null) {
                lTools.add(enabledTool);
            }
        }
        return lTools;
    }

    
    private ProblemDescriptionsProcessor getProblemDescriptionProcessor( final GlobalInspectionToolWrapper toolWrapper,
                                                                         final Map<String, InspectionToolWrapper> wrappersMap) {
        return new ProblemDescriptionsProcessor() {
            
            @Override
            public CommonProblemDescriptor[] getDescriptions( RefEntity refEntity) {
                return new CommonProblemDescriptor[0];
            }

            @Override
            public void ignoreElement( RefEntity refEntity) {

            }

            @Override
            public void addProblemElement( RefEntity refEntity,  CommonProblemDescriptor... commonProblemDescriptors) {
                for (CommonProblemDescriptor problemDescriptor : commonProblemDescriptors) {
                    if (!(problemDescriptor instanceof ProblemDescriptor)) {
                        continue;
                    }
                    ProblemGroup problemGroup = ((ProblemDescriptor)problemDescriptor).getProblemGroup();

                    InspectionToolWrapper targetWrapper = problemGroup == null ? toolWrapper : wrappersMap.get(problemGroup.getProblemName());
                    if (targetWrapper != null) { // Else it's switched off
                        InspectionToolPresentation toolPresentation = getPresentation(targetWrapper);
                        toolPresentation.addProblemElement(refEntity, problemDescriptor);
                    }
                }
            }

            @Override
            public RefEntity getElement( CommonProblemDescriptor descriptor) {
                return null;
            }
        };
    }

    
    private static Map<String, InspectionToolWrapper> getInspectionWrappersMap( List<Tools> tools) {
        Map<String, InspectionToolWrapper> name2Inspection = new HashMap<String, InspectionToolWrapper>(tools.size());
        for (Tools tool : tools) {
            InspectionToolWrapper toolWrapper = tool.getTool();
            name2Inspection.put(toolWrapper.getShortName(), toolWrapper);
        }

        return name2Inspection;
    }

    private static final TripleFunction<LocalInspectionTool,PsiElement,GlobalInspectionContext,RefElement> CONVERT =
            new TripleFunction<LocalInspectionTool, PsiElement, GlobalInspectionContext, RefElement>() {
                @Override
                public RefElement fun(LocalInspectionTool tool,
                                      PsiElement elt,
                                      GlobalInspectionContext context) {
                    final PsiNamedElement problemElement = PsiTreeUtil.getNonStrictParentOfType(elt, PsiFile.class);

                    RefElement refElement = context.getRefManager().getReference(problemElement);
                    if (refElement == null && problemElement != null) {  // no need to lose collected results
                        refElement = GlobalInspectionContextUtil.retrieveRefElement(elt, context);
                    }
                    return refElement;
                }
            };


    @Override
    public void close(boolean noSuspisiousCodeFound) {
        if (!noSuspisiousCodeFound && (myView == null || myView.isRerun())) return;
        AnalysisUIOptions.getInstance(getProject()).save(myUIOptions);
        if (myContent != null) {
            final ContentManager contentManager = getContentManager();
            contentManager.removeContent(myContent, true);
        }
        myView = null;
        super.close(noSuspisiousCodeFound);
    }

    @Override
    public void cleanup() {
        ((InspectionManagerEx)InspectionManager.getInstance(getProject())).closeRunningContext(this);
        for (Tools tools : myTools.values()) {
            for (ScopeToolState state : tools.getTools()) {
                InspectionToolWrapper toolWrapper = state.getTool();
                getPresentation(toolWrapper).finalCleanup();
            }
        }
        super.cleanup();
    }

    public void refreshViews() {
        if (myView != null) {
            myView.updateView(false);
        }
    }

    private final ConcurrentMap<InspectionToolWrapper, InspectionToolPresentation> myPresentationMap = ContainerUtil.newConcurrentMap();
    
    public InspectionToolPresentation getPresentation( InspectionToolWrapper toolWrapper) {
        InspectionToolPresentation presentation = myPresentationMap.get(toolWrapper);
        if (presentation == null) {
            String presentationClass = StringUtil.notNullize(toolWrapper.myEP == null ? null : toolWrapper.myEP.presentation, DefaultInspectionToolPresentation.class.getName());

            try {
                Constructor<?> constructor = Class.forName(presentationClass).getConstructor(InspectionToolWrapper.class, GlobalInspectionContextImpl.class);
                presentation = (InspectionToolPresentation)constructor.newInstance(toolWrapper, this);
            }
            catch (Exception e) {
                LOG.error(e);
                throw new RuntimeException(e);
            }
            presentation = ConcurrencyUtil.cacheOrGet(myPresentationMap, toolWrapper, presentation);
        }
        return presentation;
    }

    @Override
    public void codeCleanup( final Project project,
                             final AnalysisScope scope,
                             final InspectionProfile profile,
                             final String commandName,
                             final Runnable postRunnable,
                            final boolean modal) {
        Task task = modal ? new Task.Modal(project, "Inspect code...", true) {
            @Override
            public void run( ProgressIndicator indicator) {
                cleanup(scope, profile, project, postRunnable, commandName);
            }
        } : new Task.Backgroundable(project, "Inspect code...", true) {
            @Override
            public void run( ProgressIndicator indicator) {
                cleanup(scope, profile, project, postRunnable, commandName);
            }
        };
        ProgressManager.getInstance().run(task);
    }

    private void cleanup( AnalysisScope scope,
                          InspectionProfile profile,
                          final Project project,
                          final Runnable postRunnable,
                          final String commandName) {
        final int fileCount = scope.getFileCount();
        final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        final List<LocalInspectionToolWrapper> lTools = new ArrayList<LocalInspectionToolWrapper>();

        final LinkedHashMap<PsiFile, List<HighlightInfo>> results = new LinkedHashMap<PsiFile, List<HighlightInfo>>();

        final SearchScope searchScope = scope.toSearchScope();
        final TextRange range;
        if (searchScope instanceof LocalSearchScope) {
            final PsiElement[] elements = ((LocalSearchScope)searchScope).getScope();
            range = elements.length == 1 ? ApplicationManager.getApplication().runReadAction(new Computable<TextRange>() {
                @Override
                public TextRange compute() {
                    return elements[0].getTextRange();
                }
            }) : null;
        }
        else {
            range = null;
        }
        final Iterable<Tools> inspectionTools = ContainerUtil.filter(profile.getAllEnabledInspectionTools(project), new Condition<Tools>() {
            @Override
            public boolean value(Tools tools) {
                assert tools != null;
                return tools.getTool().getTool() instanceof CleanupLocalInspectionTool;
            }
        });
        scope.accept(new PsiElementVisitor() {
            private int myCount;
            @Override
            public void visitFile(PsiFile file) {
                if (progressIndicator != null) {
                    progressIndicator.setFraction(((double)++ myCount)/fileCount);
                }
                if (isBinary(file)) return;
                for (final Tools tools : inspectionTools) {
                    final InspectionToolWrapper tool = tools.getEnabledTool(file);
                    if (tool instanceof LocalInspectionToolWrapper) {
                        lTools.add((LocalInspectionToolWrapper)tool);
                        tool.initialize(GlobalInspectionContextImpl.this);
                    }
                }

                if (!lTools.isEmpty()) {
                    final LocalInspectionsPass pass = new LocalInspectionsPass(file, PsiDocumentManager.getInstance(project).getDocument(file), range != null ? range.getStartOffset() : 0,
                            range != null ? range.getEndOffset() : file.getTextLength(), LocalInspectionsPass.EMPTY_PRIORITY_RANGE, true,
                            HighlightInfoProcessor.getEmpty());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            pass.doInspectInBatch(GlobalInspectionContextImpl.this, InspectionManager.getInstance(project), lTools);
                        }
                    };
                    ApplicationManager.getApplication().runReadAction(runnable);
                    final List<HighlightInfo> infos = pass.getInfos();
                    if (searchScope instanceof LocalSearchScope) {
                        for (Iterator<HighlightInfo> iterator = infos.iterator(); iterator.hasNext(); ) {
                            final HighlightInfo info = iterator.next();
                            final TextRange infoRange = new TextRange(info.getStartOffset(), info.getEndOffset());
                            if (!((LocalSearchScope)searchScope).containsRange(file, infoRange)) {
                                iterator.remove();
                            }
                        }
                    }
                    if (!infos.isEmpty()) {
                        results.put(file, infos);
                    }
                }
            }
        });

        if (results.isEmpty()) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    if (commandName != null) {
                        NOTIFICATION_GROUP.createNotification(InspectionsBundle.message("inspection.no.problems.message"), MessageType.INFO).notify(getProject());
                    }
                    if (postRunnable != null) {
                        postRunnable.run();
                    }
                }
            });
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!FileModificationService.getInstance().preparePsiElementsForWrite(results.keySet())) return;

                final SequentialModalProgressTask progressTask = new SequentialModalProgressTask(project, "Code Cleanup", true);
                progressTask.setMinIterationTime(200);
                progressTask.setTask(new SequentialCleanupTask(project, results, progressTask));
                CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                    @Override
                    public void run() {
                        if (commandName != null) {
                            CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
                        }
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                ProgressManager.getInstance().run(progressTask);
                            }
                        });
                        if (postRunnable != null) {
                            ApplicationManager.getApplication().invokeLater(postRunnable);
                        }
                    }
                }, commandName, null);
            }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(runnable);
        }
    }

    private static boolean isBinary( PsiFile file) {
        return file instanceof PsiBinaryFile || file.getFileType().isBinary();
    }
}
