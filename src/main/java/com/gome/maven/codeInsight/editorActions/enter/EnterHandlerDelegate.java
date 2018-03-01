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

package com.gome.maven.codeInsight.editorActions.enter;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.editor.actionSystem.EditorActionHandler;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.psi.PsiFile;

/**
 * @author yole
 */
public interface EnterHandlerDelegate {
    ExtensionPointName<EnterHandlerDelegate> EP_NAME = ExtensionPointName.create("com.gome.maven.enterHandlerDelegate");

    enum Result {
        Default, Continue, DefaultForceIndent, DefaultSkipIndent, Stop
    }

    Result preprocessEnter( final PsiFile file,  final Editor editor,  final Ref<Integer> caretOffset,
                            final Ref<Integer> caretAdvance,  final DataContext dataContext,
                            final EditorActionHandler originalHandler);

    Result postProcessEnter( PsiFile file,  Editor editor,  DataContext dataContext);
}
