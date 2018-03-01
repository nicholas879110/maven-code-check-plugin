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

package com.gome.maven.psi.impl.source.tree.injected;

import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.gome.maven.concurrency.JobLauncher;
import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.injected.editor.DocumentWindowImpl;
import com.gome.maven.injected.editor.EditorWindowImpl;
import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.lang.injection.MultiHostInjector;
import com.gome.maven.lang.injection.MultiHostRegistrar;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.extensions.ExtensionPoint;
import com.gome.maven.openapi.extensions.ExtensionPointListener;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.extensions.PluginDescriptor;
import com.gome.maven.openapi.extensions.impl.ExtensionPointImpl;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiDocumentManagerBase;
import com.gome.maven.psi.impl.source.resolve.FileContextUtil;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Processor;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ConcurrentList;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashSet;

import java.util.*;

/**
 * @author cdr
 */
public class InjectedLanguageManagerImpl extends InjectedLanguageManager implements Disposable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageManagerImpl");
    private final Project myProject;
    private final DumbService myDumbService;
    private volatile DaemonProgressIndicator myProgress;

    public static InjectedLanguageManagerImpl getInstanceImpl(Project project) {
        return (InjectedLanguageManagerImpl)InjectedLanguageManager.getInstance(project);
    }

    public InjectedLanguageManagerImpl(Project project, DumbService dumbService) {
        myProject = project;
        myDumbService = dumbService;

        final ExtensionPoint<MultiHostInjector> multiPoint = Extensions.getArea(project).getExtensionPoint(MultiHostInjector.MULTIHOST_INJECTOR_EP_NAME);
        ((ExtensionPointImpl<MultiHostInjector>)multiPoint).addExtensionPointListener(new ExtensionPointListener<MultiHostInjector>() {
            @Override
            public void extensionAdded( MultiHostInjector injector,  PluginDescriptor pluginDescriptor) {
                clearInjectorCache();
            }

            @Override
            public void extensionRemoved( MultiHostInjector injector,  PluginDescriptor pluginDescriptor) {
                clearInjectorCache();
            }
        }, false, this);
        final ExtensionPointListener<LanguageInjector> myListener = new ExtensionPointListener<LanguageInjector>() {
            @Override
            public void extensionAdded( LanguageInjector extension,  PluginDescriptor pluginDescriptor) {
                clearInjectorCache();
            }

            @Override
            public void extensionRemoved( LanguageInjector extension,  PluginDescriptor pluginDescriptor) {
                clearInjectorCache();
            }
        };
        final ExtensionPoint<LanguageInjector> psiManagerPoint = Extensions.getRootArea().getExtensionPoint(LanguageInjector.EXTENSION_POINT_NAME);
        ((ExtensionPointImpl<LanguageInjector>)psiManagerPoint).addExtensionPointListener(myListener, false, this);
        myProgress = new DaemonProgressIndicator();
        project.getMessageBus().connect(this).subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new DaemonCodeAnalyzer.DaemonListenerAdapter() {
            @Override
            public void daemonCancelEventOccurred() {
                myProgress.cancel();
            }
        });
    }

    @Override
    public void dispose() {
        myProgress.cancel();
        EditorWindowImpl.disposeInvalidEditors();
    }

    @Override
    public void startRunInjectors( final Document hostDocument, final boolean synchronously) {
        if (myProject.isDisposed()) return;
        if (!synchronously && ApplicationManager.getApplication().isWriteAccessAllowed()) return;
        // use cached to avoid recreate PSI in alien project
        final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
        final PsiFile hostPsiFile = documentManager.getCachedPsiFile(hostDocument);
        if (hostPsiFile == null) return;

        final ConcurrentList<DocumentWindow> injected = InjectedLanguageUtil.getCachedInjectedDocuments(hostPsiFile);
        if (injected.isEmpty()) return;

        if (myProgress.isCanceled()) {
            myProgress = new DaemonProgressIndicator();
        }
        final Set<DocumentWindow> newDocuments = Collections.synchronizedSet(new THashSet());
        final Processor<DocumentWindow> commitProcessor = new Processor<DocumentWindow>() {
            @Override
            public boolean process(DocumentWindow documentWindow) {
                if (myProject.isDisposed()) return false;
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                if (indicator != null && indicator.isCanceled()) return false;
                if (documentManager.isUncommited(hostDocument) || !hostPsiFile.isValid()) return false; // will be committed later

                // it is here where the reparse happens and old file contents replaced
                InjectedLanguageUtil.enumerate(documentWindow, hostPsiFile, new PsiLanguageInjectionHost.InjectedPsiVisitor() {
                    @Override
                    public void visit( PsiFile injectedPsi,  List<PsiLanguageInjectionHost.Shred> places) {
                        DocumentWindow newDocument = (DocumentWindow)injectedPsi.getViewProvider().getDocument();
                        if (newDocument != null) {
                            PsiDocumentManagerBase.checkConsistency(injectedPsi, newDocument);
                            newDocuments.add(newDocument);
                        }
                    }
                });
                return true;
            }
        };
        final Runnable commitInjectionsRunnable = new Runnable() {
            @Override
            public void run() {
                if (myProgress.isCanceled()) return;
                JobLauncher.getInstance().invokeConcurrentlyUnderProgress(new ArrayList<DocumentWindow>(injected), myProgress, true, commitProcessor);

                synchronized (PsiLock.LOCK) {
                    injected.clear();
                    injected.addAll(newDocuments);
                }
            }
        };

        if (synchronously) {
            if (Thread.holdsLock(PsiLock.LOCK)) {
                // hack for the case when docCommit was called from within PSI modification, e.g. in formatter.
                // we can't spawn threads to do injections there, otherwise a deadlock is imminent
                ContainerUtil.process(new ArrayList<DocumentWindow>(injected), commitProcessor);
            }
            else {
                commitInjectionsRunnable.run();
            }
        }
        else {
            JobLauncher.getInstance().submitToJobThread(new Runnable() {
                @Override
                public void run() {
                    ApplicationManagerEx.getApplicationEx().tryRunReadAction(commitInjectionsRunnable);
                }
            }, null);
        }
    }

    @Override
    public PsiLanguageInjectionHost getInjectionHost( PsiElement element) {
        final PsiFile file = element.getContainingFile();
        final VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        if (virtualFile instanceof VirtualFileWindow) {
            PsiElement host = FileContextUtil.getFileContext(file); // use utility method in case the file's overridden getContext()
            if (host instanceof PsiLanguageInjectionHost) {
                return (PsiLanguageInjectionHost)host;
            }
        }
        return null;
    }

    @Override
    
    public TextRange injectedToHost( PsiElement injectedContext,  TextRange injectedTextRange) {
        PsiFile file = injectedContext.getContainingFile();
        if (file == null) return injectedTextRange;
        Document document = PsiDocumentManager.getInstance(file.getProject()).getCachedDocument(file);
        if (!(document instanceof DocumentWindowImpl)) return injectedTextRange;
        DocumentWindowImpl documentWindow = (DocumentWindowImpl)document;
        return documentWindow.injectedToHost(injectedTextRange);
    }
    @Override
    public int injectedToHost( PsiElement element, int offset) {
        PsiFile file = element.getContainingFile();
        if (file == null) return offset;
        Document document = PsiDocumentManager.getInstance(file.getProject()).getCachedDocument(file);
        if (!(document instanceof DocumentWindowImpl)) return offset;
        DocumentWindowImpl documentWindow = (DocumentWindowImpl)document;
        return documentWindow.injectedToHost(offset);
    }

    // used only from tests => no need for complex synchronization
    private final Set<MultiHostInjector> myManualInjectors = Collections.synchronizedSet(new LinkedHashSet<MultiHostInjector>());
    private volatile ClassMapCachingNulls<MultiHostInjector> cachedInjectors;

    public void processInjectableElements(Collection<PsiElement> in, Processor<PsiElement> processor) {
        ClassMapCachingNulls<MultiHostInjector> map = getInjectorMap();
        for (PsiElement element : in) {
            if (map.get(element.getClass()) != null)
                processor.process(element);
        }
    }

    private ClassMapCachingNulls<MultiHostInjector> getInjectorMap() {
        ClassMapCachingNulls<MultiHostInjector> cached = cachedInjectors;
        if (cached != null) {
            return cached;
        }

        Map<Class, MultiHostInjector[]> injectors = ContainerUtil.newHashMap();

        List<MultiHostInjector> allInjectors = ContainerUtil.newArrayList();
        allInjectors.addAll(myManualInjectors);
        Collections.addAll(allInjectors, MultiHostInjector.MULTIHOST_INJECTOR_EP_NAME.getExtensions(myProject));
        if (LanguageInjector.EXTENSION_POINT_NAME.getExtensions().length > 0) {
            allInjectors.add(PsiManagerRegisteredInjectorsAdapter.INSTANCE);
        }

        for (MultiHostInjector injector : allInjectors) {
            for (Class<? extends PsiElement> place : injector.elementsToInjectIn()) {
                LOG.assertTrue(place != null, injector);
                MultiHostInjector[] existing = injectors.get(place);
                injectors.put(place, existing == null ? new MultiHostInjector[]{injector} : ArrayUtil.append(existing, injector));
            }
        }

        ClassMapCachingNulls<MultiHostInjector> result = new ClassMapCachingNulls<MultiHostInjector>(injectors, new MultiHostInjector[0]);
        cachedInjectors = result;
        return result;
    }

    private void clearInjectorCache() {
        cachedInjectors = null;
    }

    @Override
    public void registerMultiHostInjector( MultiHostInjector injector) {
        myManualInjectors.add(injector);
        clearInjectorCache();
    }

    @Override
    public boolean unregisterMultiHostInjector( MultiHostInjector injector) {
        try {
            return myManualInjectors.remove(injector);
        }
        finally {
            clearInjectorCache();
        }
    }


    @Override
    public String
    getUnescapedText( final PsiElement injectedNode) {
        final StringBuilder text = new StringBuilder(injectedNode.getTextLength());
        // gather text from (patched) leaves
        injectedNode.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                String leafText = InjectedLanguageUtil.getUnescapedLeafText(element, false);
                if (leafText != null) {
                    text.append(leafText);
                    return;
                }
                super.visitElement(element);
            }
        });
        return text.toString();
    }

    /**
     *  intersection may spread over several injected fragments
     *  @param rangeToEdit range in encoded(raw) PSI
     *  @return list of ranges in encoded (raw) PSI
     */
    @Override
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    
    public List<TextRange> intersectWithAllEditableFragments( PsiFile injectedPsi,  TextRange rangeToEdit) {
        Place shreds = InjectedLanguageUtil.getShreds(injectedPsi);
        if (shreds == null) return Collections.emptyList();
        Object result = null; // optimization: TextRange or ArrayList
        int count = 0;
        int offset = 0;
        for (PsiLanguageInjectionHost.Shred shred : shreds) {
            TextRange encodedRange = TextRange.from(offset + shred.getPrefix().length(), shred.getRangeInsideHost().getLength());
            TextRange intersection = encodedRange.intersection(rangeToEdit);
            if (intersection != null) {
                count++;
                if (count == 1) {
                    result = intersection;
                }
                else if (count == 2) {
                    TextRange range = (TextRange)result;
                    if (range.isEmpty()) {
                        result = intersection;
                        count = 1;
                    }
                    else if (intersection.isEmpty()) {
                        count = 1;
                    }
                    else {
                        List<TextRange> list = new ArrayList<TextRange>();
                        list.add(range);
                        list.add(intersection);
                        result = list;
                    }
                }
                else if (intersection.isEmpty()) {
                    count--;
                }
                else {
                    ((List<TextRange>)result).add(intersection);
                }
            }
            offset += shred.getPrefix().length() + shred.getRangeInsideHost().getLength() + shred.getSuffix().length();
        }
        return count == 0 ? Collections.<TextRange>emptyList() : count == 1 ? Collections.singletonList((TextRange)result) : (List<TextRange>)result;
    }

    @Override
    public boolean isInjectedFragment( final PsiFile file) {
        return file.getViewProvider() instanceof InjectedFileViewProvider;
    }

    @Override
    public PsiElement findInjectedElementAt( PsiFile hostFile, int hostDocumentOffset) {
        return InjectedLanguageUtil.findInjectedElementNoCommit(hostFile, hostDocumentOffset);
    }

    @Override
    public void dropFileCaches( PsiFile file) {
        InjectedLanguageUtil.clearCachedInjectedFragmentsForFile(file);
    }

    @Override
    public PsiFile getTopLevelFile( PsiElement element) {
        return InjectedLanguageUtil.getTopLevelFile(element);
    }

    
    @Override
    public List<DocumentWindow> getCachedInjectedDocuments( PsiFile hostPsiFile) {
        return InjectedLanguageUtil.getCachedInjectedDocuments(hostPsiFile);
    }

    @Override
    public void enumerate( PsiElement host,  PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
        InjectedLanguageUtil.enumerate(host, visitor);
    }

    @Override
    public void enumerateEx( PsiElement host,
                             PsiFile containingFile,
                            boolean probeUp,
                             PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
        InjectedLanguageUtil.enumerate(host, containingFile, probeUp, visitor);
    }

    private final Map<Class,MultiHostInjector[]> myInjectorsClone = new HashMap<Class, MultiHostInjector[]>();
    
    public static void pushInjectors( Project project) {
        InjectedLanguageManagerImpl cachedManager = (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
        if (cachedManager == null) return;
        try {
            assert cachedManager.myInjectorsClone.isEmpty() : cachedManager.myInjectorsClone;
        }
        finally {
            cachedManager.myInjectorsClone.clear();
        }
        cachedManager.myInjectorsClone.putAll(cachedManager.getInjectorMap().getBackingMap());
    }
    
    public static void checkInjectorsAreDisposed( Project project) {
        InjectedLanguageManagerImpl cachedManager = (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
        if (cachedManager == null) {
            return;
        }
        try {
            ClassMapCachingNulls<MultiHostInjector> cached = cachedManager.cachedInjectors;
            if (cached == null) return;
            for (Map.Entry<Class, MultiHostInjector[]> entry : cached.getBackingMap().entrySet()) {
                Class key = entry.getKey();
                if (cachedManager.myInjectorsClone.isEmpty()) return;
                MultiHostInjector[] oldInjectors = cachedManager.myInjectorsClone.get(key);
                for (MultiHostInjector injector : entry.getValue()) {
                    if (!ArrayUtil.contains(injector, oldInjectors)) {
                        throw new AssertionError("Injector was not disposed: " + key + " -> " + injector);
                    }
                }
            }
        }
        finally {
            cachedManager.myInjectorsClone.clear();
        }
    }

    public interface InjProcessor {
        boolean process(PsiElement element, MultiHostInjector injector);
    }
    public void processInPlaceInjectorsFor( PsiElement element,  InjProcessor processor) {
        MultiHostInjector[] infos = getInjectorMap().get(element.getClass());
        if (infos != null) {
            final boolean dumb = myDumbService.isDumb();
            for (MultiHostInjector injector : infos) {
                if (dumb && !DumbService.isDumbAware(injector)) {
                    continue;
                }

                if (!processor.process(element, injector)) return;
            }
        }
    }

    @Override
    
    public List<Pair<PsiElement, TextRange>> getInjectedPsiFiles( final PsiElement host) {
        if (!(host instanceof PsiLanguageInjectionHost) || !((PsiLanguageInjectionHost) host).isValidHost()) {
            return null;
        }
        final PsiElement inTree = InjectedLanguageUtil.loadTree(host, host.getContainingFile());
        final List<Pair<PsiElement, TextRange>> result = new SmartList<Pair<PsiElement, TextRange>>();
        InjectedLanguageUtil.enumerate(inTree, new PsiLanguageInjectionHost.InjectedPsiVisitor() {
            @Override
            public void visit( PsiFile injectedPsi,  List<PsiLanguageInjectionHost.Shred> places) {
                for (PsiLanguageInjectionHost.Shred place : places) {
                    if (place.getHost() == inTree) {
                        result.add(new Pair<PsiElement, TextRange>(injectedPsi, place.getRangeInsideHost()));
                    }
                }
            }
        });
        return result.isEmpty() ? null : result;
    }

    private static class PsiManagerRegisteredInjectorsAdapter implements MultiHostInjector {
        public static final PsiManagerRegisteredInjectorsAdapter INSTANCE = new PsiManagerRegisteredInjectorsAdapter();
        @Override
        public void getLanguagesToInject( final MultiHostRegistrar injectionPlacesRegistrar,  PsiElement context) {
            final PsiLanguageInjectionHost host = (PsiLanguageInjectionHost)context;
            InjectedLanguagePlaces placesRegistrar = new InjectedLanguagePlaces() {
                @Override
                public void addPlace( Language language,  TextRange rangeInsideHost,   String prefix,   String suffix) {
                    injectionPlacesRegistrar
                            .startInjecting(language)
                            .addPlace(prefix, suffix, host, rangeInsideHost)
                            .doneInjecting();
                }
            };
            for (LanguageInjector injector : Extensions.getExtensions(LanguageInjector.EXTENSION_POINT_NAME)) {
                injector.getLanguagesToInject(host, placesRegistrar);
            }
        }

        @Override
        
        public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
            return Arrays.asList(PsiLanguageInjectionHost.class);
        }
    }
}
