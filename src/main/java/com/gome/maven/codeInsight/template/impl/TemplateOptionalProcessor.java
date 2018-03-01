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

package com.gome.maven.codeInsight.template.impl;

import com.gome.maven.codeInsight.template.Template;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;

/**
 * @author yole
 */
public interface TemplateOptionalProcessor {
    ExtensionPointName<TemplateOptionalProcessor> EP_NAME = ExtensionPointName.create("com.gome.maven.liveTemplateOptionalProcessor");

    void processText(final Project project, final Template template, final Document document, final RangeMarker templateRange, final Editor editor);

    String getOptionName();

    boolean isEnabled(final Template template);
    void setEnabled(Template template, boolean value);

    boolean isVisible(final Template template);
}
