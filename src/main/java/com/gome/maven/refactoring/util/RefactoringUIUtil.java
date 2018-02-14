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

package com.gome.maven.refactoring.util;

import com.gome.maven.lang.findUsages.DescriptiveNameUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.ElementDescriptionUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.usageView.UsageViewUtil;

/**
 * @author yole
 */
public class RefactoringUIUtil {
    private RefactoringUIUtil() {
    }

    public static String getDescription( PsiElement element, boolean includeParent) {
        return ElementDescriptionUtil.getElementDescription(element, includeParent
                ? RefactoringDescriptionLocation.WITH_PARENT
                : RefactoringDescriptionLocation.WITHOUT_PARENT);
    }

    public static void processIncorrectOperation(final Project project, IncorrectOperationException e) {
         String message = e.getMessage();
        final int index = message != null ? message.indexOf("java.io.IOException") : -1;
        if (index > 0) {
            message = message.substring(index + "java.io.IOException".length());
        }

        final String s = message;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Messages.showMessageDialog(project, s, RefactoringBundle.message("error.title"), Messages.getErrorIcon());
            }
        });
    }

    public static String calculatePsiElementDescriptionList(PsiElement[] elements) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) buffer.append(", ");
            buffer.append(UsageViewUtil.getType(elements[i]));
            buffer.append(" ");
            buffer.append(DescriptiveNameUtil.getDescriptiveName(elements[i]));
        }

        return buffer.toString();
    }
}
