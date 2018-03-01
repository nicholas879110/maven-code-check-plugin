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
package com.gome.maven.refactoring.ui;

import com.gome.maven.ide.util.PackageChooserDialog;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiPackage;
import com.gome.maven.ui.ReferenceEditorComboWithBrowseButton;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author yole
 */
public class PackageNameReferenceEditorCombo extends ReferenceEditorComboWithBrowseButton {
    public PackageNameReferenceEditorCombo(final String text,  final Project project,
                                           final String recentsKey, final String chooserTitle) {
        super(null, text, project, false, recentsKey);
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final PackageChooserDialog chooser = new PackageChooserDialog(chooserTitle, project);
                chooser.selectPackage(getText());
                if (chooser.showAndGet()) {
                    final PsiPackage aPackage = chooser.getSelectedPackage();
                    if (aPackage != null) {
                        setText(aPackage.getQualifiedName());
                    }
                }
            }
        });
    }
}