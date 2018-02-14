/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.highlighting;

import com.gome.maven.codeInsight.CodeInsightBundle;
import com.gome.maven.codeInsight.hint.HintManager;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.colors.EditorColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.Consumer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public abstract class HighlightUsagesHandlerBase<T extends PsiElement> {
    protected final Editor myEditor;
    protected final PsiFile myFile;

    protected List<TextRange> myReadUsages = new ArrayList<TextRange>();
    protected List<TextRange> myWriteUsages = new ArrayList<TextRange>();
    protected String myStatusText;
    protected String myHintText;

    protected HighlightUsagesHandlerBase(final Editor editor, final PsiFile file) {
        myEditor = editor;
        myFile = file;
    }

    public void highlightUsages() {
        List<T> targets = getTargets();
        if (targets == null) {
            return;
        }
        selectTargets(targets, new Consumer<List<T>>() {
            @Override
            public void consume(final List<T> targets) {
                computeUsages(targets);
                performHighlighting();
            }
        });
    }

    protected void performHighlighting() {
        boolean clearHighlights = HighlightUsagesHandler.isClearHighlights(myEditor);
        EditorColorsManager manager = EditorColorsManager.getInstance();
        TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        TextAttributes writeAttributes = manager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
        HighlightUsagesHandler.highlightRanges(HighlightManager.getInstance(myEditor.getProject()),
                myEditor, attributes, clearHighlights, myReadUsages);
        HighlightUsagesHandler.highlightRanges(HighlightManager.getInstance(myEditor.getProject()),
                myEditor, writeAttributes, clearHighlights, myWriteUsages);
        if (!clearHighlights) {
            WindowManager.getInstance().getStatusBar(myEditor.getProject()).setInfo(myStatusText);

            HighlightHandlerBase.setupFindModel(myEditor.getProject()); // enable f3 navigation
        }
        if (myHintText != null) {
            HintManager.getInstance().showInformationHint(myEditor, myHintText);
        }
    }

    protected void buildStatusText( String elementName, int refCount) {
        if (refCount > 0) {
            myStatusText = CodeInsightBundle.message(elementName != null ?
                            "status.bar.highlighted.usages.message" :
                            "status.bar.highlighted.usages.no.target.message", refCount, elementName,
                    HighlightUsagesHandler.getShortcutText());
        }
        else {
            myHintText = CodeInsightBundle.message(elementName != null ?
                    "status.bar.highlighted.usages.not.found.message" :
                    "status.bar.highlighted.usages.not.found.no.target.message", elementName);
        }
    }

    public abstract List<T> getTargets();

    
    public String getFeatureId() {
        return null;
    }

    protected abstract void selectTargets(List<T> targets, Consumer<List<T>> selectionConsumer);

    public abstract void computeUsages(List<T> targets);

    protected void addOccurrence( PsiElement element) {
        TextRange range = element.getTextRange();
        range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
        myReadUsages.add(range);
    }

    public List<TextRange> getReadUsages() {
        return myReadUsages;
    }

    public List<TextRange> getWriteUsages() {
        return myWriteUsages;
    }
}
