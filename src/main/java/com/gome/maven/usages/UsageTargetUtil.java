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
package com.gome.maven.usages;

import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataProvider;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UsageTargetUtil {
    private static final ExtensionPointName<UsageTargetProvider> EP_NAME = ExtensionPointName.create("com.intellij.usageTargetProvider");

    public static UsageTarget[] findUsageTargets(DataProvider dataProvider) {
        Editor editor = CommonDataKeys.EDITOR.getData(dataProvider);
        PsiFile file = CommonDataKeys.PSI_FILE.getData(dataProvider);

        List<UsageTarget> result = new ArrayList<UsageTarget>();
        if (file != null && editor != null) {
            UsageTarget[] targets = findUsageTargets(editor, file);
            if (targets != null) Collections.addAll(result, targets);
        }
        PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataProvider);
        if (psiElement != null) {
            UsageTarget[] targets = findUsageTargets(psiElement);
            if (targets != null)Collections.addAll(result, targets);
        }

        return result.isEmpty() ? null : result.toArray(new UsageTarget[result.size()]);
    }

    public static UsageTarget[] findUsageTargets(Editor editor, PsiFile file) {
        List<UsageTarget> result = new ArrayList<UsageTarget>();
        for (UsageTargetProvider provider : getProviders(file.getProject())) {
            UsageTarget[] targets = provider.getTargets(editor, file);
            if (targets != null) Collections.addAll(result, targets);
        }
        return result.isEmpty() ? null : result.toArray(new UsageTarget[result.size()]);
    }

    public static UsageTarget[] findUsageTargets(PsiElement psiElement) {
        List<UsageTarget> result = new ArrayList<UsageTarget>();
        for (UsageTargetProvider provider : getProviders(psiElement.getProject())) {
            UsageTarget[] targets = provider.getTargets(psiElement);
            if (targets != null) Collections.addAll(result, targets);
        }
        return result.isEmpty() ? null : result.toArray(new UsageTarget[result.size()]);
    }


    private static List<UsageTargetProvider> getProviders(Project project) {
        return DumbService.getInstance(project).filterByDumbAwareness(Extensions.getExtensions(EP_NAME));
    }
}
