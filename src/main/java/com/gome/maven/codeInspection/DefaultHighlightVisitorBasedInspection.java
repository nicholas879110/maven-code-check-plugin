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

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeHighlighting.TextEditorHighlightingPass;
import com.gome.maven.codeInsight.daemon.impl.*;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.NotNullProducer;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.FilteringIterator;

import java.util.ArrayList;
import java.util.List;

public abstract class DefaultHighlightVisitorBasedInspection extends GlobalSimpleInspectionTool {
    private final boolean highlightErrorElements;
    private final boolean runAnnotators;

    public DefaultHighlightVisitorBasedInspection(boolean highlightErrorElements, boolean runAnnotators) {
        this.highlightErrorElements = highlightErrorElements;
        this.runAnnotators = runAnnotators;
    }

    public static class AnnotatorBasedInspection extends DefaultHighlightVisitorBasedInspection {
        public static final String ANNOTATOR_SHORT_NAME = "Annotator";

        public AnnotatorBasedInspection() {
            super(false, true);
        }
        
        
        @Override
        public String getDisplayName() {
            return getShortName();
        }

        
        @Override
        public String getShortName() {
            return ANNOTATOR_SHORT_NAME;
        }

    }
    public static class SyntaxErrorInspection extends DefaultHighlightVisitorBasedInspection {
        public SyntaxErrorInspection() {
            super(true, false);
        }
        
        
        @Override
        public String getDisplayName() {
            return "Syntax error";
        }

        
        @Override
        public String getShortName() {
            return "SyntaxError";
        }
    }

    
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public void checkFile( PsiFile originalFile,
                           final InspectionManager manager,
                           ProblemsHolder problemsHolder,
                           final GlobalInspectionContext globalContext,
                           final ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        for (Pair<PsiFile, HighlightInfo> pair : runGeneralHighlighting(originalFile, highlightErrorElements, runAnnotators,
                problemsHolder.isOnTheFly())) {
            PsiFile file = pair.first;
            HighlightInfo info = pair.second;
            TextRange range = new TextRange(info.startOffset, info.endOffset);
            PsiElement element = file.findElementAt(info.startOffset);

            while (element != null && !element.getTextRange().contains(range)) {
                element = element.getParent();
            }

            if (element == null) {
                element = file;
            }

            GlobalInspectionUtil.createProblem(
                    element,
                    info,
                    range.shiftRight(-element.getNode().getStartOffset()),
                    info.getProblemGroup(),
                    manager,
                    problemDescriptionsProcessor,
                    globalContext
            );

        }
    }

    public static List<Pair<PsiFile,HighlightInfo>> runGeneralHighlighting(PsiFile file,
                                                                           final boolean highlightErrorElements,
                                                                           final boolean runAnnotators, boolean isOnTheFly) {
        MyPsiElementVisitor visitor = new MyPsiElementVisitor(highlightErrorElements, runAnnotators, isOnTheFly);
        file.accept(visitor);
        return new ArrayList<Pair<PsiFile, HighlightInfo>>(visitor.result);
    }

    
    
    @Override
    public String getGroupDisplayName() {
        return GENERAL_GROUP_NAME;
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final boolean highlightErrorElements;
        private final boolean runAnnotators;
        private final boolean myOnTheFly;
        private List<Pair<PsiFile, HighlightInfo>> result = new ArrayList<Pair<PsiFile, HighlightInfo>>();

        public MyPsiElementVisitor(boolean highlightErrorElements, boolean runAnnotators, boolean isOnTheFly) {
            this.highlightErrorElements = highlightErrorElements;
            this.runAnnotators = runAnnotators;
            myOnTheFly = isOnTheFly;
        }

        @Override
        public void visitFile(final PsiFile file) {
            final VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile == null) {
                return;
            }

            final Project project = file.getProject();
            Document document = PsiDocumentManager.getInstance(project).getDocument(file);
            if (document == null) return;
            DaemonProgressIndicator progress = new DaemonProgressIndicator();
            progress.start();
            try {
                TextEditorHighlightingPassRegistrarEx passRegistrarEx = TextEditorHighlightingPassRegistrarEx.getInstanceEx(project);
                List<TextEditorHighlightingPass> passes = passRegistrarEx.instantiateMainPasses(file, document, HighlightInfoProcessor.getEmpty());
                List<GeneralHighlightingPass> gpasses = ContainerUtil.collect(passes.iterator(), FilteringIterator.instanceOf(GeneralHighlightingPass.class));
                for (final GeneralHighlightingPass gpass : gpasses) {
                    gpass.setHighlightVisitorProducer(new NotNullProducer<HighlightVisitor[]>() {
                        
                        @Override
                        public HighlightVisitor[] produce() {
                            gpass.incVisitorUsageCount(1);
                            return new HighlightVisitor[]{new DefaultHighlightVisitor(project, highlightErrorElements, runAnnotators, true)};
                        }
                    });
                }


                for (TextEditorHighlightingPass pass : gpasses) {
                    pass.doCollectInformation(progress);
                    List<HighlightInfo> infos = pass.getInfos();
                    for (HighlightInfo info : infos) {
                        if (info == null) continue;
                        //if (info.type == HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT) continue;
                        if (info.getSeverity().compareTo(HighlightSeverity.INFORMATION) <= 0) continue;
                        result.add(Pair.create(file, info));
                    }
                }
            }
            finally {
                progress.stop();
            }
        }
    }
}
