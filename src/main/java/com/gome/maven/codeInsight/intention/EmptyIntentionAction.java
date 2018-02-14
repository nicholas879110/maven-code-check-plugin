/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.IncorrectOperationException;

import javax.swing.*;
import java.util.List;

/**
 * User: anna
 * Date: May 11, 2005
 */
public final class EmptyIntentionAction implements IntentionAction, LowPriorityAction, Iconable {
    private final String myName;

    public EmptyIntentionAction( String name) {
        myName = name;
    }

    @Override
    
    public String getText() {
        return InspectionsBundle.message("inspection.options.action.text", myName);
    }

    @Override
    
    public String getFamilyName() {
        return myName;
    }

    @Override
    public boolean isAvailable( Project project, Editor editor, PsiFile file) {
        return true; //edit inspection settings is always enabled
    }

    @Override
    public void invoke( Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final EmptyIntentionAction that = (EmptyIntentionAction)o;

        return myName.equals(that.myName);
    }

    public int hashCode() {
        return myName.hashCode();
    }

    // used by TeamCity plugin
    @Deprecated
    public EmptyIntentionAction( final String name,  List<IntentionAction> options) {
        myName = name;
    }

    @Override
    public Icon getIcon(@IconFlags int flags) {
        return AllIcons.Actions.RealIntentionBulb;
    }
}
