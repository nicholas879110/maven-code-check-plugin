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
package com.gome.maven.ui.popup;

import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.ui.popup.JBPopup;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.ui.ListUtil;
import com.gome.maven.ui.components.JBList;
import com.gome.maven.ui.table.JBTable;
import com.gome.maven.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * Use this class to make various hints like QuickDocumentation, ShowImplementations, etc.
 * respond to the selection change in the original component like ProjectView, various GoTo popups, etc.
 *
 * @author gregsh
 */
public abstract class HintUpdateSupply {
    private static final Key<HintUpdateSupply> HINT_UPDATE_MARKER = Key.create("HINT_UPDATE_MARKER");

    
    private JBPopup myHint;

    
    public static HintUpdateSupply getSupply( JComponent component) {
        return (HintUpdateSupply)component.getClientProperty(HINT_UPDATE_MARKER);
    }

    public static void hideHint( JComponent component) {
        HintUpdateSupply supply = getSupply(component);
        if (supply != null) supply.hideHint();
    }

    public static void installSimpleHintUpdateSupply( final JComponent component) {
        HintUpdateSupply supply = new HintUpdateSupply(component) {
            
            @Override
            protected PsiElement getPsiElementForHint( Object selectedValue) {
                return selectedValue instanceof PsiElement ? (PsiElement)selectedValue :
                        CommonDataKeys.PSI_ELEMENT.getData(DataManager.getInstance().getDataContext(component));
            }
        };
        if (component instanceof JList) supply.installListListener((JList)component);
        if (component instanceof JTree) supply.installTreeListener((JTree)component);
        if (component instanceof JTable) supply.installTableListener((JTable)component);
    }

    protected HintUpdateSupply( JComponent component) {
        installSupply(component);
    }

    public HintUpdateSupply( JBTable table) {
        installSupply(table);
        installTableListener(table);
    }

    public HintUpdateSupply( Tree tree) {
        installSupply(tree);
        installTreeListener(tree);
    }

    public HintUpdateSupply( JBList list) {
        installSupply(list);
        installListListener(list);
    }

    protected void installTableListener( final JTable table) {
        ListSelectionListener listener = new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (!isHintVisible(HintUpdateSupply.this.myHint) || isSelectedByMouse(table)) return;

                int selected = ((ListSelectionModel)e.getSource()).getLeadSelectionIndex();
                int rowCount = table.getRowCount();
                if (selected == -1 || rowCount == 0) return;

                PsiElement element = getPsiElementForHint(table.getValueAt(Math.min(selected, rowCount - 1), 0));
                if (element != null && element.isValid()) {
                    updateHint(element);
                }
            }
        };
        table.getSelectionModel().addListSelectionListener(listener);
        table.getColumnModel().getSelectionModel().addListSelectionListener(listener);
    }

    protected void installTreeListener( final JTree tree) {
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(final TreeSelectionEvent e) {
                if (!isHintVisible(HintUpdateSupply.this.myHint) || isSelectedByMouse(tree)) return;

                TreePath path = tree.getSelectionPath();
                if (path != null) {
                    final PsiElement psiElement = getPsiElementForHint(path.getLastPathComponent());
                    if (psiElement != null && psiElement.isValid()) {
                        updateHint(psiElement);
                    }
                }
            }
        });
    }

    protected void installListListener( final JList list) {
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (!isHintVisible(HintUpdateSupply.this.myHint) || isSelectedByMouse(list)) return;

                Object[] selectedValues = ((JList)e.getSource()).getSelectedValues();
                if (selectedValues.length != 1) return;

                PsiElement element = getPsiElementForHint(selectedValues[0]);
                if (element != null && element.isValid()) {
                    updateHint(element);
                }
            }
        });
    }

    
    protected abstract PsiElement getPsiElementForHint( Object selectedValue);

    private void installSupply( JComponent component) {
        component.putClientProperty(HINT_UPDATE_MARKER, this);
    }

    public void registerHint(JBPopup hint) {
        hideHint();
        myHint = hint;
    }

    public void hideHint() {
        if (isHintVisible(myHint)) {
            myHint.cancel();
        }

        myHint = null;
    }

    public void updateHint(PsiElement element) {
        if (!isHintVisible(myHint)) return;

        PopupUpdateProcessorBase updateProcessor = myHint.getUserData(PopupUpdateProcessorBase.class);
        if (updateProcessor != null) {
            updateProcessor.updatePopup(element);
        }
    }

    private static boolean isHintVisible(JBPopup hint) {
        return hint != null && hint.isVisible();
    }

    private static boolean isSelectedByMouse( JComponent c) {
        return Boolean.TRUE.equals(c.getClientProperty(ListUtil.SELECTED_BY_MOUSE_EVENT));
    }
}
