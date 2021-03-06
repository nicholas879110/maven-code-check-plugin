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

import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.injected.editor.DocumentWindowImpl;
import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.FreeThreadedFileViewProvider;
import com.gome.maven.psi.impl.source.tree.MarkersHolderFileViewProvider;
import com.gome.maven.util.SmartList;

import java.util.List;

/**
 * @author cdr
 */
public class InjectedFileViewProvider extends SingleRootFileViewProvider implements FreeThreadedFileViewProvider,
        MarkersHolderFileViewProvider {
    private Project myProject;
    private final Object myLock = new Object();
    private final DocumentWindowImpl myDocumentWindow;
    private static final ThreadLocal<Boolean> disabledTemporarily = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private boolean myPatchingLeaves;

    InjectedFileViewProvider( PsiManager psiManager,
                              VirtualFileWindow virtualFile,
                              DocumentWindowImpl documentWindow,
                              Language language) {
        super(psiManager, (VirtualFile)virtualFile, true, language);
        myDocumentWindow = documentWindow;
        myProject = documentWindow.getShreds().getHostPointer().getProject();
    }

    @Override
    public void rootChanged( PsiFile psiFile) {
        super.rootChanged(psiFile);
        if (!isPhysical()) return; // injected PSI change happened inside reparse; ignore
        if (myPatchingLeaves) return;

        DocumentWindowImpl documentWindow = myDocumentWindow;
        List<PsiLanguageInjectionHost.Shred> shreds = documentWindow.getShreds();
        assert documentWindow.getHostRanges().length == shreds.size();
        String[] changes = documentWindow.calculateMinEditSequence(psiFile.getNode().getText());
        assert changes.length == shreds.size();
        for (int i = 0; i < changes.length; i++) {
            String change = changes[i];
            if (change != null) {
                PsiLanguageInjectionHost.Shred shred = shreds.get(i);
                PsiLanguageInjectionHost host = shred.getHost();
                TextRange rangeInsideHost = shred.getRangeInsideHost();
                String newHostText = StringUtil.replaceSubstring(host.getText(), rangeInsideHost, change);
                //shred.host =
                host.updateText(newHostText);
            }
        }
    }

    @Override
    public FileViewProvider clone() {
        final DocumentWindow oldDocumentWindow = ((VirtualFileWindow)getVirtualFile()).getDocumentWindow();
        Document hostDocument = oldDocumentWindow.getDelegate();
        final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getManager().getProject());
        PsiFile hostFile = documentManager.getPsiFile(hostDocument);
        Language language = getBaseLanguage();
        PsiFile file = getPsi(language);
        final Language hostFileLanguage = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file).getLanguage();
        PsiFile hostPsiFileCopy = (PsiFile)hostFile.copy();
        Segment firstTextRange = oldDocumentWindow.getHostRanges()[0];
        PsiElement hostElementCopy = hostPsiFileCopy.getViewProvider().findElementAt(firstTextRange.getStartOffset(), hostFileLanguage);
        assert hostElementCopy != null;
        final Ref<FileViewProvider> provider = new Ref<FileViewProvider>();
        PsiLanguageInjectionHost.InjectedPsiVisitor visitor = new PsiLanguageInjectionHost.InjectedPsiVisitor() {
            @Override
            public void visit( PsiFile injectedPsi,  List<PsiLanguageInjectionHost.Shred> places) {
                Document document = documentManager.getCachedDocument(injectedPsi);
                if (document instanceof DocumentWindowImpl && oldDocumentWindow.areRangesEqual((DocumentWindowImpl)document)) {
                    provider.set(injectedPsi.getViewProvider());
                }
            }
        };
        for (PsiElement current = hostElementCopy; current != null && current != hostPsiFileCopy; current = current.getParent()) {
            current.putUserData(LANGUAGE_FOR_INJECTED_COPY_KEY, language);
            try {
                InjectedLanguageUtil.enumerate(current, hostPsiFileCopy, false, visitor);
            }
            finally {
                current.putUserData(LANGUAGE_FOR_INJECTED_COPY_KEY, null);
            }
            if (provider.get() != null) break;
        }
        return provider.get();
    }

    static Key<Language> LANGUAGE_FOR_INJECTED_COPY_KEY = Key.create("LANGUAGE_FOR_INJECTED_COPY_KEY");
    // returns true if shreds were set, false if old ones were reused
    boolean setShreds( Place newShreds,  Project project) {
        synchronized (myLock) {
            myProject = project;
            Place oldShreds = myDocumentWindow.getShreds();
            // try to reuse shreds, otherwise there are too many range markers disposals/re-creations
            if (same(oldShreds, newShreds)) {
                return false;
            }
            else {
                myDocumentWindow.setShreds(newShreds);
                return true;
            }
        }
    }

    private static boolean same(Place oldShreds, Place newShreds) {
        if (oldShreds == newShreds) return true;
        if (oldShreds.size() != newShreds.size()) return false;
        for (int i = 0; i < oldShreds.size(); i++) {
            PsiLanguageInjectionHost.Shred oldShred = oldShreds.get(i);
            PsiLanguageInjectionHost.Shred newShred = newShreds.get(i);
            if (!oldShred.equals(newShred)) return false;
        }
        return true;
    }

    boolean isValid() {
        return getShreds().isValid();
    }

    boolean isDisposed() {
        synchronized (myLock) {
            return myProject.isDisposed();
        }
    }

    Place getShreds() {
        return myDocumentWindow.getShreds();
    }

    @Override
    
    public DocumentWindow getDocument() {
        return myDocumentWindow;
    }

    @Override
    public boolean isEventSystemEnabled() {
        if (myLock == null) return true; // hack to avoid NPE when this method called from super class constructor
        return !disabledTemporarily.get();
    }

    @Override
    public boolean isPhysical() {
        return isEventSystemEnabled();
    }

    public void performNonPhysically(Runnable runnable) {
        synchronized (myLock) {
            disabledTemporarily.set(true);
            try {
                runnable.run();
            }
            finally {
                disabledTemporarily.set(false);
            }
        }
    }


    @Override
    public String toString() {
        return "Injected file '"+getVirtualFile().getName()+"' " + (isValid() ? "" : " invalid") + (isPhysical() ? "" : " nonphysical");
    }

    void setPatchingLeaves(boolean patchingLeaves) {
        myPatchingLeaves = patchingLeaves;
    }

    @Override
    
    public RangeMarker[] getCachedMarkers() {
        List<RangeMarker> markers = new SmartList<RangeMarker>();
        for (PsiLanguageInjectionHost.Shred shred : myDocumentWindow.getShreds()) {
            RangeMarker marker = (RangeMarker)shred.getHostRangeMarker();
            if (marker != null) {
                markers.add(marker);
            }
        }
        return markers.toArray(new RangeMarker[markers.size()]);
    }
}
