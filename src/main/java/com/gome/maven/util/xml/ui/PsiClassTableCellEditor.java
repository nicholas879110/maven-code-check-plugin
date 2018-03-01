package com.gome.maven.util.xml.ui;

import com.gome.maven.ide.util.TreeClassChooser;
import com.gome.maven.ide.util.TreeClassChooserFactory;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.FixedSizeButton;
import com.gome.maven.openapi.util.Conditions;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.ui.EditorTextField;
import com.gome.maven.ui.JavaReferenceEditorUtil;
import com.gome.maven.ui.UIBundle;
import com.gome.maven.util.ui.AbstractTableCellEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

/**
 * @author peter
 */
public class PsiClassTableCellEditor extends AbstractTableCellEditor {
    private final Project myProject;
    private final GlobalSearchScope mySearchScope;
    private EditorTextField myEditor;

    public PsiClassTableCellEditor(final Project project, final GlobalSearchScope searchScope) {
        myProject = project;
        mySearchScope = searchScope;
    }

    public final Object getCellEditorValue() {
        return myEditor.getText();
    }

    public final boolean stopCellEditing() {
        final boolean b = super.stopCellEditing();
        myEditor = null;
        return b;
    }

    public boolean isCellEditable(EventObject e) {
        return !(e instanceof MouseEvent) || ((MouseEvent)e).getClickCount() >= 2;
    }

    public final Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        final Document document = JavaReferenceEditorUtil.createDocument(value == null ? "" : (String)value, myProject, true);
        myEditor = new EditorTextField(document, myProject, StdFileTypes.JAVA){
            protected boolean shouldHaveBorder() {
                return false;
            }

            public void addNotify() {
                super.addNotify();
                final JComponent editorComponent = getEditor().getContentComponent();
                editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ENTER");
                editorComponent.getActionMap().put("ENTER", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        stopCellEditing();
                    }
                });
            }
        };
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(myEditor);
        final FixedSizeButton button = new FixedSizeButton(myEditor);
        panel.add(button, BorderLayout.EAST);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TreeClassChooser chooser = TreeClassChooserFactory.getInstance(myProject)
                        .createInheritanceClassChooser(UIBundle.message("choose.class"), mySearchScope, null, true, true, Conditions.alwaysTrue());
                chooser.showDialog();
                final PsiClass psiClass = chooser.getSelected();
                if (psiClass != null) {
                    myEditor.setText(psiClass.getQualifiedName());
                }
            }
        });
        panel.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (!e.isTemporary() && myEditor != null) {
                    myEditor.requestFocus();
                }
            }

            public void focusLost(FocusEvent e) {
            }
        });
        myEditor.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
            }

            public void focusLost(FocusEvent e) {
                if (!e.isTemporary()) {
                    stopCellEditing();
                }
            }
        });

        //ComponentWithBrowseButton.MyDoClickAction.addTo(button, myEditor);

        return panel;
    }
}
