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
 * Date: 26-Jun-2007
 */
package com.gome.maven.codeInsight;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.NotNullLazyKey;
import com.gome.maven.psi.*;
import com.gome.maven.util.messages.Topic;

import java.util.List;

public abstract class ExternalAnnotationsManager {
     public static final String ANNOTATIONS_XML = "annotations.xml";

    public static final Topic<ExternalAnnotationsListener> TOPIC = Topic.create("external annotations", ExternalAnnotationsListener.class);

    public enum AnnotationPlace {
        IN_CODE,
        EXTERNAL,
        NOWHERE
    }

    private static final NotNullLazyKey<ExternalAnnotationsManager, Project> INSTANCE_KEY = ServiceManager.createLazyKey(ExternalAnnotationsManager.class);

    public static ExternalAnnotationsManager getInstance( Project project) {
        return INSTANCE_KEY.getValue(project);
    }

    public abstract boolean isExternalAnnotation( PsiAnnotation annotation);

    
    public abstract PsiAnnotation findExternalAnnotation( PsiModifierListOwner listOwner,  String annotationFQN);

    // Method used in Kotlin plugin
    public abstract boolean isExternalAnnotationWritable( PsiModifierListOwner listOwner,  String annotationFQN);

    
    public abstract PsiAnnotation[] findExternalAnnotations( PsiModifierListOwner listOwner);

    public abstract void annotateExternally( PsiModifierListOwner listOwner,
                                             String annotationFQName,
                                             PsiFile fromFile,
                                             PsiNameValuePair[] value);

    public abstract boolean deannotate( PsiModifierListOwner listOwner,  String annotationFQN);

    // Method used in Kotlin plugin when it is necessary to leave external annotation, but modify its arguments
    public abstract boolean editExternalAnnotation( PsiModifierListOwner listOwner,  String annotationFQN,
                                                    PsiNameValuePair[] value);

    public abstract AnnotationPlace chooseAnnotationsPlace( PsiElement element);

    
    public abstract List<PsiFile> findExternalAnnotationsFiles( PsiModifierListOwner listOwner);

}
