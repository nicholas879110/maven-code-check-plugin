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
package com.gome.maven.find.impl.livePreview;

import com.gome.maven.find.FindResult;
import com.gome.maven.find.FindUtil;
import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.util.TextRange;

public class SelectionManager {
     private final SearchResults mySearchResults;

    public SelectionManager( SearchResults results) {
        mySearchResults = results;
    }

    public void updateSelection(boolean removePreviousSelection, boolean removeAllPreviousSelections) {
        Editor editor = mySearchResults.getEditor();
        if (removeAllPreviousSelections) {
            editor.getCaretModel().removeSecondaryCarets();
        }
        final FindResult cursor = mySearchResults.getCursor();
        if (cursor == null) {
            return;
        }
        if (mySearchResults.getFindModel().isGlobal()) {
            if (removePreviousSelection || removeAllPreviousSelections) {
                FoldingModel foldingModel = editor.getFoldingModel();
                final FoldRegion[] allRegions = editor.getFoldingModel().getAllFoldRegions();

                foldingModel.runBatchFoldingOperation(new Runnable() {
                    @Override
                    public void run() {
                        for (FoldRegion region : allRegions) {
                            if (!region.isValid()) continue;
                            if (cursor.intersects(TextRange.create(region))) {
                                region.setExpanded(true);
                            }
                        }
                    }
                });
                editor.getSelectionModel().setSelection(cursor.getStartOffset(), cursor.getEndOffset());
                editor.getCaretModel().moveToOffset(cursor.getEndOffset());
            }
            else {
                FindUtil.selectSearchResultInEditor(editor, cursor, -1);
            }
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        } else {
            if (!SearchResults.insideVisibleArea(editor, cursor)) {
                LogicalPosition pos = editor.offsetToLogicalPosition(cursor.getStartOffset());
                editor.getScrollingModel().scrollTo(pos, ScrollType.CENTER);
            }
        }
    }

    public boolean removeCurrentSelection() {
        Editor editor = mySearchResults.getEditor();
        CaretModel caretModel = editor.getCaretModel();
        Caret primaryCaret = caretModel.getPrimaryCaret();
        if (caretModel.getCaretCount() > 1) {
            caretModel.removeCaret(primaryCaret);
            return true;
        }
        else {
            primaryCaret.moveToOffset(primaryCaret.getSelectionStart());
            primaryCaret.removeSelection();
            return false;
        }
    }

    public boolean isSelected( FindResult result) {
        Editor editor = mySearchResults.getEditor();
        return editor.getCaretModel().getCaretAt(editor.offsetToVisualPosition(result.getEndOffset())) != null;
    }
}
