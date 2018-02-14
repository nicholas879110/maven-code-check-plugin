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

package com.gome.maven.codeInsight.highlighting;

import com.gome.maven.injected.editor.EditorWindow;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.ex.ActionManagerEx;
import com.gome.maven.openapi.actionSystem.ex.AnActionListener;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.ScrollType;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.event.DocumentListener;
import com.gome.maven.openapi.editor.ex.MarkupModelEx;
import com.gome.maven.openapi.editor.markup.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.UserDataHolderEx;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.util.containers.HashMap;

import java.awt.*;
import java.util.*;

public class HighlightManagerImpl extends HighlightManager {
    private final Project myProject;

    public HighlightManagerImpl(Project project) {
        myProject = project;
        ActionManagerEx.getInstanceEx().addAnActionListener(new MyAnActionListener(), myProject);

        DocumentListener documentListener = new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent event) {
                Document document = event.getDocument();
                Editor[] editors = EditorFactory.getInstance().getEditors(document);
                for (Editor editor : editors) {
                    Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
                    if (map == null) return;

                    ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<RangeHighlighter>();
                    for (RangeHighlighter highlighter : map.keySet()) {
                        HighlightInfo info = map.get(highlighter);
                        if (!info.editor.getDocument().equals(document)) continue;
                        if ((info.flags & HIDE_BY_TEXT_CHANGE) != 0) {
                            highlightersToRemove.add(highlighter);
                        }
                    }

                    for (RangeHighlighter highlighter : highlightersToRemove) {
                        removeSegmentHighlighter(editor, highlighter);
                    }
                }
            }
        };
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener, myProject);
    }

    
    public Map<RangeHighlighter, HighlightInfo> getHighlightInfoMap( Editor editor, boolean toCreate) {
        if (editor instanceof EditorWindow) return getHighlightInfoMap(((EditorWindow)editor).getDelegate(), toCreate);
        Map<RangeHighlighter, HighlightInfo> map = editor.getUserData(HIGHLIGHT_INFO_MAP_KEY);
        if (map == null && toCreate) {
            map = ((UserDataHolderEx)editor).putUserDataIfAbsent(HIGHLIGHT_INFO_MAP_KEY, new HashMap<RangeHighlighter, HighlightInfo>());
        }
        return map;
    }

    
    public RangeHighlighter[] getHighlighters( Editor editor) {
        Map<RangeHighlighter, HighlightInfo> highlightersMap = getHighlightInfoMap(editor, false);
        if (highlightersMap == null) return RangeHighlighter.EMPTY_ARRAY;
        Set<RangeHighlighter> set = new HashSet<RangeHighlighter>();
        for (Map.Entry<RangeHighlighter, HighlightInfo> entry : highlightersMap.entrySet()) {
            HighlightInfo info = entry.getValue();
            if (info.editor.equals(editor)) set.add(entry.getKey());
        }
        return set.toArray(new RangeHighlighter[set.size()]);
    }

    private RangeHighlighter addSegmentHighlighter( Editor editor, int startOffset, int endOffset, TextAttributes attributes, @HideFlags int flags) {
        RangeHighlighter highlighter = editor.getMarkupModel()
                .addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION - 1, attributes, HighlighterTargetArea.EXACT_RANGE);
        HighlightInfo info = new HighlightInfo(editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor, flags);
        Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, true);
        map.put(highlighter, info);
        return highlighter;
    }

    @Override
    public boolean removeSegmentHighlighter( Editor editor,  RangeHighlighter highlighter) {
        Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
        if (map == null) return false;
        HighlightInfo info = map.get(highlighter);
        if (info == null) return false;
        MarkupModel markupModel = info.editor.getMarkupModel();
        if (((MarkupModelEx)markupModel).containsHighlighter(highlighter)) {
            highlighter.dispose();
        }
        map.remove(highlighter);
        return true;
    }

    @Override
    public void addOccurrenceHighlights( Editor editor,
                                         PsiReference[] occurrences,
                                         TextAttributes attributes,
                                        boolean hideByTextChange,
                                        Collection<RangeHighlighter> outHighlighters) {
        if (occurrences.length == 0) return;
        int flags = HIDE_BY_ESCAPE;
        if (hideByTextChange) {
            flags |= HIDE_BY_TEXT_CHANGE;
        }
        Color scrollmarkColor = getScrollMarkColor(attributes);

        int oldOffset = editor.getCaretModel().getOffset();
        int horizontalScrollOffset = editor.getScrollingModel().getHorizontalScrollOffset();
        int verticalScrollOffset = editor.getScrollingModel().getVerticalScrollOffset();
        for (PsiReference occurrence : occurrences) {
            PsiElement element = occurrence.getElement();
            int startOffset = element.getTextRange().getStartOffset();
            int start = startOffset + occurrence.getRangeInElement().getStartOffset();
            int end = startOffset + occurrence.getRangeInElement().getEndOffset();
            PsiFile containingFile = element.getContainingFile();
            Project project = element.getProject();
            // each reference can reside in its own injected editor
            Editor textEditor = InjectedLanguageUtil.openEditorFor(containingFile, project);
            if (textEditor != null) {
                addOccurrenceHighlight(textEditor, start, end, attributes, flags, outHighlighters, scrollmarkColor);
            }
        }
        editor.getCaretModel().moveToOffset(oldOffset);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        editor.getScrollingModel().scrollHorizontally(horizontalScrollOffset);
        editor.getScrollingModel().scrollVertically(verticalScrollOffset);
    }

    @Override
    public void addElementsOccurrenceHighlights( Editor editor,
                                                 PsiElement[] elements,
                                                 TextAttributes attributes,
                                                boolean hideByTextChange,
                                                Collection<RangeHighlighter> outHighlighters) {
        addOccurrenceHighlights(editor, elements, attributes, hideByTextChange, outHighlighters);
    }

    @Override
    public void addOccurrenceHighlight( Editor editor,
                                       int start,
                                       int end,
                                       TextAttributes attributes,
                                       int flags,
                                       Collection<RangeHighlighter> outHighlighters,
                                       Color scrollmarkColor) {
        RangeHighlighter highlighter = addSegmentHighlighter(editor, start, end, attributes, flags);
        if (outHighlighters != null) {
            outHighlighters.add(highlighter);
        }
        if (scrollmarkColor != null) {
            highlighter.setErrorStripeMarkColor(scrollmarkColor);
        }
    }

    @Override
    public void addRangeHighlight( Editor editor,
                                  int startOffset,
                                  int endOffset,
                                   TextAttributes attributes,
                                  boolean hideByTextChange,
                                   Collection<RangeHighlighter> highlighters) {
        addRangeHighlight(editor, startOffset, endOffset, attributes, hideByTextChange, false, highlighters);
    }

    @Override
    public void addRangeHighlight( Editor editor,
                                  int startOffset,
                                  int endOffset,
                                   TextAttributes attributes,
                                  boolean hideByTextChange,
                                  boolean hideByAnyKey,
                                   Collection<RangeHighlighter> highlighters) {
        int flags = HIDE_BY_ESCAPE;
        if (hideByTextChange) {
            flags |= HIDE_BY_TEXT_CHANGE;
        }
        if (hideByAnyKey) {
            flags |= HIDE_BY_ANY_KEY;
        }

        Color scrollmarkColor = getScrollMarkColor(attributes);

        addOccurrenceHighlight(editor, startOffset, endOffset, attributes, flags, highlighters, scrollmarkColor);
    }

    @Override
    public void addOccurrenceHighlights( Editor editor,
                                         PsiElement[] elements,
                                         TextAttributes attributes,
                                        boolean hideByTextChange,
                                        Collection<RangeHighlighter> outHighlighters) {
        if (elements.length == 0) return;
        int flags = HIDE_BY_ESCAPE;
        if (hideByTextChange) {
            flags |= HIDE_BY_TEXT_CHANGE;
        }

        Color scrollmarkColor = getScrollMarkColor(attributes);
        if (editor instanceof EditorWindow) {
            editor = ((EditorWindow)editor).getDelegate();
        }

        for (PsiElement element : elements) {
            TextRange range = element.getTextRange();
            range = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, range);
            addOccurrenceHighlight(editor, range.getStartOffset(), range.getEndOffset(), attributes, flags, outHighlighters, scrollmarkColor);
        }
    }

    
    private static Color getScrollMarkColor( TextAttributes attributes) {
        if (attributes.getErrorStripeColor() != null) return attributes.getErrorStripeColor();
        if (attributes.getBackgroundColor() != null) return attributes.getBackgroundColor().darker();
        return null;
    }

    public boolean hideHighlights( Editor editor, @HideFlags int mask) {
        Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
        if (map == null) return false;

        boolean done = false;
        ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<RangeHighlighter>();
        for (RangeHighlighter highlighter : map.keySet()) {
            HighlightInfo info = map.get(highlighter);
            if (!info.editor.equals(editor)) continue;
            if ((info.flags & mask) != 0) {
                highlightersToRemove.add(highlighter);
                done = true;
            }
        }

        for (RangeHighlighter highlighter : highlightersToRemove) {
            removeSegmentHighlighter(editor, highlighter);
        }

        return done;
    }

    private class MyAnActionListener implements AnActionListener {
        @Override
        public void beforeActionPerformed(AnAction action, final DataContext dataContext, AnActionEvent event) {
            requestHideHighlights(dataContext);
        }


        @Override
        public void afterActionPerformed(final AnAction action, final DataContext dataContext, AnActionEvent event) {
        }

        @Override
        public void beforeEditorTyping(char c, DataContext dataContext) {
            requestHideHighlights(dataContext);
        }

        private void requestHideHighlights(final DataContext dataContext) {
            final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
            if (editor == null) return;
            hideHighlights(editor, HIDE_BY_ANY_KEY);
        }
    }


    private final Key<Map<RangeHighlighter, HighlightInfo>> HIGHLIGHT_INFO_MAP_KEY = Key.create("HIGHLIGHT_INFO_MAP_KEY");

    static class HighlightInfo {
        final Editor editor;
        @HideFlags final int flags;

        public HighlightInfo(Editor editor, @HideFlags int flags) {
            this.editor = editor;
            this.flags = flags;
        }
    }


}
