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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeHighlighting.TextEditorHighlightingPass;
import com.gome.maven.codeInsight.CodeInsightSettings;
import com.gome.maven.codeInsight.daemon.*;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInspection.HintAction;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.keymap.KeymapUtil;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.util.Processor;

import java.util.ArrayList;
import java.util.List;

public class ShowAutoImportPass extends TextEditorHighlightingPass {
    private final Editor myEditor;

    private final PsiFile myFile;

    private final int myStartOffset;
    private final int myEndOffset;

    public ShowAutoImportPass( Project project,  final PsiFile file,  Editor editor) {
        super(project, editor.getDocument(), false);
        ApplicationManager.getApplication().assertIsDispatchThread();

        myEditor = editor;

        TextRange range = VisibleHighlightingPassFactory.calculateVisibleRange(myEditor);
        myStartOffset = range.getStartOffset();
        myEndOffset = range.getEndOffset();

        myFile = file;
    }

    @Override
    public void doCollectInformation( ProgressIndicator progress) {
    }

    @Override
    public void doApplyInformationToEditor() {
        Application application = ApplicationManager.getApplication();
        application.assertIsDispatchThread();
        if (!application.isUnitTestMode() && !myEditor.getContentComponent().hasFocus()) return;
        int caretOffset = myEditor.getCaretModel().getOffset();
        importUnambiguousImports(caretOffset);
        List<HighlightInfo> visibleHighlights = getVisibleHighlights(myStartOffset, myEndOffset, myProject, myEditor);

        for (int i = visibleHighlights.size() - 1; i >= 0; i--) {
            HighlightInfo info = visibleHighlights.get(i);
            if (info.startOffset <= caretOffset && showAddImportHint(info)) return;
        }

        for (HighlightInfo visibleHighlight : visibleHighlights) {
            if (visibleHighlight.startOffset > caretOffset && showAddImportHint(visibleHighlight)) return;
        }
    }

    private void importUnambiguousImports(final int caretOffset) {
        if (!DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled()) return;
        if (!DaemonCodeAnalyzer.getInstance(myProject).isImportHintsEnabled(myFile)) return;
        if (!CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY) return;

        Document document = getDocument();
        final List<HighlightInfo> infos = new ArrayList<HighlightInfo>();
        DaemonCodeAnalyzerEx.processHighlights(document, myProject, null, 0, document.getTextLength(), new Processor<HighlightInfo>() {
            @Override
            public boolean process(HighlightInfo info) {
                if (!info.hasHint() || info.getSeverity() != HighlightSeverity.ERROR) {
                    return true;
                }
                PsiReference reference = myFile.findReferenceAt(info.getActualStartOffset());
                if (reference != null && reference.getElement().getTextRange().containsOffset(caretOffset)) return true;
                infos.add(info);
                return true;
            }
        });

        ReferenceImporter[] importers = Extensions.getExtensions(ReferenceImporter.EP_NAME);
        for (HighlightInfo info : infos) {
            for(ReferenceImporter importer: importers) {
                if (importer.autoImportReferenceAt(myEditor, myFile, info.getActualStartOffset())) break;
            }
        }
    }

    
    private static List<HighlightInfo> getVisibleHighlights(final int startOffset, final int endOffset, Project project, final Editor editor) {
        final List<HighlightInfo> highlights = new ArrayList<HighlightInfo>();
        DaemonCodeAnalyzerEx.processHighlights(editor.getDocument(), project, null, startOffset, endOffset, new Processor<HighlightInfo>() {
            @Override
            public boolean process(HighlightInfo info) {
                if (info.hasHint() && !editor.getFoldingModel().isOffsetCollapsed(info.startOffset)) {
                    highlights.add(info);
                }
                return true;
            }
        });
        return highlights;
    }

    private boolean showAddImportHint(HighlightInfo info) {
        if (!DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled()) return false;
        if (!DaemonCodeAnalyzer.getInstance(myProject).isImportHintsEnabled(myFile)) return false;
        PsiElement element = myFile.findElementAt(info.startOffset);
        if (element == null || !element.isValid()) return false;

        final List<Pair<HighlightInfo.IntentionActionDescriptor, TextRange>> list = info.quickFixActionRanges;
        for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> pair : list) {
            final IntentionAction action = pair.getFirst().getAction();
            if (action instanceof HintAction && action.isAvailable(myProject, myEditor, myFile)) {
                return ((HintAction)action).showHint(myEditor);
            }
        }
        return false;
    }

    public static String getMessage(final boolean multiple, final String name) {
        final String messageKey = multiple ? "import.popup.multiple" : "import.popup.text";
        String hintText = DaemonBundle.message(messageKey, name);
        hintText += " " + KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
        return hintText;
    }
}
