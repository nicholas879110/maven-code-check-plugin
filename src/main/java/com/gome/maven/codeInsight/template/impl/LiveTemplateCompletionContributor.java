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
package com.gome.maven.codeInsight.template.impl;

import com.gome.maven.codeInsight.completion.*;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.template.CustomLiveTemplate;
import com.gome.maven.codeInsight.template.CustomLiveTemplateBase;
import com.gome.maven.codeInsight.template.CustomTemplateCallback;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.patterns.PlatformPatterns;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiPlainTextFile;
import com.gome.maven.ui.EditorTextField;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.ProcessingContext;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gome.maven.codeInsight.template.impl.ListTemplatesHandler.filterTemplatesByPrefix;

/**
 * @author peter
 */
public class LiveTemplateCompletionContributor extends CompletionContributor {
    private static boolean ourShowTemplatesInTests = false;

    
    public static void setShowTemplatesInTests(boolean show,  Disposable parentDisposable) {
        ourShowTemplatesInTests = show;
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                ourShowTemplatesInTests = false;
            }
        });
    }

    public static boolean shouldShowAllTemplates() {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return ourShowTemplatesInTests;
        }
        return Registry.is("show.live.templates.in.completion");
    }

    public LiveTemplateCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions( final CompletionParameters parameters,
                                          ProcessingContext context,
                                           CompletionResultSet result) {
                final PsiFile file = parameters.getPosition().getContainingFile();
                if (file instanceof PsiPlainTextFile && parameters.getEditor().getComponent().getParent() instanceof EditorTextField) {
                    return;
                }

                Editor editor = parameters.getEditor();
                int offset = editor.getCaretModel().getOffset();
                final List<TemplateImpl> availableTemplates = TemplateManagerImpl.listApplicableTemplates(file, offset, false);
                final Map<TemplateImpl, String> templates = filterTemplatesByPrefix(availableTemplates, editor, offset, false, false);
                if (showAllTemplates()) {
                    final AtomicBoolean templatesShown = new AtomicBoolean(false);
                    final CompletionResultSet finalResult = result;
                    result.runRemainingContributors(parameters, new Consumer<CompletionResult>() {
                        @Override
                        public void consume(CompletionResult completionResult) {
                            finalResult.passResult(completionResult);
                            ensureTemplatesShown(templatesShown, templates, parameters, finalResult);
                        }
                    });

                    ensureTemplatesShown(templatesShown, templates, parameters, result);
                    return;
                }

                if (parameters.getInvocationCount() > 0) return; //only in autopopups for now

                // custom templates should handle this situation by itself (return true from hasCompletionItems() and provide lookup element)
                // regular templates won't be shown in this case
                if (!customTemplateAvailableAndHasCompletionItem(null, editor, file, offset)) {
                    TemplateImpl template = findFullMatchedApplicableTemplate(editor, offset, availableTemplates);
                    if (template != null) {
                        result.withPrefixMatcher(result.getPrefixMatcher().cloneWithPrefix(template.getKey()))
                                .addElement(new LiveTemplateLookupElementImpl(template, true));
                    }
                }

                for (Map.Entry<TemplateImpl, String> possible : templates.entrySet()) {
                    String templateKey = possible.getKey().getKey();
                    String currentPrefix = possible.getValue();
                    result.withPrefixMatcher(result.getPrefixMatcher().cloneWithPrefix(currentPrefix))
                            .restartCompletionOnPrefixChange(templateKey);
                }
            }
        });
    }

    public static boolean customTemplateAvailableAndHasCompletionItem( Character shortcutChar,  Editor editor,  PsiFile file, int offset) {
        CustomTemplateCallback callback = new CustomTemplateCallback(editor, file);
        for (CustomLiveTemplate customLiveTemplate : TemplateManagerImpl.listApplicableCustomTemplates(editor, file, false)) {
            if (customLiveTemplate instanceof CustomLiveTemplateBase) {
                if ((shortcutChar == null || customLiveTemplate.getShortcut() == shortcutChar.charValue())
                        && ((CustomLiveTemplateBase)customLiveTemplate).hasCompletionItem(file, offset)) {
                    return customLiveTemplate.computeTemplateKey(callback) != null;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("MethodMayBeStatic") //for Kotlin
    protected boolean showAllTemplates() {
        return shouldShowAllTemplates();
    }

    private static void ensureTemplatesShown(AtomicBoolean templatesShown, Map<TemplateImpl, String> templates,
                                             CompletionParameters parameters, CompletionResultSet result) {
        if (!templatesShown.getAndSet(true)) {
            for (final Map.Entry<TemplateImpl, String> entry : templates.entrySet()) {
                result.withPrefixMatcher(result.getPrefixMatcher().cloneWithPrefix(StringUtil.notNullize(entry.getValue())))
                        .addElement(new LiveTemplateLookupElementImpl(entry.getKey(), false));
            }

            PsiFile file = parameters.getPosition().getContainingFile();
            Editor editor = parameters.getEditor();
            for (CustomLiveTemplate customLiveTemplate : TemplateManagerImpl.listApplicableCustomTemplates(editor, file, false)) {
                if (customLiveTemplate instanceof CustomLiveTemplateBase) {
                    ((CustomLiveTemplateBase)customLiveTemplate).addCompletions(parameters, result);
                }
            }
        }
    }

    
    public static TemplateImpl findFullMatchedApplicableTemplate( Editor editor,
                                                                 int offset,
                                                                  Collection<TemplateImpl> availableTemplates) {
        Map<TemplateImpl, String> templates = filterTemplatesByPrefix(availableTemplates, editor, offset, true, false);
        if (templates.size() == 1) {
            TemplateImpl template = ContainerUtil.getFirstItem(templates.keySet());
            if (template != null) {
                return template;
            }
        }
        return null;
    }

    public static class Skipper extends CompletionPreselectSkipper {

        @Override
        public boolean skipElement(LookupElement element, CompletionLocation location) {
            return element instanceof LiveTemplateLookupElement && ((LiveTemplateLookupElement)element).sudden && !Registry.is("ide.completion.autopopup.select.live.templates");
        }
    }
}
