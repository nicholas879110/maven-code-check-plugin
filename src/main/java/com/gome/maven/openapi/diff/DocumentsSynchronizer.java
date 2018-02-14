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
package com.gome.maven.openapi.diff;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.project.Project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

abstract class DocumentsSynchronizer {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.diff.DocumentsSynchonizer");
    private Document myOriginal = null;
    private Document myCopy = null;
    private final Project myProject;

    private volatile boolean myDuringModification = false;
    private int myAssignedCount = 0;

    private final DocumentAdapter myOriginalListener = new DocumentAdapter() {
        @Override
        public void documentChanged(DocumentEvent e) {
            if (myDuringModification) return;
            onOriginalChanged(e, getCopy());
        }
    };

    private final DocumentAdapter myCopyListener = new DocumentAdapter() {
        @Override
        public void documentChanged(DocumentEvent e) {
            if (myDuringModification) return;
            onCopyChanged(e, getOriginal());
        }
    };
    private final PropertyChangeListener myROListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (Document.PROP_WRITABLE.equals(evt.getPropertyName())) getCopy().setReadOnly(!getOriginal().isWritable());
        }
    };

    protected DocumentsSynchronizer(Project project) {
        myProject = project;
    }

    protected abstract void onCopyChanged( DocumentEvent event,  Document original);

    protected abstract void onOriginalChanged( DocumentEvent event,  Document copy);

    protected abstract void beforeListenersAttached( Document original,  Document copy);

    protected abstract Document createOriginal();

    
    protected abstract Document createCopy();

    protected void replaceString( final Document document, final int startOffset, final int endOffset,  final String newText) {
        LOG.assertTrue(!myDuringModification);
        try {
            myDuringModification = true;
            CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
                @Override
                public void run() {
                    LOG.assertTrue(endOffset <= document.getTextLength());
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            document.replaceString(startOffset, endOffset, newText);
                        }
                    });
                }
            }, DiffBundle.message("save.merge.result.command.name"), document);
        }
        finally {
            myDuringModification = false;
        }
    }

    public void listenDocuments(boolean startListen) {
        int prevAssignedCount = myAssignedCount;
        if (startListen) {
            myAssignedCount++;
        }
        else {
            myAssignedCount--;
        }
        LOG.assertTrue(myAssignedCount >= 0);
        if (prevAssignedCount == 0 && myAssignedCount > 0) startListen();
        if (myAssignedCount == 0 && prevAssignedCount > 0) stopListen();
    }

    private void startListen() {
        final Document original = getOriginal();
        final Document copy = getCopy();
        if (original == null) return;

        beforeListenersAttached(original, copy);
        original.addDocumentListener(myOriginalListener);
        copy.addDocumentListener(myCopyListener);
        original.addPropertyChangeListener(myROListener);
    }


    private void stopListen() {
        if (myOriginal != null) {
            myOriginal.removeDocumentListener(myOriginalListener);
            myOriginal.removePropertyChangeListener(myROListener);
        }

        if (myCopy != null) {
            myCopy.removeDocumentListener(myCopyListener);
        }

        myOriginal = null;
        myCopy = null;
    }

    public Document getOriginal() {
        if (myOriginal == null) myOriginal = createOriginal();
        return myOriginal;
    }

    
    public Document getCopy() {
        if (myCopy == null) myCopy = createCopy();
        return myCopy;
    }
}
