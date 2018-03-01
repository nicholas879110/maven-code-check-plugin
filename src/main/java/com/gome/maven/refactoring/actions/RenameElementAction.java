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

package com.gome.maven.refactoring.actions;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.psi.SyntheticElement;
import com.gome.maven.refactoring.RefactoringActionHandler;
import com.gome.maven.refactoring.rename.RenameHandlerRegistry;

public class RenameElementAction extends BaseRefactoringAction {

    public RenameElementAction() {
        setInjectedContext(true);
    }

    @Override
    public boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    public boolean isEnabledOnElements( PsiElement[] elements) {
        if (elements.length != 1) return false;

        PsiElement element = elements[0];
        return element instanceof PsiNamedElement && !(element instanceof SyntheticElement);
    }

    @Override
    public RefactoringActionHandler getHandler( DataContext dataContext) {
        return RenameHandlerRegistry.getInstance().getRenameHandler(dataContext);
    }

    @Override
    protected boolean hasAvailableHandler( DataContext dataContext) {
        return isEnabledOnDataContext(dataContext);
    }

    @Override
    protected boolean isEnabledOnDataContext(DataContext dataContext) {
        return RenameHandlerRegistry.getInstance().hasAvailableHandler(dataContext);
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        return true;
    }

    @Override
    protected boolean isAvailableOnElementInEditorAndFile( PsiElement element,  Editor editor,  PsiFile file,  DataContext context) {
        return RenameHandlerRegistry.getInstance().hasAvailableHandler(context);
    }
}
