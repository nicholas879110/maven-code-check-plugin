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
package com.gome.maven.psi.search;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.editor.colors.CodeInsightColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.markup.TextAttributes;

public class TodoAttributesUtil {
    
    public static TodoAttributes createDefault() {
        return new TodoAttributes(AllIcons.General.TodoDefault, getDefaultColorSchemeTextAttributes());
    }

    
    public static TextAttributes getDefaultColorSchemeTextAttributes() {
        return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.TODO_DEFAULT_ATTRIBUTES).clone();
    }
}
