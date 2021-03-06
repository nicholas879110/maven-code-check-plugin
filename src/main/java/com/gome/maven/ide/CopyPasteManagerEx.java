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
package com.gome.maven.ide;

import com.gome.maven.ide.ui.UISettings;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.ide.CopyPasteManager;
import com.gome.maven.openapi.ide.CutElementMarker;
import com.gome.maven.openapi.ide.KillRingTransferable;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.util.EventDispatcher;

import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CopyPasteManagerEx extends CopyPasteManager implements ClipboardOwner {
    private final List<Transferable> myData = new ArrayList<Transferable>();
    private final EventDispatcher<ContentChangedListener> myDispatcher = EventDispatcher.create(ContentChangedListener.class);
    private final ClipboardSynchronizer myClipboardSynchronizer;
    private boolean myOwnContent = false;

    public static CopyPasteManagerEx getInstanceEx() {
        return (CopyPasteManagerEx)getInstance();
    }

    public CopyPasteManagerEx(ClipboardSynchronizer clipboardSynchronizer) {
        myClipboardSynchronizer = clipboardSynchronizer;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        myOwnContent = false;
        myClipboardSynchronizer.resetContent();
        fireContentChanged(contents, null);
    }

    private void fireContentChanged( Transferable oldContent,  Transferable newContent) {
        myDispatcher.getMulticaster().contentChanged(oldContent, newContent);
    }

    @Override
    public void addContentChangedListener( ContentChangedListener listener) {
        myDispatcher.addListener(listener);
    }

    @Override
    public void addContentChangedListener( final ContentChangedListener listener,  Disposable parentDisposable) {
        myDispatcher.addListener(listener, parentDisposable);
    }

    @Override
    public void removeContentChangedListener( ContentChangedListener listener) {
        myDispatcher.removeListener(listener);
    }

    @Override
    public boolean areDataFlavorsAvailable( DataFlavor... flavors) {
        return flavors.length > 0 &&  myClipboardSynchronizer.areDataFlavorsAvailable(flavors);
    }

    @Override
    public void setContents( Transferable content) {
        Transferable oldContent = myOwnContent && !myData.isEmpty() ? myData.get(0) : null;

        Transferable contentToUse = addNewContentToStack(content);
        setSystemClipboardContent(contentToUse);

        fireContentChanged(oldContent, contentToUse);
    }

    @Override
    public boolean isCutElement( final Object element) {
        for (CutElementMarker marker : Extensions.getExtensions(CutElementMarker.EP_NAME)) {
            if (marker.isCutElement(element)) return true;
        }
        return false;
    }

    @Override
    public void stopKillRings() {
        for (Transferable data : myData) {
            if (data instanceof KillRingTransferable) {
                ((KillRingTransferable)data).setReadyToCombine(false);
            }
        }
    }

    private void setSystemClipboardContent(Transferable content) {
        myClipboardSynchronizer.setContent(content, this);
        myOwnContent = true;
    }

    /**
     * Stores given content within the current manager. It is merged with already stored ones
     * if necessary (see {@link KillRingTransferable}).
     *
     * @param content content to store
     * @return content that is either the given one or the one that was assembled from it and already stored one
     */
    
    private Transferable addNewContentToStack( Transferable content) {
        try {
            String clipString = getStringContent(content);
            if (clipString == null) {
                return content;
            }

            if (content instanceof KillRingTransferable) {
                KillRingTransferable killRingContent = (KillRingTransferable)content;
                if (killRingContent.isReadyToCombine() && !myData.isEmpty()) {
                    Transferable prev = myData.get(0);
                    if (prev instanceof KillRingTransferable) {
                        Transferable merged = merge(killRingContent, (KillRingTransferable)prev);
                        if (merged != null) {
                            myData.set(0, merged);
                            return merged;
                        }
                    }
                }
                if (killRingContent.isReadyToCombine()) {
                    addToTheTopOfTheStack(killRingContent);
                    return killRingContent;
                }
            }

            Transferable same = null;
            for (Transferable old : myData) {
                if (clipString.equals(getStringContent(old))) {
                    same = old;
                    break;
                }
            }

            if (same == null) {
                addToTheTopOfTheStack(content);
            }
            else {
                moveContentToStackTop(same, false); // notification is done in setContents() method
            }
        }
        catch (UnsupportedFlavorException ignore) { }
        catch (IOException ignore) { }
        return content;
    }

    private void addToTheTopOfTheStack( Transferable content) {
        myData.add(0, content);
        deleteAfterAllowedMaximum();
    }

    /**
     * Merges given new data with the given old one and returns merge result in case of success.
     *
     * @param newData new data to merge
     * @param oldData old data to merge
     * @return merge result of the given data if possible; <code>null</code> otherwise
     * @throws IOException                as defined by {@link Transferable#getTransferData(DataFlavor)}
     * @throws UnsupportedFlavorException as defined by {@link Transferable#getTransferData(DataFlavor)}
     */
    
    private static Transferable merge( KillRingTransferable newData,  KillRingTransferable oldData)
            throws IOException, UnsupportedFlavorException {
        if (!oldData.isReadyToCombine() || !newData.isReadyToCombine()) {
            return null;
        }

        Document document = newData.getDocument();
        if (document == null || document != oldData.getDocument()) {
            return null;
        }

        Object newDataText = newData.getTransferData(DataFlavor.stringFlavor);
        Object oldDataText = oldData.getTransferData(DataFlavor.stringFlavor);
        if (newDataText == null || oldDataText == null) {
            return null;
        }

        if (oldData.isCut()) {
            if (newData.getStartOffset() == oldData.getStartOffset()) {
                return new KillRingTransferable(
                        oldDataText.toString() + newDataText, document, oldData.getStartOffset(), newData.getEndOffset(), newData.isCut()
                );
            }
        }

        if (newData.getStartOffset() == oldData.getEndOffset()) {
            return new KillRingTransferable(
                    oldDataText.toString() + newDataText, document, oldData.getStartOffset(), newData.getEndOffset(), false
            );
        }

        if (newData.getEndOffset() == oldData.getStartOffset()) {
            return new KillRingTransferable(
                    newDataText.toString() + oldDataText, document, newData.getStartOffset(), oldData.getEndOffset(), false
            );
        }

        return null;
    }

    private static String getStringContent(Transferable content) {
        try {
            return (String)content.getTransferData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException ignore) { }
        catch (IOException ignore) { }
        return null;
    }

    private void deleteAfterAllowedMaximum() {
        int max = UISettings.getInstance().MAX_CLIPBOARD_CONTENTS;
        for (int i = myData.size() - 1; i >= max; i--) {
            myData.remove(i);
        }
    }

    @Override
    public Transferable getContents() {
        return myClipboardSynchronizer.getContents();
    }

    
    @Override
    public <T> T getContents( DataFlavor flavor) {
        if (areDataFlavorsAvailable(flavor)) {
            try {
                Transferable contents = getContents();
                if (contents != null) {
                    @SuppressWarnings("unchecked") T data = (T)contents.getTransferData(flavor);
                    return data;
                }
            }
            catch (UnsupportedFlavorException ignore) { }
            catch (IOException ignore) { }
        }

        return null;
    }

    
    @Override
    public Transferable[] getAllContents() {
        String clipString = getContents(DataFlavor.stringFlavor);
        if (clipString != null && (myData.isEmpty() || !Comparing.equal(clipString, getStringContent(myData.get(0))))) {
            addToTheTopOfTheStack(new StringSelection(clipString));
        }
        return myData.toArray(new Transferable[myData.size()]);
    }

    public void removeContent(Transferable t) {
        Transferable current = myData.isEmpty() ? null : myData.get(0);
        myData.remove(t);
        if (Comparing.equal(t, current)) {
            Transferable newContent = !myData.isEmpty() ? myData.get(0) : new StringSelection("");
            setSystemClipboardContent(newContent);
            fireContentChanged(current, newContent);
        }
    }

    /** @deprecated use {@link #moveContentToStackTop(Transferable)} (to remove in IDEA 14) */
    @SuppressWarnings("UnusedDeclaration")
    public void moveContentTopStackTop(Transferable t) {
        moveContentToStackTop(t);
    }

    public void moveContentToStackTop(Transferable t) {
        moveContentToStackTop(t, true);
    }

    private void moveContentToStackTop(Transferable t, boolean notifyOthers) {
        Transferable current = myData.isEmpty() ? null : myData.get(0);
        if (!Comparing.equal(t, current)) {
            myData.remove(t);
            myData.add(0, t);
            if (notifyOthers) {
                setSystemClipboardContent(t);
                fireContentChanged(current, t);
            }
        }
    }
}
