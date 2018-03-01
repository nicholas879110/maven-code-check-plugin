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
package com.gome.maven.psi.filters.getters;

import com.gome.maven.codeInsight.completion.InsertionContext;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupElementPresentation;
import com.gome.maven.codeInsight.lookup.TypedLookupItem;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;

/**
 * @author peter
 */
public class ClassLiteralLookupElement extends LookupElement implements TypedLookupItem {
     private static final String DOT_CLASS = ".class";
    private final PsiExpression myExpr;
    private final String myPresentableText;
    private final String myCanonicalText;

    public ClassLiteralLookupElement(PsiClassType type, PsiElement context) {
        myCanonicalText = type.getCanonicalText();
        myPresentableText = type.getPresentableText();
        myExpr = JavaPsiFacade.getInstance(context.getProject()).getElementFactory().createExpressionFromText(myCanonicalText + DOT_CLASS, context);
    }

    
    @Override
    public String getLookupString() {
        return myPresentableText + ".class";
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(myExpr.getIcon(0));
        String pkg = StringUtil.getPackageName(myCanonicalText);
        if (StringUtil.isNotEmpty(pkg)) {
            presentation.setTailText(" (" + pkg + ")", true);
        }
    }

    
    @Override
    public Object getObject() {
        return myExpr;
    }

    @Override
    public PsiType getType() {
        return myExpr.getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassLiteralLookupElement)) return false;

        return myCanonicalText.equals(((ClassLiteralLookupElement)o).myCanonicalText);
    }

    @Override
    public int hashCode() {
        return myCanonicalText.hashCode();
    }

    @Override
    public void handleInsert(InsertionContext context) {
        final Document document = context.getEditor().getDocument();
        document.replaceString(context.getStartOffset(), context.getTailOffset(), myCanonicalText + DOT_CLASS);
        final Project project = context.getProject();
        PsiDocumentManager.getInstance(project).commitDocument(document);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(context.getFile(), context.getStartOffset(), context.getTailOffset());
    }
}
