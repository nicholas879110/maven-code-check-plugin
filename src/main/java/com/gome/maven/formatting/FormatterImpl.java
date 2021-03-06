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

package com.gome.maven.formatting;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.codeStyle.CodeStyleSettings;
import com.gome.maven.psi.codeStyle.CommonCodeStyleSettings;
import com.gome.maven.psi.formatter.FormatterUtil;
import com.gome.maven.psi.formatter.FormattingDocumentModelImpl;
import com.gome.maven.psi.formatter.PsiBasedFormattingModel;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.SequentialTask;
import com.gome.maven.util.text.CharArrayUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FormatterImpl extends FormatterEx
        implements IndentFactory,
        WrapFactory,
        AlignmentFactory,
        SpacingFactory,
        FormattingModelFactory {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.formatting.FormatterImpl");

    private final AtomicReference<FormattingProgressTask> myProgressTask = new AtomicReference<FormattingProgressTask>();

    private final AtomicInteger myIsDisabledCount = new AtomicInteger();
    private final IndentImpl NONE_INDENT = new IndentImpl(Indent.Type.NONE, false, false);
    private final IndentImpl myAbsoluteNoneIndent = new IndentImpl(Indent.Type.NONE, true, false);
    private final IndentImpl myLabelIndent = new IndentImpl(Indent.Type.LABEL, false, false);
    private final IndentImpl myContinuationIndentRelativeToDirectParent = new IndentImpl(Indent.Type.CONTINUATION, false, true);
    private final IndentImpl myContinuationIndentNotRelativeToDirectParent = new IndentImpl(Indent.Type.CONTINUATION, false, false);
    private final IndentImpl myContinuationWithoutFirstIndentRelativeToDirectParent
            = new IndentImpl(Indent.Type.CONTINUATION_WITHOUT_FIRST, false, true);
    private final IndentImpl myContinuationWithoutFirstIndentNotRelativeToDirectParent
            = new IndentImpl(Indent.Type.CONTINUATION_WITHOUT_FIRST, false, false);
    private final IndentImpl myAbsoluteLabelIndent = new IndentImpl(Indent.Type.LABEL, true, false);
    private final IndentImpl myNormalIndentRelativeToDirectParent = new IndentImpl(Indent.Type.NORMAL, false, true);
    private final IndentImpl myNormalIndentNotRelativeToDirectParent = new IndentImpl(Indent.Type.NORMAL, false, false);
    private final SpacingImpl myReadOnlySpacing = new SpacingImpl(0, 0, 0, true, false, true, 0, false, 0);

    public FormatterImpl() {
        Indent.setFactory(this);
        Wrap.setFactory(this);
        Alignment.setFactory(this);
        Spacing.setFactory(this);
        FormattingModelProvider.setFactory(this);
    }

    @Override
    public Alignment createAlignment(boolean applyToNonFirstBlocksOnLine,  Alignment.Anchor anchor) {
        return new AlignmentImpl(applyToNonFirstBlocksOnLine, anchor);
    }

    @Override
    public Alignment createChildAlignment(final Alignment base) {
        AlignmentImpl result = new AlignmentImpl();
        result.setParent(base);
        return result;
    }

    @Override
    public Indent getNormalIndent(boolean relative) {
        return relative ? myNormalIndentRelativeToDirectParent : myNormalIndentNotRelativeToDirectParent;
    }

    @Override
    public Indent getNoneIndent() {
        return NONE_INDENT;
    }

    @Override
    public void setProgressTask( FormattingProgressTask progressIndicator) {
        if (!FormatterUtil.isFormatterCalledExplicitly()) {
            return;
        }
        myProgressTask.set(progressIndicator);
    }

    @Override
    public int getSpacingForBlockAtOffset(FormattingModel model, int offset) {
        Couple<Block> blockWithParent = getBlockAtOffset(null, model.getRootBlock(), offset);
        if (blockWithParent != null) {
            Block parentBlock = blockWithParent.first;
            Block targetBlock = blockWithParent.second;
            if (parentBlock != null && targetBlock != null) {
                Block prevBlock = findPreviousSibling(parentBlock, targetBlock);
                if (prevBlock != null) {
                    SpacingImpl spacing = (SpacingImpl)parentBlock.getSpacing(prevBlock, targetBlock);
                    if (spacing != null) {
                        int minSpaces = spacing.getMinSpaces();
                        if (minSpaces > 0) {
                            return minSpaces;
                        }
                    }
                }
            }
        }
        return 0;
    }

    
    private static Couple<Block> getBlockAtOffset( Block parent,  Block block, int offset) {
        TextRange textRange = block.getTextRange();
        int startOffset = textRange.getStartOffset();
        int endOffset = textRange.getEndOffset();
        if (startOffset == offset) {
            return Couple.of(parent, block);
        }
        if (startOffset > offset || endOffset < offset || block.isLeaf()) {
            return null;
        }
        for (Block subBlock : block.getSubBlocks()) {
            Couple<Block> result = getBlockAtOffset(block, subBlock, offset);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    
    private static Block findPreviousSibling( Block parent, Block block) {
        Block result = null;
        for (Block subBlock : parent.getSubBlocks()) {
            if (subBlock == block) {
                return result;
            }
            result = subBlock;
        }
        return null;
    }

    @Override
    public void format(final FormattingModel model, final CodeStyleSettings settings,
                       final CommonCodeStyleSettings.IndentOptions indentOptions,
                       final CommonCodeStyleSettings.IndentOptions javaIndentOptions,
                       final FormatTextRanges affectedRanges) throws IncorrectOperationException
    {
        try {
            validateModel(model);
            SequentialTask task = new MyFormattingTask() {
                
                @Override
                protected FormatProcessor buildProcessor() {
                    FormatProcessor processor = new FormatProcessor(
                            model.getDocumentModel(), model.getRootBlock(), settings, indentOptions, affectedRanges, FormattingProgressCallback.EMPTY
                    );
                    processor.setJavaIndentOptions(javaIndentOptions);

                    processor.format(model);
                    return processor;
                }
            };
            execute(task);
        }
        catch (FormattingModelInconsistencyException e) {
            LOG.error(e);
        }
    }

    @Override
    public Wrap createWrap(WrapType type, boolean wrapFirstElement) {
        return new WrapImpl(type, wrapFirstElement);
    }

    @Override
    public Wrap createChildWrap(final Wrap parentWrap, final WrapType wrapType, final boolean wrapFirstElement) {
        final WrapImpl result = new WrapImpl(wrapType, wrapFirstElement);
        result.registerParent((WrapImpl)parentWrap);
        return result;
    }

    @Override
    
    public Spacing createSpacing(int minOffset,
                                 int maxOffset,
                                 int minLineFeeds,
                                 final boolean keepLineBreaks,
                                 final int keepBlankLines) {
        return getSpacingImpl(minOffset, maxOffset, minLineFeeds, false, false, keepLineBreaks, keepBlankLines,false, 0);
    }

    @Override
    
    public Spacing getReadOnlySpacing() {
        return myReadOnlySpacing;
    }

    
    @Override
    public Spacing createDependentLFSpacing(int minSpaces,
                                            int maxSpaces,
                                             TextRange dependencyRange,
                                            boolean keepLineBreaks,
                                            int keepBlankLines,
                                             DependentSpacingRule rule)
    {
        return new DependantSpacingImpl(minSpaces, maxSpaces, dependencyRange, keepLineBreaks, keepBlankLines, rule);
    }

    
    private FormattingProgressCallback getProgressCallback() {
        FormattingProgressCallback result = myProgressTask.get();
        return result == null ? FormattingProgressCallback.EMPTY : result;
    }

    @Override
    public void format(final FormattingModel model,
                       final CodeStyleSettings settings,
                       final CommonCodeStyleSettings.IndentOptions indentOptions,
                       final FormatTextRanges affectedRanges) throws IncorrectOperationException {
        try {
            validateModel(model);
            SequentialTask task = new MyFormattingTask() {
                
                @Override
                protected FormatProcessor buildProcessor() {
                    FormatProcessor processor = new FormatProcessor(
                            model.getDocumentModel(), model.getRootBlock(), settings, indentOptions, affectedRanges, getProgressCallback()
                    );
                    processor.format(model, true);
                    return processor;
                }
            };
            execute(task);
        }
        catch (FormattingModelInconsistencyException e) {
            LOG.error(e);
        }
    }

    public void formatWithoutModifications(final FormattingDocumentModel model,
                                           final Block rootBlock,
                                           final CodeStyleSettings settings,
                                           final CommonCodeStyleSettings.IndentOptions indentOptions,
                                           final TextRange affectedRange) throws IncorrectOperationException
    {
        SequentialTask task = new MyFormattingTask() {
            
            @Override
            protected FormatProcessor buildProcessor() {
                FormatProcessor result = new FormatProcessor(
                        model, rootBlock, settings, indentOptions, new FormatTextRanges(affectedRange, true), FormattingProgressCallback.EMPTY
                );
                result.formatWithoutRealModifications();
                return result;
            }
        };
        execute(task);
    }

    /**
     * Execute given sequential formatting task. Two approaches are possible:
     * <pre>
     * <ul>
     *   <li>
     *      <b>synchronous</b> - the task is completely executed during the current method processing;
     *   </li>
     *   <li>
     *       <b>asynchronous</b> - the task is executed at background thread under the progress dialog;
     *   </li>
     * </ul>
     * </pre>
     *
     * @param task    task to execute
     */
    private void execute( SequentialTask task) {
        disableFormatting();
        Application application = ApplicationManager.getApplication();
        FormattingProgressTask progressTask = myProgressTask.getAndSet(null);
        if (progressTask == null || !application.isDispatchThread() || application.isUnitTestMode()) {
            try {
                task.prepare();
                while (!task.isDone()) {
                    task.iteration();
                }
            }
            finally {
                enableFormatting();
            }
        }
        else {
            progressTask.setTask(task);
            Runnable callback = new Runnable() {
                @Override
                public void run() {
                    enableFormatting();
                }
            };
            for (FormattingProgressCallback.EventType eventType : FormattingProgressCallback.EventType.values()) {
                progressTask.addCallback(eventType, callback);
            }
            ProgressManager.getInstance().run(progressTask);
        }
    }

    @Override
    public IndentInfo getWhiteSpaceBefore(final FormattingDocumentModel model,
                                          final Block block,
                                          final CodeStyleSettings settings,
                                          final CommonCodeStyleSettings.IndentOptions indentOptions,
                                          final TextRange affectedRange, final boolean mayChangeLineFeeds)
    {
        disableFormatting();
        try {
            final FormatProcessor processor = buildProcessorAndWrapBlocks(
                    model, block, settings, indentOptions, new FormatTextRanges(affectedRange, true)
            );
            final LeafBlockWrapper blockBefore = processor.getBlockAtOrAfter(affectedRange.getStartOffset());
            LOG.assertTrue(blockBefore != null);
            WhiteSpace whiteSpace = blockBefore.getWhiteSpace();
            LOG.assertTrue(whiteSpace != null);
            if (!mayChangeLineFeeds) {
                whiteSpace.setLineFeedsAreReadOnly();
            }
            processor.setAllWhiteSpacesAreReadOnly();
            whiteSpace.setReadOnly(false);
            processor.formatWithoutRealModifications();
            return new IndentInfo(whiteSpace.getLineFeeds(), whiteSpace.getIndentOffset(), whiteSpace.getSpaces());
        }
        finally {
            enableFormatting();
        }
    }

    @Override
    public void adjustLineIndentsForRange(final FormattingModel model,
                                          final CodeStyleSettings settings,
                                          final CommonCodeStyleSettings.IndentOptions indentOptions,
                                          final TextRange rangeToAdjust) {
        disableFormatting();
        try {
            validateModel(model);
            final FormattingDocumentModel documentModel = model.getDocumentModel();
            final Block block = model.getRootBlock();
            final FormatProcessor processor = buildProcessorAndWrapBlocks(
                    documentModel, block, settings, indentOptions, new FormatTextRanges(rangeToAdjust, true)
            );
            LeafBlockWrapper tokenBlock = processor.getFirstTokenBlock();
            while (tokenBlock != null) {
                final WhiteSpace whiteSpace = tokenBlock.getWhiteSpace();
                whiteSpace.setLineFeedsAreReadOnly(true);
                if (!whiteSpace.containsLineFeeds()) {
                    whiteSpace.setIsReadOnly(true);
                }
                tokenBlock = tokenBlock.getNextBlock();
            }
            processor.formatWithoutRealModifications();
            processor.performModifications(model);
        }
        catch (FormattingModelInconsistencyException e) {
            LOG.error(e);
        }
        finally {
            enableFormatting();
        }
    }

    @Override
    public void formatAroundRange(final FormattingModel model,
                                  final CodeStyleSettings settings,
                                  final TextRange textRange,
                                  final FileType fileType) {
        disableFormatting();
        try {
            validateModel(model);
            final FormattingDocumentModel documentModel = model.getDocumentModel();
            final Block block = model.getRootBlock();
            final FormatProcessor processor = buildProcessorAndWrapBlocks(
                    documentModel, block, settings, settings.getIndentOptions(fileType), null
            );
            LeafBlockWrapper tokenBlock = processor.getFirstTokenBlock();
            while (tokenBlock != null) {
                final WhiteSpace whiteSpace = tokenBlock.getWhiteSpace();

                if (whiteSpace.getEndOffset() < textRange.getStartOffset() || whiteSpace.getEndOffset() > textRange.getEndOffset() + 1) {
                    whiteSpace.setIsReadOnly(true);
                } else if (whiteSpace.getStartOffset() > textRange.getStartOffset() &&
                        whiteSpace.getEndOffset() < textRange.getEndOffset())
                {
                    if (whiteSpace.containsLineFeeds()) {
                        whiteSpace.setLineFeedsAreReadOnly(true);
                    } else {
                        whiteSpace.setIsReadOnly(true);
                    }
                }

                tokenBlock = tokenBlock.getNextBlock();
            }
            processor.formatWithoutRealModifications();
            processor.performModifications(model);
        }
        catch (FormattingModelInconsistencyException e) {
            LOG.error(e);
        }
        finally{
            enableFormatting();
        }
    }

    @Override
    public int adjustLineIndent(final FormattingModel model,
                                final CodeStyleSettings settings,
                                final CommonCodeStyleSettings.IndentOptions indentOptions,
                                final int offset,
                                final TextRange affectedRange) throws IncorrectOperationException {
        disableFormatting();
        try {
            validateModel(model);
            if (model instanceof PsiBasedFormattingModel) {
                ((PsiBasedFormattingModel)model).canModifyAllWhiteSpaces();
            }
            final FormattingDocumentModel documentModel = model.getDocumentModel();
            final Block block = model.getRootBlock();
            final FormatProcessor processor = buildProcessorAndWrapBlocks(
                    documentModel, block, settings, indentOptions, new FormatTextRanges(affectedRange, true), offset
            );

            final LeafBlockWrapper blockAfterOffset = processor.getBlockAtOrAfter(offset);

            if (blockAfterOffset != null && blockAfterOffset.contains(offset)) {
                return offset;
            }

            WhiteSpace whiteSpace = blockAfterOffset != null ? blockAfterOffset.getWhiteSpace() : processor.getLastWhiteSpace();
            return adjustLineIndent(offset, documentModel, processor, indentOptions, model, whiteSpace,
                    blockAfterOffset != null ? blockAfterOffset.getNode() : null);
        }
        catch (FormattingModelInconsistencyException e) {
            LOG.error(e);
        }
        finally {
            enableFormatting();
        }
        return offset;
    }

    /**
     * Delegates to
     * {@link #buildProcessorAndWrapBlocks(FormattingDocumentModel, Block, CodeStyleSettings, CommonCodeStyleSettings.IndentOptions, FormatTextRanges, int)}
     * with '-1' as an interested offset.
     *
     * @param docModel
     * @param rootBlock
     * @param settings
     * @param indentOptions
     * @param affectedRanges
     * @return
     */
    private static FormatProcessor buildProcessorAndWrapBlocks(final FormattingDocumentModel docModel,
                                                               Block rootBlock,
                                                               CodeStyleSettings settings,
                                                               CommonCodeStyleSettings.IndentOptions indentOptions,
                                                                FormatTextRanges affectedRanges)
    {
        return buildProcessorAndWrapBlocks(docModel, rootBlock, settings, indentOptions, affectedRanges, -1);
    }

    /**
     * Builds {@link FormatProcessor} instance and asks it to wrap all {@link Block code blocks}
     * {@link FormattingModel#getRootBlock() derived from the given model}.
     *
     * @param docModel            target model
     * @param rootBlock           root block to process
     * @param settings            code style settings to use
     * @param indentOptions       indent options to use
     * @param affectedRanges      ranges to reformat
     * @param interestingOffset   interesting offset; <code>'-1'</code> if no particular offset has a special interest
     * @return                    format processor instance with wrapped {@link Block code blocks}
     */
    @SuppressWarnings({"StatementWithEmptyBody"})
    private static FormatProcessor buildProcessorAndWrapBlocks(final FormattingDocumentModel docModel,
                                                               Block rootBlock,
                                                               CodeStyleSettings settings,
                                                               CommonCodeStyleSettings.IndentOptions indentOptions,
                                                                FormatTextRanges affectedRanges,
                                                               int interestingOffset)
    {
        FormatProcessor processor = new FormatProcessor(
                docModel, rootBlock, settings, indentOptions, affectedRanges, interestingOffset, FormattingProgressCallback.EMPTY
        );
        while (!processor.iteration()) ;
        return processor;
    }

    private static int adjustLineIndent(
            final int offset,
            final FormattingDocumentModel documentModel,
            final FormatProcessor processor,
            final CommonCodeStyleSettings.IndentOptions indentOptions,
            final FormattingModel model,
            final WhiteSpace whiteSpace,
            ASTNode nodeAfter)
    {
        boolean wsContainsCaret = whiteSpace.getStartOffset() <= offset && offset < whiteSpace.getEndOffset();

        int lineStartOffset = getLineStartOffset(offset, whiteSpace, documentModel);

        final IndentInfo indent = calcIndent(offset, documentModel, processor, whiteSpace);

        final String newWS = whiteSpace.generateWhiteSpace(indentOptions, lineStartOffset, indent).toString();
        if (!whiteSpace.equalsToString(newWS)) {
            try {
                if (model instanceof FormattingModelEx) {
                    ((FormattingModelEx) model).replaceWhiteSpace(whiteSpace.getTextRange(), nodeAfter, newWS);
                }
                else {
                    model.replaceWhiteSpace(whiteSpace.getTextRange(), newWS);
                }
            }
            finally {
                model.commitChanges();
            }
        }

        final int defaultOffset = offset - whiteSpace.getLength() + newWS.length();

        if (wsContainsCaret) {
            final int ws = whiteSpace.getStartOffset()
                    + CharArrayUtil.shiftForward(newWS, Math.max(0, lineStartOffset - whiteSpace.getStartOffset()), " \t");
            return Math.max(defaultOffset, ws);
        } else {
            return defaultOffset;
        }
    }

    private static boolean hasContentAfterLineBreak(final FormattingDocumentModel documentModel, final int offset, final WhiteSpace whiteSpace) {
        return documentModel.getLineNumber(offset) == documentModel.getLineNumber(whiteSpace.getEndOffset()) &&
                documentModel.getTextLength() != offset;
    }

    @Override
    public String getLineIndent(final FormattingModel model,
                                final CodeStyleSettings settings,
                                final CommonCodeStyleSettings.IndentOptions indentOptions,
                                final int offset,
                                final TextRange affectedRange) {
        final FormattingDocumentModel documentModel = model.getDocumentModel();
        final Block block = model.getRootBlock();
        if (block.getTextRange().isEmpty()) return null; // handing empty document case
        final FormatProcessor processor = buildProcessorAndWrapBlocks(
                documentModel, block, settings, indentOptions, new FormatTextRanges(affectedRange, true), offset
        );
        final LeafBlockWrapper blockAfterOffset = processor.getBlockAtOrAfter(offset);

        if (blockAfterOffset != null && !blockAfterOffset.contains(offset)) {
            final WhiteSpace whiteSpace = blockAfterOffset.getWhiteSpace();
            final IndentInfo indent = calcIndent(offset, documentModel, processor, whiteSpace);

            return indent.generateNewWhiteSpace(indentOptions);
        }
        return null;
    }

    private static IndentInfo calcIndent(int offset, FormattingDocumentModel documentModel, FormatProcessor processor, WhiteSpace whiteSpace) {
        processor.setAllWhiteSpacesAreReadOnly();
        whiteSpace.setLineFeedsAreReadOnly(true);
        final IndentInfo indent;
        if (hasContentAfterLineBreak(documentModel, offset, whiteSpace)) {
            whiteSpace.setReadOnly(false);
            processor.formatWithoutRealModifications();
            indent = new IndentInfo(0, whiteSpace.getIndentOffset(), whiteSpace.getSpaces());
        }
        else {
            indent = processor.getIndentAt(offset);
        }
        return indent;
    }

    public static String getText(final FormattingDocumentModel documentModel) {
        return getCharSequence(documentModel).toString();
    }

    private static CharSequence getCharSequence(final FormattingDocumentModel documentModel) {
        return documentModel.getText(new TextRange(0, documentModel.getTextLength()));
    }

    private static int getLineStartOffset(final int offset,
                                          final WhiteSpace whiteSpace,
                                          final FormattingDocumentModel documentModel) {
        int lineStartOffset = offset;

        CharSequence text = getCharSequence(documentModel);
        lineStartOffset = CharArrayUtil.shiftBackwardUntil(text, lineStartOffset, " \t\n");
        if (lineStartOffset > whiteSpace.getStartOffset()) {
            if (lineStartOffset >= text.length()) lineStartOffset = text.length() - 1;
            final int wsStart = whiteSpace.getStartOffset();
            int prevEnd;

            if (text.charAt(lineStartOffset) == '\n'
                    && wsStart <= (prevEnd = documentModel.getLineStartOffset(documentModel.getLineNumber(lineStartOffset - 1))) &&
                    documentModel.getText(new TextRange(prevEnd, lineStartOffset)).toString().trim().length() == 0 // ws consists of space only, it is not true for <![CDATA[
                    ) {
                lineStartOffset--;
            }
            lineStartOffset = CharArrayUtil.shiftBackward(text, lineStartOffset, "\t ");
            if (lineStartOffset < 0) lineStartOffset = 0;
            if (lineStartOffset != offset && text.charAt(lineStartOffset) == '\n') {
                lineStartOffset++;
            }
        }
        return lineStartOffset;
    }

    @Override
    public void adjustTextRange(final FormattingModel model,
                                final CodeStyleSettings settings,
                                final CommonCodeStyleSettings.IndentOptions indentOptions,
                                final TextRange affectedRange,
                                final boolean keepBlankLines,
                                final boolean keepLineBreaks,
                                final boolean changeWSBeforeFirstElement,
                                final boolean changeLineFeedsBeforeFirstElement,
                                 final IndentInfoStorage indentInfoStorage) {
        disableFormatting();
        try {
            validateModel(model);
            final FormatProcessor processor = buildProcessorAndWrapBlocks(
                    model.getDocumentModel(), model.getRootBlock(), settings, indentOptions, new FormatTextRanges(affectedRange, true)
            );
            LeafBlockWrapper current = processor.getFirstTokenBlock();
            while (current != null) {
                WhiteSpace whiteSpace = current.getWhiteSpace();

                if (!whiteSpace.isReadOnly()) {
                    if (whiteSpace.getStartOffset() > affectedRange.getStartOffset()) {
                        if (whiteSpace.containsLineFeeds() && indentInfoStorage != null) {
                            whiteSpace.setLineFeedsAreReadOnly(true);
                            current.setIndentFromParent(indentInfoStorage.getIndentInfo(current.getStartOffset()));
                        }
                        else {
                            whiteSpace.setReadOnly(true);
                        }
                    }
                    else {
                        if (!changeWSBeforeFirstElement) {
                            whiteSpace.setReadOnly(true);
                        }
                        else {
                            if (!changeLineFeedsBeforeFirstElement) {
                                whiteSpace.setLineFeedsAreReadOnly(true);
                            }
                            final SpacingImpl spaceProperty = current.getSpaceProperty();
                            if (spaceProperty != null) {
                                boolean needChange = false;
                                int newKeepLineBreaks = spaceProperty.getKeepBlankLines();
                                boolean newKeepLineBreaksFlag = spaceProperty.shouldKeepLineFeeds();

                                if (!keepLineBreaks) {
                                    needChange = true;
                                    newKeepLineBreaksFlag = false;
                                }
                                if (!keepBlankLines) {
                                    needChange = true;
                                    newKeepLineBreaks = 0;
                                }

                                if (needChange) {
                                    assert !(spaceProperty instanceof DependantSpacingImpl);
                                    current.setSpaceProperty(
                                            getSpacingImpl(
                                                    spaceProperty.getMinSpaces(), spaceProperty.getMaxSpaces(), spaceProperty.getMinLineFeeds(),
                                                    spaceProperty.isReadOnly(),
                                                    spaceProperty.isSafe(), newKeepLineBreaksFlag, newKeepLineBreaks, false, spaceProperty.getPrefLineFeeds()
                                            )
                                    );
                                }
                            }
                        }
                    }
                }
                current = current.getNextBlock();
            }
            processor.format(model);
        }
        catch (FormattingModelInconsistencyException e) {
            LOG.error(e);
        }
        finally {
            enableFormatting();
        }
    }

    @Override
    public void adjustTextRange(final FormattingModel model,
                                final CodeStyleSettings settings,
                                final CommonCodeStyleSettings.IndentOptions indentOptions,
                                final TextRange affectedRange) {
        disableFormatting();
        try {
            validateModel(model);
            final FormatProcessor processor = buildProcessorAndWrapBlocks(
                    model.getDocumentModel(), model.getRootBlock(), settings, indentOptions, new FormatTextRanges(affectedRange, true)
            );
            LeafBlockWrapper current = processor.getFirstTokenBlock();
            while (current != null) {
                WhiteSpace whiteSpace = current.getWhiteSpace();

                if (!whiteSpace.isReadOnly()) {
                    if (whiteSpace.getStartOffset() > affectedRange.getStartOffset()) {
                        whiteSpace.setReadOnly(true);
                    }
                    else {
                        whiteSpace.setReadOnly(false);
                    }
                }
                current = current.getNextBlock();
            }
            processor.format(model);
        }
        catch (FormattingModelInconsistencyException e) {
            LOG.error(e);
        }
        finally {
            enableFormatting();
        }
    }

    @Override
    public void saveIndents(final FormattingModel model, final TextRange affectedRange,
                            IndentInfoStorage storage,
                            final CodeStyleSettings settings,
                            final CommonCodeStyleSettings.IndentOptions indentOptions) {
        try {
            validateModel(model);
            final Block block = model.getRootBlock();

            final FormatProcessor processor = buildProcessorAndWrapBlocks(
                    model.getDocumentModel(), block, settings, indentOptions, new FormatTextRanges(affectedRange, true)
            );
            LeafBlockWrapper current = processor.getFirstTokenBlock();
            while (current != null) {
                WhiteSpace whiteSpace = current.getWhiteSpace();

                if (!whiteSpace.isReadOnly() && whiteSpace.containsLineFeeds()) {
                    storage.saveIndentInfo(current.calcIndentFromParent(), current.getStartOffset());
                }
                current = current.getNextBlock();
            }
        }
        catch (FormattingModelInconsistencyException e) {
            LOG.error(e);
        }
    }

    @Override
    public FormattingModel createFormattingModelForPsiFile(final PsiFile file,
                                                            final Block rootBlock,
                                                           final CodeStyleSettings settings) {
        return new PsiBasedFormattingModel(file, rootBlock, FormattingDocumentModelImpl.createOn(file));
    }

    @Override
    public Indent getSpaceIndent(final int spaces, final boolean relative) {
        return getIndent(Indent.Type.SPACES, spaces, relative, false);
    }

    @Override
    public Indent getIndent( Indent.Type type, boolean relativeToDirectParent, boolean enforceIndentToChildren) {
        return getIndent(type, 0, relativeToDirectParent, enforceIndentToChildren);
    }

    @Override
    public Indent getIndent( Indent.Type type, int spaces, boolean relativeToDirectParent, boolean enforceIndentToChildren) {
        return new IndentImpl(type, false, spaces, relativeToDirectParent, enforceIndentToChildren);
    }

    @Override
    public Indent getAbsoluteLabelIndent() {
        return myAbsoluteLabelIndent;
    }

    @Override
    
    public Spacing createSafeSpacing(final boolean shouldKeepLineBreaks, final int keepBlankLines) {
        return getSpacingImpl(0, 0, 0, false, true, shouldKeepLineBreaks, keepBlankLines, false, 0);
    }

    @Override
    
    public Spacing createKeepingFirstColumnSpacing(final int minSpace,
                                                   final int maxSpace,
                                                   final boolean keepLineBreaks,
                                                   final int keepBlankLines) {
        return getSpacingImpl(minSpace, maxSpace, -1, false, false, keepLineBreaks, keepBlankLines, true, 0);
    }

    @Override
    
    public Spacing createSpacing(final int minSpaces, final int maxSpaces, final int minLineFeeds, final boolean keepLineBreaks, final int keepBlankLines,
                                 final int prefLineFeeds) {
        return getSpacingImpl(minSpaces, maxSpaces, minLineFeeds, false, false, keepLineBreaks, keepBlankLines, false, prefLineFeeds);
    }

    private final Map<SpacingImpl,SpacingImpl> ourSharedProperties = new HashMap<SpacingImpl,SpacingImpl>();
    private final SpacingImpl ourSharedSpacing = new SpacingImpl(-1,-1,-1,false,false,false,-1,false,0);

    private SpacingImpl getSpacingImpl(final int minSpaces,
                                       final int maxSpaces,
                                       final int minLineFeeds,
                                       final boolean readOnly,
                                       final boolean safe,
                                       final boolean keepLineBreaksFlag,
                                       final int keepLineBreaks,
                                       final boolean keepFirstColumn,
                                       int prefLineFeeds)
    {
        synchronized(ourSharedSpacing) {
            ourSharedSpacing.init(minSpaces, maxSpaces, minLineFeeds, readOnly, safe, keepLineBreaksFlag, keepLineBreaks, keepFirstColumn, prefLineFeeds);
            SpacingImpl spacing = ourSharedProperties.get(ourSharedSpacing);

            if (spacing == null) {
                spacing = new SpacingImpl(minSpaces, maxSpaces, minLineFeeds, readOnly, safe, keepLineBreaksFlag, keepLineBreaks, keepFirstColumn, prefLineFeeds);
                ourSharedProperties.put(spacing, spacing);
            }
            return spacing;
        }
    }

    @Override
    public Indent getAbsoluteNoneIndent() {
        return myAbsoluteNoneIndent;
    }

    @Override
    public Indent getLabelIndent() {
        return myLabelIndent;
    }

    @Override
    public Indent getContinuationIndent(boolean relative) {
        return relative ? myContinuationIndentRelativeToDirectParent : myContinuationIndentNotRelativeToDirectParent;
    }

    //is default
    @Override
    public Indent getContinuationWithoutFirstIndent(boolean relative) {
        return relative ? myContinuationWithoutFirstIndentRelativeToDirectParent : myContinuationWithoutFirstIndentNotRelativeToDirectParent;
    }

    @Override
    public boolean isDisabled() {
        return myIsDisabledCount.get() > 0;
    }

    private void disableFormatting() {
        myIsDisabledCount.incrementAndGet();
    }

    private void enableFormatting() {
        int old = myIsDisabledCount.getAndDecrement();
        if (old <= 0) {
            LOG.error("enableFormatting()/disableFormatting() not paired. DisabledLevel = " + old);
        }
    }

    
    public <T> T runWithFormattingDisabled( Computable<T> runnable) {
        disableFormatting();
        try {
            return runnable.compute();
        }
        finally {
            enableFormatting();
        }
    }

    private abstract static class MyFormattingTask implements SequentialTask {
        private FormatProcessor myProcessor;
        private boolean         myDone;

        @Override
        public void prepare() {
            myProcessor = buildProcessor();
        }

        @Override
        public boolean isDone() {
            return myDone;
        }

        @Override
        public boolean iteration() {
            return myDone = myProcessor.iteration();
        }

        @Override
        public void stop() {
            myProcessor.stopSequentialProcessing();
            myDone = true;
        }

        
        protected abstract FormatProcessor buildProcessor();
    }

    private static void validateModel(FormattingModel model) throws FormattingModelInconsistencyException {
        FormattingDocumentModel documentModel = model.getDocumentModel();
        Document document = documentModel.getDocument();
        Block rootBlock = model.getRootBlock();
        if (rootBlock instanceof ASTBlock) {
            PsiElement rootElement = ((ASTBlock)rootBlock).getNode().getPsi();
            if (!rootElement.isValid()) {
                throw new FormattingModelInconsistencyException("Invalid root block PSI element");
            }
            PsiFile file = rootElement.getContainingFile();
            Project project = file.getProject();
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            if (documentManager.isUncommited(document)) {
                throw new FormattingModelInconsistencyException("Uncommitted document");
            }
            if (document.getTextLength() != file.getTextLength()) {
                throw new FormattingModelInconsistencyException(
                        "Document length " + document.getTextLength() +
                                " doesn't match PSI file length " + file.getTextLength() + ", language: " + file.getLanguage()
                );
            }
        }
    }

    private static class FormattingModelInconsistencyException extends Exception {
        public FormattingModelInconsistencyException(String message) {
            super(message);
        }
    }
}
