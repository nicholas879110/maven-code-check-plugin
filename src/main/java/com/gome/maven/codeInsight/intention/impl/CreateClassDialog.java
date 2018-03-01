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
package com.gome.maven.codeInsight.intention.impl;

import com.gome.maven.CommonBundle;
import com.gome.maven.codeInsight.CodeInsightBundle;
import com.gome.maven.codeInsight.daemon.impl.quickfix.ClassKind;
import com.gome.maven.ide.util.PackageUtil;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CustomShortcutSet;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.JavaProjectRootsUtil;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Pass;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.JavaPsiFacade;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.PsiNameHelper;
import com.gome.maven.refactoring.MoveDestination;
import com.gome.maven.refactoring.PackageWrapper;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.move.moveClassesOrPackages.DestinationFolderComboBox;
import com.gome.maven.refactoring.ui.PackageNameReferenceEditorCombo;
import com.gome.maven.refactoring.util.RefactoringMessageUtil;
import com.gome.maven.ui.DocumentAdapter;
import com.gome.maven.ui.RecentsManager;
import com.gome.maven.ui.ReferenceEditorComboWithBrowseButton;
import com.gome.maven.ui.components.JBLabel;
import com.gome.maven.util.IncorrectOperationException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class CreateClassDialog extends DialogWrapper {
    private final JLabel myInformationLabel = new JLabel("#");
    private final JLabel myPackageLabel = new JLabel(CodeInsightBundle.message("dialog.create.class.destination.package.label"));
    private final ReferenceEditorComboWithBrowseButton myPackageComponent;
    private final JTextField myTfClassName = new MyTextField();
    private final Project myProject;
    private PsiDirectory myTargetDirectory;
    private final String myClassName;
    private final boolean myClassNameEditable;
    private final Module myModule;
    private final DestinationFolderComboBox myDestinationCB = new DestinationFolderComboBox() {
        @Override
        public String getTargetPackage() {
            return myPackageComponent.getText().trim();
        }

        @Override
        protected boolean reportBaseInTestSelectionInSource() {
            return CreateClassDialog.this.reportBaseInTestSelectionInSource();
        }

        @Override
        protected boolean reportBaseInSourceSelectionInTest() {
            return CreateClassDialog.this.reportBaseInSourceSelectionInTest();
        }
    };
     private static final String RECENTS_KEY = "CreateClassDialog.RecentsKey";

    public CreateClassDialog( Project project,
                              String title,
                              String targetClassName,
                              String targetPackageName,
                              ClassKind kind,
                             boolean classNameEditable,
                              Module defaultModule) {
        super(project, true);
        myClassNameEditable = classNameEditable;
        myModule = defaultModule;
        myClassName = targetClassName;
        myProject = project;
        myPackageComponent = new PackageNameReferenceEditorCombo(targetPackageName, myProject, RECENTS_KEY, CodeInsightBundle.message("dialog.create.class.package.chooser.title"));
        myPackageComponent.setTextFieldPreferredWidth(40);

        init();

        if (!myClassNameEditable) {
            setTitle(CodeInsightBundle.message("dialog.create.class.name", StringUtil.capitalize(kind.getDescription()), targetClassName));
        }
        else {
            myInformationLabel.setText(CodeInsightBundle.message("dialog.create.class.label", kind.getDescription()));
            setTitle(title);
        }

        myTfClassName.setText(myClassName);
        myDestinationCB.setData(myProject, getBaseDir(targetPackageName), new Pass<String>() {
            @Override
            public void pass(String s) {
                setErrorText(s);
            }
        }, myPackageComponent.getChildComponent());
    }

    protected boolean reportBaseInTestSelectionInSource() {
        return false;
    }

    protected boolean reportBaseInSourceSelectionInTest() {
        return false;
    }

    
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myClassNameEditable ? myTfClassName : myPackageComponent.getChildComponent();
    }

    @Override
    protected JComponent createCenterPanel() {
        return new JPanel(new BorderLayout());
    }

    @Override
    protected JComponent createNorthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbConstraints = new GridBagConstraints();

        gbConstraints.insets = new Insets(4, 8, 4, 8);
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.anchor = GridBagConstraints.WEST;

        if (myClassNameEditable) {
            gbConstraints.weightx = 0;
            gbConstraints.gridwidth = 1;
            panel.add(myInformationLabel, gbConstraints);
            gbConstraints.insets = new Insets(4, 8, 4, 8);
            gbConstraints.gridx = 1;
            gbConstraints.weightx = 1;
            gbConstraints.gridwidth = 1;
            gbConstraints.fill = GridBagConstraints.HORIZONTAL;
            gbConstraints.anchor = GridBagConstraints.WEST;
            panel.add(myTfClassName, gbConstraints);

            myTfClassName.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(DocumentEvent e) {
                    getOKAction().setEnabled(PsiNameHelper.getInstance(myProject).isIdentifier(myTfClassName.getText()));
                }
            });
            getOKAction().setEnabled(StringUtil.isNotEmpty(myClassName));
        }

        gbConstraints.gridx = 0;
        gbConstraints.gridy = 2;
        gbConstraints.weightx = 0;
        gbConstraints.gridwidth = 1;
        panel.add(myPackageLabel, gbConstraints);

        gbConstraints.gridx = 1;
        gbConstraints.weightx = 1;

        new AnAction() {
            @Override
            public void actionPerformed(AnActionEvent e) {
                myPackageComponent.getButton().doClick();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)), myPackageComponent.getChildComponent());

        JPanel _panel = new JPanel(new BorderLayout());
        _panel.add(myPackageComponent, BorderLayout.CENTER);
        panel.add(_panel, gbConstraints);

        gbConstraints.gridy = 3;
        gbConstraints.gridx = 0;
        gbConstraints.gridwidth = 2;
        gbConstraints.insets.top = 12;
        gbConstraints.anchor = GridBagConstraints.WEST;
        gbConstraints.fill = GridBagConstraints.NONE;
        final JBLabel label = new JBLabel(RefactoringBundle.message("target.destination.folder"));
        panel.add(label, gbConstraints);

        gbConstraints.gridy = 4;
        gbConstraints.gridx = 0;
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.insets.top = 4;
        panel.add(myDestinationCB, gbConstraints);

        final boolean isMultipleSourceRoots = JavaProjectRootsUtil.getSuitableDestinationSourceRoots(myProject).size() > 1;
        myDestinationCB.setVisible(isMultipleSourceRoots);
        label.setVisible(isMultipleSourceRoots);
        label.setLabelFor(myDestinationCB);
        return panel;
    }

    public PsiDirectory getTargetDirectory() {
        return myTargetDirectory;
    }

    private String getPackageName() {
        String name = myPackageComponent.getText();
        return name != null ? name.trim() : "";
    }

    private static class MyTextField extends JTextField {
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            FontMetrics fontMetrics = getFontMetrics(getFont());
            size.width = fontMetrics.charWidth('a') * 40;
            return size;
        }
    }

    @Override
    protected void doOKAction() {
        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, myPackageComponent.getText());
        final String packageName = getPackageName();

        final String[] errorString = new String[1];
        CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
            @Override
            public void run() {
                try {
                    final PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);
                    final MoveDestination destination = myDestinationCB.selectDirectory(targetPackage, false);
                    if (destination == null) return;
                    myTargetDirectory = ApplicationManager.getApplication().runWriteAction(new Computable<PsiDirectory>() {
                        @Override
                        public PsiDirectory compute() {
                            return destination.getTargetDirectory(getBaseDir(packageName));
                        }
                    });
                    if (myTargetDirectory == null) {
                        errorString[0] = ""; // message already reported by PackageUtil
                        return;
                    }
                    errorString[0] = RefactoringMessageUtil.checkCanCreateClass(myTargetDirectory, getClassName());
                }
                catch (IncorrectOperationException e) {
                    errorString[0] = e.getMessage();
                }
            }
        }, CodeInsightBundle.message("create.directory.command"), null);

        if (errorString[0] != null) {
            if (errorString[0].length() > 0) {
                Messages.showMessageDialog(myProject, errorString[0], CommonBundle.getErrorTitle(), Messages.getErrorIcon());
            }
            return;
        }
        super.doOKAction();
    }

    
    protected PsiDirectory getBaseDir(String packageName) {
        return myModule == null? null : PackageUtil.findPossiblePackageDirectoryInModule(myModule, packageName);
    }

    
    public String getClassName() {
        if (myClassNameEditable) {
            return myTfClassName.getText();
        }
        else {
            return myClassName;
        }
    }
}
