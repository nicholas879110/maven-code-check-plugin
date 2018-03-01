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

/*
 * Created by IntelliJ IDEA.
 * User: dsl
 * Date: 05.06.2002
 * Time: 12:43:27
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.refactoring.rename;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiMethod;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.util.CommonRefactoringUtil;
import com.gome.maven.refactoring.util.RefactoringUIUtil;
import com.gome.maven.usageView.UsageViewUtil;

public class FieldHidesLocalUsageInfo extends UnresolvableCollisionUsageInfo {
    public FieldHidesLocalUsageInfo(PsiElement element, PsiElement referencedElement) {
        super(element, referencedElement);
    }

    public String getDescription() {
        String descr = RefactoringBundle.message("local.will.be.hidden.renamed",
                RefactoringUIUtil.getDescription(getElement(), true));
        return CommonRefactoringUtil.capitalize(descr);
    }
}
