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
package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ex.MarkupModelEx;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.editor.impl.DocumentMarkupModel;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.CommonProcessors;
import com.gome.maven.util.Processor;

import java.util.List;

public abstract class DaemonCodeAnalyzerEx extends DaemonCodeAnalyzer {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.daemon.impl.DaemonCodeAnalyzerEx");
    public static DaemonCodeAnalyzerEx getInstanceEx(Project project) {
        return (DaemonCodeAnalyzerEx)project.getComponent(DaemonCodeAnalyzer.class);
    }

    public static boolean processHighlights( Document document,
                                             Project project,
                                             final HighlightSeverity minSeverity,
                                            final int startOffset,
                                            final int endOffset,
                                             final Processor<HighlightInfo> processor) {
        LOG.assertTrue(ApplicationManager.getApplication().isReadAccessAllowed());

        final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
        MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
        return model.processRangeHighlightersOverlappingWith(startOffset, endOffset, new Processor<RangeHighlighterEx>() {
            @Override
            public boolean process( RangeHighlighterEx marker) {
                Object tt = marker.getErrorStripeTooltip();
                if (!(tt instanceof HighlightInfo)) return true;
                HighlightInfo info = (HighlightInfo)tt;
                return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0
                        || info.highlighter == null
                        || processor.process(info);
            }
        });
    }

    static boolean processHighlightsOverlappingOutside( Document document,
                                                        Project project,
                                                        final HighlightSeverity minSeverity,
                                                       final int startOffset,
                                                       final int endOffset,
                                                        final Processor<HighlightInfo> processor) {
        LOG.assertTrue(ApplicationManager.getApplication().isReadAccessAllowed());

        final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
        MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
        return model.processRangeHighlightersOutside(startOffset, endOffset, new Processor<RangeHighlighterEx>() {
            @Override
            public boolean process( RangeHighlighterEx marker) {
                Object tt = marker.getErrorStripeTooltip();
                if (!(tt instanceof HighlightInfo)) return true;
                HighlightInfo info = (HighlightInfo)tt;
                return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0
                        || info.highlighter == null
                        || processor.process(info);
            }
        });
    }

    static boolean hasErrors( Project project,  Document document) {
        return !processHighlights(document, project, HighlightSeverity.ERROR, 0, document.getTextLength(),
                CommonProcessors.<HighlightInfo>alwaysFalse());
    }

    
    public abstract List<HighlightInfo> runMainPasses( PsiFile psiFile,
                                                       Document document,
                                                       ProgressIndicator progress);

    public abstract boolean isErrorAnalyzingFinished( PsiFile file);

    
    public abstract FileStatusMap getFileStatusMap();

    

    public abstract List<HighlightInfo> getFileLevelHighlights( Project project,  PsiFile file);

    public abstract void cleanFileLevelHighlights( Project project, int group, PsiFile psiFile);

    public abstract void addFileLevelHighlight( final Project project,
                                               final int group,
                                                final HighlightInfo info,
                                                final PsiFile psiFile);
}
