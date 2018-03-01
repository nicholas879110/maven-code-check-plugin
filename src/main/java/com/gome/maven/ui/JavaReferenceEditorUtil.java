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
package com.gome.maven.ui;

import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.util.NullableFunction;

import java.awt.event.ActionListener;

/**
 * @author yole
 */
public class JavaReferenceEditorUtil {
    private JavaReferenceEditorUtil() {
    }

    public static ReferenceEditorWithBrowseButton createReferenceEditorWithBrowseButton(final ActionListener browseActionListener,
                                                                                        final String text,
                                                                                        final Project project,
                                                                                        final boolean toAcceptClasses) {
        return new ReferenceEditorWithBrowseButton(browseActionListener, project,
                new NullableFunction<String,Document>() {
                    public Document fun(final String s) {
                        return createDocument(s, project, toAcceptClasses);
                    }
                }, text);
    }

    
    public static Document createDocument(final String text,
                                          Project project,
                                          boolean isClassesAccepted) {
        return createDocument(text, project, isClassesAccepted, JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE);
    }

    
    public static Document createDocument(final String text,
                                          Project project,
                                          boolean isClassesAccepted,
                                          JavaCodeFragment.VisibilityChecker visibilityChecker) {
        final PsiPackage defaultPackage = JavaPsiFacade.getInstance(project).findPackage("");
        final JavaCodeFragmentFactory factory = JavaCodeFragmentFactory.getInstance(project);
        final JavaCodeFragment fragment = factory.createReferenceCodeFragment(text, defaultPackage, true, isClassesAccepted);
        fragment.setVisibilityChecker(visibilityChecker);
        return PsiDocumentManager.getInstance(project).getDocument(fragment);
    }

    
    public static Document createTypeDocument(final String text, Project project) {
        final PsiPackage defaultPackage = JavaPsiFacade.getInstance(project).findPackage("");
        final JavaCodeFragmentFactory factory = JavaCodeFragmentFactory.getInstance(project);
        final JavaCodeFragment fragment = factory.createTypeCodeFragment(text, defaultPackage, true);
        fragment.setVisibilityChecker(JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE);
        return PsiDocumentManager.getInstance(project).getDocument(fragment);
    }
}
