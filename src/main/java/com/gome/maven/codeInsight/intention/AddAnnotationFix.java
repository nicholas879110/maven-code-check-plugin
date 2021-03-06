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

package com.gome.maven.codeInsight.intention;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiModifierListOwner;
import com.gome.maven.psi.PsiNameValuePair;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author ven
 */
public class AddAnnotationFix extends AddAnnotationPsiFix implements IntentionAction {
    public AddAnnotationFix( String fqn,  PsiModifierListOwner modifierListOwner,  String... annotationsToRemove) {
        this(fqn, modifierListOwner, PsiNameValuePair.EMPTY_ARRAY, annotationsToRemove);
    }

    public AddAnnotationFix( String fqn,
                             PsiModifierListOwner modifierListOwner,
                             PsiNameValuePair[] values,
                             String... annotationsToRemove) {
        super(fqn, modifierListOwner, values, annotationsToRemove);
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        return isAvailable();
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        applyFix();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
