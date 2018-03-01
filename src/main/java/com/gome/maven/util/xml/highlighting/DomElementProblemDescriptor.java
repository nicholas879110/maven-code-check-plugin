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

package com.gome.maven.util.xml.highlighting;

import com.gome.maven.codeInspection.CommonProblemDescriptor;
import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.codeInspection.ProblemHighlightType;
import com.gome.maven.lang.annotation.Annotation;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.util.xml.DomElement;

import java.util.List;

public interface DomElementProblemDescriptor extends CommonProblemDescriptor {

    
    DomElement getDomElement();
    
    HighlightSeverity getHighlightSeverity();
    @Override
    
    LocalQuickFix[] getFixes();
    
    List<Annotation> getAnnotations();

    void highlightWholeElement();

    
    ProblemHighlightType getHighlightType();
}
