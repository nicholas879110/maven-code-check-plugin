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
package com.gome.maven.refactoring.introduceVariable;

import com.gome.maven.openapi.help.HelpManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.psi.JavaPsiFacade;
import com.gome.maven.psi.PsiExpression;
import com.gome.maven.psi.PsiNameHelper;
import com.gome.maven.psi.PsiType;
import com.gome.maven.psi.codeStyle.CodeStyleSettingsManager;
import com.gome.maven.psi.codeStyle.SuggestedNameInfo;
import com.gome.maven.refactoring.HelpID;
import com.gome.maven.refactoring.JavaRefactoringSettings;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.ui.*;
import com.gome.maven.ui.NonFocusableCheckBox;
import com.gome.maven.ui.StateRestoringCheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

class IntroduceVariableDialog extends DialogWrapper implements IntroduceVariableSettings {
    private final Project myProject;
    private final PsiExpression myExpression;
    private final int myOccurrencesCount;
    private final boolean myAnyLValueOccurences;
    private final boolean myDeclareFinalIfAll;
    private final TypeSelectorManager myTypeSelectorManager;
    private final IntroduceVariableHandler.Validator myValidator;

    private NameSuggestionsField myNameField;
    private JCheckBox myCbReplaceAll;
    private StateRestoringCheckBox myCbReplaceWrite = null;
    private JCheckBox myCbFinal;
    private boolean myCbFinalState;
    private TypeSelector myTypeSelector;
    private NameSuggestionsManager myNameSuggestionsManager;
    private static final String REFACTORING_NAME = RefactoringBundle.message("introduce.variable.title");
    private NameSuggestionsField.DataChanged myNameChangedListener;
    private ItemListener myReplaceAllListener;
    private ItemListener myFinalListener;

    public IntroduceVariableDialog(Project project,
                                   PsiExpression expression, int occurrencesCount, boolean anyLValueOccurences,
                                   boolean declareFinalIfAll, TypeSelectorManager typeSelectorManager,
                                   IntroduceVariableHandler.Validator validator) {
        super(project, true);
        myProject = project;
        myExpression = expression;
        myOccurrencesCount = occurrencesCount;
        myAnyLValueOccurences = anyLValueOccurences;
        myDeclareFinalIfAll = declareFinalIfAll;
        myTypeSelectorManager = typeSelectorManager;
        myValidator = validator;

        setTitle(REFACTORING_NAME);
        init();
    }

    protected void dispose() {
        myNameField.removeDataChangedListener(myNameChangedListener);
        if (myCbReplaceAll != null) {
            myCbReplaceAll.removeItemListener(myReplaceAllListener);
        }
        myCbFinal.removeItemListener(myFinalListener);
        super.dispose();
    }

    
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    protected void init() {
        super.init();
        updateOkStatus();
    }

    public String getEnteredName() {
        return myNameField.getEnteredName();
    }

    public boolean isReplaceAllOccurrences() {
        if (myOccurrencesCount <= 1) return false;
        return myCbReplaceAll.isSelected();
    }

    public boolean isDeclareFinal() {
        return myCbFinal.isSelected();
    }

    public boolean isReplaceLValues() {
        if (myOccurrencesCount <= 1 || !myAnyLValueOccurences || myCbReplaceWrite == null) {
            return true;
        }
        else {
            return myCbReplaceWrite.isSelected();
        }
    }

    public PsiType getSelectedType() {
        return myTypeSelector.getSelectedType();
    }

    protected JComponent createNorthPanel() {
        myNameField = new NameSuggestionsField(myProject);
        myNameChangedListener = new NameSuggestionsField.DataChanged() {
            public void dataChanged() {
                updateOkStatus();
            }
        };
        myNameField.addDataChangedListener(myNameChangedListener);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbConstraints = new GridBagConstraints();

        gbConstraints.insets = new Insets(4, 4, 4, 4);
        gbConstraints.anchor = GridBagConstraints.WEST;
        gbConstraints.fill = GridBagConstraints.BOTH;

        gbConstraints.gridwidth = 1;
        gbConstraints.weightx = 0;
        gbConstraints.weighty = 0;
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        JLabel type = new JLabel(RefactoringBundle.message("variable.of.type"));
        panel.add(type, gbConstraints);

        gbConstraints.gridx++;
        myTypeSelector = myTypeSelectorManager.getTypeSelector();
        panel.add(myTypeSelector.getComponent(), gbConstraints);

        gbConstraints.gridwidth = 1;
        gbConstraints.weightx = 0;
        gbConstraints.weighty = 0;
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 1;
        JLabel namePrompt = new JLabel(RefactoringBundle.message("name.prompt"));
        namePrompt.setLabelFor(myNameField.getComponent());
        panel.add(namePrompt, gbConstraints);

        gbConstraints.gridwidth = 1;
        gbConstraints.weightx = 1;
        gbConstraints.gridx = 1;
        gbConstraints.gridy = 1;
        panel.add(myNameField.getComponent(), gbConstraints);

        myNameSuggestionsManager = new NameSuggestionsManager(myTypeSelector, myNameField,
                new NameSuggestionsGenerator() {
                    public SuggestedNameInfo getSuggestedNameInfo(PsiType type) {
                        return IntroduceVariableBase.getSuggestedName(type, myExpression);
                    }
                });
        myNameSuggestionsManager.setLabelsFor(type, namePrompt);

        return panel;
    }

    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbConstraints = new GridBagConstraints();
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.weightx = 1;
        gbConstraints.weighty = 0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.insets = new Insets(0, 0, 0, 0);

        if (myOccurrencesCount > 1) {
            myCbReplaceAll = new NonFocusableCheckBox();
            myCbReplaceAll.setText(RefactoringBundle.message("replace.all.occurences", myOccurrencesCount));

            panel.add(myCbReplaceAll, gbConstraints);
            myReplaceAllListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    updateControls();
                }
            };
            myCbReplaceAll.addItemListener(myReplaceAllListener);

            if (myAnyLValueOccurences) {
                myCbReplaceWrite = new StateRestoringCheckBox();
                myCbReplaceWrite.setText(RefactoringBundle.message("replace.write.access.occurrences"));
                gbConstraints.insets = new Insets(0, 8, 0, 0);
                gbConstraints.gridy++;
                panel.add(myCbReplaceWrite, gbConstraints);
                myCbReplaceWrite.addItemListener(myReplaceAllListener);
            }
        }

        myCbFinal = new NonFocusableCheckBox();
        myCbFinal.setText(RefactoringBundle.message("declare.final"));
        final Boolean createFinals = JavaRefactoringSettings.getInstance().INTRODUCE_LOCAL_CREATE_FINALS;
        myCbFinalState = createFinals == null ?
                CodeStyleSettingsManager.getSettings(myProject).GENERATE_FINAL_LOCALS :
                createFinals.booleanValue();

        gbConstraints.insets = new Insets(0, 0, 0, 0);
        gbConstraints.gridy++;
        panel.add(myCbFinal, gbConstraints);
        myFinalListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (myCbFinal.isEnabled()) {
                    myCbFinalState = myCbFinal.isSelected();
                }
            }
        };
        myCbFinal.addItemListener(myFinalListener);

        updateControls();

        return panel;
    }

    private void updateControls() {
        if (myCbReplaceWrite != null) {
            if (myCbReplaceAll.isSelected()) {
                myCbReplaceWrite.makeSelectable();
            } else {
                myCbReplaceWrite.makeUnselectable(true);
            }
        }

        if (myCbReplaceAll != null) {
            myTypeSelectorManager.setAllOccurrences(myCbReplaceAll.isSelected());
        } else {
            myTypeSelectorManager.setAllOccurrences(false);
        }

        if (myDeclareFinalIfAll && myCbReplaceAll != null && myCbReplaceAll.isSelected()) {
            myCbFinal.setEnabled(false);
            myCbFinal.setSelected(true);
        } else if (myCbReplaceWrite != null && myCbReplaceWrite.isEnabled() && myCbReplaceWrite.isSelected()) {
            myCbFinal.setEnabled(false);
            myCbFinal.setSelected(false);
        }
        else {
            myCbFinal.setEnabled(true);
            myCbFinal.setSelected(myCbFinalState);
        }
    }

    protected void doOKAction() {
        if (!myValidator.isOK(this)) return;
        myNameSuggestionsManager.nameSelected();
        myTypeSelectorManager.typeSelected(getSelectedType());
        if (myCbFinal.isEnabled()) {
            JavaRefactoringSettings.getInstance().INTRODUCE_LOCAL_CREATE_FINALS = myCbFinal.isSelected();
        }
        super.doOKAction();
    }

    private void updateOkStatus() {
        String text = getEnteredName();
        setOKActionEnabled(PsiNameHelper.getInstance(myProject).isIdentifier(text));
    }

    public JComponent getPreferredFocusedComponent() {
        return myNameField.getFocusableComponent();
    }

    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(HelpID.INTRODUCE_VARIABLE);
    }
}
