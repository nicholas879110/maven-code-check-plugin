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
package com.gome.maven.usageView;

import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ProperTextRange;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;

public class UsageInfo {
    public static final UsageInfo[] EMPTY_ARRAY = new UsageInfo[0];
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.usageView.UsageInfo");
    private final SmartPsiElementPointer<?> mySmartPointer;
    private final SmartPsiFileRange myPsiFileRange;

    public final boolean isNonCodeUsage;
    protected boolean myDynamicUsage = false;

    public UsageInfo( PsiElement element, int startOffset, int endOffset, boolean isNonCodeUsage) {
        element = element.getNavigationElement();
        PsiFile file = element.getContainingFile();
        PsiElement topElement = file == null ? element : file;
        LOG.assertTrue(topElement.isValid(), element);

        TextRange elementRange = element.getTextRange();
        if (elementRange == null) {
            throw new IllegalArgumentException("text range null for " + element + "; " + element.getClass());
        }
        if (startOffset == -1 && endOffset == -1) {
            // calculate natural element range
            startOffset = element.getTextOffset() - elementRange.getStartOffset();
            endOffset = elementRange.getEndOffset() - elementRange.getStartOffset();
        }

        if (startOffset < 0) {
            throw new IllegalArgumentException("element " + element + "; startOffset " +startOffset);
        }
        if (startOffset > endOffset) {
            throw new IllegalArgumentException("element " + element + "; diff " + (endOffset-startOffset));
        }

        Project project = topElement.getProject();
        SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(project);
        mySmartPointer = smartPointerManager.createSmartPsiElementPointer(element, file);
        if (startOffset != element.getTextOffset() - elementRange.getStartOffset() || endOffset != elementRange.getLength()) {
            TextRange rangeToStore;
            if (file != null && InjectedLanguageManager.getInstance(project).isInjectedFragment(file)) {
                rangeToStore = elementRange;
            }
            else {
                rangeToStore = TextRange.create(startOffset, endOffset).shiftRight(elementRange.getStartOffset());
            }
            myPsiFileRange = smartPointerManager.createSmartPsiFileRangePointer(file, rangeToStore);
        }
        else {
            myPsiFileRange = null;
        }
        this.isNonCodeUsage = isNonCodeUsage;
    }

    public UsageInfo( SmartPsiElementPointer<?> smartPointer,
                     SmartPsiFileRange psiFileRange,
                     boolean dynamicUsage,
                     boolean nonCodeUsage) {
        myDynamicUsage = dynamicUsage;
        isNonCodeUsage = nonCodeUsage;
        myPsiFileRange = psiFileRange;
        mySmartPointer = smartPointer;
    }

    
    public SmartPsiElementPointer<?> getSmartPointer() {
        return mySmartPointer;
    }

    public SmartPsiFileRange getPsiFileRange() {
        return myPsiFileRange;
    }

    public boolean isNonCodeUsage() {
        return isNonCodeUsage;
    }

    public void setDynamicUsage(boolean dynamicUsage) {
        myDynamicUsage = dynamicUsage;
    }

    public UsageInfo( PsiElement element, boolean isNonCodeUsage) {
        this(element, -1, -1, isNonCodeUsage);
    }

    public UsageInfo( PsiElement element, int startOffset, int endOffset) {
        this(element, startOffset, endOffset, false);
    }

    public UsageInfo( PsiReference reference) {
        this(reference.getElement(), reference.getRangeInElement().getStartOffset(), reference.getRangeInElement().getEndOffset());
        myDynamicUsage = reference.resolve() == null;
    }

    public UsageInfo( PsiQualifiedReferenceElement reference) {
        this((PsiElement)reference);
    }

    public UsageInfo( PsiElement element) {
        this(element, false);
    }

    
    public PsiElement getElement() { // SmartPointer is used to fix SCR #4572, hotya eto krivo i nado vse perepisat'
        return mySmartPointer.getElement();
    }

    
    public PsiReference getReference() {
        PsiElement element = getElement();
        return element == null ? null : element.getReference();
    }

    /**
     * @deprecated for the range in element use {@link #getRangeInElement} instead,
     *             for the whole text range in the file covered by this usage info, use {@link #getSegment()}
     */
    public TextRange getRange() {
        return getRangeInElement();
    }

    /**
     * @return range in element
     */
    public ProperTextRange getRangeInElement() {
        PsiElement element = getElement();
        if (element == null) return null;
        TextRange elementRange = element.getTextRange();
        ProperTextRange result;
        if (myPsiFileRange == null) {
            int startOffset = element.getTextOffset();
            result = ProperTextRange.create(startOffset, elementRange.getEndOffset());
        }
        else {
            Segment rangeInFile = myPsiFileRange.getRange();
            if (rangeInFile == null) return null;
            result = ProperTextRange.create(rangeInFile);
        }
        int delta = elementRange.getStartOffset();
        return result.getStartOffset() < delta ? null : result.shiftRight(-delta);
    }

    /**
     * Override this method if you want a tooltip to be displayed for this usage
     */
    public String getTooltipText() {
        return null;
    }

    public int getNavigationOffset() {
        if (myPsiFileRange  != null) {
            final Segment range = myPsiFileRange.getRange();
            if (range != null) {
                return range.getStartOffset();
            }
        }

        PsiElement element = getElement();
        if (element == null) return -1;
        TextRange range = element.getTextRange();

        TextRange rangeInElement = getRangeInElement();
        if (rangeInElement == null) return -1;
        return range.getStartOffset() + rangeInElement.getStartOffset();
    }

    public Segment getNavigationRange() {
        if (myPsiFileRange  != null) {
            final Segment range = myPsiFileRange.getRange();
            if (range != null) {
                return range;
            }
        }

        PsiElement element = getElement();
        if (element == null) return null;
        TextRange range = element.getTextRange();

        TextRange rangeInElement = getRangeInElement();
        if (rangeInElement == null) return null;
        return rangeInElement.shiftRight(range.getStartOffset());
    }

    public boolean isValid() {
        return getSegment() != null;
    }

    
    public Segment getSegment() {
        PsiElement element = getElement();
        if (element == null) return null;
        TextRange range = element.getTextRange();
        TextRange.assertProperRange(range, element);
        if (element instanceof PsiFile) {
            // hack: it's actually a range inside file, use document for range checking since during the "find|replace all" operation, file range might have been changed
            Document document = PsiDocumentManager.getInstance(getProject()).getDocument((PsiFile)element);
            if (document != null) {
                range = new ProperTextRange(0, document.getTextLength());
            }
        }
        ProperTextRange rangeInElement = getRangeInElement();
        if (rangeInElement == null) return null;
        return new ProperTextRange(Math.min(range.getEndOffset(), range.getStartOffset() + rangeInElement.getStartOffset()),
                Math.min(range.getEndOffset(), range.getStartOffset() + rangeInElement.getEndOffset()));
    }

    
    public Project getProject() {
        return mySmartPointer.getProject();
    }

    public final boolean isWritable() {
        PsiElement element = getElement();
        return element == null || element.isWritable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!getClass().equals(o.getClass())) return false;

        final UsageInfo usageInfo = (UsageInfo)o;

        if (isNonCodeUsage != usageInfo.isNonCodeUsage) return false;

        SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(getProject());
        return smartPointerManager.pointToTheSameElement(mySmartPointer, usageInfo.mySmartPointer)
                && (myPsiFileRange == null || usageInfo.myPsiFileRange != null && smartPointerManager.pointToTheSameElement(myPsiFileRange, usageInfo.myPsiFileRange));
    }

    @Override
    public int hashCode() {
        int result = mySmartPointer != null ? mySmartPointer.hashCode() : 0;
        result = 29 * result + (myPsiFileRange == null ? 0 : myPsiFileRange.hashCode());
        result = 29 * result + (isNonCodeUsage ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        PsiReference reference = getReference();
        if (reference == null) {
            return super.toString();
        }
        return reference.getCanonicalText() + " (" + reference.getClass() + ")";
    }

    
    public PsiFile getFile() {
        return mySmartPointer.getContainingFile();
    }

    
    public VirtualFile getVirtualFile() {
        return mySmartPointer.getVirtualFile();
    }

    public boolean isDynamicUsage() {
        return myDynamicUsage;
    }

    // creates new smart pointers
    public UsageInfo copy() {
        PsiElement element = mySmartPointer.getElement();
        SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(getProject());
        PsiFile containingFile = myPsiFileRange == null ? null : myPsiFileRange.getContainingFile();
        Segment segment = containingFile == null ? null : myPsiFileRange.getRange();
        TextRange range = segment == null ? null : TextRange.create(segment);
        SmartPsiFileRange psiFileRange = range == null ? null : smartPointerManager.createSmartPsiFileRangePointer(containingFile, range);
        SmartPsiElementPointer<PsiElement> pointer = element == null || !isValid() ? null : smartPointerManager.createSmartPsiElementPointer(element);
        return pointer == null ? null : new UsageInfo(pointer, psiFileRange, isDynamicUsage(), isNonCodeUsage());
    }
}
