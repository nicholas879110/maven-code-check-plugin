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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.Lookup;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.diagnostic.LogEventException;
import com.gome.maven.openapi.diagnostic.Attachment;
import com.gome.maven.injected.editor.DocumentWindow;
import com.gome.maven.injected.editor.EditorWindow;
import com.gome.maven.lang.FileASTNode;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.ex.RangeMarkerEx;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.DebugUtil;
import com.gome.maven.psi.util.PsiUtilCore;

import java.util.List;

/**
 * @author peter
 */
class CompletionAssertions {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.completion.CompletionAssertions");

    static void assertCommitSuccessful(Editor editor, PsiFile psiFile) {
        Document document = editor.getDocument();
        int docLength = document.getTextLength();
        int psiLength = psiFile.getTextLength();
        PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
        boolean committed = !manager.isUncommited(document);
        if (docLength == psiLength && committed) {
            return;
        }

        FileViewProvider viewProvider = psiFile.getViewProvider();

        String message = "unsuccessful commit:";
        message += "\nmatching=" + (psiFile == manager.getPsiFile(document));
        message += "\ninjectedEditor=" + (editor instanceof EditorWindow);
        message += "\ninjectedFile=" + InjectedLanguageManager.getInstance(psiFile.getProject()).isInjectedFragment(psiFile);
        message += "\ncommitted=" + committed;
        message += "\nfile=" + psiFile.getName();
        message += "\nfile class=" + psiFile.getClass();
        message += "\nfile.valid=" + psiFile.isValid();
        message += "\nfile.physical=" + psiFile.isPhysical();
        message += "\nfile.eventSystemEnabled=" + viewProvider.isEventSystemEnabled();
        message += "\nlanguage=" + psiFile.getLanguage();
        message += "\ndoc.length=" + docLength;
        message += "\npsiFile.length=" + psiLength;
        String fileText = psiFile.getText();
        if (fileText != null) {
            message += "\npsiFile.text.length=" + fileText.length();
        }
        FileASTNode node = psiFile.getNode();
        if (node != null) {
            message += "\nnode.length=" + node.getTextLength();
            String nodeText = node.getText();
            if (nodeText != null) {
                message += "\nnode.text.length=" + nodeText.length();
            }
        }
        VirtualFile virtualFile = viewProvider.getVirtualFile();
        message += "\nvirtualFile=" + virtualFile;
        message += "\nvirtualFile.class=" + virtualFile.getClass();
        message += "\n" + DebugUtil.currentStackTrace();

        throw new LogEventException("Commit unsuccessful", message,
                new Attachment(virtualFile.getPath() + "_file.txt", fileText),
                createAstAttachment(psiFile, psiFile),
                new Attachment("docText.txt", document.getText()));
    }

    static void checkEditorValid(Editor editor) {
        if (editor instanceof EditorWindow && !((EditorWindow)editor).isValid()) {
            throw new AssertionError();
        }
    }

    private static Attachment createAstAttachment(PsiFile fileCopy, final PsiFile originalFile) {
        return new Attachment(originalFile.getViewProvider().getVirtualFile().getPath() + " syntactic tree.txt", DebugUtil.psiToString(fileCopy, false, true));
    }

    private static Attachment createFileTextAttachment(PsiFile fileCopy, final PsiFile originalFile) {
        return new Attachment(originalFile.getViewProvider().getVirtualFile().getPath(), fileCopy.getText());
    }

    static void assertFinalOffsets(PsiFile originalFile, CompletionContext context, PsiFile injected) {
        if (context.getStartOffset() >= context.file.getTextLength()) {
            String msg = "start outside the file; file=" + context.file + " " + context.file.getTextLength();
            msg += "; injected=" + (injected != null);
            msg += "; original " + originalFile + " " + originalFile.getTextLength();
            throw new AssertionError(msg);
        }
        assert context.getStartOffset() >= 0 : "start < 0";
    }

    static void assertInjectedOffsets(int hostStartOffset,
                                      InjectedLanguageManager injectedLanguageManager,
                                      PsiFile injected,
                                      DocumentWindow documentWindow) {
        assert documentWindow != null : "no DocumentWindow for an injected fragment";

        TextRange host = injectedLanguageManager.injectedToHost(injected, injected.getTextRange());
        assert hostStartOffset >= host.getStartOffset() : "startOffset before injected";
        assert hostStartOffset <= host.getEndOffset() : "startOffset after injected";
    }

    static void assertHostInfo(PsiFile hostCopy, OffsetMap hostMap) {
        PsiUtilCore.ensureValid(hostCopy);
        if (hostMap.getOffset(CompletionInitializationContext.START_OFFSET) >= hostCopy.getTextLength()) {
            throw new AssertionError("startOffset outside the host file: " + hostMap.getOffset(CompletionInitializationContext.START_OFFSET) + "; " + hostCopy);
        }
    }

    static void assertCompletionPositionPsiConsistent(CompletionContext newContext,
                                                      int offset,
                                                      PsiFile fileCopy,
                                                      PsiFile originalFile, PsiElement insertedElement) {
        if (insertedElement == null) {
            throw new LogEventException("No element at insertion offset",
                    "offset=" +
                            newContext.getStartOffset() +
                            "\n" +
                            DebugUtil.currentStackTrace(),
                    createFileTextAttachment(fileCopy, originalFile),
                    createAstAttachment(fileCopy, originalFile));
        }

        if (fileCopy.findElementAt(offset) != insertedElement) {
            throw new AssertionError("wrong offset");
        }

        final TextRange range = insertedElement.getTextRange();
        CharSequence fileCopyText = fileCopy.getViewProvider().getContents();
        if ((range.getEndOffset() > fileCopyText.length()) ||
                !fileCopyText.subSequence(range.getStartOffset(), range.getEndOffset()).toString().equals(insertedElement.getText())) {
            throw new LogEventException("Inconsistent completion tree", "range=" + range + "\n" + DebugUtil.currentStackTrace(),
                    createFileTextAttachment(fileCopy, originalFile), createAstAttachment(fileCopy, originalFile),
                    new Attachment("Element at caret.txt", insertedElement.getText()));
        }
    }

    static class WatchingInsertionContext extends InsertionContext {
        private RangeMarkerEx tailWatcher;
        String invalidateTrace;
        DocumentEvent killer;
        private RangeMarkerSpy spy;

        public WatchingInsertionContext(OffsetMap offsetMap, PsiFile file, char completionChar, List<LookupElement> items, Editor editor) {
            super(offsetMap, completionChar, items.toArray(new LookupElement[items.size()]),
                    file, editor,
                    completionChar != Lookup.AUTO_INSERT_SELECT_CHAR && completionChar != Lookup.REPLACE_SELECT_CHAR &&
                            completionChar != Lookup.NORMAL_SELECT_CHAR);
        }

        @Override
        public void setTailOffset(int offset) {
            super.setTailOffset(offset);
            watchTail(offset);
        }

        private void watchTail(int offset) {
            stopWatching();
            tailWatcher = (RangeMarkerEx)getDocument().createRangeMarker(offset, offset);
            if (!tailWatcher.isValid()) {
                throw new AssertionError(getDocument() + "; offset=" + offset);
            }
            tailWatcher.setGreedyToRight(true);
            spy = new RangeMarkerSpy(tailWatcher) {
                @Override
                protected void invalidated(DocumentEvent e) {
                    if (ApplicationManager.getApplication().isUnitTestMode()) {
                        LOG.error("Tail offset invalidated, say thanks to the "+ e);
                    }

                    if (invalidateTrace == null) {
                        invalidateTrace = DebugUtil.currentStackTrace();
                        killer = e;
                    }
                }
            };
            getDocument().addDocumentListener(spy);
        }

        void stopWatching() {
            if (tailWatcher != null) {
                getDocument().removeDocumentListener(spy);
                tailWatcher.dispose();
            }
        }

        @Override
        public int getTailOffset() {
            int offset = super.getTailOffset();
            if (tailWatcher.getStartOffset() != tailWatcher.getEndOffset() && offset > 0) {
                watchTail(offset);
            }

            return offset;
        }
    }
}
