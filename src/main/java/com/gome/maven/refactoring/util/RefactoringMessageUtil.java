
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

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.psi.JavaDirectoryService;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.usageView.UsageViewUtil;

public class RefactoringMessageUtil {

    public static String getIncorrectIdentifierMessage(String identifierName) {
        return RefactoringBundle.message("0.is.not.a.legal.java.identifier", identifierName);
    }

    /**
     * @return null, if can create a class
     *         an error message, if cannot create a class
     *
     */
    public static String checkCanCreateClass(PsiDirectory destinationDirectory, String className) {
        PsiClass[] classes = JavaDirectoryService.getInstance().getClasses(destinationDirectory);
        VirtualFile file = destinationDirectory.getVirtualFile();
        for (PsiClass aClass : classes) {
            if (className.equals(aClass.getName())) {
                return RefactoringBundle.message("directory.0.already.contains.1.named.2",
                        file.getPresentableUrl(), UsageViewUtil.getType(aClass), className);
            }
        }
         String fileName = className+".java";
        return checkCanCreateFile(destinationDirectory, fileName);
    }
    public static String checkCanCreateFile(PsiDirectory destinationDirectory, String fileName) {
        VirtualFile file = destinationDirectory.getVirtualFile();
        VirtualFile child = file.findChild(fileName);
        if (child != null) {
            return RefactoringBundle.message("directory.0.already.contains.a.file.named.1",
                    file.getPresentableUrl(), fileName);
        }
        return null;
    }

    public static String getGetterSetterMessage(String newName, String action, PsiMethod getter, PsiMethod setter) {
        String text;
        if (getter != null && setter != null) {
            text = RefactoringBundle.message("getter.and.setter.methods.found.for.the.field.0", newName, action);
        }
        else if (getter != null) {
            text = RefactoringBundle.message("getter.method.found.for.the.field.0", newName, action);
        }
        else {
            text = RefactoringBundle.message("setter.method.found.for.the.field.0", newName, action);
        }
        return text;
    }

    public static void showNotSupportedForJspClassesError(final Project project, Editor editor, final String refactoringName, final String helpId) {
        String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("refactoring.is.not.supported.for.jsp.classes"));
        CommonRefactoringUtil.showErrorHint(project, editor, message, refactoringName, helpId);
    }
}