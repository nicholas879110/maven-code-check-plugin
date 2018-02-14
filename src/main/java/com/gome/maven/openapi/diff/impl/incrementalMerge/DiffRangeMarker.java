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
package com.gome.maven.openapi.diff.impl.incrementalMerge;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Iterator;
import java.util.Map;

class DiffRangeMarker implements RangeMarker {
    private final RangeMarker myRangeMarker;

    DiffRangeMarker( Document document,  TextRange range, RangeInvalidListener listener) {
        myRangeMarker = document.createRangeMarker(range.getStartOffset(), range.getEndOffset());
        if (listener != null) {
            InvalidRangeDispatcher.addClient(document, this, listener);
        }
    }

    public void removeListener( RangeInvalidListener listener) {
        InvalidRangeDispatcher.removeClient(getDocument(), this, listener);
    }

    interface RangeInvalidListener {
        void onRangeInvalidated();
    }

    private static class InvalidRangeDispatcher extends DocumentAdapter {
        private static final Key<InvalidRangeDispatcher> KEY = Key.create("deferedNotifier");
        private final Map<DiffRangeMarker, RangeInvalidListener> myDiffRangeMarkers = ContainerUtil.newConcurrentMap();

        @Override
        public void documentChanged(DocumentEvent e) {
            for (Iterator<Map.Entry<DiffRangeMarker, RangeInvalidListener>> iterator = myDiffRangeMarkers.entrySet().iterator();
                 iterator.hasNext(); ) {
                Map.Entry<DiffRangeMarker, RangeInvalidListener> entry = iterator.next();
                DiffRangeMarker diffRangeMarker = entry.getKey();
                RangeInvalidListener listener = entry.getValue();
                if (!diffRangeMarker.isValid() && listener != null) {
                    listener.onRangeInvalidated();
                    iterator.remove();
                }
            }
        }

        private static void addClient( Document document,
                                       DiffRangeMarker marker,
                                       RangeInvalidListener listener) {
            InvalidRangeDispatcher notifier = document.getUserData(KEY);
            if (notifier == null) {
                notifier = new InvalidRangeDispatcher();
                document.putUserData(KEY, notifier);
                document.addDocumentListener(notifier);
            }
            assert !notifier.myDiffRangeMarkers.containsKey(marker);
            notifier.myDiffRangeMarkers.put(marker, listener);
        }

        private static void removeClient( Document document,
                                          DiffRangeMarker marker,
                                          RangeInvalidListener listener) {
            InvalidRangeDispatcher notifier = document.getUserData(KEY);
            assert notifier != null;
            notifier.onClientRemoved(document, marker, listener);
        }

        private void onClientRemoved( Document document,  DiffRangeMarker marker,  RangeInvalidListener listener) {
            if (myDiffRangeMarkers.remove(marker) == listener && myDiffRangeMarkers.isEmpty()) {
                document.putUserData(KEY, null);
                document.removeDocumentListener(this);
            }
        }
    }

    /// delegates

    @Override
    
    public Document getDocument() {
        return myRangeMarker.getDocument();
    }

    @Override
    public int getStartOffset() {
        return myRangeMarker.getStartOffset();
    }

    @Override
    public int getEndOffset() {
        return myRangeMarker.getEndOffset();
    }

    @Override
    public boolean isValid() {
        return myRangeMarker.isValid();
    }

    @Override
    public void setGreedyToLeft(boolean greedy) {
        myRangeMarker.setGreedyToLeft(greedy);
    }

    @Override
    public void setGreedyToRight(boolean greedy) {
        myRangeMarker.setGreedyToRight(greedy);
    }

    @Override
    public boolean isGreedyToRight() {
        return myRangeMarker.isGreedyToRight();
    }

    @Override
    public boolean isGreedyToLeft() {
        return myRangeMarker.isGreedyToLeft();
    }

    @Override
    public void dispose() {
        myRangeMarker.dispose();
    }

    @Override
    
    public <T> T getUserData( Key<T> key) {
        return myRangeMarker.getUserData(key);
    }

    @Override
    public <T> void putUserData( Key<T> key,  T value) {
        myRangeMarker.putUserData(key, value);
    }
}
