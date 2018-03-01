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

import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.codeInspection.ProblemDescriptorUtil;
import com.gome.maven.openapi.actionSystem.Anchor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.spellchecker.SpellCheckerManager;
import com.gome.maven.spellchecker.util.SpellCheckerBundle;
import icons.SpellcheckerIcons;

import javax.swing.*;


public class AcceptWordAsCorrect implements SpellCheckerQuickFix {
    private String myWord;

    public AcceptWordAsCorrect(String word) {
        myWord = word;
    }

    public AcceptWordAsCorrect() {
    }

    
    public String getName() {
        return myWord != null ? SpellCheckerBundle.message("add.0.to.dictionary", myWord) : SpellCheckerBundle.message("add.to.dictionary");
    }

    
    public String getFamilyName() {
        return SpellCheckerBundle.message("spelling");
    }

    
    public Anchor getPopupActionAnchor() {
        return Anchor.LAST;
    }

    public void applyFix( Project project,  ProblemDescriptor descriptor) {
        SpellCheckerManager spellCheckerManager = SpellCheckerManager.getInstance(project);
        if (myWord != null) {
            spellCheckerManager.acceptWordAsCorrect(myWord, project);
        } else {
            spellCheckerManager.acceptWordAsCorrect(ProblemDescriptorUtil.extractHighlightedText(descriptor, descriptor.getPsiElement()), project);
        }
    }

    public Icon getIcon(int flags) {
        return SpellcheckerIcons.Spellcheck;
    }
}
