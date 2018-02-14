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

import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.openapi.extensions.ExtensionPointName;

/**
 * A macro which can be used in live templates.
 */
public abstract class Macro {
    public static final ExtensionPointName<Macro> EP_NAME = ExtensionPointName.create("com.intellij.liveTemplateMacro");

    
    public abstract String getName();

    /**
     * @return a presentable string that will be shown in the combobox in Edit Template Variables dialog
     */
    public abstract String getPresentableName();

    
    
    public String getDefaultValue() {
        return "";
    }

    
    public abstract Result calculateResult( Expression[] params, ExpressionContext context);

    
    public Result calculateQuickResult( Expression[] params, ExpressionContext context) {
        return null;
    }

    
    public LookupElement[] calculateLookupItems( Expression[] params, ExpressionContext context) {
        return null;
    }

    public boolean isAcceptableInContext(TemplateContextType context) {
        return true;
    }
}
