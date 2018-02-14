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

package com.gome.maven.psi.impl.search;

import com.gome.maven.concurrency.AsyncFuture;
import com.gome.maven.concurrency.AsyncUtil;
import com.gome.maven.concurrency.JobLauncher;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.application.ex.ApplicationUtil;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.progress.EmptyProgressIndicator;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressIndicatorProvider;
import com.gome.maven.openapi.progress.util.TooManyUsagesStatus;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.cache.CacheManager;
import com.gome.maven.psi.impl.cache.impl.id.IdIndex;
import com.gome.maven.psi.impl.cache.impl.id.IdIndexEntry;
import com.gome.maven.psi.search.*;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usageView.UsageInfoFactory;
import com.gome.maven.util.CommonProcessors;
import com.gome.maven.util.Function;
import com.gome.maven.util.Processor;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.codeInsight.CommentUtilCore;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.indexing.FileBasedIndex;
import com.gome.maven.util.text.StringSearcher;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntProcedure;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PsiSearchHelperImpl implements PsiSearchHelper {
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.search.PsiSearchHelperImpl");
    private final PsiManagerEx myManager;

    public enum Options {
        PROCESS_INJECTED_PSI, CASE_SENSITIVE_SEARCH, PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE
    }

    @Override
    
    public SearchScope getUseScope( PsiElement element) {
        SearchScope scope = element.getUseScope();
        for (UseScopeEnlarger enlarger : UseScopeEnlarger.EP_NAME.getExtensions()) {
            final SearchScope additionalScope = enlarger.getAdditionalUseScope(element);
            if (additionalScope != null) {
                scope = scope.union(additionalScope);
            }
        }
        for (UseScopeOptimizer optimizer : UseScopeOptimizer.EP_NAME.getExtensions()) {
            final GlobalSearchScope scopeToExclude = optimizer.getScopeToExclude(element);
            if (scopeToExclude != null) {
                scope = scope.intersectWith(GlobalSearchScope.notScope(scopeToExclude));
            }
        }
        return scope;
    }

    public PsiSearchHelperImpl( PsiManagerEx manager) {
        myManager = manager;
    }

    @Override
    
    public PsiElement[] findCommentsContainingIdentifier( String identifier,  SearchScope searchScope) {
        final List<PsiElement> results = Collections.synchronizedList(new ArrayList<PsiElement>());
        processCommentsContainingIdentifier(identifier, searchScope, new CommonProcessors.CollectProcessor<PsiElement>(results));
        return PsiUtilCore.toPsiElementArray(results);
    }

    @Override
    public boolean processCommentsContainingIdentifier( String identifier,
                                                        SearchScope searchScope,
                                                        final Processor<PsiElement> processor) {
        TextOccurenceProcessor occurrenceProcessor = new TextOccurenceProcessor() {
            @Override
            public boolean execute( PsiElement element, int offsetInElement) {
                if (CommentUtilCore.isCommentTextElement(element)) {
                    if (element.findReferenceAt(offsetInElement) == null) {
                        return processor.process(element);
                    }
                }
                return true;
            }
        };
        return processElementsWithWord(occurrenceProcessor, searchScope, identifier, UsageSearchContext.IN_COMMENTS, true);
    }

    @Override
    public boolean processElementsWithWord( TextOccurenceProcessor processor,
                                            SearchScope searchScope,
                                            String text,
                                           short searchContext,
                                           boolean caseSensitive) {
        return processElementsWithWord(processor, searchScope, text, searchContext, caseSensitive, shouldProcessInjectedPsi(searchScope));
    }

    @Override
    public boolean processElementsWithWord( TextOccurenceProcessor processor,
                                            SearchScope searchScope,
                                            String text,
                                           short searchContext,
                                           boolean caseSensitive,
                                           boolean processInjectedPsi) {
        final EnumSet<Options> options = EnumSet.of(Options.PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE);
        if (caseSensitive) options.add(Options.CASE_SENSITIVE_SEARCH);
        if (processInjectedPsi) options.add(Options.PROCESS_INJECTED_PSI);

        return processElementsWithWord(processor, searchScope, text, searchContext, options, null);
    }

    
    @Override
    public AsyncFuture<Boolean> processElementsWithWordAsync( final TextOccurenceProcessor processor,
                                                              SearchScope searchScope,
                                                              final String text,
                                                             final short searchContext,
                                                             final boolean caseSensitively) {
        boolean result = processElementsWithWord(processor, searchScope, text, searchContext, caseSensitively,
                shouldProcessInjectedPsi(searchScope));
        return AsyncUtil.wrapBoolean(result);
    }

    public boolean processElementsWithWord( final TextOccurenceProcessor processor,
                                            SearchScope searchScope,
                                            final String text,
                                           final short searchContext,
                                            EnumSet<Options> options,
                                            String containerName) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Cannot search for elements with empty text");
        }
        ProgressIndicator progress = getOrCreateIndicator();
        if (searchScope instanceof GlobalSearchScope) {
            StringSearcher searcher = new StringSearcher(text, options.contains(Options.CASE_SENSITIVE_SEARCH), true,
                    searchContext == UsageSearchContext.IN_STRINGS,
                    options.contains(Options.PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE));

            return processElementsWithTextInGlobalScope(processor,
                    (GlobalSearchScope)searchScope,
                    searcher,
                    searchContext, options.contains(Options.CASE_SENSITIVE_SEARCH), containerName, progress,
                    options.contains(Options.PROCESS_INJECTED_PSI));
        }
        LocalSearchScope scope = (LocalSearchScope)searchScope;
        PsiElement[] scopeElements = scope.getScope();
        final StringSearcher searcher = new StringSearcher(text, options.contains(Options.CASE_SENSITIVE_SEARCH), true,
                searchContext == UsageSearchContext.IN_STRINGS,
                options.contains(Options.PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE));
        Processor<PsiElement> localProcessor = localProcessor(processor, progress, options.contains(Options.PROCESS_INJECTED_PSI), searcher);
        return JobLauncher.getInstance().invokeConcurrentlyUnderProgress(Arrays.asList(scopeElements), progress, true, true, localProcessor);
    }

    
    private static ProgressIndicator getOrCreateIndicator() {
        ProgressIndicator progress = ProgressIndicatorProvider.getGlobalProgressIndicator();
        if (progress == null) progress = new EmptyProgressIndicator();
        return progress;
    }

    private static boolean shouldProcessInjectedPsi( SearchScope scope) {
        return !(scope instanceof LocalSearchScope) || !((LocalSearchScope)scope).isIgnoreInjectedPsi();
    }

    
    private static Processor<PsiElement> localProcessor( final TextOccurenceProcessor processor,
                                                         final ProgressIndicator progress,
                                                        final boolean processInjectedPsi,
                                                         final StringSearcher searcher) {
        return new Processor<PsiElement>() {
            @Override
            public boolean process(final PsiElement scopeElement) {
                return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
                    @Override
                    public Boolean compute() {
                        return LowLevelSearchUtil.processElementsContainingWordInElement(processor, scopeElement, searcher, processInjectedPsi, progress);
                    }
                }).booleanValue();
            }

            @Override
            public String toString() {
                return processor.toString();
            }
        };
    }

    private boolean processElementsWithTextInGlobalScope( final TextOccurenceProcessor processor,
                                                          final GlobalSearchScope scope,
                                                          final StringSearcher searcher,
                                                         final short searchContext,
                                                         final boolean caseSensitively,
                                                          String containerName,
                                                          ProgressIndicator progress,
                                                         final boolean processInjectedPsi) {
        if (Thread.holdsLock(PsiLock.LOCK)) {
            throw new AssertionError("You must not run search from within updating PSI activity. Please consider invokeLatering it instead.");
        }
        progress.pushState();
        progress.setText(PsiBundle.message("psi.scanning.files.progress"));

        String text = searcher.getPattern();
        Set<VirtualFile> fileSet = new THashSet<VirtualFile>();
        getFilesWithText(scope, searchContext, caseSensitively, text, progress, fileSet);

        progress.setText(PsiBundle.message("psi.search.for.word.progress", text));

        final Processor<PsiElement> localProcessor = localProcessor(processor, progress, processInjectedPsi, searcher);
        if (containerName != null) {
            List<VirtualFile> intersectionWithContainerFiles = new ArrayList<VirtualFile>();
            // intersectionWithContainerFiles holds files containing words from both `text` and `containerName`
            getFilesWithText(scope, searchContext, caseSensitively, text+" "+containerName, progress, intersectionWithContainerFiles);
            if (!intersectionWithContainerFiles.isEmpty()) {
                int totalSize = fileSet.size();
                boolean result = processPsiFileRoots(intersectionWithContainerFiles, totalSize, 0, progress,
                        localProcessor);

                if (result) {
                    fileSet.removeAll(intersectionWithContainerFiles);
                    if (!fileSet.isEmpty()) {
                        result = processPsiFileRoots(new ArrayList<VirtualFile>(fileSet), totalSize, intersectionWithContainerFiles.size(), progress, localProcessor);
                    }
                }
                progress.popState();
                return result;
            }
        }

        boolean result = fileSet.isEmpty() || processPsiFileRoots(new ArrayList<VirtualFile>(fileSet), fileSet.size(), 0, progress, localProcessor);
        progress.popState();
        return result;
    }

    /**
     * @param files to scan for references in this pass.
     * @param totalSize the number of files to scan in both passes. Can be different from <code>files.size()</code> in case of
     *                  two-pass scan, where we first scan files containing container name and then all the rest files.
     * @param alreadyProcessedFiles the number of files scanned in previous pass.
     * @return true if completed
     */
    private boolean processPsiFileRoots( List<VirtualFile> files,
                                        final int totalSize,
                                        int alreadyProcessedFiles,
                                         final ProgressIndicator progress,
                                         final Processor<? super PsiFile> localProcessor) {
        myManager.startBatchFilesProcessingMode();
        try {
            final AtomicInteger counter = new AtomicInteger(alreadyProcessedFiles);
            final AtomicBoolean canceled = new AtomicBoolean(false);

            boolean completed = true;
            while (true) {
                List<VirtualFile> failedList = new SmartList<VirtualFile>();
                final List<VirtualFile> failedFiles = Collections.synchronizedList(failedList);
                final Processor<VirtualFile> processor = new Processor<VirtualFile>() {
                    @Override
                    public boolean process(final VirtualFile vfile) {
                        try {
                            TooManyUsagesStatus.getFrom(progress).pauseProcessingIfTooManyUsages();
                            processVirtualFile(vfile, progress, localProcessor, canceled, counter, totalSize);
                        }
                        catch (ApplicationUtil.CannotRunReadActionException action) {
                            failedFiles.add(vfile);
                        }
                        return !canceled.get();
                    }
                };
                if (ApplicationManager.getApplication().isWriteAccessAllowed() || ((ApplicationEx)ApplicationManager.getApplication()).isWriteActionPending()) {
                    // no point in processing in separate threads - they are doomed to fail to obtain read action anyway
                    completed &= ContainerUtil.process(files, processor);
                }
                else {
                    completed &= JobLauncher.getInstance().invokeConcurrentlyUnderProgress(files, progress, false, false, processor);
                }

                if (failedFiles.isEmpty()) {
                    break;
                }
                // we failed to run read action in job launcher thread
                // run read action in our thread instead to wait for a write action to complete and resume parallel processing
                DumbService.getInstance(myManager.getProject()).runReadActionInSmartMode(EmptyRunnable.getInstance());
                files = failedList;
            }
            return completed;
        }
        finally {
            myManager.finishBatchFilesProcessingMode();
        }
    }

    private void processVirtualFile( final VirtualFile vfile,
                                     final ProgressIndicator progress,
                                     final Processor<? super PsiFile> localProcessor,
                                     final AtomicBoolean canceled,
                                     AtomicInteger counter,
                                    int totalSize) throws ApplicationUtil.CannotRunReadActionException {
        final PsiFile file = ApplicationUtil.tryRunReadAction(new Computable<PsiFile>() {
            @Override
            public PsiFile compute() {
                return vfile.isValid() ? myManager.findFile(vfile) : null;
            }
        });
        if (file != null && !(file instanceof PsiBinaryFile)) {
            // load contents outside read action
            if (FileDocumentManager.getInstance().getCachedDocument(vfile) == null) {
                // cache bytes in vfs
                try {
                    vfile.contentsToByteArray();
                }
                catch (IOException ignored) {
                }
            }
            ApplicationUtil.tryRunReadAction(new Computable<Void>() {
                @Override
                public Void compute() {
                    final Project project = myManager.getProject();
                    if (project.isDisposed()) throw new ProcessCanceledException();
                    if (DumbService.isDumb(project)) throw new ApplicationUtil.CannotRunReadActionException();

                    List<PsiFile> psiRoots = file.getViewProvider().getAllFiles();
                    Set<PsiFile> processed = new THashSet<PsiFile>(psiRoots.size() * 2, (float)0.5);
                    for (final PsiFile psiRoot : psiRoots) {
                        progress.checkCanceled();
                        assert psiRoot != null : "One of the roots of file " + file + " is null. All roots: " + psiRoots + "; ViewProvider: " +
                                file.getViewProvider() + "; Virtual file: " + file.getViewProvider().getVirtualFile();
                        if (!processed.add(psiRoot)) continue;
                        if (!psiRoot.isValid()) {
                            continue;
                        }

                        if (!localProcessor.process(psiRoot)) {
                            canceled.set(true);
                            break;
                        }
                    }
                    return null;
                }
            });
        }
        if (progress.isRunning()) {
            double fraction = (double)counter.incrementAndGet() / totalSize;
            progress.setFraction(fraction);
        }
    }

    private void getFilesWithText( GlobalSearchScope scope,
                                  final short searchContext,
                                  final boolean caseSensitively,
                                   String text,
                                   final ProgressIndicator progress,
                                   Collection<VirtualFile> result) {
        myManager.startBatchFilesProcessingMode();
        try {
            Processor<VirtualFile> processor = new CommonProcessors.CollectProcessor<VirtualFile>(result){
                @Override
                public boolean process(VirtualFile file) {
                    progress.checkCanceled();
                    return super.process(file);
                }
            };
            boolean success = processFilesWithText(scope, searchContext, caseSensitively, text, processor);
            // success == false means exception in index
        }
        finally {
            myManager.finishBatchFilesProcessingMode();
        }
    }

    public boolean processFilesWithText( final GlobalSearchScope scope,
                                        final short searchContext,
                                        final boolean caseSensitively,
                                         String text,
                                         final Processor<VirtualFile> processor) {
        List<IdIndexEntry> entries = getWordEntries(text, caseSensitively);
        if (entries.isEmpty()) return true;

        Condition<Integer> contextMatches = new Condition<Integer>() {
            @Override
            public boolean value(Integer integer) {
                return (integer.intValue() & searchContext) != 0;
            }
        };
        return processFilesContainingAllKeys(myManager.getProject(), scope, contextMatches, entries, processor);
    }

    @Override
    
    public PsiFile[] findFilesWithPlainTextWords( String word) {
        return CacheManager.SERVICE.getInstance(myManager.getProject()).getFilesWithWord(word, UsageSearchContext.IN_PLAIN_TEXT,
                GlobalSearchScope.projectScope(myManager.getProject()),
                true);
    }


    @Override
    public boolean processUsagesInNonJavaFiles( String qName,
                                                PsiNonJavaFileReferenceProcessor processor,
                                                GlobalSearchScope searchScope) {
        return processUsagesInNonJavaFiles(null, qName, processor, searchScope);
    }

    @Override
    public boolean processUsagesInNonJavaFiles( final PsiElement originalElement,
                                                String qName,
                                                final PsiNonJavaFileReferenceProcessor processor,
                                                final GlobalSearchScope initialScope) {
        if (qName.isEmpty()) {
            throw new IllegalArgumentException("Cannot search for elements with empty text. Element: "+originalElement+ "; "+(originalElement == null ? null : originalElement.getClass()));
        }
        final ProgressIndicator progress = getOrCreateIndicator();

        int dotIndex = qName.lastIndexOf('.');
        int dollarIndex = qName.lastIndexOf('$');
        int maxIndex = Math.max(dotIndex, dollarIndex);
        final String wordToSearch = maxIndex >= 0 ? qName.substring(maxIndex + 1) : qName;
        final GlobalSearchScope theSearchScope = ApplicationManager.getApplication().runReadAction(new Computable<GlobalSearchScope>() {
            @Override
            public GlobalSearchScope compute() {
                if (originalElement != null && myManager.isInProject(originalElement) && initialScope.isSearchInLibraries()) {
                    return initialScope.intersectWith(GlobalSearchScope.projectScope(myManager.getProject()));
                }
                return initialScope;
            }
        });
        PsiFile[] files = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile[]>() {
            @Override
            public PsiFile[] compute() {
                return CacheManager.SERVICE.getInstance(myManager.getProject()).getFilesWithWord(wordToSearch, UsageSearchContext.IN_PLAIN_TEXT, theSearchScope, true);
            }
        });

        final StringSearcher searcher = new StringSearcher(qName, true, true, false);
        final int patternLength = searcher.getPattern().length();

        progress.pushState();
        progress.setText(PsiBundle.message("psi.search.in.non.java.files.progress"));

        final SearchScope useScope = originalElement == null ? null : ApplicationManager.getApplication().runReadAction(new Computable<SearchScope>() {
            @Override
            public SearchScope compute() {
                return getUseScope(originalElement);
            }
        });

        final Ref<Boolean> cancelled = new Ref<Boolean>(Boolean.FALSE);
        for (int i = 0; i < files.length; i++) {
            progress.checkCanceled();
            final PsiFile psiFile = files[i];
            if (psiFile instanceof PsiBinaryFile) continue;

            final CharSequence text = ApplicationManager.getApplication().runReadAction(new Computable<CharSequence>() {
                @Override
                public CharSequence compute() {
                    return psiFile.getViewProvider().getContents();
                }
            });

            LowLevelSearchUtil.processTextOccurrences(text, 0, text.length(), searcher, progress, new TIntProcedure() {
                @Override
                public boolean execute(final int index) {
                    boolean isReferenceOK = ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
                        @Override
                        public Boolean compute() {
                            PsiReference referenceAt = psiFile.findReferenceAt(index);
                            return referenceAt == null || useScope == null ||
                                    !PsiSearchScopeUtil.isInScope(useScope.intersectWith(initialScope), psiFile);
                        }
                    });
                    if (isReferenceOK && !processor.process(psiFile, index, index + patternLength)) {
                        cancelled.set(Boolean.TRUE);
                        return false;
                    }

                    return true;
                }
            });
            if (cancelled.get()) break;
            progress.setFraction((double)(i + 1) / files.length);
        }

        progress.popState();
        return !cancelled.get();
    }

    @Override
    public boolean processAllFilesWithWord( String word,
                                            GlobalSearchScope scope,
                                            Processor<PsiFile> processor,
                                           final boolean caseSensitively) {
        return CacheManager.SERVICE.getInstance(myManager.getProject()).processFilesWithWord(processor, word, UsageSearchContext.IN_CODE, scope, caseSensitively);
    }

    @Override
    public boolean processAllFilesWithWordInText( final String word,
                                                  final GlobalSearchScope scope,
                                                  final Processor<PsiFile> processor,
                                                 final boolean caseSensitively) {
        return CacheManager.SERVICE.getInstance(myManager.getProject()).processFilesWithWord(processor, word, UsageSearchContext.IN_PLAIN_TEXT, scope, caseSensitively);
    }

    @Override
    public boolean processAllFilesWithWordInComments( String word,
                                                      GlobalSearchScope scope,
                                                      Processor<PsiFile> processor) {
        return CacheManager.SERVICE.getInstance(myManager.getProject()).processFilesWithWord(processor, word, UsageSearchContext.IN_COMMENTS, scope, true);
    }

    @Override
    public boolean processAllFilesWithWordInLiterals( String word,
                                                      GlobalSearchScope scope,
                                                      Processor<PsiFile> processor) {
        return CacheManager.SERVICE.getInstance(myManager.getProject()).processFilesWithWord(processor, word, UsageSearchContext.IN_STRINGS, scope, true);
    }

    private static class RequestWithProcessor {
         private final PsiSearchRequest request;
         private Processor<PsiReference> refProcessor;

        private RequestWithProcessor( PsiSearchRequest first,  Processor<PsiReference> second) {
            request = first;
            refProcessor = second;
        }

        private boolean uniteWith( final RequestWithProcessor another) {
            if (request.equals(another.request)) {
                final Processor<PsiReference> myProcessor = refProcessor;
                if (myProcessor != another.refProcessor) {
                    refProcessor = new Processor<PsiReference>() {
                        @Override
                        public boolean process(PsiReference psiReference) {
                            return myProcessor.process(psiReference) && another.refProcessor.process(psiReference);
                        }
                    };
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return request.toString();
        }
    }

    @Override
    public boolean processRequests( SearchRequestCollector collector,  Processor<PsiReference> processor) {
        final Map<SearchRequestCollector, Processor<PsiReference>> collectors = ContainerUtil.newHashMap();
        collectors.put(collector, processor);

        ProgressIndicator progress = getOrCreateIndicator();
        appendCollectorsFromQueryRequests(collectors);
        boolean result;
        do {
            MultiMap<Set<IdIndexEntry>, RequestWithProcessor> globals = new MultiMap<Set<IdIndexEntry>, RequestWithProcessor>();
            final List<Computable<Boolean>> customs = ContainerUtil.newArrayList();
            final Set<RequestWithProcessor> locals = ContainerUtil.newLinkedHashSet();
            Map<RequestWithProcessor, Processor<PsiElement>> localProcessors = new THashMap<RequestWithProcessor, Processor<PsiElement>>();
            distributePrimitives(collectors, locals, globals, customs, localProcessors, progress);
            result = processGlobalRequestsOptimized(globals, progress, localProcessors);
            if (result) {
                for (RequestWithProcessor local : locals) {
                    result = processSingleRequest(local.request, local.refProcessor);
                    if (!result) break;
                }
                if (result) {
                    for (Computable<Boolean> custom : customs) {
                        result = custom.compute();
                        if (!result) break;
                    }
                }
                if (!result) break;
            }
        }
        while(appendCollectorsFromQueryRequests(collectors));
        return result;
    }

    
    @Override
    public AsyncFuture<Boolean> processRequestsAsync( SearchRequestCollector collector,  Processor<PsiReference> processor) {
        return AsyncUtil.wrapBoolean(processRequests(collector, processor));
    }

    private static boolean appendCollectorsFromQueryRequests( Map<SearchRequestCollector, Processor<PsiReference>> collectors) {
        boolean changed = false;
        Deque<SearchRequestCollector> queue = new LinkedList<SearchRequestCollector>(collectors.keySet());
        while (!queue.isEmpty()) {
            final SearchRequestCollector each = queue.removeFirst();
            for (QuerySearchRequest request : each.takeQueryRequests()) {
                request.runQuery();
                assert !collectors.containsKey(request.collector) || collectors.get(request.collector) == request.processor;
                collectors.put(request.collector, request.processor);
                queue.addLast(request.collector);
                changed = true;
            }
        }
        return changed;
    }

    private boolean processGlobalRequestsOptimized( MultiMap<Set<IdIndexEntry>, RequestWithProcessor> singles,
                                                    ProgressIndicator progress,
                                                    final Map<RequestWithProcessor, Processor<PsiElement>> localProcessors) {
        if (singles.isEmpty()) {
            return true;
        }

        if (singles.size() == 1) {
            final Collection<? extends RequestWithProcessor> requests = singles.values();
            if (requests.size() == 1) {
                final RequestWithProcessor theOnly = requests.iterator().next();
                return processSingleRequest(theOnly.request, theOnly.refProcessor);
            }
        }

        progress.pushState();
        progress.setText(PsiBundle.message("psi.scanning.files.progress"));
        boolean result;

        try {
            // intersectionCandidateFiles holds files containing words from all requests in `singles` and words in corresponding container names
            final MultiMap<VirtualFile, RequestWithProcessor> intersectionCandidateFiles = createMultiMap();
            // restCandidateFiles holds files containing words from all requests in `singles` but EXCLUDING words in corresponding container names
            final MultiMap<VirtualFile, RequestWithProcessor> restCandidateFiles = createMultiMap();
            collectFiles(singles, progress, intersectionCandidateFiles, restCandidateFiles);

            if (intersectionCandidateFiles.isEmpty() && restCandidateFiles.isEmpty()) {
                return true;
            }

            final Set<String> allWords = new TreeSet<String>();
            for (RequestWithProcessor singleRequest : localProcessors.keySet()) {
                allWords.add(singleRequest.request.word);
            }
            progress.setText(PsiBundle.message("psi.search.for.word.progress", getPresentableWordsDescription(allWords)));

            if (intersectionCandidateFiles.isEmpty()) {
                result = processCandidates(localProcessors, restCandidateFiles, progress, restCandidateFiles.size(), 0);
            }
            else {
                int totalSize = restCandidateFiles.size() + intersectionCandidateFiles.size();
                result = processCandidates(localProcessors, intersectionCandidateFiles, progress, totalSize, 0);
                if (result) {
                    result = processCandidates(localProcessors, restCandidateFiles, progress, totalSize, intersectionCandidateFiles.size());
                }
            }
        }
        finally {
            progress.popState();
        }

        return result;
    }

    private boolean processCandidates( final Map<RequestWithProcessor, Processor<PsiElement>> localProcessors,
                                       final MultiMap<VirtualFile, RequestWithProcessor> candidateFiles,
                                       ProgressIndicator progress,
                                      int totalSize,
                                      int alreadyProcessedFiles) {
        List<VirtualFile> files = new ArrayList<VirtualFile>(candidateFiles.keySet());

        return processPsiFileRoots(files, totalSize, alreadyProcessedFiles, progress, new Processor<PsiFile>() {
            @Override
            public boolean process(final PsiFile psiRoot) {
                final VirtualFile vfile = psiRoot.getVirtualFile();
                for (final RequestWithProcessor singleRequest : candidateFiles.get(vfile)) {
                    Processor<PsiElement> localProcessor = localProcessors.get(singleRequest);
                    if (!localProcessor.process(psiRoot)) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    
    private static String getPresentableWordsDescription( Set<String> allWords) {
        final StringBuilder result = new StringBuilder();
        for (String string : allWords) {
            if (string != null && !string.isEmpty()) {
                if (result.length() > 50) {
                    result.append("...");
                    break;
                }
                if (result.length() != 0) result.append(", ");
                result.append(string);
            }
        }
        return result.toString();
    }

    
    private static TextOccurenceProcessor adaptProcessor( PsiSearchRequest singleRequest,
                                                          final Processor<PsiReference> consumer) {
        final SearchScope searchScope = singleRequest.searchScope;
        final boolean ignoreInjectedPsi = searchScope instanceof LocalSearchScope && ((LocalSearchScope)searchScope).isIgnoreInjectedPsi();
        final RequestResultProcessor wrapped = singleRequest.processor;
        return new TextOccurenceProcessor() {
            @Override
            public boolean execute( PsiElement element, int offsetInElement) {
                if (ignoreInjectedPsi && element instanceof PsiLanguageInjectionHost) return true;

                return wrapped.processTextOccurrence(element, offsetInElement, consumer);
            }

            @Override
            public String toString() {
                return consumer.toString();
            }
        };
    }

    private void collectFiles( MultiMap<Set<IdIndexEntry>, RequestWithProcessor> singles,
                               ProgressIndicator progress,
                               final MultiMap<VirtualFile, RequestWithProcessor> intersectionResult,
                               final MultiMap<VirtualFile, RequestWithProcessor> restResult) {
        for (final Set<IdIndexEntry> keys : singles.keySet()) {
            if (keys.isEmpty()) {
                continue;
            }

            final Collection<RequestWithProcessor> data = singles.get(keys);
            final GlobalSearchScope commonScope = uniteScopes(data);
            final Set<VirtualFile> intersectionWithContainerNameFiles = intersectionWithContainerNameFiles(commonScope, data, keys);

            List<VirtualFile> files = new ArrayList<VirtualFile>();
            Processor<VirtualFile> processor = new CommonProcessors.CollectProcessor<VirtualFile>(files);
            processFilesContainingAllKeys(myManager.getProject(), commonScope, null, keys, processor);
            for (final VirtualFile file : files) {
                progress.checkCanceled();
                for (final IdIndexEntry entry : keys) {
                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            FileBasedIndex.getInstance().processValues(IdIndex.NAME, entry, file, new FileBasedIndex.ValueProcessor<Integer>() {
                                @Override
                                public boolean process(VirtualFile file, Integer value) {
                                    int mask = value.intValue();
                                    for (RequestWithProcessor single : data) {
                                        final PsiSearchRequest request = single.request;
                                        if ((mask & request.searchContext) != 0 && ((GlobalSearchScope)request.searchScope).contains(file)) {
                                            MultiMap<VirtualFile, RequestWithProcessor> result =
                                                    intersectionWithContainerNameFiles == null || !intersectionWithContainerNameFiles.contains(file) ? restResult : intersectionResult;
                                            result.putValue(file, single);
                                        }
                                    }
                                    return true;
                                }
                            }, commonScope);
                        }
                    });
                }
            }
        }
    }

    private Set<VirtualFile> intersectionWithContainerNameFiles( GlobalSearchScope commonScope,
                                                                 Collection<RequestWithProcessor> data,
                                                                 Set<IdIndexEntry> keys) {
        String commonName = null;
        short searchContext = 0;
        boolean caseSensitive = true;
        for (RequestWithProcessor r : data) {
            String name = r.request.containerName;
            if (name != null) {
                if (commonName == null) {
                    commonName = r.request.containerName;
                    searchContext = r.request.searchContext;
                    caseSensitive = r.request.caseSensitive;
                }
                else if (commonName.equals(name)) {
                    searchContext |= r.request.searchContext;
                    caseSensitive &= r.request.caseSensitive;
                }
                else {
                    return null;
                }
            }
        }
        if (commonName == null) return null;
        Set<VirtualFile> containerFiles = new THashSet<VirtualFile>();

        List<IdIndexEntry> entries = getWordEntries(commonName, caseSensitive);
        if (entries.isEmpty()) return null;
        entries.addAll(keys); // should find words from both text and container names

        final short finalSearchContext = searchContext;
        Condition<Integer> contextMatches = new Condition<Integer>() {
            @Override
            public boolean value(Integer context) {
                return (context.intValue() & finalSearchContext) != 0;
            }
        };
        processFilesContainingAllKeys(myManager.getProject(), commonScope, contextMatches, entries, new CommonProcessors.CollectProcessor<VirtualFile>(containerFiles));

        return containerFiles;
    }

    
    private static MultiMap<VirtualFile, RequestWithProcessor> createMultiMap() {
        // usually there is just one request
        return MultiMap.createSmart();
    }

    
    private static GlobalSearchScope uniteScopes( Collection<RequestWithProcessor> requests) {
        Set<GlobalSearchScope> scopes = ContainerUtil.map2LinkedSet(requests, new Function<RequestWithProcessor, GlobalSearchScope>() {
            @Override
            public GlobalSearchScope fun(RequestWithProcessor r) {
                return (GlobalSearchScope)r.request.searchScope;
            }
        });
        return GlobalSearchScope.union(scopes.toArray(new GlobalSearchScope[scopes.size()]));
    }

    private static void distributePrimitives( Map<SearchRequestCollector, Processor<PsiReference>> collectors,
                                              Set<RequestWithProcessor> locals,
                                              MultiMap<Set<IdIndexEntry>, RequestWithProcessor> singles,
                                              List<Computable<Boolean>> customs,
                                              Map<RequestWithProcessor, Processor<PsiElement>> localProcessors,
                                              ProgressIndicator progress) {
        for (final Map.Entry<SearchRequestCollector, Processor<PsiReference>> entry : collectors.entrySet()) {
            final Processor<PsiReference> processor = entry.getValue();
            SearchRequestCollector collector = entry.getKey();
            for (final PsiSearchRequest primitive : collector.takeSearchRequests()) {
                final SearchScope scope = primitive.searchScope;
                if (scope instanceof LocalSearchScope) {
                    registerRequest(locals, primitive, processor);
                }
                else {
                    Set<IdIndexEntry> key = new HashSet<IdIndexEntry>(getWordEntries(primitive.word, primitive.caseSensitive));
                    registerRequest(singles.getModifiable(key), primitive, processor);
                }
            }
            for (final Processor<Processor<PsiReference>> customAction : collector.takeCustomSearchActions()) {
                customs.add(new Computable<Boolean>() {
                    @Override
                    public Boolean compute() {
                        return customAction.process(processor);
                    }
                });
            }
        }

        for (Map.Entry<Set<IdIndexEntry>, Collection<RequestWithProcessor>> entry : singles.entrySet()) {
            for (RequestWithProcessor singleRequest : entry.getValue()) {
                PsiSearchRequest primitive = singleRequest.request;
                StringSearcher searcher = new StringSearcher(primitive.word, primitive.caseSensitive, true, false);
                final TextOccurenceProcessor adapted = adaptProcessor(primitive, singleRequest.refProcessor);

                Processor<PsiElement> localProcessor = localProcessor(adapted, progress, true, searcher);

                assert !localProcessors.containsKey(singleRequest) || localProcessors.get(singleRequest) == localProcessor;
                localProcessors.put(singleRequest, localProcessor);
            }
        }
    }

    private static void registerRequest( Collection<RequestWithProcessor> collection,
                                         PsiSearchRequest primitive,
                                         Processor<PsiReference> processor) {
        RequestWithProcessor singleRequest = new RequestWithProcessor(primitive, processor);

        for (RequestWithProcessor existing : collection) {
            if (existing.uniteWith(singleRequest)) {
                return;
            }
        }
        collection.add(singleRequest);
    }

    private boolean processSingleRequest( PsiSearchRequest single,  Processor<PsiReference> consumer) {
        final EnumSet<Options> options = EnumSet.of(Options.PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE);
        if (single.caseSensitive) options.add(Options.CASE_SENSITIVE_SEARCH);
        if (shouldProcessInjectedPsi(single.searchScope)) options.add(Options.PROCESS_INJECTED_PSI);

        return processElementsWithWord(adaptProcessor(single, consumer), single.searchScope, single.word, single.searchContext, options,
                single.containerName);
    }

    
    @Override
    public SearchCostResult isCheapEnoughToSearch( String name,
                                                   final GlobalSearchScope scope,
                                                   final PsiFile fileToIgnoreOccurrencesIn,
                                                   final ProgressIndicator progress) {
        final AtomicInteger count = new AtomicInteger();
        final ProgressIndicator indicator = progress == null ? new EmptyProgressIndicator() : progress;
        final Processor<VirtualFile> processor = new Processor<VirtualFile>() {
            private final VirtualFile virtualFileToIgnoreOccurrencesIn =
                    fileToIgnoreOccurrencesIn == null ? null : fileToIgnoreOccurrencesIn.getVirtualFile();

            @Override
            public boolean process(VirtualFile file) {
                indicator.checkCanceled();
                if (Comparing.equal(file, virtualFileToIgnoreOccurrencesIn)) return true;
                final int value = count.incrementAndGet();
                return value < 10;
            }
        };
        List<IdIndexEntry> keys = getWordEntries(name, true);
        boolean cheap = keys.isEmpty() || processFilesContainingAllKeys(myManager.getProject(), scope, null, keys, processor);

        if (!cheap) {
            return SearchCostResult.TOO_MANY_OCCURRENCES;
        }

        return count.get() == 0 ? SearchCostResult.ZERO_OCCURRENCES : SearchCostResult.FEW_OCCURRENCES;
    }

    private static boolean processFilesContainingAllKeys( Project project,
                                                          final GlobalSearchScope scope,
                                                          final Condition<Integer> checker,
                                                          final Collection<IdIndexEntry> keys,
                                                          final Processor<VirtualFile> processor) {
        final FileIndexFacade index = FileIndexFacade.getInstance(project);
        return DumbService.getInstance(project).runReadActionInSmartMode(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return FileBasedIndex.getInstance().processFilesContainingAllKeys(IdIndex.NAME, keys, scope, checker, new Processor<VirtualFile>() {
                    @Override
                    public boolean process(VirtualFile file) {
                        return !index.shouldBeFound(scope, file) || processor.process(file);
                    }
                });
            }
        });
    }

    
    private static List<IdIndexEntry> getWordEntries( String name, final boolean caseSensitively) {
        List<String> words = StringUtil.getWordsInStringLongestFirst(name);
        if (words.isEmpty()) {
            String trimmed = name.trim();
            if (StringUtil.isNotEmpty(trimmed)) {
                words = Collections.singletonList(trimmed);
            }
        }
        if (words.isEmpty()) return Collections.emptyList();
        return ContainerUtil.map2List(words, new Function<String, IdIndexEntry>() {
            @Override
            public IdIndexEntry fun(String word) {
                return new IdIndexEntry(word, caseSensitively);
            }
        });
    }

    public static boolean processTextOccurrences( final PsiElement element,
                                                  String stringToSearch,
                                                  GlobalSearchScope searchScope,
                                                  final Processor<UsageInfo> processor,
                                                  final UsageInfoFactory factory) {
        PsiSearchHelper helper = ApplicationManager.getApplication().runReadAction(new Computable<PsiSearchHelper>() {
            @Override
            public PsiSearchHelper compute() {
                return SERVICE.getInstance(element.getProject());
            }
        });

        return helper.processUsagesInNonJavaFiles(element, stringToSearch, new PsiNonJavaFileReferenceProcessor() {
            @Override
            public boolean process(final PsiFile psiFile, final int startOffset, final int endOffset) {
                try {
                    UsageInfo usageInfo = ApplicationManager.getApplication().runReadAction(new Computable<UsageInfo>() {
                        @Override
                        public UsageInfo compute() {
                            return factory.createUsageInfo(psiFile, startOffset, endOffset);
                        }
                    });
                    return usageInfo == null || processor.process(usageInfo);
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Exception e) {
                    LOG.error(e);
                    return true;
                }
            }
        }, searchScope);
    }
}
