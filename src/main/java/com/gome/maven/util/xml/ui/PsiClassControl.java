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
package com.gome.maven.util.xml.ui;

import com.gome.maven.ide.util.ClassFilter;
import com.gome.maven.ide.util.TreeClassChooser;
import com.gome.maven.ide.util.TreeClassChooserFactory;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.module.ModuleUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.PsiCodeFragmentImpl;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.ui.EditorTextField;
import com.gome.maven.ui.JavaReferenceEditorUtil;
import com.gome.maven.ui.ReferenceEditorWithBrowseButton;
import com.gome.maven.ui.UIBundle;
import com.gome.maven.util.xml.DomElement;
import com.gome.maven.util.xml.ExtendClass;
import com.gome.maven.util.xml.GenericDomValue;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author peter
 */
public class PsiClassControl extends EditorTextFieldControl<PsiClassPanel> {

    public PsiClassControl(final DomWrapper<String> domWrapper) {
        super(domWrapper);
    }

    public PsiClassControl(final DomWrapper<String> domWrapper, final boolean commitOnEveryChange) {
        super(domWrapper, commitOnEveryChange);
    }

    protected EditorTextField getEditorTextField( final PsiClassPanel component) {
        return ((ReferenceEditorWithBrowseButton)component.getComponent(0)).getEditorTextField();
    }

    protected PsiClassPanel createMainComponent(PsiClassPanel boundedComponent, final Project project) {
        if (boundedComponent == null) {
            boundedComponent = new PsiClassPanel();
        }
        ReferenceEditorWithBrowseButton editor = JavaReferenceEditorUtil.createReferenceEditorWithBrowseButton(null, "", project, true);
        Document document = editor.getChildComponent().getDocument();
        PsiCodeFragmentImpl fragment = (PsiCodeFragmentImpl) PsiDocumentManager.getInstance(project).getPsiFile(document);
        assert fragment != null;
        fragment.setIntentionActionsFilter(IntentionFilterOwner.IntentionActionsFilter.EVERYTHING_AVAILABLE);
        fragment.putUserData(ModuleUtil.KEY_MODULE, getDomWrapper().getExistingDomElement().getModule());
        return initReferenceEditorWithBrowseButton(boundedComponent, editor, this);
    }

    protected static <T extends JPanel> T initReferenceEditorWithBrowseButton(final T boundedComponent,
                                                                              final ReferenceEditorWithBrowseButton editor,
                                                                              final EditorTextFieldControl control) {
        boundedComponent.removeAll();
        boundedComponent.add(editor);
        final GlobalSearchScope resolveScope = control.getDomWrapper().getResolveScope();
        editor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                final DomElement domElement = control.getDomElement();
                ExtendClass extend = domElement.getAnnotation(ExtendClass.class);
                PsiClass baseClass = null;
                ClassFilter filter = null;
                if (extend != null) {
                    baseClass = JavaPsiFacade.getInstance(control.getProject()).findClass(extend.value(), resolveScope);
                    if (extend.instantiatable()) {
                        filter = ClassFilter.INSTANTIABLE;
                    }
                }

                PsiClass initialClass = null;
                if (domElement instanceof GenericDomValue) {
                    final Object value = ((GenericDomValue)domElement).getValue();
                    if (value instanceof PsiClass)
                        initialClass = (PsiClass)value;
                }

                TreeClassChooser chooser = TreeClassChooserFactory.getInstance(control.getProject())
                        .createInheritanceClassChooser(UIBundle.message("choose.class"), resolveScope, baseClass, initialClass, filter);
                chooser.showDialog();
                final PsiClass psiClass = chooser.getSelected();
                if (psiClass != null) {
                    control.setValue(psiClass.getQualifiedName());
                }
            }
        });
        return boundedComponent;
    }

}
