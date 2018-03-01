/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.refactoring;

import com.gome.maven.codeInsight.unwrap.ScopeHighlighter;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.ui.popup.JBPopupAdapter;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.LightweightWindowEvent;
import com.gome.maven.openapi.util.Pass;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.SmartPointerManager;
import com.gome.maven.psi.SmartPsiElementPointer;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.components.JBList;
import com.gome.maven.util.Function;
import com.gome.maven.util.NotNullFunction;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class IntroduceTargetChooser {
    private IntroduceTargetChooser() {
    }

    public static <T extends PsiElement> void showChooser(final Editor editor, final List<T> expressions, final Pass<T> callback,
                                                          final Function<T, String> renderer) {
        showChooser(editor, expressions, callback, renderer, "Expressions");
    }

    public static <T extends PsiElement> void showChooser(final Editor editor,
                                                          final List<T> expressions,
                                                          final Pass<T> callback,
                                                          final Function<T, String> renderer,
                                                          String title) {
        showChooser(editor, expressions, callback, renderer, title, ScopeHighlighter.NATURAL_RANGER);
    }

    public static <T extends PsiElement> void showChooser(final Editor editor,
                                                          final List<T> expressions,
                                                          final Pass<T> callback,
                                                          final Function<T, String> renderer,
                                                          String title,
                                                          NotNullFunction<PsiElement, TextRange> ranger) {
        showChooser(editor, expressions, callback, renderer, title, -1, ranger);
    }

    public static <T extends PsiElement> void showChooser(final Editor editor,
                                                          final List<T> expressions,
                                                          final Pass<T> callback,
                                                          final Function<T, String> renderer,
                                                          String title,
                                                          int selection,
                                                          NotNullFunction<PsiElement, TextRange> ranger) {
        final ScopeHighlighter highlighter = new ScopeHighlighter(editor, ranger);
        final DefaultListModel model = new DefaultListModel();
        for (T expr : expressions) {
            model.addElement(SmartPointerManager.getInstance(expr.getProject()).createSmartPsiElementPointer(expr));
        }
        final JList list = new JBList(model);
        list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (selection > -1) list.setSelectedIndex(selection);
        list.setCellRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(final JList list,
                                                          final Object value,
                                                          final int index,
                                                          final boolean isSelected,
                                                          final boolean cellHasFocus) {
                final Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                SmartPsiElementPointer<T> pointer = (SmartPsiElementPointer<T>)value;
                final T expr = pointer.getElement();
                if (expr != null) {
                    String text = renderer.fun(expr);
                    int firstNewLinePos = text.indexOf('\n');
                    String trimmedText = text.substring(0, firstNewLinePos != -1 ? firstNewLinePos : Math.min(100, text.length()));
                    if (trimmedText.length() != text.length()) trimmedText += " ...";
                    setText(trimmedText);
                }
                else {
                    setForeground(JBColor.RED);
                    setText("Invalid");
                }
                return rendererComponent;
            }
        });

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                highlighter.dropHighlight();
                final int index = list.getSelectedIndex();
                if (index < 0) return;
                SmartPsiElementPointer<T> pointer = ((SmartPsiElementPointer<T>)model.get(index));
                final T expr = pointer.getElement();
                if (expr != null) {
                    highlighter.highlight(expr, Collections.<PsiElement>singletonList(expr));
                }
            }
        });

        JBPopupFactory.getInstance().createListPopupBuilder(list)
                .setTitle(title)
                .setMovable(false)
                .setResizable(false)
                .setRequestFocus(true)
                .setItemChoosenCallback(new Runnable() {
                    @Override
                    public void run() {
                        SmartPsiElementPointer<T> value = (SmartPsiElementPointer<T>)list.getSelectedValue();
                        T expr = value.getElement();
                        if (expr != null) {
                            callback.pass(expr);
                        }
                    }
                })
                .addListener(new JBPopupAdapter() {
                    @Override
                    public void onClosed(LightweightWindowEvent event) {
                        highlighter.dropHighlight();
                    }
                })
                .createPopup().showInBestPositionFor(editor);
    }
}
