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

package com.gome.maven.openapi.fileEditor;

import com.gome.maven.pom.Navigatable;

/**
 * File editor which supports possibility to navigate to {@link com.gome.maven.pom.Navigatable} element
 *
 * @author spleaner
 */
public interface NavigatableFileEditor extends FileEditor {

    /**
     * Check whatever the editor can navigate to the given element
     *
     * @return true if editor can navigate, false otherwise
     */
    boolean canNavigateTo( final Navigatable navigatable);

    /**
     * Navigate editor to the given navigatable if {@link #canNavigateTo(com.gome.maven.pom.Navigatable)} is true
     *
     * @param navigatable navigation target
     */
    void navigateTo( final Navigatable navigatable);

}
