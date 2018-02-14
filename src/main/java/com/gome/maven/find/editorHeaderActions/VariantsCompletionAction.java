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
package com.gome.maven.find.editorHeaderActions;

import com.gome.maven.featureStatistics.FeatureUsageTracker;
import com.gome.maven.find.EditorSearchComponent;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.colors.EditorFontType;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.codeStyle.NameUtil;
import com.gome.maven.psi.impl.cache.impl.id.IdTableBuilding;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.components.JBList;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.text.Matcher;
import com.gome.maven.util.ui.GraphicsUtil;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VariantsCompletionAction extends AnAction {
    private Getter<JTextComponent> myTextField;
    private final EditorSearchComponent myEditorSearchComponent;

    public EditorSearchComponent getEditorSearchComponent() {
        return myEditorSearchComponent;
    }

    public VariantsCompletionAction(EditorSearchComponent editorSearchComponent, Getter<JTextComponent> textField) {
        myEditorSearchComponent = editorSearchComponent;
        final AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION);
        setTextField(textField);
        if (action != null) {
            registerCustomShortcutSet(action.getShortcutSet(), getTextField());
        }
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final String prefix = getPrefix();
        if (StringUtil.isEmpty(prefix)) return;

        Editor editor = getEditorSearchComponent().getEditor();
        if (editor != null) {
            final String[] array = calcWords(prefix, editor);
            if (array.length == 0) {
                return;
            }

            FeatureUsageTracker.getInstance().triggerFeatureUsed("find.completion");
            final JList list = new JBList(array) {
                @Override
                protected void paintComponent(final Graphics g) {
                    GraphicsUtil.setupAntialiasing(g);
                    super.paintComponent(g);
                }
            };
            list.setBackground(new JBColor(EditorSearchComponent.COMPLETION_BACKGROUND_COLOR, new Color(0x4C4F51)));
            list.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));

            Utils.showCompletionPopup(
                    e.getInputEvent() instanceof MouseEvent ? getEditorSearchComponent().getToolbarComponent() : null,
                    list, null, getTextField(), null);
        }
    }

    
    private String getPrefix() {
        //Editor editor = myTextField.getEditor();
        //if (editor != null){
        //  int offset = editor.getCaretModel().getOffset();
        //  return myTextField.getText().substring(0, offset);
        //}
        int offset = getTextField().getCaretPosition();
        return getTextField().getText().substring(0, offset);
    }

    public JTextComponent getTextField() {
        return myTextField.get();
    }

    public void setTextField(Getter<JTextComponent> textField) {
        myTextField = textField;
    }

    private static String[] calcWords(final String prefix, Editor editor) {
        final Matcher matcher = NameUtil.buildMatcher(prefix, 0, true, true);
        final Set<String> words = new HashSet<String>();
        CharSequence chars = editor.getDocument().getCharsSequence();

        IdTableBuilding.scanWords(new IdTableBuilding.ScanWordProcessor() {
            @Override
            public void run(final CharSequence chars,  char[] charsArray, final int start, final int end) {
                final String word = chars.subSequence(start, end).toString();
                if (matcher.matches(word)) {
                    words.add(word);
                }
            }
        }, chars, 0, chars.length());


        ArrayList<String> sortedWords = new ArrayList<String>(words);
        Collections.sort(sortedWords);

        return ArrayUtil.toStringArray(sortedWords);
    }
}
