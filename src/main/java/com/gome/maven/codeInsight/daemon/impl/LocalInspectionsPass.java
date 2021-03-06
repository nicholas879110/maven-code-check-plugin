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

import com.gome.maven.codeHighlighting.Pass;
import com.gome.maven.codeInsight.daemon.DaemonBundle;
import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.gome.maven.codeInsight.daemon.impl.quickfix.QuickFixAction;
import com.gome.maven.codeInsight.intention.EmptyIntentionAction;
import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.ex.*;
import com.gome.maven.codeInspection.ui.InspectionToolPresentation;
import com.gome.maven.concurrency.JobLauncher;
import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.keymap.Keymap;
import com.gome.maven.openapi.keymap.KeymapManager;
import com.gome.maven.openapi.keymap.KeymapUtil;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManagerImpl;
import com.gome.maven.profile.codeInspection.SeverityProvider;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.containers.TransferToEDTQueue;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.xml.util.XmlStringUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * @author max
 */
public class LocalInspectionsPass extends ProgressableTextEditorHighlightingPass implements DumbAware {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.daemon.impl.LocalInspectionsPass");
    public static final TextRange EMPTY_PRIORITY_RANGE = TextRange.EMPTY_RANGE;
    private static final Condition<PsiFile> FILE_FILTER = new Condition<PsiFile>() {
        @Override
        public boolean value(PsiFile file) {
            return HighlightingLevelManager.getInstance(file.getProject()).shouldInspect(file);
        }
    };
    private final TextRange myPriorityRange;
    private final boolean myIgnoreSuppressed;
    private final ConcurrentMap<PsiFile, List<InspectionResult>> result = ContainerUtil.newConcurrentMap();
    private static final String PRESENTABLE_NAME = DaemonBundle.message("pass.inspection");
    private volatile List<HighlightInfo> myInfos = Collections.emptyList();
    private final String myShortcutText;
    private final SeverityRegistrar mySeverityRegistrar;
    private final InspectionProfileWrapper myProfileWrapper;
    private boolean myFailFastOnAcquireReadAction;

    public LocalInspectionsPass( PsiFile file,
                                 Document document,
                                int startOffset,
                                int endOffset,
                                 TextRange priorityRange,
                                boolean ignoreSuppressed,
                                 HighlightInfoProcessor highlightInfoProcessor) {
        super(file.getProject(), document, PRESENTABLE_NAME, file, null, new TextRange(startOffset, endOffset), true, highlightInfoProcessor);
        assert file.isPhysical() : "can't inspect non-physical file: " + file + "; " + file.getVirtualFile();
        myPriorityRange = priorityRange;
        myIgnoreSuppressed = ignoreSuppressed;
        setId(Pass.LOCAL_INSPECTIONS);

        final KeymapManager keymapManager = KeymapManager.getInstance();
        if (keymapManager != null) {
            final Keymap keymap = keymapManager.getActiveKeymap();
            myShortcutText = keymap == null ? "" : "(" + KeymapUtil.getShortcutsText(keymap.getShortcuts(IdeActions.ACTION_SHOW_ERROR_DESCRIPTION)) + ")";
        }
        else {
            myShortcutText = "";
        }
        InspectionProfileWrapper profileToUse = InspectionProjectProfileManagerImpl.getInstanceImpl(myProject).getProfileWrapper();

        Function<InspectionProfileWrapper,InspectionProfileWrapper> custom = file.getUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY);
        if (custom != null) {
            profileToUse = custom.fun(profileToUse);
        }

        myProfileWrapper = profileToUse;
        assert myProfileWrapper != null;
        mySeverityRegistrar = ((SeverityProvider)myProfileWrapper.getInspectionProfile().getProfileManager()).getSeverityRegistrar();

        // initial guess
        setProgressLimit(300 * 2);
    }

    @Override
    protected void collectInformationWithProgress( ProgressIndicator progress) {
        try {
            if (!HighlightingLevelManager.getInstance(myProject).shouldInspect(myFile)) return;
            final InspectionManager iManager = InspectionManager.getInstance(myProject);
            final InspectionProfileWrapper profile = myProfileWrapper;
            inspect(getInspectionTools(profile), iManager, true, true, DumbService.isDumb(myProject), progress);
        }
        finally {
            disposeDescriptors();
        }
    }

    private void disposeDescriptors() {
        result.clear();
    }

    public void doInspectInBatch( final GlobalInspectionContextImpl context,
                                  final InspectionManager iManager,
                                  final List<LocalInspectionToolWrapper> toolWrappers) {
        final ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
        inspect(new ArrayList<LocalInspectionToolWrapper>(toolWrappers), iManager, false, false, false, progress);
        addDescriptorsFromInjectedResults(iManager, context);
        List<InspectionResult> resultList = result.get(myFile);
        if (resultList == null) return;
        for (InspectionResult inspectionResult : resultList) {
            LocalInspectionToolWrapper toolWrapper = inspectionResult.tool;
            for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {
                addDescriptors(toolWrapper, descriptor, context);
            }
        }
    }

    private void addDescriptors( LocalInspectionToolWrapper toolWrapper,
                                 ProblemDescriptor descriptor,
                                 GlobalInspectionContextImpl context) {
        InspectionToolPresentation toolPresentation = context.getPresentation(toolWrapper);
        LocalDescriptorsUtil.addProblemDescriptors(Collections.singletonList(descriptor), toolPresentation, myIgnoreSuppressed,
                context,
                toolWrapper.getTool());
    }

    private void addDescriptorsFromInjectedResults( InspectionManager iManager,  GlobalInspectionContextImpl context) {
        InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);

        for (Map.Entry<PsiFile, List<InspectionResult>> entry : result.entrySet()) {
            PsiFile file = entry.getKey();
            if (file == myFile) continue; // not injected
            DocumentWindow documentRange = (DocumentWindow)documentManager.getDocument(file);
            List<InspectionResult> resultList = entry.getValue();
            for (InspectionResult inspectionResult : resultList) {
                LocalInspectionToolWrapper toolWrapper = inspectionResult.tool;
                for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {

                    PsiElement psiElement = descriptor.getPsiElement();
                    if (psiElement == null) continue;
                    if (SuppressionUtil.inspectionResultSuppressed(psiElement, toolWrapper.getTool())) continue;
                    List<TextRange> editables = ilManager.intersectWithAllEditableFragments(file, ((ProblemDescriptorBase)descriptor).getTextRange());
                    for (TextRange editable : editables) {
                        TextRange hostRange = documentRange.injectedToHost(editable);
                        QuickFix[] fixes = descriptor.getFixes();
                        LocalQuickFix[] localFixes = null;
                        if (fixes != null) {
                            localFixes = new LocalQuickFix[fixes.length];
                            for (int k = 0; k < fixes.length; k++) {
                                QuickFix fix = fixes[k];
                                localFixes[k] = (LocalQuickFix)fix;
                            }
                        }
                        ProblemDescriptor patchedDescriptor = iManager.createProblemDescriptor(myFile, hostRange, descriptor.getDescriptionTemplate(),
                                descriptor.getHighlightType(), true, localFixes);
                        addDescriptors(toolWrapper, patchedDescriptor, context);
                    }
                }
            }
        }
    }

    private void inspect( final List<LocalInspectionToolWrapper> toolWrappers,
                          final InspectionManager iManager,
                         final boolean isOnTheFly,
                         boolean failFastOnAcquireReadAction,
                         boolean checkDumbAwareness,
                          final ProgressIndicator progress) {
        myFailFastOnAcquireReadAction = failFastOnAcquireReadAction;
        if (toolWrappers.isEmpty()) return;

        List<PsiElement> inside = new ArrayList<PsiElement>();
        List<PsiElement> outside = new ArrayList<PsiElement>();
        Divider.divideInsideAndOutside(myFile, myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(), myPriorityRange, inside, new ArrayList<ProperTextRange>(), outside, new ArrayList<ProperTextRange>(),
                true, FILE_FILTER);

        MultiMap<LocalInspectionToolWrapper, String> toolToLanguages = InspectionEngine.getToolsForElements(toolWrappers, checkDumbAwareness, inside, outside);

        setProgressLimit(toolToLanguages.size() * 2L);
        final LocalInspectionToolSession session = new LocalInspectionToolSession(myFile, myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset());

        List<InspectionContext> init =
                visitPriorityElementsAndInit(toolToLanguages, iManager, isOnTheFly, progress, inside, session, toolWrappers, checkDumbAwareness);
        visitRestElementsAndCleanup(progress, outside, session, init);
        inspectInjectedPsi(outside, isOnTheFly, progress, iManager, false, checkDumbAwareness, toolWrappers);

        progress.checkCanceled();

        myInfos = new ArrayList<HighlightInfo>();
        addHighlightsFromResults(myInfos, progress);
    }

    
    private List<InspectionContext> visitPriorityElementsAndInit( MultiMap<LocalInspectionToolWrapper, String> toolToLanguages,
                                                                  final InspectionManager iManager,
                                                                 final boolean isOnTheFly,
                                                                  final ProgressIndicator indicator,
                                                                  final List<PsiElement> elements,
                                                                  final LocalInspectionToolSession session,
                                                                  List<LocalInspectionToolWrapper> wrappers,
                                                                 boolean checkDumbAwareness) {
        final List<InspectionContext> init = new ArrayList<InspectionContext>();
        List<Map.Entry<LocalInspectionToolWrapper, Collection<String>>> entries = new ArrayList<Map.Entry<LocalInspectionToolWrapper, Collection<String>>>(toolToLanguages.entrySet());
        Processor<Map.Entry<LocalInspectionToolWrapper, Collection<String>>> processor =
                new Processor<Map.Entry<LocalInspectionToolWrapper, Collection<String>>>() {
                    @Override
                    public boolean process(final Map.Entry<LocalInspectionToolWrapper, Collection<String>> pair) {
                        return runToolOnElements(pair.getKey(), pair.getValue(), iManager, isOnTheFly, indicator, elements, session, init);
                    }
                };
        boolean result = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(entries, indicator, myFailFastOnAcquireReadAction, processor);
        if (!result) throw new ProcessCanceledException();
        inspectInjectedPsi(elements, isOnTheFly, indicator, iManager, true, checkDumbAwareness, wrappers);
        return init;
    }

    private boolean runToolOnElements( final LocalInspectionToolWrapper toolWrapper,
                                      Collection<String> languages,
                                       final InspectionManager iManager,
                                      final boolean isOnTheFly,
                                       final ProgressIndicator indicator,
                                       final List<PsiElement> elements,
                                       final LocalInspectionToolSession session,
                                       List<InspectionContext> init) {
        indicator.checkCanceled();

        ApplicationManager.getApplication().assertReadAccessAllowed();
        LocalInspectionTool tool = toolWrapper.getTool();
        final boolean[] applyIncrementally = {isOnTheFly};
        ProblemsHolder holder = new ProblemsHolder(iManager, myFile, isOnTheFly) {
            @Override
            public void registerProblem( ProblemDescriptor descriptor) {
                super.registerProblem(descriptor);
                if (applyIncrementally[0]) {
                    addDescriptorIncrementally(descriptor, toolWrapper, indicator);
                }
            }
        };
        PsiElementVisitor visitor = InspectionEngine.createVisitorAndAcceptElements(tool, holder, isOnTheFly, session, elements, languages);

        synchronized (init) {
            init.add(new InspectionContext(toolWrapper, holder, holder.getResultCount(), visitor, languages));
        }
        advanceProgress(1);

        if (holder.hasResults()) {
            appendDescriptors(myFile, holder.getResults(), toolWrapper);
        }
        applyIncrementally[0] = false; // do not apply incrementally outside visible range
        return true;
    }

    private void visitRestElementsAndCleanup( final ProgressIndicator indicator,
                                              final List<PsiElement> elements,
                                              final LocalInspectionToolSession session,
                                              List<InspectionContext> init) {
        Processor<InspectionContext> processor =
                new Processor<InspectionContext>() {
                    @Override
                    public boolean process(InspectionContext context) {
                        indicator.checkCanceled();
                        ApplicationManager.getApplication().assertReadAccessAllowed();
                        InspectionEngine.acceptElements(elements, context.visitor, context.languageIds);
                        advanceProgress(1);
                        context.tool.getTool().inspectionFinished(session, context.holder);

                        if (context.holder.hasResults()) {
                            List<ProblemDescriptor> allProblems = context.holder.getResults();
                            List<ProblemDescriptor> restProblems = allProblems.subList(context.problemsSize, allProblems.size());
                            appendDescriptors(myFile, restProblems, context.tool);
                        }
                        return true;
                    }
                };
        boolean result = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(init, indicator, myFailFastOnAcquireReadAction, processor);
        if (!result) {
            throw new ProcessCanceledException();
        }
    }

    void inspectInjectedPsi( final List<PsiElement> elements,
                            final boolean onTheFly,
                             final ProgressIndicator indicator,
                             final InspectionManager iManager,
                            final boolean inVisibleRange,
                            final boolean checkDumbAwareness,
                             final List<LocalInspectionToolWrapper> wrappers) {
        final Set<PsiFile> injected = new THashSet<PsiFile>();
        for (PsiElement element : elements) {
            InjectedLanguageUtil.enumerate(element, myFile, false, new PsiLanguageInjectionHost.InjectedPsiVisitor() {
                @Override
                public void visit( PsiFile injectedPsi,  List<PsiLanguageInjectionHost.Shred> places) {
                    injected.add(injectedPsi);
                }
            });
        }
        if (injected.isEmpty()) return;
        Processor<PsiFile> processor = new Processor<PsiFile>() {
            @Override
            public boolean process(final PsiFile injectedPsi) {
                doInspectInjectedPsi(injectedPsi, onTheFly, indicator, iManager, inVisibleRange, wrappers, checkDumbAwareness);
                return true;
            }
        };
        if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(new ArrayList<PsiFile>(injected), indicator, myFailFastOnAcquireReadAction, processor)) {
            throw new ProcessCanceledException();
        }
    }

    
    private HighlightInfo highlightInfoFromDescriptor( ProblemDescriptor problemDescriptor,
                                                       HighlightInfoType highlightInfoType,
                                                       String message,
                                                      String toolTip,
                                                      PsiElement psiElement) {
        TextRange textRange = ((ProblemDescriptorBase)problemDescriptor).getTextRange();
        if (textRange == null || psiElement == null) return null;
        boolean isFileLevel = psiElement instanceof PsiFile && textRange.equals(psiElement.getTextRange());

        final HighlightSeverity severity = highlightInfoType.getSeverity(psiElement);
        TextAttributes attributes = mySeverityRegistrar.getTextAttributesBySeverity(severity);
        HighlightInfo.Builder b = HighlightInfo.newHighlightInfo(highlightInfoType)
                .range(psiElement, textRange.getStartOffset(), textRange.getEndOffset())
                .description(message)
                .severity(severity);
        if (toolTip != null) b.escapedToolTip(toolTip);
        if (attributes != null) b.textAttributes(attributes);
        if (problemDescriptor.isAfterEndOfLine()) b.endOfLine();
        if (isFileLevel) b.fileLevelAnnotation();
        if (problemDescriptor.getProblemGroup() != null) b.problemGroup(problemDescriptor.getProblemGroup());

        return b.create();
    }

    private final Map<TextRange, RangeMarker> ranges2markersCache = new THashMap<TextRange, RangeMarker>();
    private final TransferToEDTQueue<Trinity<ProblemDescriptor, LocalInspectionToolWrapper,ProgressIndicator>> myTransferToEDTQueue
            = new TransferToEDTQueue<Trinity<ProblemDescriptor, LocalInspectionToolWrapper,ProgressIndicator>>("Apply inspection results", new Processor<Trinity<ProblemDescriptor, LocalInspectionToolWrapper,ProgressIndicator>>() {
        private final InspectionProfile inspectionProfile = InspectionProjectProfileManager.getInstance(myProject).getInspectionProfile();
        private final InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);
        private final List<HighlightInfo> infos = new ArrayList<HighlightInfo>(2);
        private final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
        @Override
        public boolean process(Trinity<ProblemDescriptor, LocalInspectionToolWrapper,ProgressIndicator> trinity) {
            ProgressIndicator indicator = trinity.getThird();
            if (indicator.isCanceled()) {
                return false;
            }

            ProblemDescriptor descriptor = trinity.first;
            LocalInspectionToolWrapper tool = trinity.second;
            PsiElement psiElement = descriptor.getPsiElement();
            if (psiElement == null) return true;
            PsiFile file = psiElement.getContainingFile();
            Document thisDocument = documentManager.getDocument(file);

            HighlightSeverity severity = inspectionProfile.getErrorLevel(HighlightDisplayKey.find(tool.getShortName()), file).getSeverity();

            infos.clear();
            createHighlightsForDescriptor(infos, emptyActionRegistered, ilManager, file, thisDocument, tool, severity, descriptor, psiElement);
            for (HighlightInfo info : infos) {
                final EditorColorsScheme colorsScheme = getColorsScheme();
                UpdateHighlightersUtil.addHighlighterToEditorIncrementally(myProject, myDocument, myFile, myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(),
                        info, colorsScheme, getId(), ranges2markersCache);
            }

            return true;
        }
    }, myProject.getDisposed(), 200);

    private final Set<Pair<TextRange, String>> emptyActionRegistered = Collections.synchronizedSet(new THashSet<Pair<TextRange, String>>());

    private void addDescriptorIncrementally( final ProblemDescriptor descriptor,
                                             final LocalInspectionToolWrapper tool,
                                             final ProgressIndicator indicator) {
        if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(descriptor.getPsiElement(), tool.getTool())) {
            return;
        }
        myTransferToEDTQueue.offer(Trinity.create(descriptor, tool, indicator));
    }

    private void appendDescriptors( PsiFile file,  List<ProblemDescriptor> descriptors,  LocalInspectionToolWrapper tool) {
        for (ProblemDescriptor descriptor : descriptors) {
            if (descriptor == null) {
                LOG.error("null descriptor. all descriptors(" + descriptors.size() +"): " +
                        descriptors + "; file: " + file + " (" + file.getVirtualFile() +"); tool: " + tool);
            }
        }
        InspectionResult res = new InspectionResult(tool, descriptors);
        appendResult(file, res);
    }

    private void appendResult( PsiFile file,  InspectionResult res) {
        List<InspectionResult> resultList = result.get(file);
        if (resultList == null) {
            resultList = ConcurrencyUtil.cacheOrGet(result, file, new ArrayList<InspectionResult>());
        }
        synchronized (resultList) {
            resultList.add(res);
        }
    }

    @Override
    protected void applyInformationWithProgress() {
        UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(), myInfos, getColorsScheme(), getId());
    }

    private void addHighlightsFromResults( List<HighlightInfo> outInfos,  ProgressIndicator indicator) {
        InspectionProfile inspectionProfile = InspectionProjectProfileManager.getInstance(myProject).getInspectionProfile();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
        InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);
        Set<Pair<TextRange, String>> emptyActionRegistered = new THashSet<Pair<TextRange, String>>();

        for (Map.Entry<PsiFile, List<InspectionResult>> entry : result.entrySet()) {
            indicator.checkCanceled();
            PsiFile file = entry.getKey();
            Document documentRange = documentManager.getDocument(file);
            if (documentRange == null) continue;
            List<InspectionResult> resultList = entry.getValue();
            synchronized (resultList) {
                for (InspectionResult inspectionResult : resultList) {
                    indicator.checkCanceled();
                    LocalInspectionToolWrapper tool = inspectionResult.tool;
                    HighlightSeverity severity = inspectionProfile.getErrorLevel(HighlightDisplayKey.find(tool.getShortName()), file).getSeverity();
                    for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {
                        indicator.checkCanceled();
                        PsiElement element = descriptor.getPsiElement();
                        createHighlightsForDescriptor(outInfos, emptyActionRegistered, ilManager, file, documentRange, tool, severity, descriptor, element);
                    }
                }
            }
        }
    }

    private void createHighlightsForDescriptor( List<HighlightInfo> outInfos,
                                                Set<Pair<TextRange, String>> emptyActionRegistered,
                                                InjectedLanguageManager ilManager,
                                                PsiFile file,
                                                Document documentRange,
                                                LocalInspectionToolWrapper tool,
                                                HighlightSeverity severity,
                                                ProblemDescriptor descriptor,
                                               PsiElement element) {
        if (element == null) return;
        if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(element, tool.getTool())) return;
        HighlightInfoType level = ProblemDescriptorUtil.highlightTypeFromDescriptor(descriptor, severity, mySeverityRegistrar);
        HighlightInfo info = createHighlightInfo(descriptor, tool, level, emptyActionRegistered, element);
        if (info == null) return;

        if (file == myFile) {
            // not injected
            outInfos.add(info);
            return;
        }
        // todo we got to separate our "internal" prefixes/suffixes from user-defined ones
        // todo in the latter case the errors should be highlighted, otherwise not
        List<TextRange> editables = ilManager.intersectWithAllEditableFragments(file, new TextRange(info.startOffset, info.endOffset));
        for (TextRange editable : editables) {
            TextRange hostRange = ((DocumentWindow)documentRange).injectedToHost(editable);
            int start = hostRange.getStartOffset();
            int end = hostRange.getEndOffset();
            HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(info.type).range(element, start, end);
            String description = info.getDescription();
            if (description != null) {
                builder.description(description);
            }
            String toolTip = info.getToolTip();
            if (toolTip != null) {
                builder.escapedToolTip(toolTip);
            }
            HighlightInfo patched = builder.createUnconditionally();
            if (patched.startOffset != patched.endOffset || info.startOffset == info.endOffset) {
                patched.setFromInjection(true);
                registerQuickFixes(tool, descriptor, patched, emptyActionRegistered);
                outInfos.add(patched);
            }
        }
    }

    
    private HighlightInfo createHighlightInfo( ProblemDescriptor descriptor,
                                               LocalInspectionToolWrapper tool,
                                               HighlightInfoType level,
                                               Set<Pair<TextRange, String>> emptyActionRegistered,
                                               PsiElement element) {
         String message = ProblemDescriptorUtil.renderDescriptionMessage(descriptor, element);

        final HighlightDisplayKey key = HighlightDisplayKey.find(tool.getShortName());
        final InspectionProfile inspectionProfile = myProfileWrapper.getInspectionProfile();
        if (!inspectionProfile.isToolEnabled(key, myFile)) return null;

        HighlightInfoType type = new HighlightInfoType.HighlightInfoTypeImpl(level.getSeverity(element), level.getAttributesKey());
        final String plainMessage = message.startsWith("<html>") ? StringUtil.unescapeXml(XmlStringUtil.stripHtml(message).replaceAll("<[^>]*>", "")) : message;
         final String link = " <a "
                +"href=\"#inspection/" + tool.getShortName() + "\""
                + (UIUtil.isUnderDarcula() ? " color=\"7AB4C9\" " : "")
                +">" + DaemonBundle.message("inspection.extended.description")
                +"</a> " + myShortcutText;

         String tooltip = null;
        if (descriptor.showTooltip()) {
            if (message.startsWith("<html>")) {
                tooltip = XmlStringUtil.wrapInHtml(XmlStringUtil.stripHtml(message) + link);
            }
            else {
                tooltip = XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message) + link);
            }
        }
        HighlightInfo highlightInfo = highlightInfoFromDescriptor(descriptor, type, plainMessage, tooltip,element);
        if (highlightInfo != null) {
            registerQuickFixes(tool, descriptor, highlightInfo, emptyActionRegistered);
        }
        return highlightInfo;
    }

    private static void registerQuickFixes( LocalInspectionToolWrapper tool,
                                            ProblemDescriptor descriptor,
                                            HighlightInfo highlightInfo,
                                            Set<Pair<TextRange,String>> emptyActionRegistered) {
        final HighlightDisplayKey key = HighlightDisplayKey.find(tool.getShortName());
        boolean needEmptyAction = true;
        final QuickFix[] fixes = descriptor.getFixes();
        if (fixes != null && fixes.length > 0) {
            for (int k = 0; k < fixes.length; k++) {
                if (fixes[k] != null) { // prevent null fixes from var args
                    QuickFixAction.registerQuickFixAction(highlightInfo, QuickFixWrapper.wrap(descriptor, k), key);
                    needEmptyAction = false;
                }
            }
        }
        HintAction hintAction = descriptor instanceof ProblemDescriptorImpl ? ((ProblemDescriptorImpl)descriptor).getHintAction() : null;
        if (hintAction != null) {
            QuickFixAction.registerQuickFixAction(highlightInfo, hintAction, key);
            needEmptyAction = false;
        }
        if (((ProblemDescriptorBase)descriptor).getEnforcedTextAttributes() != null) {
            needEmptyAction = false;
        }
        if (needEmptyAction && emptyActionRegistered.add(Pair.<TextRange, String>create(highlightInfo.getFixTextRange(), tool.getShortName()))) {
            EmptyIntentionAction emptyIntentionAction = new EmptyIntentionAction(tool.getDisplayName());
            QuickFixAction.registerQuickFixAction(highlightInfo, emptyIntentionAction, key);
        }
    }

    
    private static List<PsiElement> getElementsFrom( PsiFile file) {
        final FileViewProvider viewProvider = file.getViewProvider();
        final Set<PsiElement> result = new LinkedHashSet<PsiElement>();
        final PsiElementVisitor visitor = new PsiRecursiveElementVisitor() {
            @Override public void visitElement(PsiElement element) {
                ProgressManager.checkCanceled();
                PsiElement child = element.getFirstChild();
                if (child == null) {
                    // leaf element
                }
                else {
                    // composite element
                    while (child != null) {
                        child.accept(this);
                        result.add(child);

                        child = child.getNextSibling();
                    }
                }
            }
        };
        for (Language language : viewProvider.getLanguages()) {
            final PsiFile psiRoot = viewProvider.getPsi(language);
            if (psiRoot == null || !HighlightingLevelManager.getInstance(file.getProject()).shouldInspect(psiRoot)) {
                continue;
            }
            psiRoot.accept(visitor);
            result.add(psiRoot);
        }
        return new ArrayList<PsiElement>(result);
    }


    
    List<LocalInspectionToolWrapper> getInspectionTools( InspectionProfileWrapper profile) {
        List<LocalInspectionToolWrapper> enabled = new ArrayList<LocalInspectionToolWrapper>();
        final InspectionToolWrapper[] toolWrappers = profile.getInspectionTools(myFile);
        InspectionProfileWrapper.checkInspectionsDuplicates(toolWrappers);
        for (InspectionToolWrapper toolWrapper : toolWrappers) {
            ProgressManager.checkCanceled();
            if (!profile.isToolEnabled(HighlightDisplayKey.find(toolWrapper.getShortName()), myFile)) continue;
            LocalInspectionToolWrapper wrapper = null;
            if (toolWrapper instanceof LocalInspectionToolWrapper) {
                wrapper = (LocalInspectionToolWrapper)toolWrapper;
            }
            else if (toolWrapper instanceof GlobalInspectionToolWrapper) {
                final GlobalInspectionToolWrapper globalInspectionToolWrapper = (GlobalInspectionToolWrapper)toolWrapper;
                wrapper = globalInspectionToolWrapper.getSharedLocalInspectionToolWrapper();
            }
            if (wrapper == null) continue;
            String language = wrapper.getLanguage();
            if (language != null && Language.findLanguageByID(language) == null) {
                continue; // filter out at least unknown languages
            }
            if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(myFile, wrapper.getTool())) {
                continue;
            }
            enabled.add(wrapper);
        }
        return enabled;
    }

    private void doInspectInjectedPsi( PsiFile injectedPsi,
                                      final boolean isOnTheFly,
                                       final ProgressIndicator indicator,
                                       InspectionManager iManager,
                                      final boolean inVisibleRange,
                                       List<LocalInspectionToolWrapper> wrappers,
                                      boolean checkDumbAwareness) {
        final PsiElement host = InjectedLanguageManager.getInstance(injectedPsi.getProject()).getInjectionHost(injectedPsi);

        final List<PsiElement> elements = getElementsFrom(injectedPsi);
        if (elements.isEmpty()) {
            return;
        }
        MultiMap<LocalInspectionToolWrapper, String> toolToLanguages =
                InspectionEngine.getToolsForElements(wrappers, checkDumbAwareness, elements, Collections.<PsiElement>emptyList());
        for (final Map.Entry<LocalInspectionToolWrapper, Collection<String>> pair : toolToLanguages.entrySet()) {
            indicator.checkCanceled();
            final LocalInspectionToolWrapper wrapper = pair.getKey();
            final LocalInspectionTool tool = wrapper.getTool();
            if (host != null && myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(host, tool)) {
                continue;
            }
            ProblemsHolder holder = new ProblemsHolder(iManager, injectedPsi, isOnTheFly) {
                @Override
                public void registerProblem( ProblemDescriptor descriptor) {
                    super.registerProblem(descriptor);
                    if (isOnTheFly && inVisibleRange) {
                        addDescriptorIncrementally(descriptor, wrapper, indicator);
                    }
                }
            };

            LocalInspectionToolSession injSession = new LocalInspectionToolSession(injectedPsi, 0, injectedPsi.getTextLength());
            Collection<String> languages = pair.getValue();
            InspectionEngine.createVisitorAndAcceptElements(tool, holder, isOnTheFly, injSession, elements, languages);
            tool.inspectionFinished(injSession, holder);
            List<ProblemDescriptor> problems = holder.getResults();
            if (!problems.isEmpty()) {
                appendDescriptors(injectedPsi, problems, wrapper);
            }
        }
    }

    @Override
    
    public List<HighlightInfo> getInfos() {
        return myInfos;
    }

    private static class InspectionResult {
         private final LocalInspectionToolWrapper tool;
         private final List<ProblemDescriptor> foundProblems;

        private InspectionResult( LocalInspectionToolWrapper tool,  List<ProblemDescriptor> foundProblems) {
            this.tool = tool;
            this.foundProblems = new ArrayList<ProblemDescriptor>(foundProblems);
        }
    }

    private static class InspectionContext {
        private InspectionContext( LocalInspectionToolWrapper tool,
                                   ProblemsHolder holder,
                                  int problemsSize, // need this to diff between found problems in visible part and the rest
                                   PsiElementVisitor visitor,
                                   Collection<String> languageIds) {
            this.tool = tool;
            this.holder = holder;
            this.problemsSize = problemsSize;
            this.visitor = visitor;
            this.languageIds = languageIds;
        }

         private final LocalInspectionToolWrapper tool;
         private final ProblemsHolder holder;
        private final int problemsSize;
         private final PsiElementVisitor visitor;
         private final Collection<String> languageIds;
    }
}
