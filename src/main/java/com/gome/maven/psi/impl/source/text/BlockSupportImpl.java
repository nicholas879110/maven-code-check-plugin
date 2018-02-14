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

package com.gome.maven.psi.impl.source.text;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Attachment;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.ex.DocumentBulkUpdateListener;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.PlainTextLanguage;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.PsiManagerImpl;
import com.gome.maven.psi.impl.PsiTreeChangeEventImpl;
import com.gome.maven.psi.impl.source.DummyHolder;
import com.gome.maven.psi.impl.source.DummyHolderFactory;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.templateLanguages.ITemplateDataElementType;
import com.gome.maven.psi.text.BlockSupport;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.IReparseableElementType;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.CharTable;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.diff.DiffTree;
import com.gome.maven.util.diff.DiffTreeChangeBuilder;
import com.gome.maven.util.diff.FlyweightCapableTreeStructure;
import com.gome.maven.util.diff.ShallowNodeComparator;

public class BlockSupportImpl extends BlockSupport {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.text.BlockSupportImpl");

    public BlockSupportImpl(Project project) {
        project.getMessageBus().connect().subscribe(DocumentBulkUpdateListener.TOPIC, new DocumentBulkUpdateListener.Adapter() {
            @Override
            public void updateStarted( final Document doc) {
                doc.putUserData(DO_NOT_REPARSE_INCREMENTALLY, Boolean.TRUE);
            }
        });
    }

    @Override
    public void reparseRange(PsiFile file, int startOffset, int endOffset, CharSequence newTextS) throws IncorrectOperationException {
        LOG.assertTrue(file.isValid());
        final PsiFileImpl psiFile = (PsiFileImpl)file;
        final Document document = psiFile.getViewProvider().getDocument();
        assert document != null;
        document.replaceString(startOffset, endOffset, newTextS);
        PsiDocumentManager.getInstance(psiFile.getProject()).commitDocument(document);
    }

    @Override
    
    public DiffLog reparseRange( final PsiFile file,
                                 TextRange changedPsiRange,
                                 final CharSequence newFileText,
                                 final ProgressIndicator indicator) {
        final PsiFileImpl fileImpl = (PsiFileImpl)file;

        final Couple<ASTNode> reparseableRoots = findReparseableRoots(fileImpl, changedPsiRange, newFileText);
        return reparseableRoots != null
                ? mergeTrees(fileImpl, reparseableRoots.first, reparseableRoots.second, indicator)
                : makeFullParse(fileImpl.getTreeElement(), newFileText, newFileText.length(), fileImpl, indicator);
    }

    /**
     * This method searches ast node that could be reparsed incrementally and returns pair of target reparseable node and new replacement node.
     * Returns null if there is no any chance to make incremental parsing.
     */
    
    public Couple<ASTNode> findReparseableRoots( PsiFileImpl file,
                                                 TextRange changedPsiRange,
                                                 CharSequence newFileText) {
        Project project = file.getProject();
        final FileElement fileElement = file.getTreeElement();
        final CharTable charTable = fileElement.getCharTable();
        int lengthShift = newFileText.length() - fileElement.getTextLength();

        if (fileElement.getElementType() instanceof ITemplateDataElementType || isTooDeep(file)) {
            // unable to perform incremental reparse for template data in JSP, or in exceptionally deep trees
            return null;
        }

        final ASTNode leafAtStart = fileElement.findLeafElementAt(Math.max(0, changedPsiRange.getStartOffset() - 1));
        final ASTNode leafAtEnd = fileElement.findLeafElementAt(Math.min(changedPsiRange.getEndOffset(), fileElement.getTextLength() - 1));
        ASTNode node = leafAtStart != null && leafAtEnd != null ? TreeUtil.findCommonParent(leafAtStart, leafAtEnd) : fileElement;
        Language baseLanguage = file.getViewProvider().getBaseLanguage();

        while (node != null && !(node instanceof FileElement)) {
            IElementType elementType = node.getElementType();
            if (elementType instanceof IReparseableElementType) {
                final TextRange textRange = node.getTextRange();
                final IReparseableElementType reparseable = (IReparseableElementType)elementType;

                if (baseLanguage.isKindOf(reparseable.getLanguage()) && textRange.getLength() + lengthShift > 0) {
                    final int start = textRange.getStartOffset();
                    final int end = start + textRange.getLength() + lengthShift;
                    if (end > newFileText.length()) {
                        reportInconsistentLength(file, newFileText, node, start, end);
                        break;
                    }

                    CharSequence newTextStr = newFileText.subSequence(start, end);

                    if (reparseable.isParsable(node.getTreeParent(), newTextStr, baseLanguage, project)) {
                        ASTNode chameleon = reparseable.createNode(newTextStr);
                        if (chameleon != null) {
                            DummyHolder holder = DummyHolderFactory.createHolder(file.getManager(), null, node.getPsi(), charTable);
                            holder.getTreeElement().rawAddChildren((TreeElement)chameleon);

                            if (holder.getTextLength() != newTextStr.length()) {
                                String details = ApplicationManager.getApplication().isInternal()
                                        ? "text=" + newTextStr + "; treeText=" + holder.getText() + ";"
                                        : "";
                                LOG.error("Inconsistent reparse: " + details + " type=" + elementType);
                            }

                            return Couple.of(node, chameleon);
                        }
                    }
                }
            }
            node = node.getTreeParent();
        }
        return null;
    }

    private static void reportInconsistentLength(PsiFile file, CharSequence newFileText, ASTNode node, int start, int end) {
        String message = "Index out of bounds: type=" + node.getElementType() +
                "; file=" + file +
                "; file.class=" + file.getClass() +
                "; start=" + start +
                "; end=" + end +
                "; length=" + node.getTextLength();
        String newTextBefore = newFileText.subSequence(0, start).toString();
        String oldTextBefore = file.getText().subSequence(0, start).toString();
        if (oldTextBefore.equals(newTextBefore)) {
            message += "; oldTextBefore==newTextBefore";
        }
        LOG.error(message,
                new Attachment(file.getName() + "_oldNodeText.txt", node.getText()),
                new Attachment(file.getName() + "_oldFileText.txt", file.getText()),
                new Attachment(file.getName() + "_newFileText.txt", newFileText.toString())
        );
    }

    
    private static DiffLog makeFullParse(ASTNode parent,
                                          CharSequence newFileText,
                                         int textLength,
                                          PsiFileImpl fileImpl,
                                          ProgressIndicator indicator) {
        if (fileImpl instanceof PsiCodeFragment) {
            final FileElement holderElement = new DummyHolder(fileImpl.getManager(), null).getTreeElement();
            holderElement.rawAddChildren(fileImpl.createContentLeafElement(holderElement.getCharTable().intern(newFileText, 0, textLength)));
            DiffLog diffLog = new DiffLog();
            diffLog.appendReplaceFileElement((FileElement)parent, (FileElement)holderElement.getFirstChildNode());

            return diffLog;
        }
        else {
            FileViewProvider viewProvider = fileImpl.getViewProvider();
            viewProvider.getLanguages();
            FileType fileType = viewProvider.getVirtualFile().getFileType();
            String fileName = fileImpl.getName();
            final LightVirtualFile lightFile = new LightVirtualFile(fileName, fileType, newFileText, viewProvider.getVirtualFile().getCharset(),
                    fileImpl.getViewProvider().getModificationStamp());
            lightFile.setOriginalFile(viewProvider.getVirtualFile());

            FileViewProvider copy = viewProvider.createCopy(lightFile);
            if (copy.isEventSystemEnabled()) {
                throw new AssertionError("Copied view provider must be non-physical for reparse to deliver correct events: " + viewProvider);
            }
            copy.getLanguages();
            SingleRootFileViewProvider.doNotCheckFileSizeLimit(lightFile); // optimization: do not convert file contents to bytes to determine if we should codeinsight it
            PsiFileImpl newFile = getFileCopy(fileImpl, copy);

            newFile.setOriginalFile(fileImpl);

            final FileElement newFileElement = (FileElement)newFile.getNode();
            final FileElement oldFileElement = (FileElement)fileImpl.getNode();

            DiffLog diffLog = mergeTrees(fileImpl, oldFileElement, newFileElement, indicator);

            ((PsiManagerEx)fileImpl.getManager()).getFileManager().setViewProvider(lightFile, null);
            return diffLog;
        }
    }

    
    public static PsiFileImpl getFileCopy(PsiFileImpl originalFile, FileViewProvider providerCopy) {
        FileViewProvider viewProvider = originalFile.getViewProvider();
        Language language = originalFile.getLanguage();

        PsiFile file = providerCopy.getPsi(language);
        if (file != null && !(file instanceof PsiFileImpl)) {
            throw new RuntimeException("View provider " + viewProvider + " refused to provide PsiFileImpl for " + language + details(providerCopy, viewProvider));
        }

        PsiFileImpl newFile = (PsiFileImpl)file;

        if (newFile == null && language == PlainTextLanguage.INSTANCE && originalFile == viewProvider.getPsi(viewProvider.getBaseLanguage())) {
            newFile = (PsiFileImpl)providerCopy.getPsi(providerCopy.getBaseLanguage());
        }

        if (newFile == null) {
            throw new RuntimeException("View provider " + viewProvider + " refused to parse text with " + language + details(providerCopy, viewProvider));
        }

        return newFile;
    }

    private static String details(FileViewProvider providerCopy, FileViewProvider viewProvider) {
        return "; languages: " + viewProvider.getLanguages() +
                "; base: " + viewProvider.getBaseLanguage() +
                "; copy: " + providerCopy +
                "; copy.base: " + providerCopy.getBaseLanguage() +
                "; vFile: " + viewProvider.getVirtualFile() +
                "; copy.vFile: " + providerCopy.getVirtualFile() +
                "; fileType: " + viewProvider.getVirtualFile().getFileType() +
                "; copy.original(): " +
                (providerCopy.getVirtualFile() instanceof LightVirtualFile ? ((LightVirtualFile)providerCopy.getVirtualFile()).getOriginalFile() : null);
    }

    
    private static DiffLog replaceElementWithEvents( CompositeElement oldRoot,  CompositeElement newRoot) {
        DiffLog diffLog = new DiffLog();
        diffLog.appendReplaceElementWithEvents(oldRoot, newRoot);
        return diffLog;
    }

    
    public static DiffLog mergeTrees( final PsiFileImpl fileImpl,
                                      final ASTNode oldRoot,
                                      final ASTNode newRoot,
                                      ProgressIndicator indicator) {
        if (newRoot instanceof FileElement) {
            ((FileElement)newRoot).setCharTable(fileImpl.getTreeElement().getCharTable());
        }

        try {
            newRoot.putUserData(TREE_TO_BE_REPARSED, oldRoot);
            if (isReplaceWholeNode(fileImpl, newRoot)) {
                DiffLog treeChangeEvent = replaceElementWithEvents((CompositeElement)oldRoot, (CompositeElement)newRoot);
                fileImpl.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);

                return treeChangeEvent;
            }
            newRoot.getFirstChildNode();  // maybe reparsed in PsiBuilderImpl and have thrown exception here
        }
        catch (ReparsedSuccessfullyException e) {
            // reparsed in PsiBuilderImpl
            return e.getDiffLog();
        }
        finally {
            newRoot.putUserData(TREE_TO_BE_REPARSED, null);
        }

        final ASTShallowComparator comparator = new ASTShallowComparator(indicator);
        final ASTStructure treeStructure = createInterruptibleASTStructure(newRoot, indicator);

        DiffLog diffLog = new DiffLog();
        diffTrees(oldRoot, diffLog, comparator, treeStructure, indicator);
        return diffLog;
    }

    public static <T> void diffTrees( final ASTNode oldRoot,
                                      final DiffTreeChangeBuilder<ASTNode, T> builder,
                                      final ShallowNodeComparator<ASTNode, T> comparator,
                                      final FlyweightCapableTreeStructure<T> newTreeStructure,
                                      ProgressIndicator indicator) {
        TreeUtil.ensureParsedRecursivelyCheckingProgress(oldRoot, indicator);
        DiffTree.diff(createInterruptibleASTStructure(oldRoot, indicator), newTreeStructure, comparator, builder);
    }

    private static ASTStructure createInterruptibleASTStructure( final ASTNode oldRoot,  final ProgressIndicator indicator) {
        return new ASTStructure(oldRoot) {
            @Override
            public int getChildren( ASTNode astNode,  Ref<ASTNode[]> into) {
                indicator.checkCanceled();
                return super.getChildren(astNode, into);
            }
        };
    }

    private static boolean isReplaceWholeNode( PsiFileImpl fileImpl,  ASTNode newRoot) throws ReparsedSuccessfullyException {
        final Boolean data = fileImpl.getUserData(DO_NOT_REPARSE_INCREMENTALLY);
        if (data != null) fileImpl.putUserData(DO_NOT_REPARSE_INCREMENTALLY, null);

        boolean explicitlyMarkedDeep = Boolean.TRUE.equals(data);

        if (explicitlyMarkedDeep || isTooDeep(fileImpl)) {
            return true;
        }

        final ASTNode childNode = newRoot.getFirstChildNode();  // maybe reparsed in PsiBuilderImpl and have thrown exception here
        boolean childTooDeep = isTooDeep(childNode);
        if (childTooDeep) {
            childNode.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, null);
            fileImpl.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);
        }
        return childTooDeep;
    }

    public static void sendBeforeChildrenChangeEvent( PsiManagerImpl manager,  PsiElement scope, boolean isGenericChange) {
        if (!scope.isPhysical()) {
            manager.beforeChange(false);
            return;
        }
        PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(manager);
        event.setParent(scope);
        event.setFile(scope.getContainingFile());
        TextRange range = scope.getTextRange();
        event.setOffset(range == null ? 0 : range.getStartOffset());
        event.setOldLength(scope.getTextLength());
        // the "generic" event is being sent on every PSI change. It does not carry any specific info except the fact that "something has changed"
        event.setGenericChange(isGenericChange);
        manager.beforeChildrenChange(event);
    }

    public static void sendAfterChildrenChangedEvent( PsiManagerImpl manager,
                                                      PsiFile scope,
                                                     int oldLength,
                                                     boolean isGenericChange) {
        if (!scope.isPhysical()) {
            manager.afterChange(false);
            return;
        }
        PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(manager);
        event.setParent(scope);
        event.setFile(scope);
        event.setOffset(0);
        event.setOldLength(oldLength);
        event.setGenericChange(isGenericChange);
        manager.childrenChanged(event);
    }
}
