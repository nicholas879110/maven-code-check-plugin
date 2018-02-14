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

import com.gome.maven.lang.LanguageUtil;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.ProperTextRange;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.FreeThreadedFileViewProvider;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

class SmartPsiElementPointerImpl<E extends PsiElement> implements SmartPointerEx<E> {
    private Reference<E> myElement;
    private final SmartPointerElementInfo myElementInfo;
    private final Class<? extends PsiElement> myElementClass;
    private byte myReferenceCount;

    SmartPsiElementPointerImpl( Project project,  E element,  PsiFile containingFile) {
        this(element, createElementInfo(project, element, containingFile), element.getClass());
    }
    SmartPsiElementPointerImpl( E element,
                                SmartPointerElementInfo elementInfo,
                                Class<? extends PsiElement> elementClass) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        cacheElement(element);
        myElementClass = elementClass;
        myElementInfo = elementInfo;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SmartPsiElementPointer && pointsToTheSameElementAs(this, (SmartPsiElementPointer)obj);
    }

    @Override
    public int hashCode() {
        return myElementInfo.elementHashCode();
    }

    @Override
    
    public Project getProject() {
        return myElementInfo.getProject();
    }

    @Override
    
    public E getElement() {
        E element = getCachedElement();
        if (element != null && !element.isValid()) {
            element = null;
        }
        if (element == null) {
            //noinspection unchecked
            element = (E)myElementInfo.restoreElement();
            if (element != null && (!element.getClass().equals(myElementClass) || !element.isValid())) {
                element = null;
            }

            cacheElement(element);
        }

        return element;
    }

    private void cacheElement(E element) {
        myElement = element == null ? null : new SoftReference<E>(element);
    }

    @Override
    public E getCachedElement() {
        return com.gome.maven.reference.SoftReference.dereference(myElement);
    }

    @Override
    public PsiFile getContainingFile() {
        PsiFile file = getElementInfo().restoreFile();

        if (file != null) {
            return file;
        }

        final Document doc = myElementInfo.getDocumentToSynchronize();
        if (doc == null) {
            final E resolved = getElement();
            return resolved == null ? null : resolved.getContainingFile();
        }
        return PsiDocumentManager.getInstance(getProject()).getPsiFile(doc);
    }

    @Override
    public VirtualFile getVirtualFile() {
        return myElementInfo.getVirtualFile();
    }

    @Override
    public Segment getRange() {
        return myElementInfo.getRange();
    }

    
    private static <E extends PsiElement> SmartPointerElementInfo createElementInfo( Project project,
                                                                                     E element,
                                                                                    PsiFile containingFile) {
        if (element instanceof PsiDirectory) {
            return new DirElementInfo((PsiDirectory)element);
        }
        if (element instanceof PsiCompiledElement || containingFile == null || !containingFile.isPhysical() || !element.isPhysical()) {
            if (element instanceof StubBasedPsiElement && element instanceof PsiCompiledElement) {
                if (element instanceof PsiFile) {
                    return new FileElementInfo((PsiFile)element);
                }
                PsiAnchor.StubIndexReference stubReference = PsiAnchor.createStubReference(element, containingFile);
                if (stubReference != null) {
                    return new ClsElementInfo(stubReference);
                }
            }
            return new HardElementInfo(project, element);
        }

        FileViewProvider viewProvider = containingFile.getViewProvider();
        if (viewProvider instanceof FreeThreadedFileViewProvider) {
            PsiLanguageInjectionHost hostContext = InjectedLanguageManager.getInstance(containingFile.getProject()).getInjectionHost(containingFile);
            TextRange elementRange = element.getTextRange();
            if (hostContext != null && elementRange != null) {
                SmartPsiElementPointer<PsiLanguageInjectionHost> hostPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(hostContext);
                return new InjectedSelfElementInfo(project, element, elementRange, containingFile, hostPointer);
            }
        }

        for(SmartPointerElementInfoFactory factory: Extensions.getExtensions(SmartPointerElementInfoFactory.EP_NAME)) {
            final SmartPointerElementInfo result = factory.createElementInfo(element);
            if (result != null) return result;
        }

        if (element instanceof PsiFile) {
            return new FileElementInfo((PsiFile)element);
        }

        TextRange elementRange = element.getTextRange();
        if (elementRange == null) {
            return new HardElementInfo(project, element);
        }
        ProperTextRange proper = ProperTextRange.create(elementRange);

        return new SelfElementInfo(project, proper, element.getClass(), containingFile, LanguageUtil.getRootLanguage(element));
    }

    @Override
    public void unfastenBelt(int offset) {
        myElementInfo.unfastenBelt(offset);
    }

    @Override
    public void fastenBelt(int offset,  RangeMarker[] cachedRangeMarkers) {
        myElementInfo.fastenBelt(offset, cachedRangeMarkers);
    }

    
    public SmartPointerElementInfo getElementInfo() {
        return myElementInfo;
    }

    static boolean pointsToTheSameElementAs( SmartPsiElementPointer pointer1,  SmartPsiElementPointer pointer2) {
        if (pointer1 == pointer2) return true;
        if (pointer1 instanceof SmartPsiElementPointerImpl && pointer2 instanceof SmartPsiElementPointerImpl) {
            SmartPsiElementPointerImpl impl1 = (SmartPsiElementPointerImpl)pointer1;
            SmartPsiElementPointerImpl impl2 = (SmartPsiElementPointerImpl)pointer2;
            SmartPointerElementInfo elementInfo1 = impl1.getElementInfo();
            SmartPointerElementInfo elementInfo2 = impl2.getElementInfo();
            if (!elementInfo1.pointsToTheSameElementAs(elementInfo2)) return false;
            PsiElement cachedElement1 = impl1.getCachedElement();
            PsiElement cachedElement2 = impl2.getCachedElement();
            return cachedElement1 == null || cachedElement2 == null || Comparing.equal(cachedElement1, cachedElement2);
        }
        return Comparing.equal(pointer1.getElement(), pointer2.getElement());
    }

    int incrementAndGetReferenceCount(int delta) {
        if (myReferenceCount == Byte.MAX_VALUE) return Byte.MAX_VALUE; // saturated
        return myReferenceCount += delta;
    }
}
