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

package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupItem;
import com.gome.maven.codeInsight.template.Template;
import com.gome.maven.codeInsight.template.TemplateEditingAdapter;
import com.gome.maven.codeInsight.template.TemplateManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;

/**
 * @author spleaner
 */
public abstract class TemplateInsertHandler<T extends LookupElement> implements InsertHandler<T> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.completion.TemplateInsertHandler");

    protected static final Object EXPANDED_TEMPLATE_ATTR = Key.create("EXPANDED_TEMPLATE_ATTR");

    @Override
    public void handleInsert(final InsertionContext context, final LookupElement item) {
        if (isTemplateToBeCompleted(item)) {
            context.setAddCompletionChar(false);
            handleTemplate((LookupItem) item, context);
        }
    }

    protected static boolean isTemplateToBeCompleted(final LookupElement lookupItem) {
        return lookupItem instanceof LookupItem && lookupItem.getObject() instanceof Template
                && ((LookupItem)lookupItem).getAttribute(EXPANDED_TEMPLATE_ATTR) == null;
    }

    protected void handleTemplate( final LookupItem lookupItem,
                                   final InsertionContext context) {
        LOG.assertTrue(CommandProcessor.getInstance().getCurrentCommand() != null);
        PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getEditor().getDocument());

        Template template = (Template)lookupItem.getObject();

        final Editor editor = context.getEditor();
        final Document document = editor.getDocument();

        final int templateStartOffset = context.getStartOffset();
        document.replaceString(templateStartOffset, templateStartOffset + lookupItem.getLookupString().length(), "");

        final RangeMarker offsetRangeMarker = document.createRangeMarker(templateStartOffset, templateStartOffset);

        TemplateManager.getInstance(editor.getProject()).startTemplate(editor, template, new TemplateEditingAdapter() {
            @Override
            public void templateFinished(Template template, boolean brokenOff) {
                lookupItem.setAttribute(EXPANDED_TEMPLATE_ATTR, Boolean.TRUE);

                if (!offsetRangeMarker.isValid()) return;

                final Editor editor = context.getEditor();
                final int startOffset = offsetRangeMarker.getStartOffset();
                final int endOffset = editor.getCaretModel().getOffset();
                String lookupString = editor.getDocument().getCharsSequence().subSequence(startOffset, endOffset).toString();
                lookupItem.setLookupString(lookupString);

                final OffsetMap offsetMap = new OffsetMap(document);
                offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, startOffset);
                offsetMap.addOffset(CompletionInitializationContext.SELECTION_END_OFFSET, endOffset);
                offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, endOffset);

                final PsiFile psiFile = context.getFile();

                InsertionContext newContext =
                        new InsertionContext(offsetMap, context.getCompletionChar(), LookupElement.EMPTY_ARRAY, psiFile, editor, context.shouldAddCompletionChar());

                populateInsertMap(psiFile, offsetMap);

                handleInsert(newContext, lookupItem);
            }
        });
    }

    protected void populateInsertMap( final PsiFile file,  final OffsetMap offsetMap) {
    }

}