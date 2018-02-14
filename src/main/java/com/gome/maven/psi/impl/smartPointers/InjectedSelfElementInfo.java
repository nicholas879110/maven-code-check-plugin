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
package com.gome.maven.psi.impl.smartPointers;

import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.ProperTextRange;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.FreeThreadedFileViewProvider;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

/**
 * User: cdr
 */
class InjectedSelfElementInfo implements SmartPointerElementInfo {
    private final SmartPsiFileRange myInjectedFileRangeInHostFile;
    private final Class<? extends PsiElement> anchorClass;
    private final Language anchorLanguage;
    
    private final SmartPsiElementPointer<PsiLanguageInjectionHost> myHostContext;

    InjectedSelfElementInfo( Project project,
                             PsiElement injectedElement,
                             TextRange injectedRange,
                             PsiFile containingFile,
                             SmartPsiElementPointer<PsiLanguageInjectionHost> hostContext) {
        myHostContext = hostContext;
        assert containingFile.getViewProvider() instanceof FreeThreadedFileViewProvider : "element parameter must be an injected element: "+injectedElement+"; "+containingFile;
        assert containingFile.getTextRange().contains(injectedRange) : "Injected range outside the file: "+injectedRange +"; file: "+containingFile.getTextRange();

        TextRange hostRange = InjectedLanguageManager.getInstance(project).injectedToHost(injectedElement, injectedRange);
        PsiFile hostFile = hostContext.getContainingFile();
        assert !(hostFile.getViewProvider() instanceof FreeThreadedFileViewProvider) : "hostContext parameter must not be and injected element: "+hostContext;
        SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(project);
        myInjectedFileRangeInHostFile = smartPointerManager.createSmartPsiFileRangePointer(hostFile, hostRange);
        anchorLanguage = containingFile.getLanguage();
        anchorClass = injectedElement.getClass(); //containingFile.findElementAt(injectedRange.getStartOffset()).getClass();
    }

    @Override
    public VirtualFile getVirtualFile() {
        PsiElement element = restoreElement();
        if (element == null) return null;
        return element.getContainingFile().getVirtualFile();
    }

    @Override
    public Segment getRange() {
        return getInjectedRange();
    }

    @Override
    public PsiElement restoreElement() {
        PsiFile hostFile = myHostContext.getContainingFile();
        if (hostFile == null || !hostFile.isValid()) return null;

        PsiElement hostContext = myHostContext.getElement();
        if (hostContext == null) return null;

        Segment segment = myInjectedFileRangeInHostFile.getRange();
        if (segment == null) return null;
        final TextRange rangeInHostFile = TextRange.create(segment);

        PsiElement result = null;
        PsiFile injectedPsi = getInjectedFileIn(hostContext, hostFile, rangeInHostFile);
        if (injectedPsi != null) {
            Document document = PsiDocumentManager.getInstance(getProject()).getDocument(injectedPsi);
            int start = ((DocumentWindow)document).hostToInjected(rangeInHostFile.getStartOffset());
            int end = ((DocumentWindow)document).hostToInjected(rangeInHostFile.getEndOffset());
            result = SelfElementInfo.findElementInside(injectedPsi, start, end, anchorClass, anchorLanguage);
        }

        return result;
    }

    private PsiFile getInjectedFileIn( final PsiElement hostContext,
                                       final PsiFile hostFile,
                                       final TextRange rangeInHostFile) {
        final InjectedLanguageManager manager = InjectedLanguageManager.getInstance(getProject());
        final PsiFile[] result = {null};
        final PsiLanguageInjectionHost.InjectedPsiVisitor visitor = new PsiLanguageInjectionHost.InjectedPsiVisitor() {
            @Override
            public void visit( PsiFile injectedPsi,  List<PsiLanguageInjectionHost.Shred> places) {
                TextRange hostRange = manager.injectedToHost(injectedPsi, new TextRange(0, injectedPsi.getTextLength()));
                Document document = PsiDocumentManager.getInstance(getProject()).getDocument(injectedPsi);
                if (hostRange.contains(rangeInHostFile) && document instanceof DocumentWindow) {
                    result[0] = injectedPsi;
                }
            }
        };

        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getProject());
        Document document = documentManager.getDocument(hostFile);
        if (document != null && documentManager.isUncommited(document)) {
            for (DocumentWindow documentWindow : InjectedLanguageManager.getInstance(getProject()).getCachedInjectedDocuments(hostFile)) {
                PsiFile injected = documentManager.getPsiFile(documentWindow);
                if (injected != null) {
                    visitor.visit(injected, Collections.<PsiLanguageInjectionHost.Shred>emptyList());
                }
            }
        }
        else {
            List<Pair<PsiElement,TextRange>> injected = InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(hostContext);
            if (injected != null) {
                for (Pair<PsiElement, TextRange> pair : injected) {
                    PsiFile injectedFile = pair.first.getContainingFile();
                    visitor.visit(injectedFile, ContainerUtil.<PsiLanguageInjectionHost.Shred>emptyList());
                }
            }
        }

        return result[0];
    }

    @Override
    public boolean pointsToTheSameElementAs( SmartPointerElementInfo other) {
        if (getClass() != other.getClass()) return false;
        if (!(((InjectedSelfElementInfo)other).myHostContext).equals(myHostContext)) return false;
        SmartPointerElementInfo myElementInfo = ((SmartPsiElementPointerImpl)myInjectedFileRangeInHostFile).getElementInfo();
        SmartPointerElementInfo oElementInfo = ((SmartPsiElementPointerImpl)((InjectedSelfElementInfo)other).myInjectedFileRangeInHostFile).getElementInfo();
        return myElementInfo.pointsToTheSameElementAs(oElementInfo);
    }

    @Override
    public PsiFile restoreFile() {
        PsiFile hostFile = myHostContext.getContainingFile();
        if (hostFile == null || !hostFile.isValid()) return null;

        PsiElement hostContext = myHostContext.getElement();
        if (hostContext == null) return null;

        Segment segment = myInjectedFileRangeInHostFile.getRange();
        if (segment == null) return null;
        final TextRange rangeInHostFile = TextRange.create(segment);
        return getInjectedFileIn(hostContext, hostFile, rangeInHostFile);
    }

    private ProperTextRange getInjectedRange() {
        PsiFile hostFile = myHostContext.getContainingFile();
        if (hostFile == null || !hostFile.isValid()) return null;

        PsiElement hostContext = myHostContext.getElement();
        if (hostContext == null) return null;

        Segment hostElementRange = myInjectedFileRangeInHostFile.getRange();
        if (hostElementRange == null) return null;

        PsiFile injectedFile = restoreFile();
        if (injectedFile == null) return null;
        VirtualFile virtualFile = injectedFile.getVirtualFile();
        DocumentWindow documentWindow = virtualFile instanceof VirtualFileWindow ?  ((VirtualFileWindow)virtualFile).getDocumentWindow() : null;
        if (documentWindow==null) return null;
        int start = documentWindow.hostToInjected(hostElementRange.getStartOffset());
        int end = documentWindow.hostToInjected(hostElementRange.getEndOffset());
        return ProperTextRange.create(start, end);
    }

    @Override
    public void cleanup() {
        SmartPointerManager.getInstance(getProject()).removePointer(myInjectedFileRangeInHostFile);
    }

    @Override
    public Document getDocumentToSynchronize() {
        return ((SmartPsiElementPointerImpl)myHostContext).getElementInfo().getDocumentToSynchronize();
    }

    @Override
    public void fastenBelt(int offset, RangeMarker[] cachedRangeMarkers) {

    }

    @Override
    public void unfastenBelt(int offset) {

    }

    @Override
    public int elementHashCode() {
        return ((SmartPsiElementPointerImpl)myHostContext).getElementInfo().elementHashCode();
    }

    
    @Override
    public Project getProject() {
        return myHostContext.getProject();
    }
}
