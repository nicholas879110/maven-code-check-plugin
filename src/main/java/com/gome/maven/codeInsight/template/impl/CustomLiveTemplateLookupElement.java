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
package com.gome.maven.codeInsight.template.impl;

import com.gome.maven.codeInsight.completion.InsertionContext;
import com.gome.maven.codeInsight.template.CustomLiveTemplateBase;
import com.gome.maven.codeInsight.template.CustomTemplateCallback;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.psi.PsiFile;

public class CustomLiveTemplateLookupElement extends LiveTemplateLookupElement {
     private final CustomLiveTemplateBase myCustomLiveTemplate;

     private final String myTemplateKey;
     private final String myItemText;

    public CustomLiveTemplateLookupElement( CustomLiveTemplateBase customLiveTemplate,
                                            String templateKey,
                                            String itemText,
                                            String description,
                                           boolean sudden,
                                           boolean worthShowingInAutoPopup) {
        super(templateKey, description, sudden, worthShowingInAutoPopup);
        myCustomLiveTemplate = customLiveTemplate;
        myTemplateKey = templateKey;
        myItemText = itemText;
    }

    
    @Override
    protected String getItemText() {
        return myItemText;
    }

    
    public CustomLiveTemplateBase getCustomLiveTemplate() {
        return myCustomLiveTemplate;
    }

    @Override
    public char getTemplateShortcut() {
        return myCustomLiveTemplate.getShortcut();
    }

    @Override
    public void handleInsert(InsertionContext context) {
        context.setAddCompletionChar(false);
        expandTemplate(context.getEditor(), context.getFile());
    }

    public void expandTemplate( Editor editor,  PsiFile file) {
        myCustomLiveTemplate.expand(myTemplateKey, new CustomTemplateCallback(editor, file));
    }
}
