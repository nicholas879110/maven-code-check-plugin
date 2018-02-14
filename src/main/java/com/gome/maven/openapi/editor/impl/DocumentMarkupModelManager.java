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
package com.gome.maven.openapi.editor.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.AbstractProjectComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.util.containers.WeakList;

/**
 * @author max
 */
public class DocumentMarkupModelManager extends AbstractProjectComponent {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.editor.impl.DocumentMarkupModelManager");

    private final WeakList<Document> myDocumentSet = new WeakList<Document>();
    private volatile boolean myDisposed;

    public static DocumentMarkupModelManager getInstance(Project project) {
        return project.getComponent(DocumentMarkupModelManager.class);
    }

    public DocumentMarkupModelManager( Project project) {
        super(project);
        Disposer.register(project, new Disposable() {
            @Override
            public void dispose() {
                cleanupProjectMarkups();
            }
        });
    }

    public void registerDocument(Document document) {
        LOG.assertTrue(!myDisposed);
        myDocumentSet.add(document);
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    private void cleanupProjectMarkups() {
        if (!myDisposed) {
            myDisposed = true;
            for (Document document : myDocumentSet.toStrongList()) {
                DocumentMarkupModel.removeMarkupModel(document, myProject);
            }
        }
    }
}
