/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.project.Project;

/**
 * Defines common contract for building {@link EditorTextField} with necessary combinations of features.
 *
 * @author Denis Zhdanov
 * @since Aug 18, 2010 1:37:55 PM
 */
public interface EditorTextFieldProvider {

    /**
     * This factory method allows creation of an editor where the given customizations are applied to the editor.
     *
     * @param language   target language used by document that will be displayed by returned editor
     * @param project    target project
     * @return {@link EditorTextField} with specified customizations applied to its editor.
     */
    
    EditorTextField getEditorField( Language language,   Project project,  Iterable<EditorCustomization> features);

}
