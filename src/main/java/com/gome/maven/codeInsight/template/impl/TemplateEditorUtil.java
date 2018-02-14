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

package com.gome.maven.codeInsight.template.impl;

import com.gome.maven.codeInsight.template.TemplateContextType;
import com.gome.maven.ide.DataManager;
import com.gome.maven.lexer.Lexer;
import com.gome.maven.lexer.MergingLexerAdapter;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.EditorSettings;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.ex.util.LayerDescriptor;
import com.gome.maven.openapi.editor.ex.util.LayeredLexerEditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighter;
import com.gome.maven.openapi.editor.highlighter.EditorHighlighterFactory;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileTypes.PlainSyntaxHighlighter;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighter;
import com.gome.maven.openapi.fileTypes.SyntaxHighlighterBase;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.ObjectUtils;

public class TemplateEditorUtil {
    private TemplateEditorUtil() {}

    public static Editor createEditor(boolean isReadOnly, CharSequence text) {
        return createEditor(isReadOnly, text, null);
    }

    public static Editor createEditor(boolean isReadOnly, CharSequence text,  TemplateContext context) {
        final Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext());
        return createEditor(isReadOnly, createDocument(text, context, project), project);
    }

    private static Document createDocument(CharSequence text,  TemplateContext context, Project project) {
        if (context != null) {
            for (TemplateContextType type : TemplateManagerImpl.getAllContextTypes()) {
                if (context.isExplicitlyEnabled(type)) {
                    return type.createDocument(text, project);
                }
            }
        }

        return EditorFactory.getInstance().createDocument(text);
    }

    private static Editor createEditor(boolean isReadOnly, final Document document, final Project project) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Editor editor = (isReadOnly ? editorFactory.createViewer(document, project) : editorFactory.createEditor(document, project));
        editor.getContentComponent().setFocusable(!isReadOnly);

        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setVirtualSpace(false);
        editorSettings.setLineMarkerAreaShown(false);
        editorSettings.setIndentGuidesShown(false);
        editorSettings.setLineNumbersShown(false);
        editorSettings.setFoldingOutlineShown(false);
        editorSettings.setCaretRowShown(false);

        EditorColorsScheme scheme = editor.getColorsScheme();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null) {
            EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(file, scheme, project);
            ((EditorEx) editor).setHighlighter(highlighter);

        }

        return editor;
    }

    public static void setHighlighter(Editor editor,  TemplateContext templateContext) {
        SyntaxHighlighter highlighter = null;
        if (templateContext != null) {
            for(TemplateContextType type: TemplateManagerImpl.getAllContextTypes()) {
                if (templateContext.isEnabled(type)) {
                    highlighter = type.createHighlighter();
                    if (highlighter != null) break;
                }
            }
        }
        setHighlighter((EditorEx)editor, highlighter);
    }

    public static void setHighlighter( Editor editor,  TemplateContextType templateContextType) {
        setHighlighter((EditorEx)editor, templateContextType != null ? templateContextType.createHighlighter() : null);
    }

    private static void setHighlighter(EditorEx editor,  SyntaxHighlighter highlighter) {
        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        LayeredLexerEditorHighlighter layeredHighlighter = new LayeredLexerEditorHighlighter(new TemplateHighlighter(), editorColorsScheme);
        layeredHighlighter.registerLayer(TemplateTokenType.TEXT, new LayerDescriptor(ObjectUtils.notNull(highlighter, new PlainSyntaxHighlighter()), ""));
        editor.setHighlighter(layeredHighlighter);
    }

    private static class TemplateHighlighter extends SyntaxHighlighterBase {
        private final Lexer myLexer;

        public TemplateHighlighter() {
            myLexer = new MergingLexerAdapter(new TemplateTextLexer(), TokenSet.create(TemplateTokenType.TEXT));
        }

        @Override
        
        public Lexer getHighlightingLexer() {
            return myLexer;
        }

        @Override
        
        public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
            return tokenType == TemplateTokenType.VARIABLE ? pack(TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES) : EMPTY;
        }
    }
}
