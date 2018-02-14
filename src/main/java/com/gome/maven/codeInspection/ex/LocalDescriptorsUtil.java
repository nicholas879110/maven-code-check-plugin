/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefManagerImpl;
import com.gome.maven.codeInspection.ui.InspectionToolPresentation;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.util.TripleFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalDescriptorsUtil {
    private static final TripleFunction<LocalInspectionTool, PsiElement, GlobalInspectionContext,RefElement> CONVERT = new TripleFunction<LocalInspectionTool, PsiElement, GlobalInspectionContext,RefElement>() {
        @Override
        public RefElement fun(LocalInspectionTool tool, PsiElement element, GlobalInspectionContext context) {
            final PsiNamedElement problemElement = tool.getProblemElement(element);

            RefElement refElement = context.getRefManager().getReference(problemElement);
            if (refElement == null && problemElement != null) {  // no need to lose collected results
                refElement = GlobalInspectionContextUtil.retrieveRefElement(element, context);
            }
            return refElement;
        }
    };

    static void addProblemDescriptors( List<ProblemDescriptor> descriptors,
                                      boolean filterSuppressed,
                                       GlobalInspectionContext context,
                                       LocalInspectionTool tool,
                                       TripleFunction<LocalInspectionTool, PsiElement, GlobalInspectionContext, RefElement> getProblemElementFunction,
                                       InspectionToolPresentation dpi) {
        if (descriptors.isEmpty()) return;

        Map<RefElement, List<ProblemDescriptor>> problems = new HashMap<RefElement, List<ProblemDescriptor>>();
        final RefManagerImpl refManager = (RefManagerImpl)context.getRefManager();
        for (ProblemDescriptor descriptor : descriptors) {
            final PsiElement element = descriptor.getPsiElement();
            if (element == null) continue;
            if (filterSuppressed) {
                String alternativeId;
                String id;
                if (refManager.isDeclarationsFound() &&
                        (context.isSuppressed(element, id = tool.getID()) ||
                                (alternativeId = tool.getAlternativeID()) != null &&
                                        !alternativeId.equals(id) &&
                                        context.isSuppressed(element, alternativeId))) {
                    continue;
                }
                if (SuppressionUtil.inspectionResultSuppressed(element, tool)) continue;
            }


            RefElement refElement = getProblemElementFunction.fun(tool, element, context);

            List<ProblemDescriptor> elementProblems = problems.get(refElement);
            if (elementProblems == null) {
                elementProblems = new ArrayList<ProblemDescriptor>();
                problems.put(refElement, elementProblems);
            }
            elementProblems.add(descriptor);
        }

        for (Map.Entry<RefElement, List<ProblemDescriptor>> entry : problems.entrySet()) {
            final List<ProblemDescriptor> problemDescriptors = entry.getValue();
            RefElement refElement = entry.getKey();
            CommonProblemDescriptor[] descriptions = problemDescriptors.toArray(new CommonProblemDescriptor[problemDescriptors.size()]);
            dpi.addProblemElement(refElement, filterSuppressed, descriptions);
        }
    }

    public static void addProblemDescriptors( List<ProblemDescriptor> descriptors,
                                              InspectionToolPresentation dpi,
                                             boolean filterSuppressed,
                                              GlobalInspectionContext inspectionContext,
                                              LocalInspectionTool tool) {
        addProblemDescriptors(descriptors, filterSuppressed, inspectionContext, tool, CONVERT, dpi);
    }
}
