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

package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInspection.*;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;

/**
 * @author max
 */
public class ProblemDescriptorImpl extends ProblemDescriptorBase implements ProblemDescriptor {
    private final HintAction myHintAction;

    public ProblemDescriptorImpl( PsiElement startElement,
                                  PsiElement endElement,
                                  String descriptionTemplate,
                                 LocalQuickFix[] fixes,
                                  ProblemHighlightType highlightType,
                                 boolean isAfterEndOfLine,
                                  TextRange rangeInElement,
                                 boolean onTheFly) {
        this(startElement, endElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, rangeInElement, null, onTheFly);
    }

    public ProblemDescriptorImpl( PsiElement startElement,
                                  PsiElement endElement,
                                  String descriptionTemplate,
                                 LocalQuickFix[] fixes,
                                  ProblemHighlightType highlightType,
                                 boolean isAfterEndOfLine,
                                  TextRange rangeInElement,
                                  HintAction hintAction,
                                 boolean onTheFly) {
        this(startElement, endElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, rangeInElement, true, hintAction, onTheFly);
    }

    public ProblemDescriptorImpl( PsiElement startElement,
                                  PsiElement endElement,
                                  String descriptionTemplate,
                                 LocalQuickFix[] fixes,
                                  ProblemHighlightType highlightType,
                                 boolean isAfterEndOfLine,
                                  TextRange rangeInElement,
                                 final boolean tooltip,
                                  HintAction hintAction,
                                 boolean onTheFly) {
        super(startElement, endElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, rangeInElement, tooltip, onTheFly);
        myHintAction = hintAction;
    }

    public HintAction getHintAction() {
        return myHintAction;
    }
}
