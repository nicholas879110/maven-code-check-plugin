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
package com.gome.maven.psi.impl;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Attachment;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.progress.EmptyProgressIndicator;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.pom.PomManager;
import com.gome.maven.pom.PomModel;
import com.gome.maven.pom.event.PomModelEvent;
import com.gome.maven.pom.impl.PomTransactionBase;
import com.gome.maven.pom.tree.TreeAspect;
import com.gome.maven.pom.tree.TreeAspectEvent;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiLock;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.impl.source.text.DiffLog;
import com.gome.maven.psi.impl.source.tree.FileElement;
import com.gome.maven.psi.impl.source.tree.ForeignLeafPsiElement;
import com.gome.maven.psi.impl.source.tree.TreeUtil;
import com.gome.maven.psi.text.BlockSupport;
import com.gome.maven.util.Processor;

public abstract class DocumentCommitProcessor {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.DocumentCommitThread");

    public abstract void commitSynchronously( Document document,  Project project);
    public abstract void commitAsynchronously( final Project project,  final Document document,   Object reason);

    protected static class CommitTask {
         public final Document document;
         public final Project project;

        // when queued it's not started
        // when dequeued it's started
        // when failed it's canceled
         public final ProgressIndicator indicator; // progress to commit this doc under.
         public final Object reason;
        public boolean removed; // task marked as removed, should be ignored.

        public CommitTask( Document document,
                           Project project,
                           ProgressIndicator indicator,
                           Object reason) {
            this.document = document;
            this.project = project;
            this.indicator = indicator;
            this.reason = reason;
        }

        
        @Override
        public String toString() {
            return "Project: " + project.getName()
                    + ", Doc: "+ document +" ("+  StringUtil.first(document.getImmutableCharSequence(), 12, true).toString().replaceAll("\n", " ")+")"
                    +(indicator.isCanceled() ? " (Canceled)" : "") + (removed ? "Removed" : "");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CommitTask)) return false;

            CommitTask task = (CommitTask)o;

            return document.equals(task.document) && project.equals(task.project);
        }

        @Override
        public int hashCode() {
            int result = document.hashCode();
            result = 31 * result + project.hashCode();
            return result;
        }
    }

    // public for Upsource
    public Processor<Document> doCommit( final CommitTask task,
                                         final PsiFile file,
                                        final boolean synchronously) {
        Document document = task.document;
        final long startDocModificationTimeStamp = document.getModificationStamp();
        final FileElement myTreeElementBeingReparsedSoItWontBeCollected = ((PsiFileImpl)file).calcTreeElement();
        final CharSequence chars = document.getImmutableCharSequence();
        final TextRange changedPsiRange = getChangedPsiRange(file, myTreeElementBeingReparsedSoItWontBeCollected, chars);
        if (changedPsiRange == null) {
            return null;
        }

        final Boolean data = document.getUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY);
        if (data != null) {
            document.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, null);
            file.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, data);
        }

        BlockSupport blockSupport = BlockSupport.getInstance(file.getProject());
        final DiffLog diffLog = blockSupport.reparseRange(file, changedPsiRange, chars, task.indicator);

        return new Processor<Document>() {
            @Override
            public boolean process(Document document) {
                ApplicationManager.getApplication().assertWriteAccessAllowed();
                log("Finishing", task, synchronously, document.getModificationStamp(), startDocModificationTimeStamp);
                if (document.getModificationStamp() != startDocModificationTimeStamp ||
                        ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(file.getProject())).getCachedViewProvider(document) != file.getViewProvider()) {
                    return false; // optimistic locking failed
                }

                doActualPsiChange(file, diffLog);

                assertAfterCommit(document, file, myTreeElementBeingReparsedSoItWontBeCollected);

                return true;
            }
        };
    }

    private static int getLeafMatchingLength(CharSequence leafText, CharSequence pattern, int patternIndex, int finalPatternIndex, int direction) {
        int leafIndex = direction == 1 ? 0 : leafText.length() - 1;
        int finalLeafIndex = direction == 1 ? leafText.length() - 1 : 0;
        int result = 0;
        while (leafText.charAt(leafIndex) == pattern.charAt(patternIndex)) {
            result++;
            if (leafIndex == finalLeafIndex || patternIndex == finalPatternIndex) {
                break;
            }
            leafIndex += direction;
            patternIndex += direction;
        }
        return result;
    }

    private static int getMatchingLength( FileElement treeElement,  CharSequence text, boolean fromStart) {
        int patternIndex = fromStart ? 0 : text.length() - 1;
        int finalPatternIndex = fromStart ? text.length() - 1 : 0;
        int direction = fromStart ? 1 : -1;
        ASTNode leaf = fromStart ? TreeUtil.findFirstLeaf(treeElement, false) : TreeUtil.findLastLeaf(treeElement, false);
        int result = 0;
        while (leaf != null && (fromStart ? patternIndex <= finalPatternIndex : patternIndex >= finalPatternIndex)) {
            if (!(leaf instanceof ForeignLeafPsiElement)) {
                CharSequence chars = leaf.getChars();
                if (chars.length() > 0) {
                    int matchingLength = getLeafMatchingLength(chars, text, patternIndex, finalPatternIndex, direction);
                    result += matchingLength;
                    if (matchingLength != chars.length()) {
                        break;
                    }
                    patternIndex += (fromStart ? matchingLength : -matchingLength);
                }
            }
            leaf = fromStart ? TreeUtil.nextLeaf(leaf, false) : TreeUtil.prevLeaf(leaf, false);
        }
        return result;
    }

    
    public static TextRange getChangedPsiRange( PsiFile file,  FileElement treeElement,  CharSequence newDocumentText) {
        int psiLength = treeElement.getTextLength();
        if (!file.getViewProvider().supportsIncrementalReparse(file.getLanguage())) {
            return new TextRange(0, psiLength);
        }

        int commonPrefixLength = getMatchingLength(treeElement, newDocumentText, true);
        if (commonPrefixLength == newDocumentText.length() && newDocumentText.length() == psiLength) {
            return null;
        }

        int commonSuffixLength = Math.min(getMatchingLength(treeElement, newDocumentText, false), psiLength - commonPrefixLength);
        return new TextRange(commonPrefixLength, psiLength - commonSuffixLength);
    }

    public static void doActualPsiChange( final PsiFile file,  final DiffLog diffLog) {
        CodeStyleManager.getInstance(file.getProject()).performActionWithFormatterDisabled(new Runnable() {
            @Override
            public void run() {
                synchronized (PsiLock.LOCK) {
                    file.getViewProvider().beforeContentsSynchronized();

                    final Document document = file.getViewProvider().getDocument();
                    PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(file.getProject());
                    PsiToDocumentSynchronizer.DocumentChangeTransaction transaction = documentManager.getSynchronizer().getTransaction(document);

                    final PsiFileImpl fileImpl = (PsiFileImpl)file;

                    if (transaction == null) {
                        final PomModel model = PomManager.getModel(fileImpl.getProject());

                        model.runTransaction(new PomTransactionBase(fileImpl, model.getModelAspect(TreeAspect.class)) {
                            @Override
                            public PomModelEvent runInner() {
                                return new TreeAspectEvent(model, diffLog.performActualPsiChange(file));
                            }
                        });
                    }
                    else {
                        diffLog.performActualPsiChange(file);
                    }
                }
            }
        });
    }

    private void assertAfterCommit( Document document,
                                    final PsiFile file,
                                    FileElement myTreeElementBeingReparsedSoItWontBeCollected) {
        if (myTreeElementBeingReparsedSoItWontBeCollected.getTextLength() != document.getTextLength()) {
            final String documentText = document.getText();
            String fileText = file.getText();
            LOG.error("commitDocument left PSI inconsistent: " + file +
                            "; file len=" + myTreeElementBeingReparsedSoItWontBeCollected.getTextLength() +
                            "; doc len=" + document.getTextLength() +
                            "; doc.getText() == file.getText(): " + Comparing.equal(fileText, documentText),
                    new Attachment("file psi text", fileText),
                    new Attachment("old text", documentText));

            file.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, Boolean.TRUE);
            try {
                BlockSupport blockSupport = BlockSupport.getInstance(file.getProject());
                final DiffLog diffLog = blockSupport.reparseRange(file, new TextRange(0, documentText.length()), documentText, createProgressIndicator());
                doActualPsiChange(file, diffLog);

                if (myTreeElementBeingReparsedSoItWontBeCollected.getTextLength() != document.getTextLength()) {
                    LOG.error("PSI is broken beyond repair in: " + file);
                }
            }
            finally {
                file.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, null);
            }
        }
    }

    public void log( String msg,  CommitTask task, boolean synchronously,  Object... args) {
    }

    
    protected ProgressIndicator createProgressIndicator() {
        return new EmptyProgressIndicator();
    }
}
