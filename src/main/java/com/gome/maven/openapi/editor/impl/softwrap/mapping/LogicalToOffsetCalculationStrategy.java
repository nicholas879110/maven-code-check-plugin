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
package com.gome.maven.openapi.editor.impl.softwrap.mapping;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.FoldRegion;
import com.gome.maven.openapi.editor.LogicalPosition;
import com.gome.maven.openapi.editor.SoftWrap;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.impl.SoftWrapModelImpl;
import com.gome.maven.openapi.editor.impl.softwrap.SoftWrapsStorage;

import java.util.List;

class LogicalToOffsetCalculationStrategy extends AbstractMappingStrategy<Integer> {

    private LogicalPosition targetPosition;

    LogicalToOffsetCalculationStrategy( EditorEx editor,  SoftWrapsStorage storage,  List<CacheEntry> cache) {
        super(editor, storage, cache);
    }

    void init(LogicalPosition logical) {
        CacheEntry entry = MappingUtil.getCacheEntryForLogicalPosition(logical, myCache);
        if (entry == null) {
            if (logical.line >= myEditor.getDocument().getLineCount()) {
                setEagerMatch(myEditor.getDocument().getTextLength());
            }
            else {
                setEagerMatch(Math.min(myEditor.getDocument().getLineStartOffset(logical.line) + logical.column,
                        myEditor.getDocument().getLineEndOffset(logical.line)));
            }
            return;
        }
        if (entry.endLogicalLine == logical.line && entry.endLogicalColumn <= logical.column) {
            setEagerMatch(entry.endOffset);
            return;
        }
        reset();
        targetPosition = logical;
        setTargetEntry(entry, true);
    }

    
    @Override
    protected Integer buildIfExceeds(EditorPosition position, int offset) {
        if (position.logicalLine != targetPosition.line) {
            return null;
        }
        int result = position.offset + targetPosition.column - position.logicalColumn;
        return result > offset ? null : result;
    }

    @Override
    public Integer processFoldRegion( EditorPosition position,  FoldRegion foldRegion) {
        int startLine = position.logicalLine;
        int startColumn = position.logicalColumn;
        int startX = position.x;

        advancePositionOnFolding(position, foldRegion);

        if (position.logicalLine < targetPosition.line
                || position.logicalLine == targetPosition.line && position.logicalColumn <= targetPosition.column) {
            return null;
        }

        Document document = myEditor.getDocument();
        int lineEndOffset = document.getLineEndOffset(targetPosition.line);
        int result = SoftWrapModelImpl.getEditorTextRepresentationHelper(myEditor)
                .calcSoftWrapUnawareOffset(targetPosition.line == startLine ? foldRegion.getStartOffset()
                                : document.getLineStartOffset(targetPosition.line),
                        lineEndOffset,
                        targetPosition.line == startLine ? startColumn : 0,
                        targetPosition.column,
                        targetPosition.line == startLine ? startX : 0);
        return result < 0 ? lineEndOffset : result;
    }

    
    @Override
    protected Integer buildIfExceeds( EditorPosition position,  FoldRegion foldRegion) {
        throw new RuntimeException("Unexpected invocation");
    }

    
    @Override
    protected Integer buildIfExceeds(EditorPosition position, TabData tabData) {
        if (position.logicalLine != targetPosition.line) {
            return null;
        }
        return position.logicalColumn + tabData.widthInColumns > targetPosition.column ? position.offset : null;
    }

    
    @Override
    public Integer processSoftWrap( EditorPosition position, SoftWrap softWrap) {
        return null;
    }

    
    @Override
    public Integer build( EditorPosition position) {
        return position.offset + targetPosition.column - position.logicalColumn;
    }
}
