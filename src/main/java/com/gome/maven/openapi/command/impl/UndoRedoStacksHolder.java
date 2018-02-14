/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.command.impl;

import com.gome.maven.openapi.command.undo.DocumentReference;
import com.gome.maven.openapi.command.undo.DocumentReferenceManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.containers.WeakList;
import gnu.trove.THashSet;

import java.util.*;

class UndoRedoStacksHolder {
    private final Key<LinkedList<UndoableGroup>> STACK_IN_DOCUMENT_KEY = Key.create("STACK_IN_DOCUMENT_KEY");

    private final boolean myUndo;

    private final LinkedList<UndoableGroup> myGlobalStack = new LinkedList<UndoableGroup>();
    // strongly reference local files for which we can undo file removal
    // document without files and nonlocal files are stored without strong reference
    private final Map<DocumentReference, LinkedList<UndoableGroup>> myDocumentStacks = new HashMap<DocumentReference, LinkedList<UndoableGroup>>();
    private final WeakList<Document> myDocumentsWithStacks = new WeakList<Document>();
    private final WeakList<VirtualFile> myNonlocalVirtualFilesWithStacks = new WeakList<VirtualFile>();

    public UndoRedoStacksHolder(boolean isUndo) {
        myUndo = isUndo;
    }

    
    LinkedList<UndoableGroup> getStack( DocumentReference r) {
        return r.getFile() != null ? doGetStackForFile(r) : doGetStackForDocument(r);
    }

    
    private LinkedList<UndoableGroup> doGetStackForFile( DocumentReference r) {
        LinkedList<UndoableGroup> result;
        VirtualFile file = r.getFile();

        if (!file.isInLocalFileSystem()) {
            result = addWeaklyTrackedEmptyStack(file, myNonlocalVirtualFilesWithStacks);
        }
        else {
            result = myDocumentStacks.get(r);
            if (result == null) {
                result = new LinkedList<UndoableGroup>();
                myDocumentStacks.put(r, result);
            }
        }

        return result;
    }

    
    private LinkedList<UndoableGroup> doGetStackForDocument( DocumentReference r) {
        // If document is not associated with file, we have to store its stack in document
        // itself to avoid memory leaks caused by holding stacks of all documents, ever created, here.
        // And to know, what documents do exist now, we have to maintain weak reference list of them.

        return addWeaklyTrackedEmptyStack(r.getDocument(), myDocumentsWithStacks);
    }

    private <T extends UserDataHolder> LinkedList<UndoableGroup> addWeaklyTrackedEmptyStack(T holder, WeakList<T> allHolders) {
        LinkedList<UndoableGroup> result;
        result = holder.getUserData(STACK_IN_DOCUMENT_KEY);
        if (result == null) {
            holder.putUserData(STACK_IN_DOCUMENT_KEY, result = new LinkedList<UndoableGroup>());
            allHolders.add(holder);
        }
        return result;
    }

    public boolean canBeUndoneOrRedone( Collection<DocumentReference> refs) {
        if (refs.isEmpty()) return !myGlobalStack.isEmpty() && myGlobalStack.getLast().isValid();
        for (DocumentReference each : refs) {
            if (!getStack(each).isEmpty() && getStack(each).getLast().isValid()) return true;
        }
        return false;
    }

    
    public UndoableGroup getLastAction(Collection<DocumentReference> refs) {
        if (refs.isEmpty()) return myGlobalStack.getLast();

        UndoableGroup mostRecentAction = null;
        int mostRecentDocTimestamp = myUndo ? -1 : Integer.MAX_VALUE;

        for (DocumentReference each : refs) {
            LinkedList<UndoableGroup> stack = getStack(each);
            // the stack for a document can be empty in case of compound editors with several documents
            if (stack.isEmpty()) continue;
            UndoableGroup lastAction = stack.getLast();

            int timestamp = lastAction.getCommandTimestamp();
            if (myUndo ? timestamp > mostRecentDocTimestamp : timestamp < mostRecentDocTimestamp) {
                mostRecentAction = lastAction;
                mostRecentDocTimestamp = timestamp;
            }
        }

        // result must not be null
        return mostRecentAction;
    }

    
    public Set<DocumentReference> collectClashingActions( UndoableGroup group) {
        Set<DocumentReference> result = new THashSet<DocumentReference>();

        for (DocumentReference each : group.getAffectedDocuments()) {
            UndoableGroup last = getStack(each).getLast();
            if (last != group) {
                result.addAll(last.getAffectedDocuments());
            }
        }

        if (group.isGlobal()) {
            UndoableGroup last = myGlobalStack.getLast();
            if (last != group) {
                result.addAll(last.getAffectedDocuments());
            }
        }

        return result;
    }

    public void addToStacks( UndoableGroup group) {
        for (LinkedList<UndoableGroup> each : getAffectedStacks(group)) {
            doAddToStack(each, group, each == myGlobalStack ? UndoManagerImpl.getGlobalUndoLimit() : UndoManagerImpl.getDocumentUndoLimit());
        }
    }

    private void doAddToStack( LinkedList<UndoableGroup> stack,  UndoableGroup group, int limit) {
        if (!group.isUndoable() && stack.isEmpty()) return;

        stack.addLast(group);
        while (stack.size() > limit) {
            clearStacksFrom(stack.getFirst());
        }
    }

    public void removeFromStacks( UndoableGroup group) {
        for (LinkedList<UndoableGroup> each : getAffectedStacks(group)) {
            assert each.getLast() == group;
            each.removeLast();
        }
    }

    public void clearStacks(boolean clearGlobal,  Set<DocumentReference> refs) {
        for (LinkedList<UndoableGroup> each : getAffectedStacks(clearGlobal, refs)) {
            while(!each.isEmpty()) {
                clearStacksFrom(each.getLast());
            }
        }

        Set<DocumentReference> stacksToDrop = new THashSet<DocumentReference>();
        for (Map.Entry<DocumentReference, LinkedList<UndoableGroup>> each : myDocumentStacks.entrySet()) {
            if (each.getValue().isEmpty()) stacksToDrop.add(each.getKey());
        }
        for (DocumentReference each : stacksToDrop) {
            myDocumentStacks.remove(each);
        }


        cleanWeaklyTrackedEmptyStacks(myDocumentsWithStacks);
        cleanWeaklyTrackedEmptyStacks(myNonlocalVirtualFilesWithStacks);
    }

    private <T extends UserDataHolder> void cleanWeaklyTrackedEmptyStacks(WeakList<T> stackHolders) {
        Set<T> holdersToDrop = new THashSet<T>();
        for (T holder : stackHolders) {
            LinkedList<UndoableGroup> stack = holder.getUserData(STACK_IN_DOCUMENT_KEY);
            if (stack != null && stack.isEmpty()) {
                holder.putUserData(STACK_IN_DOCUMENT_KEY, null);
                holdersToDrop.add(holder);
            }
        }
        stackHolders.removeAll(holdersToDrop);
    }

    private void clearStacksFrom( UndoableGroup from) {
        for (LinkedList<UndoableGroup> each : getAffectedStacks(from)) {
            int pos = each.indexOf(from);
            if (pos == -1) continue;

            if (pos > 0) {
                int top = each.size() - pos;
                clearStacksFrom(each.get(pos - 1));
                assert each.size() == top && each.indexOf(from) == 0;
            }
            each.removeFirst();
        }
    }

    
    private List<LinkedList<UndoableGroup>> getAffectedStacks( UndoableGroup group) {
        return getAffectedStacks(group.isGlobal(), group.getAffectedDocuments());
    }

    
    private List<LinkedList<UndoableGroup>> getAffectedStacks(boolean global,  Collection<DocumentReference> refs) {
        List<LinkedList<UndoableGroup>> result = new ArrayList<LinkedList<UndoableGroup>>(refs.size() + 1);
        if (global) result.add(myGlobalStack);
        for (DocumentReference each : refs) {
            result.add(getStack(each));
        }
        return result;
    }

    public void clearAllStacksInTests() {
        clearStacks(true, getAffectedDocuments());
    }

    public void collectAllAffectedDocuments( Collection<DocumentReference> result) {
        for (UndoableGroup each : myGlobalStack) {
            result.addAll(each.getAffectedDocuments());
        }
        collectLocalAffectedDocuments(result);
    }

    private void collectLocalAffectedDocuments( Collection<DocumentReference> result) {
        result.addAll(myDocumentStacks.keySet());
        DocumentReferenceManager documentReferenceManager = DocumentReferenceManager.getInstance();

        for (Document each : myDocumentsWithStacks) {
            result.add(documentReferenceManager.create(each));
        }
        for (VirtualFile each : myNonlocalVirtualFilesWithStacks) {
            result.add(documentReferenceManager.create(each));
        }
    }

    
    private Set<DocumentReference> getAffectedDocuments() {
        Set<DocumentReference> result = new THashSet<DocumentReference>();
        collectAllAffectedDocuments(result);
        return result;
    }

    public int getLastCommandTimestamp( DocumentReference r) {
        LinkedList<UndoableGroup> stack = getStack(r);
        if (stack.isEmpty()) return 0;
        return Math.max(stack.getFirst().getCommandTimestamp(), stack.getLast().getCommandTimestamp());
    }

    public void invalidateActionsFor( DocumentReference ref) {
        for (LinkedList<UndoableGroup> eachStack : getAffectedStacks(true, Collections.singleton(ref))) {
            for (UndoableGroup eachGroup : eachStack) {
                eachGroup.invalidateActionsFor(ref);
            }
        }
    }
}
