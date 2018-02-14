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
 * User: anna
 * Date: 24-Apr-2009
 */
package com.gome.maven.codeInspection.ex;

import com.gome.maven.psi.PsiElement;

import java.util.List;

public interface Tools {
    
    InspectionToolWrapper getInspectionTool(PsiElement element);

    
    String getShortName();

    
    InspectionToolWrapper getTool();

    
    List<ScopeToolState> getTools();

    
    ScopeToolState getDefaultState();

    boolean isEnabled();

    boolean isEnabled( PsiElement element);

    
    InspectionToolWrapper getEnabledTool( PsiElement element);
}