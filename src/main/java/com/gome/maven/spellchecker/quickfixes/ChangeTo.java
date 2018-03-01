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
package com.gome.maven.spellchecker.quickfixes;

import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupElementBuilder;
import com.gome.maven.codeInsight.lookup.LookupManager;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.codeInspection.ProblemDescriptorBase;
import com.gome.maven.openapi.actionSystem.Anchor;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.util.PsiUtilBase;
import com.gome.maven.spellchecker.util.SpellCheckerBundle;

import java.util.ArrayList;
import java.util.List;

public class ChangeTo extends ShowSuggestions implements SpellCheckerQuickFix {

    public ChangeTo(String wordWithTypo) {
        super(wordWithTypo);
    }


    
    public String getName() {
        return SpellCheckerBundle.message("change.to");
    }

    
    public String getFamilyName() {
        return SpellCheckerBundle.message("change.to");
    }

    
    public Anchor getPopupActionAnchor() {
        return Anchor.FIRST;
    }


    public void applyFix( Project project,  ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        if (element == null) return;
        Editor editor = PsiUtilBase.findEditor(element);

        if (editor == null) {
            return;
        }

        TextRange textRange = ((ProblemDescriptorBase)descriptor).getTextRange();
        editor.getSelectionModel().setSelection(textRange.getStartOffset(), textRange.getEndOffset());

        String word = editor.getSelectionModel().getSelectedText();

        if (word == null || StringUtil.isEmpty(word)) {
            return;
        }

        List<LookupElement> lookupItems = new ArrayList<LookupElement>();
        for (String variant : getSuggestions(project)) {
            lookupItems.add(LookupElementBuilder.create(variant));
        }
        LookupElement[] items = new LookupElement[lookupItems.size()];
        items = lookupItems.toArray(items);
        LookupManager lookupManager = LookupManager.getInstance(project);
        lookupManager.showLookup(editor, items);

    }


}