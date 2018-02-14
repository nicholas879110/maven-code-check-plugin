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

import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.ex.GlobalInspectionContextUtil;
import com.gome.maven.lang.annotation.ProblemGroup;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Maxim.Mossienko
 * Date: 16.09.2009
 * Time: 20:35:06
 */
public class GlobalInspectionUtil {
    private static final String LOC_MARKER = " #loc";

    
    public static String createInspectionMessage( String message) {
        //TODO: FIXME!
        return message + LOC_MARKER;
    }

    public static void createProblem( PsiElement elt,
                                      HighlightInfo info,
                                     TextRange range,
                                      ProblemGroup problemGroup,
                                      InspectionManager manager,
                                      ProblemDescriptionsProcessor problemDescriptionsProcessor,
                                      GlobalInspectionContext globalContext) {
        List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>();
        if (info.quickFixActionRanges != null) {
            for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> actionRange : info.quickFixActionRanges) {
                final IntentionAction action = actionRange.getFirst().getAction();
                if (action instanceof LocalQuickFix) {
                    fixes.add((LocalQuickFix)action);
                }
            }
        }
        ProblemDescriptor descriptor = manager.createProblemDescriptor(elt, range, createInspectionMessage(StringUtil.notNullize(info.getDescription())),
                HighlightInfo.convertType(info.type), false,
                fixes.isEmpty() ? null : fixes.toArray(new LocalQuickFix[fixes.size()]));
        descriptor.setProblemGroup(problemGroup);
        problemDescriptionsProcessor.addProblemElement(
                GlobalInspectionContextUtil.retrieveRefElement(elt, globalContext),
                descriptor
        );
    }
}
