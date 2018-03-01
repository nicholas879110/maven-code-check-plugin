/*
 * Copyright 2001-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.java.generate.inspection;

import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.util.PsiTreeUtil;
import org.jetbrains.java.generate.GenerateToStringActionHandler;

/**
 * Quick fix to run Generate toString() to fix any code inspection problems.
 */
public class GenerateToStringQuickFix implements LocalQuickFix {

  public static final GenerateToStringQuickFix INSTANCE = new GenerateToStringQuickFix();

  private GenerateToStringQuickFix() {}

  public static GenerateToStringQuickFix getInstance() {
    return INSTANCE;
  }

  @Override
  
  public String getName() {
    return "Generate toString()";
  }

  @Override
  
  public String getFamilyName() {
    return "Generate";
  }

  @Override
  public void applyFix( Project project,  ProblemDescriptor desc) {
    final PsiClass clazz = PsiTreeUtil.getParentOfType(desc.getPsiElement(), PsiClass.class);
    if (clazz == null) {
      return; // no class to fix
    }
    GenerateToStringActionHandler handler = ServiceManager.getService(GenerateToStringActionHandler.class);
    handler.executeActionQuickFix(project, clazz);
  }
}
