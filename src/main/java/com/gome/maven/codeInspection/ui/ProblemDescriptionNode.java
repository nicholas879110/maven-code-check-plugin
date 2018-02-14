/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.codeInspection.ui;

import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.xml.util.XmlStringUtil;

import javax.swing.*;

import static com.gome.maven.codeInspection.ProblemDescriptorUtil.APPEND_LINE_NUMBER;
import static com.gome.maven.codeInspection.ProblemDescriptorUtil.TRIM_AT_TREE_END;

/**
 * @author max
 */
public class ProblemDescriptionNode extends InspectionTreeNode {
    protected RefEntity myElement;
    private final CommonProblemDescriptor myDescriptor;
    protected final InspectionToolWrapper myToolWrapper;
    
    protected final InspectionToolPresentation myPresentation;

    public ProblemDescriptionNode( Object userObject,
                                   InspectionToolWrapper toolWrapper,
                                   InspectionToolPresentation presentation) {
        this(userObject, null, null, toolWrapper, presentation);
    }

    public ProblemDescriptionNode( RefEntity element,
                                   CommonProblemDescriptor descriptor,
                                   InspectionToolWrapper toolWrapper,
                                   InspectionToolPresentation presentation) {
        this(descriptor, element, descriptor, toolWrapper, presentation);
    }

    private ProblemDescriptionNode( Object userObject,
                                   RefEntity element,
                                   CommonProblemDescriptor descriptor,
                                    InspectionToolWrapper toolWrapper,
                                    InspectionToolPresentation presentation) {
        super(userObject);
        myElement = element;
        myDescriptor = descriptor;
        myToolWrapper = toolWrapper;
        myPresentation = presentation;
    }

    
    public RefEntity getElement() {
        return myElement;
    }

    
    public CommonProblemDescriptor getDescriptor() {
        return myDescriptor;
    }

    @Override
    public Icon getIcon(boolean expanded) {
        if (myDescriptor instanceof ProblemDescriptorBase) {
            ProblemHighlightType problemHighlightType = ((ProblemDescriptorBase)myDescriptor).getHighlightType();
            if (problemHighlightType == ProblemHighlightType.ERROR) return AllIcons.General.Error;
            if (problemHighlightType == ProblemHighlightType.GENERIC_ERROR_OR_WARNING) return AllIcons.General.Warning;
        }
        return AllIcons.General.Information;
    }

    @Override
    public int getProblemCount() {
        return 1;
    }

    @Override
    public boolean isValid() {
        if (myElement instanceof RefElement && !myElement.isValid()) return false;
        final CommonProblemDescriptor descriptor = getDescriptor();
        if (descriptor instanceof ProblemDescriptor) {
            final PsiElement psiElement = ((ProblemDescriptor)descriptor).getPsiElement();
            return psiElement != null && psiElement.isValid();
        }
        return true;
    }


    @Override
    public boolean isResolved() {
        return myElement instanceof RefElement && getPresentation().isProblemResolved(myElement, getDescriptor());
    }

    @Override
    public void ignoreElement() {
        InspectionToolPresentation presentation = getPresentation();
        presentation.ignoreCurrentElementProblem(getElement(), getDescriptor());
    }

    @Override
    public void amnesty() {
        InspectionToolPresentation presentation = getPresentation();
        presentation.amnesty(getElement());
    }

    
    private InspectionToolPresentation getPresentation() {
        return myPresentation;
    }

    @Override
    public FileStatus getNodeStatus() {
        if (myElement instanceof RefElement){
            return getPresentation().getProblemStatus(myDescriptor);
        }
        return FileStatus.NOT_CHANGED;
    }

    public String toString() {
        CommonProblemDescriptor descriptor = getDescriptor();
        if (descriptor == null) return "";
        PsiElement element = descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getPsiElement() : null;

        return XmlStringUtil.stripHtml(ProblemDescriptorUtil.renderDescriptionMessage(descriptor, element,
                APPEND_LINE_NUMBER | TRIM_AT_TREE_END));
    }
}
