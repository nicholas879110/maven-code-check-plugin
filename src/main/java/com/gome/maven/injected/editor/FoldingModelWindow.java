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

package com.gome.maven.injected.editor;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.FoldRegion;
import com.gome.maven.openapi.editor.FoldingGroup;
import com.gome.maven.openapi.editor.ex.FoldingListener;
import com.gome.maven.openapi.editor.ex.FoldingModelEx;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
class FoldingModelWindow implements FoldingModelEx{
    private final FoldingModelEx myDelegate;
    private final DocumentWindow myDocumentWindow;
    private final EditorWindow myEditorWindow;

    FoldingModelWindow( FoldingModelEx delegate,  DocumentWindow documentWindow,  EditorWindow editorWindow) {
        myDelegate = delegate;
        myDocumentWindow = documentWindow;
        myEditorWindow = editorWindow;
    }

    @Override
    public void setFoldingEnabled(boolean isEnabled) {
        myDelegate.setFoldingEnabled(isEnabled);
    }

    @Override
    public boolean isFoldingEnabled() {
        return myDelegate.isFoldingEnabled();
    }

    @Override
    public FoldRegion getFoldingPlaceholderAt(Point p) {
        return myDelegate.getFoldingPlaceholderAt(p);
    }

    @Override
    public boolean intersectsRegion(int startOffset, int endOffset) {
        int hostStart = myDocumentWindow.injectedToHost(startOffset);
        int hostEnd = myDocumentWindow.injectedToHost(endOffset);
        return myDelegate.intersectsRegion(hostStart, hostEnd);
    }

    @Override
    public FoldRegion addFoldRegion(int startOffset, int endOffset,  String placeholderText) {
        FoldRegion region = createFoldRegion(startOffset, endOffset, placeholderText, null, false);
        if (region == null) return null;
        if (!addFoldRegion(region)) {
            region.dispose();
            return null;
        }

        return region;
    }

    @Override
    public boolean addFoldRegion( final FoldRegion region) {
        return myDelegate.addFoldRegion((FoldRegion)((FoldingRegionWindow)region).getDelegate());
    }

    @Override
    public void removeFoldRegion( FoldRegion region) {
        myDelegate.removeFoldRegion((FoldRegion)((FoldingRegionWindow)region).getDelegate());
    }

    @Override
    
    public FoldRegion[] getAllFoldRegions() {
        FoldRegion[] all = myDelegate.getAllFoldRegions();
        List<FoldRegion> result = new ArrayList<FoldRegion>();
        for (FoldRegion region : all) {
            FoldingRegionWindow window = region.getUserData(FOLD_REGION_WINDOW);
            if (window != null && window.getEditor() == myEditorWindow) {
                result.add(window);
            }
        }
        return result.toArray(new FoldRegion[result.size()]);
    }

    @Override
    public boolean isOffsetCollapsed(int offset) {
        return myDelegate.isOffsetCollapsed(myDocumentWindow.injectedToHost(offset));
    }

    @Override
    public FoldRegion getCollapsedRegionAtOffset(int offset) {
        FoldRegion host = myDelegate.getCollapsedRegionAtOffset(myDocumentWindow.injectedToHost(offset));
        return host; //todo convert to window?
    }

    
    @Override
    public FoldRegion getFoldRegion(int startOffset, int endOffset) {
        TextRange range = new TextRange(startOffset, endOffset);
        TextRange hostRange = myDocumentWindow.injectedToHost(range);
        FoldRegion hostRegion = myDelegate.getFoldRegion(hostRange.getStartOffset(), hostRange.getEndOffset());
        if (hostRegion == null) {
            return null;
        }
        FoldingRegionWindow window = hostRegion.getUserData(FOLD_REGION_WINDOW);
        return window != null && window.getEditor() == myEditorWindow ? window : null;
    }

    @Override
    public void runBatchFoldingOperation( Runnable operation) {
        myDelegate.runBatchFoldingOperation(operation);
    }

    @Override
    public void runBatchFoldingOperation( Runnable operation, boolean moveCaretFromCollapsedRegion) {
        myDelegate.runBatchFoldingOperation(operation, moveCaretFromCollapsedRegion);
    }

    @Override
    public void runBatchFoldingOperationDoNotCollapseCaret( Runnable operation) {
        myDelegate.runBatchFoldingOperationDoNotCollapseCaret(operation);
    }

    @Override
    public FoldRegion fetchOutermost(int offset) {
        FoldRegion host = myDelegate.fetchOutermost(myDocumentWindow.injectedToHost(offset));
        return host; //todo convert to window?
    }

    @Override
    public int getLastCollapsedRegionBefore(int offset) {
        return -1; //todo implement
    }

    @Override
    public TextAttributes getPlaceholderAttributes() {
        return myDelegate.getPlaceholderAttributes();
    }

    @Override
    public FoldRegion[] fetchTopLevel() {
        return FoldRegion.EMPTY_ARRAY; //todo implement
    }

    private static final Key<FoldingRegionWindow> FOLD_REGION_WINDOW = Key.create("FOLD_REGION_WINDOW");
    @Override
    public FoldRegion createFoldRegion(int startOffset, int endOffset,  String placeholder, FoldingGroup group, boolean neverExpands) {
        TextRange hostRange = myDocumentWindow.injectedToHost(new TextRange(startOffset, endOffset));
        if (hostRange.getLength() < 2) return null;
        FoldRegion hostRegion = myDelegate.createFoldRegion(hostRange.getStartOffset(), hostRange.getEndOffset(), placeholder, group, neverExpands);
        int startShift = Math.max(0, myDocumentWindow.hostToInjected(hostRange.getStartOffset()) - startOffset);
        int endShift = Math.max(0, endOffset - myDocumentWindow.hostToInjected(hostRange.getEndOffset()) - startShift);
        FoldingRegionWindow window = new FoldingRegionWindow(myDocumentWindow, myEditorWindow, hostRegion, startShift, endShift);
        hostRegion.putUserData(FOLD_REGION_WINDOW, window);
        return window;
    }

    @Override
    public void addListener( FoldingListener listener,  Disposable parentDisposable) {
        myDelegate.addListener(listener, parentDisposable);
    }

    @Override
    public void rebuild() {
        myDelegate.rebuild();
    }

    @Override
    public void clearFoldRegions() {
        myDelegate.clearFoldRegions();
    }
}
