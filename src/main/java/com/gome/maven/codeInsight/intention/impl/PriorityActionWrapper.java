/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.intention.impl;

import com.gome.maven.codeInsight.intention.HighPriorityAction;
import com.gome.maven.codeInsight.intention.LowPriorityAction;
import com.gome.maven.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;

public abstract class PriorityActionWrapper extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final LocalQuickFixAndIntentionActionOnPsiElement fix;

    private PriorityActionWrapper(PsiElement element,  LocalQuickFixAndIntentionActionOnPsiElement fix) {
        super(element);
        this.fix = fix;
    }

    
    @Override
    public String getFamilyName() {
        return fix.getFamilyName();
    }

    @Override
    public void invoke( Project project,
                        PsiFile file,
                        Editor editor,
                        PsiElement startElement,
                        PsiElement endElement) {
        fix.invoke(project, file, editor, startElement, endElement);
    }

    
    @Override
    public String getText() {
        return fix.getName();
    }

    private static class HighPriorityLocalQuickFixWrapper extends PriorityActionWrapper implements HighPriorityAction {
        protected HighPriorityLocalQuickFixWrapper(PsiElement element,  LocalQuickFixAndIntentionActionOnPsiElement fix) {
            super(element, fix);
        }
    }

    private static class NormalPriorityLocalQuickFixWrapper extends PriorityActionWrapper {
        protected NormalPriorityLocalQuickFixWrapper(PsiElement element,  LocalQuickFixAndIntentionActionOnPsiElement fix) {
            super(element, fix);
        }
    }


    private static class LowPriorityLocalQuickFixWrapper extends PriorityActionWrapper implements LowPriorityAction {
        protected LowPriorityLocalQuickFixWrapper(PsiElement element,  LocalQuickFixAndIntentionActionOnPsiElement fix) {
            super(element, fix);
        }
    }

    
    public static LocalQuickFixAndIntentionActionOnPsiElement highPriority(PsiElement element,  LocalQuickFixAndIntentionActionOnPsiElement fix) {
        return new HighPriorityLocalQuickFixWrapper(element, fix);
    }

    
    public static LocalQuickFixAndIntentionActionOnPsiElement normalPriority(PsiElement element,  LocalQuickFixAndIntentionActionOnPsiElement fix) {
        return new NormalPriorityLocalQuickFixWrapper(element, fix);
    }

    
    public static LocalQuickFixAndIntentionActionOnPsiElement lowPriority(PsiElement element,  LocalQuickFixAndIntentionActionOnPsiElement fix) {
        return new LowPriorityLocalQuickFixWrapper(element, fix);
    }
}
