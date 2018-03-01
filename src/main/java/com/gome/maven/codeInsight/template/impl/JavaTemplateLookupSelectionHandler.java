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
package com.gome.maven.codeInsight.template.impl;

import com.gome.maven.codeInsight.template.TemplateLookupSelectionHandler;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.openapi.editor.Document;

/**
 * @author yole
 */
public class JavaTemplateLookupSelectionHandler implements TemplateLookupSelectionHandler {
    @Override
    public void itemSelected(final LookupElement item,
                             final PsiFile psiFile, final Document document, final int segmentStart, final int segmentEnd) {
        JavaTemplateUtil.updateTypeBindings(item.getObject(), psiFile, document, segmentStart, segmentEnd);
    }
}