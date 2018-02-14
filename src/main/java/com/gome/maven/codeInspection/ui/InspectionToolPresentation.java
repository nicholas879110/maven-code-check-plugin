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
package com.gome.maven.codeInspection.ui;

import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.CommonProblemDescriptor;
import com.gome.maven.codeInspection.ProblemDescriptionsProcessor;
import com.gome.maven.codeInspection.QuickFix;
import com.gome.maven.codeInspection.ex.GlobalInspectionContextImpl;
import com.gome.maven.codeInspection.ex.HTMLComposerImpl;
import com.gome.maven.codeInspection.ex.InspectionRVContentProvider;
import com.gome.maven.codeInspection.ex.QuickFixAction;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.reference.RefModule;
import com.gome.maven.openapi.vcs.FileStatus;
import org.jdom.Element;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface InspectionToolPresentation extends ProblemDescriptionsProcessor {
    
    InspectionNode createToolNode( GlobalInspectionContextImpl globalInspectionContext,
                                   InspectionNode node,
                                   InspectionRVContentProvider provider,
                                   InspectionTreeNode parentNode,
                                  final boolean showStructure);
    void updateContent();

    boolean hasReportedProblems();

    
    Map<String, Set<RefEntity>> getContent();

    Map<String, Set<RefEntity>> getOldContent();
    void ignoreCurrentElement(RefEntity refEntity);
    void amnesty(RefEntity refEntity);
    void cleanup();
    void finalCleanup();
    boolean isGraphNeeded();
    boolean isElementIgnored(final RefEntity element);
    
    FileStatus getElementStatus(final RefEntity element);
    
    Collection<RefEntity> getIgnoredRefElements();
    
    IntentionAction findQuickFixes( CommonProblemDescriptor descriptor, final String hint);
    
    HTMLComposerImpl getComposer();
    void exportResults( final Element parentNode,  RefEntity refEntity);
    
    Set<RefModule> getModuleProblems();
    
    QuickFixAction[] getQuickFixes( final RefEntity[] refElements);
    
    Map<RefEntity, CommonProblemDescriptor[]> getProblemElements();
    
    Collection<CommonProblemDescriptor> getProblemDescriptors();
    
    FileStatus getProblemStatus( CommonProblemDescriptor descriptor);
    boolean isOldProblemsIncluded();
    
    Map<RefEntity, CommonProblemDescriptor[]> getOldProblemElements();
    boolean isProblemResolved(RefEntity refEntity, CommonProblemDescriptor descriptor);
    void ignoreCurrentElementProblem(RefEntity refEntity, CommonProblemDescriptor descriptor);
    void addProblemElement(RefEntity refElement, boolean filterSuppressed,  CommonProblemDescriptor... descriptions);
    void ignoreProblem( CommonProblemDescriptor descriptor,  QuickFix fix);

    
    GlobalInspectionContextImpl getContext();
    void ignoreProblem(RefEntity refEntity, CommonProblemDescriptor problem, int idx);
    
    QuickFixAction[] extractActiveFixes( RefEntity[] refElements,  Map<RefEntity, Set<QuickFix>> actions);
    void exportResults( final Element parentNode);
}
