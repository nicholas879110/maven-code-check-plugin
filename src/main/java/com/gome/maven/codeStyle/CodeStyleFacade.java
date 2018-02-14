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

/*
 * @author max
 */
package com.gome.maven.codeStyle;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.project.Project;

public abstract class CodeStyleFacade {
    public static CodeStyleFacade getInstance() {
        return ServiceManager.getService(CodeStyleFacade.class);
    }

    public static CodeStyleFacade getInstance( Project project) {
        if (project == null) return getInstance();
        return ServiceManager.getService(project, CodeStyleFacade.class);
    }

    /**
     * Calculates the indent that should be used for the line at specified offset in the specified
     * document.
     *
     * @param document the document for which the indent should be calculated.
     * @param offset the caret offset in the editor.
     * @return the indent string (containing of tabs and/or white spaces), or null if it
     *         was not possible to calculate the indent.
     */
    
    public abstract String getLineIndent( Document document, int offset);

    public abstract int getIndentSize(FileType fileType);

    public abstract boolean isSmartTabs(final FileType fileType);

    public abstract int getRightMargin(Language language);

    public abstract boolean isWrapWhenTypingReachesRightMargin();

    public abstract int getTabSize(final FileType fileType);

    public abstract boolean useTabCharacter(final FileType fileType);

    public abstract String getLineSeparator();

    public abstract boolean projectUsesOwnSettings();

    public abstract boolean isUnsuitableCodeStyleConfigurable(Configurable c);
}