/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.ui.components.editors;

import com.gome.maven.openapi.ui.popup.JBPopup;
import com.gome.maven.openapi.ui.popup.JBPopupAdapter;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.LightweightWindowEvent;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.TableUtil;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.ui.components.JBComboBoxLabel;
import com.gome.maven.ui.components.JBLabel;
import com.gome.maven.ui.components.JBList;
import com.gome.maven.ui.table.JBTable;
import com.gome.maven.util.Function;
import com.gome.maven.util.PlatformIcons;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.EmptyIcon;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Solves rendering problems in JTable components when JComboBox objects are used as cell
 * editors components. Known issues of using JComboBox component are the following:
 *   <p>1. Ugly view if row height is small enough
 *   <p>2. Truncated strings in the combobox popup if column width is less than text value width
 *   <p>
 *   <b>How to use:</b>
 *   <p>1. In get <code>getTableCellEditorComponent</code> method create or use existent
 *   <code>JBComboBoxTableCellEditorComponent</code> instance<br/>
 *   <p>2. Init component by calling <code>setCell</code>, <code>setOptions</code>,
 *   <code>setDefaultValue</code> methods
 *   <p>3. Return the instance
 *
 * @author Konstantin Bulenkov
 * @see JBComboBoxLabel
 */
public class JBComboBoxTableCellEditorComponent extends JBLabel {
    private JTable myTable;
    private int myRow = 0;
    private int myColumn = 0;
    private final JBList myList = new JBList();
    private Object[] myOptions = {};
    private Object myValue;
    public boolean myWide = false;
    private Function<Object, String> myToString = StringUtil.createToStringFunction(Object.class);
    private final List<ActionListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    @SuppressWarnings({"GtkPreferredJComboBoxRenderer"})
    private ListCellRenderer myRenderer = new DefaultListCellRenderer() {
        public Icon myEmptyIcon;

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel label = (JLabel)super.getListCellRendererComponent(list, myToString.fun(value), index, isSelected, cellHasFocus);
            if (value == myValue) {
                label.setIcon(getIcon(isSelected));
            } else {
                label.setIcon(getEmptyIcon());
            }
            return label;
        }

        private Icon getEmptyIcon() {
            if (myEmptyIcon == null) {
                myEmptyIcon = EmptyIcon.create(getIcon(true).getIconWidth());
            }
            return myEmptyIcon;
        }

        private Icon getIcon(boolean selected) {
            final boolean small = "small".equals(JBComboBoxTableCellEditorComponent.this.getClientProperty("JComponent.sizeVariant"));
            return small
                    ? selected ? PlatformIcons.CHECK_ICON_SMALL_SELECTED : PlatformIcons.CHECK_ICON_SMALL
                    : selected ? PlatformIcons.CHECK_ICON_SELECTED : PlatformIcons.CHECK_ICON;
        }
    };

    public JBComboBoxTableCellEditorComponent() {
    }

    public JBComboBoxTableCellEditorComponent(JTable table) {
        myTable = table;
    }

    public void setCell(JTable table, int row, int column) {
        setTable(table);
        setRow(row);
        setColumn(column);
    }

    public void setTable(JTable table) {
        myTable = table;
    }

    public void setRow(int row) {
        myRow = row;
    }

    public void setColumn(int column) {
        myColumn = column;
    }

    public Object[] getOptions() {
        return myOptions;
    }

    public void setOptions(Object... options) {
        myOptions = options;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        initAndShowPopup();
    }

    private void initAndShowPopup() {
        myList.removeAll();
        myList.setModel(JBList.createDefaultListModel(myOptions));
        if (myRenderer != null) {
            myList.setCellRenderer(myRenderer);
        }
        final Rectangle rect = myTable.getCellRect(myRow, myColumn, true);
        Point point = new Point(rect.x, rect.y);
        final boolean surrendersFocusOnKeystrokeOldValue = myTable instanceof JBTable ? ((JBTable)myTable).surrendersFocusOnKeyStroke() : myTable.getSurrendersFocusOnKeystroke();
        final JBPopup popup = JBPopupFactory.getInstance()
                .createListPopupBuilder(myList)
                .setItemChoosenCallback(new Runnable() {
                    @Override
                    public void run() {
                        final ActionEvent event = new ActionEvent(myList, ActionEvent.ACTION_PERFORMED, "elementChosen");
                        for (ActionListener listener : myListeners) {
                            listener.actionPerformed(event);
                        }
                        myValue = myList.getSelectedValue();
                        TableUtil.stopEditing(myTable);

                        myTable.setValueAt(myValue, myRow, myColumn); // on Mac getCellEditorValue() called before myValue is set.
                        myTable.tableChanged(new TableModelEvent(myTable.getModel(), myRow));  // force repaint
                    }
                })
                .setCancelCallback(new Computable<Boolean>() {
                    @Override
                    public Boolean compute() {
                        TableUtil.stopEditing(myTable);
                        return true;
                    }
                })
                .addListener(new JBPopupAdapter() {
                    @Override
                    public void beforeShown(LightweightWindowEvent event) {
                        super.beforeShown(event);
                        myTable.setSurrendersFocusOnKeystroke(false);
                    }

                    @Override
                    public void onClosed(LightweightWindowEvent event) {
                        myTable.setSurrendersFocusOnKeystroke(surrendersFocusOnKeystrokeOldValue);
                        super.onClosed(event);
                    }
                })
                .setMinSize(myWide ? new Dimension(((int)rect.getSize().getWidth()), -1) : null)
                .createPopup();
        popup.show(new RelativePoint(myTable, point));
    }

    public void setWide(boolean wide) {
        this.myWide = wide;
    }

    public Object getEditorValue() {
        return myValue;
    }

    public void setRenderer(ListCellRenderer renderer) {
        myRenderer = renderer;
    }

    public void setDefaultValue(Object value) {
        myValue = value;
    }

    public void setToString(Function<Object, String> toString) {
        myToString = toString;
    }

    public void addActionListener(ActionListener listener) {
        myListeners.add(listener);
    }
}
