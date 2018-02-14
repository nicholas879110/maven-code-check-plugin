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
package com.gome.maven.diff.actions;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.project.Project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

abstract class DocumentsSynchronizer {
     protected final Document myDocument1;
     protected final Document myDocument2;
     private final Project myProject;

    private volatile boolean myDuringModification = false;

    private final DocumentAdapter myListener1 = new DocumentAdapter() {
        @Override
        public void documentChanged(DocumentEvent e) {
            if (myDuringModification) return;
            onDocumentChanged1(e);
        }
    };

    private final DocumentAdapter myListener2 = new DocumentAdapter() {
        @Override
        public void documentChanged(DocumentEvent e) {
            if (myDuringModification) return;
            onDocumentChanged2(e);
        }
    };

    private final PropertyChangeListener myROListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (Document.PROP_WRITABLE.equals(evt.getPropertyName())) getDocument2().setReadOnly(!getDocument1().isWritable());
        }
    };

    protected DocumentsSynchronizer( Project project,  Document document1,  Document document2) {
        myProject = project;
        myDocument1 = document1;
        myDocument2 = document2;
    }

    
    public Document getDocument1() {
        return myDocument1;
    }

    
    public Document getDocument2() {
        return myDocument2;
    }

    protected abstract void onDocumentChanged1( DocumentEvent event);

    protected abstract void onDocumentChanged2( DocumentEvent event);

//    @CalledInAwt
    protected void replaceString( final Document document,
                                 final int startOffset,
                                 final int endOffset,
                                  final CharSequence newText) {
        try {
            myDuringModification = true;
            CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
                @Override
                public void run() {
                    assert endOffset <= document.getTextLength();
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            document.replaceString(startOffset, endOffset, newText);
                        }
                    });
                }
            }, "Synchronize document and its fragment", document);
        }
        finally {
            myDuringModification = false;
        }
    }

    public void startListen() {
        myDocument1.addDocumentListener(myListener1);
        myDocument2.addDocumentListener(myListener2);
        myDocument1.addPropertyChangeListener(myROListener);
    }

    public void stopListen() {
        myDocument1.removeDocumentListener(myListener1);
        myDocument2.removeDocumentListener(myListener2);
        myDocument1.removePropertyChangeListener(myROListener);
    }
}