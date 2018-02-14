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

import com.gome.maven.codeInsight.daemon.DaemonBundle;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.codeInsight.daemon.impl.analysis.CustomHighlightInfoHolder;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.gome.maven.codeInsight.problems.ProblemImpl;
import com.gome.maven.concurrency.JobScheduler;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.problems.Problem;
import com.gome.maven.problems.WolfTheProblemSolver;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiErrorElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.search.PsiTodoSearchHelper;
import com.gome.maven.psi.search.TodoItem;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.NotNullProducer;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.Stack;
import gnu.trove.THashSet;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GeneralHighlightingPass extends ProgressableTextEditorHighlightingPass implements DumbAware {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.daemon.impl.GeneralHighlightingPass");
    private static final String PRESENTABLE_NAME = DaemonBundle.message("pass.syntax");
    private static final Key<Boolean> HAS_ERROR_ELEMENT = Key.create("HAS_ERROR_ELEMENT");
    protected static final Condition<PsiFile> SHOULD_HIGHIGHT_FILTER = new Condition<PsiFile>() {
        @Override
        public boolean value(PsiFile file) {
            return HighlightingLevelManager.getInstance(file.getProject()).shouldHighlight(file);
        }
    };
    private static final Random RESTART_DAEMON_RANDOM = new Random();

    protected final boolean myUpdateAll;
    protected final ProperTextRange myPriorityRange;

    protected final List<HighlightInfo> myHighlights = new ArrayList<HighlightInfo>();

    protected volatile boolean myHasErrorElement;
    private volatile boolean myErrorFound;
    protected final EditorColorsScheme myGlobalScheme;
    private volatile NotNullProducer<HighlightVisitor[]> myHighlightVisitorProducer = new NotNullProducer<HighlightVisitor[]>() {
        
        @Override
        public HighlightVisitor[] produce() {
            return cloneHighlightVisitors();
        }
    };

    public GeneralHighlightingPass( Project project,
                                    PsiFile file,
                                    Document document,
                                   int startOffset,
                                   int endOffset,
                                   boolean updateAll,
                                    ProperTextRange priorityRange,
                                    Editor editor,
                                    HighlightInfoProcessor highlightInfoProcessor) {
        super(project, document, PRESENTABLE_NAME, file, editor, TextRange.create(startOffset, endOffset), true, highlightInfoProcessor);
        myUpdateAll = updateAll;
        myPriorityRange = priorityRange;

        PsiUtilCore.ensureValid(file);
        boolean wholeFileHighlighting = isWholeFileHighlighting();
        myHasErrorElement = !wholeFileHighlighting && Boolean.TRUE.equals(myFile.getUserData(HAS_ERROR_ELEMENT));
        final DaemonCodeAnalyzerEx daemonCodeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(myProject);
        FileStatusMap fileStatusMap = daemonCodeAnalyzer.getFileStatusMap();
        myErrorFound = !wholeFileHighlighting && fileStatusMap.wasErrorFound(myDocument);

        // initial guess to show correct progress in the traffic light icon
        setProgressLimit(document.getTextLength()/2); // approx number of PSI elements = file length/2
        myGlobalScheme = editor != null ? editor.getColorsScheme() : EditorColorsManager.getInstance().getGlobalScheme();
    }

    private static final Key<AtomicInteger> HIGHLIGHT_VISITOR_INSTANCE_COUNT = new Key<AtomicInteger>("HIGHLIGHT_VISITOR_INSTANCE_COUNT");
    
    private HighlightVisitor[] cloneHighlightVisitors() {
        int oldCount = incVisitorUsageCount(1);
        HighlightVisitor[] highlightVisitors = Extensions.getExtensions(HighlightVisitor.EP_HIGHLIGHT_VISITOR, myProject);
        if (oldCount != 0) {
            HighlightVisitor[] clones = new HighlightVisitor[highlightVisitors.length];
            for (int i = 0; i < highlightVisitors.length; i++) {
                HighlightVisitor highlightVisitor = highlightVisitors[i];
                HighlightVisitor cloned = highlightVisitor.clone();
                assert cloned.getClass() == highlightVisitor.getClass() : highlightVisitor.getClass()+".clone() must return a copy of "+highlightVisitor.getClass()+"; but got: "+cloned+" of "+cloned.getClass();
                clones[i] = cloned;
            }
            highlightVisitors = clones;
        }
        return highlightVisitors;
    }

    
    private HighlightVisitor[] filterVisitors( HighlightVisitor[] highlightVisitors,  PsiFile psiFile) {
        final List<HighlightVisitor> visitors = new ArrayList<HighlightVisitor>(highlightVisitors.length);
        List<HighlightVisitor> list = Arrays.asList(highlightVisitors);
        for (HighlightVisitor visitor : DumbService.getInstance(myProject).filterByDumbAwareness(list)) {
            if (visitor.suitableForFile(psiFile)) {
                visitors.add(visitor);
            }
        }
        if (visitors.isEmpty()) {
            LOG.error("No visitors registered. list=" +
                    list +
                    "; all visitors are:" +
                    Arrays.asList(Extensions.getExtensions(HighlightVisitor.EP_HIGHLIGHT_VISITOR, myProject)));
        }

        return visitors.toArray(new HighlightVisitor[visitors.size()]);
    }

    public void setHighlightVisitorProducer( NotNullProducer<HighlightVisitor[]> highlightVisitorProducer) {
        myHighlightVisitorProducer = highlightVisitorProducer;
    }

    
    protected HighlightVisitor[] getHighlightVisitors( PsiFile psiFile) {
        return filterVisitors(myHighlightVisitorProducer.produce(), psiFile);
    }

    // returns old value
    public int incVisitorUsageCount(int delta) {
        AtomicInteger count = myProject.getUserData(HIGHLIGHT_VISITOR_INSTANCE_COUNT);
        if (count == null) {
            count = ((UserDataHolderEx)myProject).putUserDataIfAbsent(HIGHLIGHT_VISITOR_INSTANCE_COUNT, new AtomicInteger(0));
        }
        int old = count.getAndAdd(delta);
        assert old + delta >= 0 : old +";" + delta;
        return old;
    }

    @Override
    protected void collectInformationWithProgress( final ProgressIndicator progress) {
        final List<HighlightInfo> outsideResult = new ArrayList<HighlightInfo>(100);
        final List<HighlightInfo> insideResult = new ArrayList<HighlightInfo>(100);

        final DaemonCodeAnalyzerEx daemonCodeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(myProject);
        final HighlightVisitor[] filteredVisitors = getHighlightVisitors(myFile);
        final List<PsiElement> insideElements = new ArrayList<PsiElement>();
        final List<PsiElement> outsideElements = new ArrayList<PsiElement>();
        try {
            List<ProperTextRange> insideRanges = new ArrayList<ProperTextRange>();
            List<ProperTextRange> outsideRanges = new ArrayList<ProperTextRange>();
            Divider.divideInsideAndOutside(myFile, myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(), myPriorityRange, insideElements, insideRanges, outsideElements,
                    outsideRanges, false, SHOULD_HIGHIGHT_FILTER);
            // put file element always in outsideElements
            if (!insideElements.isEmpty() && insideElements.get(insideElements.size()-1) instanceof PsiFile) {
                PsiElement file = insideElements.remove(insideElements.size() - 1);
                outsideElements.add(file);
                ProperTextRange range = insideRanges.remove(insideRanges.size() - 1);
                outsideRanges.add(range);
            }

            setProgressLimit((long)(insideElements.size()+outsideElements.size()));

            final boolean forceHighlightParents = forceHighlightParents();

            if (!isDumbMode()) {
                highlightTodos(myFile, myDocument.getCharsSequence(), myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(), progress, myPriorityRange, insideResult,
                        outsideResult);
            }

            boolean success = collectHighlights(insideElements, insideRanges, outsideElements, outsideRanges, progress, filteredVisitors, insideResult, outsideResult, forceHighlightParents);

            if (success) {
                myHighlightInfoProcessor.highlightsOutsideVisiblePartAreProduced(myHighlightingSession, outsideResult, myPriorityRange,
                        myRestrictRange,
                        getId());

                if (myUpdateAll) {
                    daemonCodeAnalyzer.getFileStatusMap().setErrorFoundFlag(myProject, myDocument, myErrorFound);
                }
            }
            else {
                cancelAndRestartDaemonLater(progress, myProject);
            }
        }
        finally {
            incVisitorUsageCount(-1);
            myHighlights.addAll(insideResult);
            myHighlights.addAll(outsideResult);
        }
    }

    protected boolean isFailFastOnAcquireReadAction() {
        return true;
    }

    private boolean isWholeFileHighlighting() {
        return myUpdateAll && myRestrictRange.equalsToRange(0, myDocument.getTextLength());
    }

    @Override
    protected void applyInformationWithProgress() {
        myFile.putUserData(HAS_ERROR_ELEMENT, myHasErrorElement);

        if (myUpdateAll) {
            reportErrorsToWolf();
        }
    }

    @Override
    
    public List<HighlightInfo> getInfos() {
        return new ArrayList<HighlightInfo>(myHighlights);
    }

    private boolean collectHighlights( final List<PsiElement> elements1,
                                       final List<ProperTextRange> ranges1,
                                       final List<PsiElement> elements2,
                                       final List<ProperTextRange> ranges2,
                                       final ProgressIndicator progress,
                                       final HighlightVisitor[] visitors,
                                       final List<HighlightInfo> insideResult,
                                       final List<HighlightInfo> outsideResult,
                                      final boolean forceHighlightParents) {
        final Set<PsiElement> skipParentsSet = new THashSet<PsiElement>();

        // TODO - add color scheme to holder
        final HighlightInfoHolder holder = createInfoHolder(myFile);

        final int chunkSize = Math.max(1, (elements1.size()+elements2.size()) / 100); // one percent precision is enough

        boolean success = analyzeByVisitors(visitors, holder, 0, new Runnable() {
            @Override
            public void run() {
                runVisitors(elements1, ranges1, chunkSize, progress, skipParentsSet, holder, insideResult, outsideResult, forceHighlightParents, visitors);
                final TextRange priorityIntersection = myPriorityRange.intersection(myRestrictRange);
                if ((!elements1.isEmpty() || !insideResult.isEmpty()) && priorityIntersection != null) { // do not apply when there were no elements to highlight
                    myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, insideResult, myPriorityRange, myRestrictRange, getId());
                }
                runVisitors(elements2, ranges2, chunkSize, progress, skipParentsSet, holder, insideResult, outsideResult, forceHighlightParents, visitors);
            }
        });
        List<HighlightInfo> postInfos = new ArrayList<HighlightInfo>(holder.size());
        // there can be extra highlights generated in PostHighlightVisitor
        for (int j = 0; j < holder.size(); j++) {
            final HighlightInfo info = holder.get(j);
            assert info != null;
            postInfos.add(info);
        }
        myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, postInfos, myFile.getTextRange(), myFile.getTextRange(), POST_UPDATE_ALL);
        return success;
    }

    private boolean analyzeByVisitors( final HighlightVisitor[] visitors,
                                       final HighlightInfoHolder holder,
                                      final int i,
                                       final Runnable action) {
        final boolean[] success = {true};
        if (i == visitors.length) {
            action.run();
        }
        else {
            if (!visitors[i].analyze(myFile, myUpdateAll, holder, new Runnable() {
                @Override
                public void run() {
                    success[0] = analyzeByVisitors(visitors, holder, i + 1, action);
                }
            })) {
                success[0] = false;
            }
        }
        return success[0];
    }

    private void runVisitors( List<PsiElement> elements,
                              List<ProperTextRange> ranges,
                             int chunkSize,
                              ProgressIndicator progress,
                              Set<PsiElement> skipParentsSet,
                              HighlightInfoHolder holder,
                              List<HighlightInfo> insideResult,
                              List<HighlightInfo> outsideResult,
                             boolean forceHighlightParents,
                              HighlightVisitor[] visitors) {
        Stack<TextRange> nestedRange = new Stack<TextRange>();
        Stack<List<HighlightInfo>> nestedInfos = new Stack<List<HighlightInfo>>();
        boolean failed = false;
        int nextLimit = chunkSize;
        for (int i = 0; i < elements.size(); i++) {
            PsiElement element = elements.get(i);
            progress.checkCanceled();

            PsiElement parent = element.getParent();
            if (element != myFile && !skipParentsSet.isEmpty() && element.getFirstChild() != null && skipParentsSet.contains(element)) {
                skipParentsSet.add(parent);
                continue;
            }

            boolean isErrorElement = element instanceof PsiErrorElement;
            if (isErrorElement) {
                myHasErrorElement = true;
            }

            for (HighlightVisitor visitor : visitors) {
                try {
                    visitor.visit(element);
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (IndexNotReadyException e) {
                    throw e;
                }
                catch (Exception e) {
                    if (!failed) {
                        LOG.error(e);
                    }
                    failed = true;
                }
            }

            if (i == nextLimit) {
                advanceProgress(chunkSize);
                nextLimit = i + chunkSize;
            }

            TextRange elementRange = ranges.get(i);
            List<HighlightInfo> infosForThisRange = holder.size() == 0 ? null : new ArrayList<HighlightInfo>(holder.size());
            for (int j = 0; j < holder.size(); j++) {
                final HighlightInfo info = holder.get(j);

                if (!myRestrictRange.containsRange(info.getStartOffset(), info.getEndOffset())) continue;
                List<HighlightInfo> result = myPriorityRange.containsRange(info.getStartOffset(), info.getEndOffset()) && !(element instanceof PsiFile) ? insideResult : outsideResult;
                // have to filter out already obtained highlights
                if (!result.add(info)) continue;
                boolean isError = info.getSeverity() == HighlightSeverity.ERROR;
                if (isError) {
                    if (!forceHighlightParents) {
                        skipParentsSet.add(parent);
                    }
                    myErrorFound = true;
                }
                // if this highlight info range is exactly the same as the element range we are visiting
                // that means we can clear this highlight as soon as visitors won't produce any highlights during visiting the same range next time.
                // We also know that we can remove syntax error element.
                info.setBijective(elementRange.equalsToRange(info.startOffset, info.endOffset) || isErrorElement);

                myHighlightInfoProcessor.infoIsAvailable(myHighlightingSession, info);
                infosForThisRange.add(info);
            }
            holder.clear();

            // include infos which we got while visiting nested elements with the same range
            while (true) {
                if (!nestedRange.isEmpty() && elementRange.contains(nestedRange.peek())) {
                    TextRange oldRange = nestedRange.pop();
                    List<HighlightInfo> oldInfos = nestedInfos.pop();
                    if (elementRange.equals(oldRange)) {
                        if (infosForThisRange == null) {
                            infosForThisRange = oldInfos;
                        }
                        else if (oldInfos != null) {
                            infosForThisRange.addAll(oldInfos);
                        }
                    }
                }
                else {
                    break;
                }
            }
            nestedRange.push(elementRange);
            nestedInfos.push(infosForThisRange);
            // optimisation: this element range does not equal to its parent' range if next element in "ranges" range is different since we top-sorted elements there by ancestry
            if (parent == null || i != ranges.size()-1 && !elementRange.equals(ranges.get(i+1)) || !Comparing.equal(elementRange, parent.getTextRange())) {
                myHighlightInfoProcessor.allHighlightsForRangeAreProduced(myHighlightingSession, elementRange, infosForThisRange);
            }
        }
        advanceProgress(elements.size() - (nextLimit-chunkSize));
    }

    private static final int POST_UPDATE_ALL = 5;

    private static void cancelAndRestartDaemonLater( ProgressIndicator progress,
                                                     final Project project) throws ProcessCanceledException {
        progress.cancel();
        JobScheduler.getScheduler().schedule(new Runnable() {

            @Override
            public void run() {
                Application application = ApplicationManager.getApplication();
                if (!project.isDisposed() && !application.isDisposed() && !application.isUnitTestMode()) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DaemonCodeAnalyzer.getInstance(project).restart();
                        }
                    }, project.getDisposed());
                }
            }
        }, RESTART_DAEMON_RANDOM.nextInt(100), TimeUnit.MILLISECONDS);
        throw new ProcessCanceledException();
    }

    private boolean forceHighlightParents() {
        boolean forceHighlightParents = false;
        for(HighlightRangeExtension extension: Extensions.getExtensions(HighlightRangeExtension.EP_NAME)) {
            if (extension.isForceHighlightParents(myFile)) {
                forceHighlightParents = true;
                break;
            }
        }
        return forceHighlightParents;
    }

    protected HighlightInfoHolder createInfoHolder(final PsiFile file) {
        final HighlightInfoFilter[] filters = HighlightInfoFilter.EXTENSION_POINT_NAME.getExtensions();
        return new CustomHighlightInfoHolder(file, getColorsScheme(), filters);
    }

    static void highlightTodos( PsiFile file,
                                CharSequence text,
                               int startOffset,
                               int endOffset,
                                ProgressIndicator progress,
                                ProperTextRange priorityRange,
                                Collection<HighlightInfo> insideResult,
                                Collection<HighlightInfo> outsideResult) {
        PsiTodoSearchHelper helper = PsiTodoSearchHelper.SERVICE.getInstance(file.getProject());
        if (helper == null) return;
        TodoItem[] todoItems = helper.findTodoItems(file, startOffset, endOffset);
        if (todoItems.length == 0) return;

        for (TodoItem todoItem : todoItems) {
            progress.checkCanceled();
            TextRange range = todoItem.getTextRange();
            String description = text.subSequence(range.getStartOffset(), range.getEndOffset()).toString();
            TextAttributes attributes = todoItem.getPattern().getAttributes().getTextAttributes();
            HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.TODO).range(range);
            builder.textAttributes(attributes);
            builder.descriptionAndTooltip(description);
            HighlightInfo info = builder.createUnconditionally();
            (priorityRange.containsRange(info.getStartOffset(), info.getEndOffset()) ? insideResult : outsideResult).add(info);
        }
    }

    private void reportErrorsToWolf() {
        if (!myFile.getViewProvider().isPhysical()) return; // e.g. errors in evaluate expression
        Project project = myFile.getProject();
        if (!PsiManager.getInstance(project).isInProject(myFile)) return; // do not report problems in libraries
        VirtualFile file = myFile.getVirtualFile();
        if (file == null) return;

        List<Problem> problems = convertToProblems(getInfos(), file, myHasErrorElement);
        WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(project);

        boolean hasErrors = DaemonCodeAnalyzerEx.hasErrors(project, getDocument());
        if (!hasErrors || isWholeFileHighlighting()) {
            wolf.reportProblems(file, problems);
        }
        else {
            wolf.weHaveGotProblems(file, problems);
        }
    }

    @Override
    public double getProgress() {
        // do not show progress of visible highlighters update
        return myUpdateAll ? super.getProgress() : -1;
    }

    private static List<Problem> convertToProblems( Collection<HighlightInfo> infos,
                                                    VirtualFile file,
                                                   final boolean hasErrorElement) {
        List<Problem> problems = new SmartList<Problem>();
        for (HighlightInfo info : infos) {
            if (info.getSeverity() == HighlightSeverity.ERROR) {
                Problem problem = new ProblemImpl(file, info, hasErrorElement);
                problems.add(problem);
            }
        }
        return problems;
    }

    @Override
    public String toString() {
        return super.toString() + " updateAll="+myUpdateAll+" range= "+myRestrictRange;
    }
}
