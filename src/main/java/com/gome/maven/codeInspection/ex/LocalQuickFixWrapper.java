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

import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.reference.RefManager;
import com.gome.maven.codeInspection.ui.InspectionToolPresentation;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.util.PsiModificationTracker;
import com.gome.maven.util.PairFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public class LocalQuickFixWrapper extends QuickFixAction {
    private final QuickFix myFix;
    private String myText;

    public LocalQuickFixWrapper( QuickFix fix,  InspectionToolWrapper toolWrapper) {
        super(fix.getName(), toolWrapper);
        myFix = fix;
        myText = myFix.getName();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        getTemplatePresentation().setText(myText);
        e.getPresentation().setText(myText);
    }

    @Override
    public String getText(RefEntity where) {
        return myText;
    }

    public void setText( String text) {
        myText = text;
    }


    @Override
    protected boolean isProblemDescriptorsAcceptable() {
        return true;
    }

    
    public QuickFix getFix() {
        return myFix;
    }

    
    protected QuickFix getWorkingQuickFix( QuickFix[] fixes) {
        final QuickFix exactResult = getWorkingQuickFix(fixes, true);
        return exactResult != null ? exactResult : getWorkingQuickFix(fixes, false);
    }

    
    protected QuickFix getWorkingQuickFix( QuickFix[] fixes, boolean exact) {
        for (QuickFix fix : fixes) {
            if (!checkFix(exact, myFix, fix)) continue;
            if (myFix instanceof IntentionWrapper && fix instanceof IntentionWrapper) {
                if (!checkFix(exact, ((IntentionWrapper)myFix).getAction(), fix)) continue;
            }
            return fix;
        }
        return null;
    }

    private static <T> boolean checkFix(boolean exact, T thisFix, T fix) {
        return exact ? thisFix.getClass() == fix.getClass() : thisFix.getClass().isInstance(fix);
    }

    @Override
    protected boolean applyFix( RefEntity[] refElements) {
        return true;
    }

    @Override
    protected void applyFix( final Project project,
                             final GlobalInspectionContextImpl context,
                             final CommonProblemDescriptor[] descriptors,
                             final Set<PsiElement> ignoredElements) {
        final PsiModificationTracker tracker = PsiManager.getInstance(project).getModificationTracker();
        if (myFix instanceof BatchQuickFix) {
            final List<PsiElement> collectedElementsToIgnore = new ArrayList<PsiElement>();
            final Runnable refreshViews = new Runnable() {
                @Override
                public void run() {
                    DaemonCodeAnalyzer.getInstance(project).restart();
                    for (CommonProblemDescriptor descriptor : descriptors) {
                        ignore(ignoredElements, descriptor, getWorkingQuickFix(descriptor.getFixes()), context);
                    }

                    final RefManager refManager = context.getRefManager();
                    final RefElement[] refElements = new RefElement[collectedElementsToIgnore.size()];
                    for (int i = 0, collectedElementsToIgnoreSize = collectedElementsToIgnore.size(); i < collectedElementsToIgnoreSize; i++) {
                        refElements[i] = refManager.getReference(collectedElementsToIgnore.get(i));
                    }

                    removeElements(refElements, project, myToolWrapper);
                }
            };

            ((BatchQuickFix)myFix).applyFix(project, descriptors, collectedElementsToIgnore, refreshViews);
            return;
        }

        boolean restart = false;
        for (CommonProblemDescriptor descriptor : descriptors) {
            if (descriptor == null) continue;
            final QuickFix[] fixes = descriptor.getFixes();
            if (fixes != null) {
                final QuickFix fix = getWorkingQuickFix(fixes);
                if (fix != null) {
                    final long startCount = tracker.getModificationCount();
                    //CCE here means QuickFix was incorrectly inherited, is there a way to signal (plugin) it is wrong?
                    fix.applyFix(project, descriptor);
                    if (startCount != tracker.getModificationCount()) {
                        restart = true;
                        ignore(ignoredElements, descriptor, fix, context);
                    }
                }
            }
        }
        if (restart) {
            DaemonCodeAnalyzer.getInstance(project).restart();
        }
    }

    private void ignore( Set<PsiElement> ignoredElements,
                         CommonProblemDescriptor descriptor,
                         QuickFix fix,
                         GlobalInspectionContextImpl context) {
        if (fix != null) {
            InspectionToolPresentation presentation = context.getPresentation(myToolWrapper);
            presentation.ignoreProblem(descriptor, fix);
        }
        if (descriptor instanceof ProblemDescriptor) {
            ignoredElements.add(((ProblemDescriptor)descriptor).getPsiElement());
        }
    }
}