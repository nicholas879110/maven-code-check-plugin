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

package com.gome.maven.codeInsight.template;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.PairProcessor;

import java.util.Map;

public abstract class TemplateManager {
    public static TemplateManager getInstance(Project project) {
        return ServiceManager.getService(project, TemplateManager.class);
    }

    public abstract void startTemplate( Editor editor,  Template template);

    public abstract void startTemplate( Editor editor, String selectionString,  Template template);

    public abstract void startTemplate( Editor editor,  Template template, TemplateEditingListener listener);

    public abstract void startTemplate( final Editor editor,
                                        final Template template,
                                       boolean inSeparateCommand,
                                       Map<String, String> predefinedVarValues,
                                        TemplateEditingListener listener);

    public abstract void startTemplate( Editor editor,
                                        Template template,
                                       TemplateEditingListener listener,
                                       final PairProcessor<String, String> callback);

    public abstract boolean startTemplate( Editor editor, char shortcutChar);

    public abstract Template createTemplate( String key, String group);

    public abstract Template createTemplate( String key, String group,  String text);

    
    public abstract Template getActiveTemplate( Editor editor);
}
