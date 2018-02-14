/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.impl.event.DocumentEventImpl;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiModificationTracker;

import java.util.LinkedList;

/**
 * @author peter
 */
public class OffsetTranslator implements Disposable {
    static final Key<OffsetTranslator> RANGE_TRANSLATION = Key.create("completion.rangeTranslation");

    private final Document myCopyDocument;
    private final LinkedList<DocumentEvent> myTranslation = new LinkedList<DocumentEvent>();

    public OffsetTranslator(final Document originalDocument, final PsiFile originalFile, Document copyDocument) {
        myCopyDocument = copyDocument;
        myCopyDocument.putUserData(RANGE_TRANSLATION, this);

        final LinkedList<DocumentEvent> sinceCommit = new LinkedList<DocumentEvent>();
        originalDocument.addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent e) {
                if (isUpToDate()) {
                    DocumentEventImpl inverse =
                            new DocumentEventImpl(originalDocument, e.getOffset(), e.getNewFragment(), e.getOldFragment(), 0, false);
                    sinceCommit.addLast(inverse);
                }
            }
        }, this);

        myCopyDocument.addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent e) {
                if (isUpToDate()) {
                    myTranslation.addFirst(e);
                }
            }
        });

        originalFile.getProject().getMessageBus().connect(this).subscribe(PsiModificationTracker.TOPIC, new PsiModificationTracker.Listener() {
            long lastModCount = originalFile.getViewProvider().getModificationStamp();
            @Override
            public void modificationCountChanged() {
                if (isUpToDate() && lastModCount != originalFile.getViewProvider().getModificationStamp()) {
                    myTranslation.addAll(sinceCommit);
                    sinceCommit.clear();
                }
            }
        });

    }

    private boolean isUpToDate() {
        return this == myCopyDocument.getUserData(RANGE_TRANSLATION);
    }

    @Override
    public void dispose() {
        if (isUpToDate()) {
            myCopyDocument.putUserData(RANGE_TRANSLATION, null);
        }
    }

    
    public Integer translateOffset(Integer offset) {
        for (DocumentEvent event : myTranslation) {
            offset = translateOffset(offset, event);
            if (offset == null) {
                return null;
            }
        }
        return offset;
    }

    
    private static Integer translateOffset(int offset, DocumentEvent event) {
        if (event.getOffset() < offset && offset < event.getOffset() + event.getNewLength()) {
            if (event.getOldLength() == 0) {
                return event.getOffset();
            }

            return null;
        }

        return offset <= event.getOffset() ? offset : offset - event.getNewLength() + event.getOldLength();
    }

}
