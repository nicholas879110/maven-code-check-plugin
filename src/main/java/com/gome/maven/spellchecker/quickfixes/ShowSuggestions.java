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

import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.spellchecker.SpellCheckerManager;
import icons.SpellcheckerIcons;

import javax.swing.*;
import java.util.List;


public abstract class ShowSuggestions implements LocalQuickFix, Iconable {

    private List<String> suggestions;
    private boolean processed;
    private final String myWordWithTypo;


    public ShowSuggestions(String wordWithTypo) {
        myWordWithTypo = wordWithTypo;
    }

    
    public List<String> getSuggestions(Project project){
        if (!processed){
            suggestions = SpellCheckerManager.getInstance(project).getSuggestions(myWordWithTypo);
            processed = true;
        }
        return suggestions;
    }

    public Icon getIcon(int flags) {
        return SpellcheckerIcons.Spellcheck;
    }
}
