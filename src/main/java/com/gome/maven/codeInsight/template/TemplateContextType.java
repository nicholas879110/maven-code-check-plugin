/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.template;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighter;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;

/**
 * @author yole
 */
public abstract class TemplateContextType {
    public static final ExtensionPointName<TemplateContextType> EP_NAME = ExtensionPointName.create("com.intellij.liveTemplateContext");

    private final String myContextId;
    private final String myPresentableName;
    private final Class<? extends TemplateContextType> myBaseContextType;

    protected TemplateContextType(  String id,  String presentableName) {
        this(id, presentableName, EverywhereContextType.class);
    }

    protected TemplateContextType(  String id,
                                   String presentableName,
                                   Class<? extends TemplateContextType> baseContextType) {
        myContextId = id;
        myPresentableName = presentableName;
        myBaseContextType = baseContextType;
    }

    public String getPresentableName() {
        return myPresentableName;
    }

    public String getContextId() {
        return myContextId;
    }

    public abstract boolean isInContext( PsiFile file, int offset);

    /**
     * @return whether an abbreviation of this context's template can be entered in editor
     * and expanded from there by Insert Live Template action
     */
    public boolean isExpandableFromEditor() {
        return true;
    }

    
    public SyntaxHighlighter createHighlighter() {
        return null;
    }

    
    public TemplateContextType getBaseContextType() {
        return myBaseContextType != null ? EP_NAME.findExtension(myBaseContextType) : null;
    }

    public Document createDocument(CharSequence text, Project project) {
        return EditorFactory.getInstance().createDocument(text);
    }
}
