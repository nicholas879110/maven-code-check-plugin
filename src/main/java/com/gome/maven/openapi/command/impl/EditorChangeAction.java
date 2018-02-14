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
package com.gome.maven.openapi.command.impl;

import com.gome.maven.openapi.command.undo.BasicUndoableAction;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.ex.DocumentEx;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vcs.impl.FileStatusManagerImpl;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.CompressionUtil;

public class EditorChangeAction extends BasicUndoableAction {
    private final int myOffset;
    private final Object myOldString;
    private final Object myNewString;
    private final long myOldTimeStamp;
    private final long myNewTimeStamp;

    public EditorChangeAction(DocumentEvent e) {
        this((DocumentEx)e.getDocument(), e.getOffset(), e.getOldFragment(), e.getNewFragment(), e.getOldTimeStamp());
    }

    public EditorChangeAction( DocumentEx document,
                              int offset,
                               CharSequence oldString,
                               CharSequence newString,
                              long oldTimeStamp) {
        super(document);
        myOffset = offset;
        myOldString = CompressionUtil.compressStringRawBytes(oldString);
        myNewString = CompressionUtil.compressStringRawBytes(newString);
        myOldTimeStamp = oldTimeStamp;
        myNewTimeStamp = document.getModificationStamp();
    }

    @Override
    public void undo() {
        DocumentUndoProvider.startDocumentUndo(getDocument());
        try {
            performUndo();
        }
        finally {
            DocumentUndoProvider.finishDocumentUndo(getDocument());
        }

        getDocument().setModificationStamp(myOldTimeStamp);
        refreshFileStatus();
    }

    public void performUndo() {
        CharSequence oldString = CompressionUtil.uncompressStringRawBytes(myOldString);
        CharSequence newString = CompressionUtil.uncompressStringRawBytes(myNewString);
        exchangeStrings(newString, oldString);
    }

    @Override
    public void redo() {
        DocumentUndoProvider.startDocumentUndo(getDocument());
        try {
            CharSequence oldString = CompressionUtil.uncompressStringRawBytes(myOldString);
            CharSequence newString = CompressionUtil.uncompressStringRawBytes(myNewString);
            exchangeStrings(oldString, newString);
        }
        finally {
            DocumentUndoProvider.finishDocumentUndo(getDocument());
        }
        getDocument().setModificationStamp(myNewTimeStamp);
        refreshFileStatus();
    }

    private void exchangeStrings( CharSequence newString,  CharSequence oldString) {
        DocumentEx d = getDocument();

        if (newString.length() > 0 && oldString.length() == 0) {
            d.deleteString(myOffset, myOffset + newString.length());
        }
        else if (oldString.length() > 0 && newString.length() == 0) {
            d.insertString(myOffset, oldString);
        }
        else if (oldString.length() > 0 && newString.length() > 0) {
            d.replaceString(myOffset, myOffset + newString.length(), oldString);
        }
    }

    private void refreshFileStatus() {
        VirtualFile f = getAffectedDocuments()[0].getFile();
        if (f == null || f instanceof LightVirtualFile) return;

        for (Project each : ProjectManager.getInstance().getOpenProjects()) {
            FileStatusManagerImpl statusManager = (FileStatusManagerImpl)FileStatusManager.getInstance(each);
            statusManager.refreshFileStatusFromDocument(f, getDocument());
        }
    }


    private DocumentEx getDocument() {
        return (DocumentEx)getAffectedDocuments()[0].getDocument();
    }

    @Override
    public String toString() {
        return "editor change: '" + myOldString + "' to '" + myNewString + "'" + " at: " + myOffset;
    }
}

