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
package com.gome.maven.codeInsight.documentation;

import com.gome.maven.openapi.preview.PreviewPanelProvider;
import com.gome.maven.openapi.preview.PreviewProviderId;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.psi.PsiElement;

import javax.swing.*;

public class DocumentationPreviewPanelProvider extends PreviewPanelProvider<Couple<PsiElement>, DocumentationComponent> {
    public static final PreviewProviderId<Couple<PsiElement>, DocumentationComponent> ID = PreviewProviderId.create("Documentation");
    private final DocumentationComponent myDocumentationComponent;
    private final DocumentationManager myDocumentationManager;

    public DocumentationPreviewPanelProvider(DocumentationManager documentationManager) {
        super(ID);
        myDocumentationManager = documentationManager;
        myDocumentationComponent = new DocumentationComponent(documentationManager) {
            @Override
            public String toString() {
                return "Preview DocumentationComponent (" + (isEmpty() ? "empty" : "not empty") + ")";
            }
        };
    }

    @Override
    public void dispose() {
        Disposer.dispose(myDocumentationComponent);
    }

    
    @Override
    protected JComponent getComponent() {
        return myDocumentationComponent;
    }

    
    @Override
    protected String getTitle( Couple<PsiElement> content) {
        return DocumentationManager.getTitle(content.getFirst(), false);
    }

    
    @Override
    protected Icon getIcon( Couple<PsiElement> content) {
        return content.getFirst().getIcon(0);
    }

    @Override
    public float getMenuOrder() {
        return 1;
    }

    @Override
    public void showInStandardPlace( Couple<PsiElement> content) {
        myDocumentationManager.showJavaDocInfo(content.getFirst(), content.getSecond());
    }

    @Override
    public void release( Couple<PsiElement> content) {
    }

    @Override
    public boolean contentsAreEqual( Couple<PsiElement> content1,  Couple<PsiElement> content2) {
        return content1.getFirst().getManager().areElementsEquivalent(content1.getFirst(), content2.getFirst());
    }

    @Override
    public boolean isModified(Couple<PsiElement> content, boolean beforeReuse) {
        return beforeReuse;
    }

    @Override
    protected DocumentationComponent initComponent(Couple<PsiElement> content, boolean requestFocus) {
        if (!content.getFirst().getManager().areElementsEquivalent(myDocumentationComponent.getElement(), content.getFirst())) {
            myDocumentationManager.fetchDocInfo(content.getFirst(), myDocumentationComponent);
        }
        return myDocumentationComponent;
    }
}
