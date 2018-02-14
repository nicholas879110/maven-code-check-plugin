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

package com.gome.maven.psi.impl.smartPointers;

import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.source.tree.MarkersHolderFileViewProvider;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.containers.ContainerUtil;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SmartPointerManagerImpl extends SmartPointerManager {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.smartPointers.SmartPointerManagerImpl");
    private static final Object lock = new Object();
    private static final ReferenceQueue<SmartPointerEx> ourQueue = new ReferenceQueue<SmartPointerEx>();
    @SuppressWarnings("unused") private static final LowMemoryWatcher ourWatcher = LowMemoryWatcher.register(new Runnable() {
        @Override
        public void run() {
            processQueue();
        }
    });

    private final Project myProject;
    private final Key<Set<PointerReference>> POINTERS_KEY;
    private final Key<Boolean> POINTERS_ARE_FASTENED_KEY;

    public SmartPointerManagerImpl(Project project) {
        myProject = project;
        POINTERS_KEY = Key.create("SMART_POINTERS for "+project);
        POINTERS_ARE_FASTENED_KEY = Key.create("SMART_POINTERS_ARE_FASTENED for "+project);
    }

    private static void processQueue() {
        while (true) {
            PointerReference reference = (PointerReference)ourQueue.poll();
            if (reference == null) break;
            synchronized (lock) {
                Set<PointerReference> pointers = reference.file.getUserData(reference.key);
                if (pointers != null) {
                    pointers.remove(reference);
                    if (pointers.isEmpty()) {
                        reference.file.putUserData(reference.key, null);
                    }
                }
            }
        }
    }

    public void fastenBelts( VirtualFile file, int offset,  RangeMarker[] cachedRangeMarkers) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        processQueue();
        synchronized (lock) {
            List<SmartPointerEx> pointers = getStrongPointers(file);
            if (pointers.isEmpty()) return;

            if (getAndFasten(file)) return;

            for (SmartPointerEx pointer : pointers) {
                pointer.fastenBelt(offset, cachedRangeMarkers);
            }

            PsiFile psiFile = ((PsiManagerEx)PsiManager.getInstance(myProject)).getFileManager().getCachedPsiFile(file);
            if (psiFile != null) {
                PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(myProject);
                for (DocumentWindow injectedDoc : InjectedLanguageManager.getInstance(myProject).getCachedInjectedDocuments(psiFile)) {
                    PsiFile injectedFile = psiDocumentManager.getPsiFile(injectedDoc);
                    if (injectedFile == null) continue;
                    RangeMarker[] cachedMarkers = getCachedRangeMarkerToInjectedFragment(injectedFile);
                    boolean relevant = false;
                    for (Segment hostSegment : injectedDoc.getHostRanges()) {
                        if (offset <= hostSegment.getEndOffset()) {
                            relevant = true;
                            break;
                        }
                    }
                    if (relevant) {
                        fastenBelts(injectedFile.getViewProvider().getVirtualFile(), 0, cachedMarkers);
                    }
                }
            }
        }
    }

    
    private static RangeMarker[] getCachedRangeMarkerToInjectedFragment( PsiFile injectedFile) {
        MarkersHolderFileViewProvider provider = (MarkersHolderFileViewProvider)injectedFile.getViewProvider();
        return provider.getCachedMarkers();
    }

    public void unfastenBelts( VirtualFile file, int offset) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        processQueue();
        synchronized (lock) {
            List<SmartPointerEx> pointers = getStrongPointers(file);
            if (pointers.isEmpty()) return;

            if (!getAndUnfasten(file)) return;

            for (SmartPointerEx pointer : pointers) {
                pointer.unfastenBelt(offset);
            }

            PsiFile psiFile = ((PsiManagerEx)PsiManager.getInstance(myProject)).getFileManager().getCachedPsiFile(file);
            if (psiFile != null) {
                PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(myProject);
                for (DocumentWindow injectedDoc : InjectedLanguageManager.getInstance(myProject).getCachedInjectedDocuments(psiFile)) {
                    PsiFile injectedFile = psiDocumentManager.getPsiFile(injectedDoc);
                    if (injectedFile == null) continue;
                    unfastenBelts(injectedFile.getViewProvider().getVirtualFile(), 0);
                }
            }
        }
    }

    private static final Key<Reference<SmartPointerEx>> CACHED_SMART_POINTER_KEY = Key.create("CACHED_SMART_POINTER_KEY");
    @Override
    
    public <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer( E element) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        PsiFile containingFile = element.getContainingFile();
        return createSmartPsiElementPointer(element, containingFile);
    }
    @Override
    
    public <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer( E element, PsiFile containingFile) {
        if (containingFile != null && !containingFile.isValid() || containingFile == null && !element.isValid()) {
            PsiUtilCore.ensureValid(element);
            LOG.error("Invalid element:" + element);
        }
        processQueue();
        SmartPointerEx<E> pointer = getCachedPointer(element);
        if (pointer != null) {
            containingFile = containingFile == null ? element.getContainingFile() : containingFile;
            if (containingFile != null && areBeltsFastened(containingFile.getViewProvider().getVirtualFile())) {
                pointer.fastenBelt(0, null);
            }
        }
        else {
            pointer = new SmartPsiElementPointerImpl<E>(myProject, element, containingFile);
            if (containingFile != null) {
                initPointer(pointer, containingFile.getViewProvider().getVirtualFile());
            }
            element.putUserData(CACHED_SMART_POINTER_KEY, new SoftReference<SmartPointerEx>(pointer));
        }
        if (pointer instanceof SmartPsiElementPointerImpl) {
            synchronized (lock) {
                ((SmartPsiElementPointerImpl)pointer).incrementAndGetReferenceCount(1);
            }
        }
        return pointer;

    }

    private static <E extends PsiElement> SmartPointerEx<E> getCachedPointer( E element) {
        Reference<SmartPointerEx> data = element.getUserData(CACHED_SMART_POINTER_KEY);
        SmartPointerEx cachedPointer = SoftReference.dereference(data);
        if (cachedPointer != null) {
            PsiElement cachedElement = cachedPointer.getCachedElement();
            if (cachedElement != null && cachedElement != element) {
                return null;
            }
        }
        //noinspection unchecked
        return cachedPointer;
    }

    @Override
    
    public SmartPsiFileRange createSmartPsiFileRangePointer( PsiFile file,  TextRange range) {
        if (!file.isValid()) {
            LOG.error("Invalid element:" + file);
        }
        processQueue();
        SmartPsiFileRangePointerImpl pointer = new SmartPsiFileRangePointerImpl(file, ProperTextRange.create(range));
        initPointer(pointer, file.getViewProvider().getVirtualFile());

        return pointer;
    }

    private <E extends PsiElement> void initPointer( SmartPointerEx<E> pointer,  VirtualFile containingFile) {
        synchronized (lock) {
            Set<PointerReference> pointers = getPointers(containingFile);
            if (pointers == null) {
                pointers = ContainerUtil.newTroveSet(); // we synchronise access anyway
                containingFile.putUserData(POINTERS_KEY, pointers);
            }
            pointers.add(new PointerReference(pointer, containingFile, ourQueue, POINTERS_KEY));

            if (areBeltsFastened(containingFile)) {
                pointer.fastenBelt(0, null);
            }
        }
    }

    @Override
    public boolean removePointer( SmartPsiElementPointer pointer) {
        synchronized (lock) {
            if (pointer instanceof SmartPsiElementPointerImpl) {
                int refCount = ((SmartPsiElementPointerImpl)pointer).incrementAndGetReferenceCount(-1);
                if (refCount == 0) {
                    PsiElement element = ((SmartPointerEx)pointer).getCachedElement();
                    if (element != null) {
                        element.putUserData(CACHED_SMART_POINTER_KEY, null);
                    }
                    PsiFile containingFile = pointer.getContainingFile();
                    if (containingFile == null) return false;

                    VirtualFile vFile = containingFile.getViewProvider().getVirtualFile();
                    Set<PointerReference> pointers = getPointers(vFile);
                    if (pointers == null) return false;

                    SmartPointerElementInfo info = ((SmartPsiElementPointerImpl)pointer).getElementInfo();
                    info.cleanup();

                    for (Iterator<PointerReference> iterator = pointers.iterator(); iterator.hasNext(); ) {
                        if (pointer == iterator.next().get()) {
                            iterator.remove();
                            if (pointers.isEmpty()) {
                                vFile.putUserData(POINTERS_KEY, null);
                            }
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    
    private Set<PointerReference> getPointers( VirtualFile containingFile) {
        return containingFile.getUserData(POINTERS_KEY);
    }

    
    private List<SmartPointerEx> getStrongPointers( VirtualFile containingFile) {
        Set<PointerReference> refs = getPointers(containingFile);
        if (refs == null) return Collections.emptyList();

        List<SmartPointerEx> result = ContainerUtil.newArrayList();
        for (PointerReference reference : refs) {
            ContainerUtil.addIfNotNull(result, reference.get());
        }
        return result;
    }

    public int getPointersNumber( PsiFile containingFile) {
        synchronized (lock) {
            return getStrongPointers(containingFile.getViewProvider().getVirtualFile()).size();
        }
    }

    private boolean getAndFasten( VirtualFile file) {
        boolean fastened = areBeltsFastened(file);
        file.putUserData(POINTERS_ARE_FASTENED_KEY, Boolean.TRUE);
        return fastened;
    }
    private boolean getAndUnfasten( VirtualFile file) {
        boolean fastened = areBeltsFastened(file);
        file.putUserData(POINTERS_ARE_FASTENED_KEY, null);
        return fastened;
    }
    private boolean areBeltsFastened(VirtualFile file) {
        return file.getUserData(POINTERS_ARE_FASTENED_KEY) == Boolean.TRUE;
    }

    @Override
    public boolean pointToTheSameElement( SmartPsiElementPointer pointer1,  SmartPsiElementPointer pointer2) {
        return SmartPsiElementPointerImpl.pointsToTheSameElementAs(pointer1, pointer2);
    }

    private static class PointerReference extends WeakReference<SmartPointerEx> {
        private final VirtualFile file;
        private final Key<Set<PointerReference>> key;

        public PointerReference(SmartPointerEx<?> pointer,
                                VirtualFile containingFile,
                                ReferenceQueue<SmartPointerEx> queue,
                                Key<Set<PointerReference>> key) {
            super(pointer, queue);
            file = containingFile;
            this.key = key;
        }
    }

}
