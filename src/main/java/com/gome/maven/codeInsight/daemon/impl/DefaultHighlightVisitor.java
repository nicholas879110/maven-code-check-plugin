/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.gome.maven.codeInsight.daemon.impl.analysis.ErrorQuickFixProvider;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.gome.maven.codeInsight.highlighting.HighlightErrorFilter;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageAnnotators;
import com.gome.maven.lang.LanguageUtil;
import com.gome.maven.lang.annotation.Annotation;
import com.gome.maven.lang.annotation.Annotator;
import com.gome.maven.openapi.extensions.ExtensionPointListener;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.extensions.PluginDescriptor;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiErrorElement;
import com.gome.maven.psi.PsiFile;

import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class DefaultHighlightVisitor implements HighlightVisitor, DumbAware {
    private AnnotationHolderImpl myAnnotationHolder;

    private final HighlightErrorFilter[] myErrorFilters;
    private final Project myProject;
    private final boolean myHighlightErrorElements;
    private final boolean myRunAnnotators;
    private final DumbService myDumbService;
    private HighlightInfoHolder myHolder;
    private final boolean myBatchMode;

    @SuppressWarnings("UnusedDeclaration")
    public DefaultHighlightVisitor( Project project) {
        this(project, true, true, false);
    }
    public DefaultHighlightVisitor( Project project, boolean highlightErrorElements, boolean runAnnotators, boolean batchMode) {
        myProject = project;
        myHighlightErrorElements = highlightErrorElements;
        myRunAnnotators = runAnnotators;
        myErrorFilters = Extensions.getExtensions(HighlightErrorFilter.EP_NAME, project);
        myDumbService = DumbService.getInstance(project);
        myBatchMode = batchMode;
    }

    @Override
    public boolean suitableForFile( final PsiFile file) {
        return true;
    }

    @Override
    public boolean analyze( final PsiFile file,
                           final boolean updateWholeFile,
                            final HighlightInfoHolder holder,
                            final Runnable action) {
        myHolder = holder;
        myAnnotationHolder = new AnnotationHolderImpl(holder.getAnnotationSession(), myBatchMode);
        try {
            action.run();
        }
        finally {
            myAnnotationHolder.clear();
            myAnnotationHolder = null;
            myHolder = null;
        }
        return true;
    }

    @Override
    public void visit( PsiElement element) {
        if (element instanceof PsiErrorElement) {
            if (myHighlightErrorElements) visitErrorElement((PsiErrorElement)element);
        }
        else {
            if (myRunAnnotators) runAnnotators(element);
        }
        if (myAnnotationHolder.hasAnnotations()) {
            for (Annotation annotation : myAnnotationHolder) {
                myHolder.add(HighlightInfo.fromAnnotation(annotation, null, myBatchMode));
            }
            myAnnotationHolder.clear();
        }
    }

    @Override
    
    public HighlightVisitor clone() {
        return new DefaultHighlightVisitor(myProject, myHighlightErrorElements, myRunAnnotators, myBatchMode);
    }

    @Override
    public int order() {
        return 2;
    }

    private static final ThreadLocalAnnotatorMap<Annotator,Language> cachedAnnotators = new ThreadLocalAnnotatorMap<Annotator, Language>() {
        
        @Override
        public Collection<Annotator> initialValue( Language key) {
            return LanguageAnnotators.INSTANCE.allForLanguage(key);
        }
    };

    static {
        LanguageAnnotators.INSTANCE.addListener(new ExtensionPointListener<Annotator>() {
            @Override
            public void extensionAdded( Annotator extension,  PluginDescriptor pluginDescriptor) {
                cachedAnnotators.clear();
            }

            @Override
            public void extensionRemoved( Annotator extension,  PluginDescriptor pluginDescriptor) {
                cachedAnnotators.clear();
            }
        });
    }

    private void runAnnotators(PsiElement element) {
        List<Annotator> annotators = cachedAnnotators.get(element.getLanguage());
        if (annotators.isEmpty()) return;
        final boolean dumb = myDumbService.isDumb();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < annotators.size(); i++) {
            Annotator annotator = annotators.get(i);
            if (dumb && !DumbService.isDumbAware(annotator)) {
                continue;
            }

            ProgressManager.checkCanceled();

            annotator.annotate(element, myAnnotationHolder);
        }
    }

    private void visitErrorElement(final PsiErrorElement element) {
        for(HighlightErrorFilter errorFilter: myErrorFilters) {
            if (!errorFilter.shouldHighlightErrorElement(element)) {
                return;
            }
        }
        HighlightInfo info = createErrorElementInfo(element);
        myHolder.add(info);
    }

    private static HighlightInfo createErrorElementInfo( PsiErrorElement element) {
        TextRange range = element.getTextRange();
        String errorDescription = element.getErrorDescription();
        if (!range.isEmpty()) {
            HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(range);
            if (errorDescription != null) {
                builder.descriptionAndTooltip(errorDescription);
            }
            final HighlightInfo info = builder.create();
            if (info != null) {
                for(ErrorQuickFixProvider provider: Extensions.getExtensions(ErrorQuickFixProvider.EP_NAME)) {
                    provider.registerErrorQuickFix(element, info);
                }
            }
            return info;
        }
        int offset = range.getStartOffset();
        PsiFile containingFile = element.getContainingFile();
        int fileLength = containingFile.getTextLength();
        FileViewProvider viewProvider = containingFile.getViewProvider();
        PsiElement elementAtOffset = viewProvider.findElementAt(offset, LanguageUtil.getRootLanguage(element));
        String text = elementAtOffset == null ? null : elementAtOffset.getText();
        HighlightInfo info;
        if (offset < fileLength && text != null && !StringUtil.startsWithChar(text, '\n') && !StringUtil.startsWithChar(text, '\r')) {
            HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(offset, offset + 1);
            if (errorDescription != null) {
                builder.descriptionAndTooltip(errorDescription);
            }
            info = builder.create();
        }
        else {
            int start;
            int end;
            if (offset > 0) {
                start = offset/* - 1*/;
                end = offset;
            }
            else {
                start = offset;
                end = offset < fileLength ? offset + 1 : offset;
            }
            HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(element, start, end);
            if (errorDescription != null) {
                builder.descriptionAndTooltip(errorDescription);
            }
            builder.endOfLine();
            info = builder.create();
        }
        return info;
    }
}
