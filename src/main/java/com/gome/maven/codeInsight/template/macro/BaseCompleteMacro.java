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

package com.gome.maven.codeInsight.template.macro;

import com.gome.maven.codeInsight.completion.CompletionPhase;
import com.gome.maven.codeInsight.completion.impl.CompletionServiceImpl;
import com.gome.maven.codeInsight.lookup.*;
import com.gome.maven.codeInsight.template.*;
import com.gome.maven.codeInsight.template.impl.TemplateManagerImpl;
import com.gome.maven.codeInsight.template.impl.TemplateState;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilBase;

public abstract class BaseCompleteMacro extends Macro {
    private final String myName;

    protected BaseCompleteMacro( String name) {
        myName = name;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String getPresentableName() {
        return myName + "()";
    }

    @Override
    
    public String getDefaultValue() {
        return "a";
    }

    @Override
    public final Result calculateResult( Expression[] params, final ExpressionContext context) {
        return new InvokeActionResult(
                new Runnable() {
                    @Override
                    public void run() {
                        invokeCompletion(context);
                    }
                }
        );
    }

    private void invokeCompletion(final ExpressionContext context) {
        final Project project = context.getProject();
        final Editor editor = context.getEditor();

        final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (project.isDisposed() || editor.isDisposed() || psiFile == null || !psiFile.isValid()) return;

                // it's invokeLater, so another completion could have started
                if (CompletionServiceImpl.getCompletionService().getCurrentCompletion() != null) return;

                CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                    @Override
                    public void run() {
                        // if we're in some completion's insert handler, make sure our new completion isn't treated as the second invocation
                        CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);

                        invokeCompletionHandler(project, editor);
                        Lookup lookup = LookupManager.getInstance(project).getActiveLookup();

                        if (lookup != null) {
                            lookup.addLookupListener(new MyLookupListener(context));
                        }
                        else {
                            considerNextTab(editor);
                        }
                    }
                }, "", null);
            }
        };
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    private static void considerNextTab(Editor editor) {
        TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
        if (templateState != null) {
            TextRange range = templateState.getCurrentVariableRange();
            if (range != null && range.getLength() > 0) {
                int caret = editor.getCaretModel().getOffset();
                if (caret == range.getEndOffset()) {
                    templateState.nextTab();
                }
                else if (caret > range.getEndOffset()) {
                    templateState.cancelTemplate();
                }
            }
        }
    }

    protected abstract void invokeCompletionHandler(Project project, Editor editor);

    private static class MyLookupListener extends LookupAdapter {
        private final ExpressionContext myContext;

        public MyLookupListener( ExpressionContext context) {
            myContext = context;
        }

        @Override
        public void itemSelected(LookupEvent event) {
            LookupElement item = event.getItem();
            if (item == null) return;

            char c = event.getCompletionChar();
            if (!LookupEvent.isSpecialCompletionChar(c)) {
                return;
            }

            for(TemplateCompletionProcessor processor: Extensions.getExtensions(TemplateCompletionProcessor.EP_NAME)) {
                if (!processor.nextTabOnItemSelected(myContext, item)) {
                    return;
                }
            }

            final Project project = myContext.getProject();
            if (project == null) {
                return;
            }

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    new WriteCommandAction(project) {
                        @Override
                        protected void run(com.gome.maven.openapi.application.Result result) throws Throwable {
                            Editor editor = myContext.getEditor();
                            if (editor != null) {
                                considerNextTab(editor);
                            }
                        }
                    }.execute();
                }
            };
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                runnable.run();
            } else {
                ApplicationManager.getApplication().invokeLater(runnable, ModalityState.current(), project.getDisposed());
            }

        }
    }
}
