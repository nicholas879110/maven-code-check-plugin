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
package com.gome.maven.codeInspection;

import com.gome.maven.lang.annotation.ProblemGroup;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.PsiTreeUtil;

public class ProblemDescriptorBase extends CommonProblemDescriptorImpl implements ProblemDescriptor {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.ex.ProblemDescriptorImpl");

     private final SmartPsiElementPointer myStartSmartPointer;
     private final SmartPsiElementPointer myEndSmartPointer;

    private final ProblemHighlightType myHighlightType;
    private Navigatable myNavigatable;
    private final boolean myAfterEndOfLine;
    private final TextRange myTextRangeInElement;
    private final boolean myShowTooltip;
    private TextAttributesKey myEnforcedTextAttributes;
    private int myLineNumber = -1;
    private ProblemGroup myProblemGroup;

    public ProblemDescriptorBase( PsiElement startElement,
                                  PsiElement endElement,
                                  String descriptionTemplate,
                                 LocalQuickFix[] fixes,
                                  ProblemHighlightType highlightType,
                                 boolean isAfterEndOfLine,
                                  TextRange rangeInElement,
                                 final boolean tooltip,
                                 boolean onTheFly) {
        super(fixes, descriptionTemplate);
        myShowTooltip = tooltip;
        PsiFile startContainingFile = startElement.getContainingFile();
        LOG.assertTrue(startContainingFile != null && startContainingFile.isValid() || startElement.isValid(), startElement);
        PsiFile endContainingFile = startElement == endElement ? startContainingFile : endElement.getContainingFile();
        LOG.assertTrue(startElement == endElement || endContainingFile != null && endContainingFile.isValid() || endElement.isValid(), endElement);
        assertPhysical(startElement);
        if (startElement != endElement) assertPhysical(endElement);

        final TextRange startElementRange = startElement.getTextRange();
        LOG.assertTrue(startElementRange != null, startElement);
        final TextRange endElementRange = endElement.getTextRange();
        LOG.assertTrue(endElementRange != null, endElement);
        if (startElementRange.getStartOffset() >= endElementRange.getEndOffset()) {
            if (!(startElement instanceof PsiFile && endElement instanceof PsiFile)) {
                LOG.error("Empty PSI elements should not be passed to createDescriptor. Start: " + startElement + ", end: " + endElement);
            }
        }

        myHighlightType = highlightType;
        final Project project = startContainingFile == null ? startElement.getProject() : startContainingFile.getProject();
        final SmartPointerManager manager = SmartPointerManager.getInstance(project);
        myStartSmartPointer = manager.createSmartPsiElementPointer(startElement, startContainingFile);
        myEndSmartPointer = startElement == endElement ? null : manager.createSmartPsiElementPointer(endElement, endContainingFile);

        myAfterEndOfLine = isAfterEndOfLine;
        myTextRangeInElement = rangeInElement;
    }

    protected void assertPhysical(final PsiElement element) {
        if (!element.isPhysical()) {
            LOG.error("Non-physical PsiElement. Physical element is required to be able to anchor the problem in the source tree: " +
                    element + "; file: " + element.getContainingFile());
        }
    }

    @Override
    public PsiElement getPsiElement() {
        PsiElement startElement = getStartElement();
        if (myEndSmartPointer == null) {
            return startElement;
        }
        PsiElement endElement = getEndElement();
        if (startElement == endElement) {
            return startElement;
        }
        if (startElement == null || endElement == null) return null;
        return PsiTreeUtil.findCommonParent(startElement, endElement);
    }

    @Override
    
    public TextRange getTextRangeInElement() {
        return myTextRangeInElement;
    }

    @Override
    public PsiElement getStartElement() {
        return myStartSmartPointer.getElement();
    }

    @Override
    public PsiElement getEndElement() {
        return myEndSmartPointer == null ? getStartElement() : myEndSmartPointer.getElement();
    }

    @Override
    public int getLineNumber() {
        if (myLineNumber == -1) {
            PsiElement psiElement = getPsiElement();
            if (psiElement == null) return -1;
            if (!psiElement.isValid()) return -1;
            LOG.assertTrue(psiElement.isPhysical());
            InjectedLanguageManager manager = InjectedLanguageManager.getInstance(psiElement.getProject());
            PsiFile containingFile = manager.getTopLevelFile(psiElement);
            Document document = PsiDocumentManager.getInstance(psiElement.getProject()).getDocument(containingFile);
            if (document == null) return -1;
            TextRange textRange = getTextRange();
            if (textRange == null) return -1;
            textRange = manager.injectedToHost(psiElement, textRange);
            final int startOffset = textRange.getStartOffset();
            final int textLength = document.getTextLength();
            LOG.assertTrue(startOffset <= textLength, getDescriptionTemplate() + " at " + startOffset + ", " + textLength);
            myLineNumber =  document.getLineNumber(startOffset) + 1;
        }
        return myLineNumber;
    }

    
    @Override
    public ProblemHighlightType getHighlightType() {
        return myHighlightType;
    }

    @Override
    public boolean isAfterEndOfLine() {
        return myAfterEndOfLine;
    }

    @Override
    public void setTextAttributes(TextAttributesKey key) {
        myEnforcedTextAttributes = key;
    }

    public TextAttributesKey getEnforcedTextAttributes() {
        return myEnforcedTextAttributes;
    }

    public TextRange getTextRangeForNavigation() {
        TextRange textRange = getTextRange();
        if (textRange == null) return null;
        PsiElement element = getPsiElement();
        return InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, textRange);
    }

    public TextRange getTextRange() {
        PsiElement startElement = getStartElement();
        PsiElement endElement = myEndSmartPointer == null ? startElement : getEndElement();
        if (startElement == null || endElement == null) {
            return null;
        }

        TextRange textRange = startElement.getTextRange();
        if (startElement == endElement) {
            if (isAfterEndOfLine()) return new TextRange(textRange.getEndOffset(), textRange.getEndOffset());
            if (myTextRangeInElement != null) {
                return new TextRange(textRange.getStartOffset() + myTextRangeInElement.getStartOffset(),
                        textRange.getStartOffset() + myTextRangeInElement.getEndOffset());
            }
            return textRange;
        }
        return new TextRange(textRange.getStartOffset(), endElement.getTextRange().getEndOffset());
    }

    public Navigatable getNavigatable() {
        return myNavigatable;
    }

    public void setNavigatable(final Navigatable navigatable) {
        myNavigatable = navigatable;
    }

    @Override
    
    public ProblemGroup getProblemGroup() {
        return myProblemGroup;
    }

    @Override
    public void setProblemGroup( ProblemGroup problemGroup) {
        myProblemGroup = problemGroup;
    }

    @Override
    public boolean showTooltip() {
        return myShowTooltip;
    }

    @Override
    public String toString() {
        PsiElement element = getPsiElement();
        return ProblemDescriptorUtil.renderDescriptionMessage(this, element);
    }
}
