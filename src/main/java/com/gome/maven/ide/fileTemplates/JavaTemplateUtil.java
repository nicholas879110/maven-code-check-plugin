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
package com.gome.maven.ide.fileTemplates;

import com.gome.maven.psi.*;

import java.util.Properties;

/**
 * @author yole
 */
public class JavaTemplateUtil {
    public static final String TEMPLATE_CATCH_BODY = "Catch Statement Body.java";
    public static final String TEMPLATE_IMPLEMENTED_METHOD_BODY = "Implemented Method Body.java";
    public static final String TEMPLATE_OVERRIDDEN_METHOD_BODY = "Overridden Method Body.java";
    public static final String TEMPLATE_FROM_USAGE_METHOD_BODY = "New Method Body.java";
    public static final String TEMPLATE_I18NIZED_EXPRESSION = "I18nized Expression.java";
    public static final String TEMPLATE_I18NIZED_CONCATENATION = "I18nized Concatenation.java";
    public static final String TEMPLATE_I18NIZED_JSP_EXPRESSION = "I18nized JSP Expression.jsp";

    public static final String INTERNAL_CLASS_TEMPLATE_NAME = "Class";
    public static final String INTERNAL_INTERFACE_TEMPLATE_NAME = "Interface";
    public static final String INTERNAL_ANNOTATION_TYPE_TEMPLATE_NAME = "AnnotationType";
    public static final String INTERNAL_ENUM_TEMPLATE_NAME = "Enum";

    public static final String[] INTERNAL_JAVA_TEMPLATES = new String[]{INTERNAL_CLASS_TEMPLATE_NAME,
            INTERNAL_INTERFACE_TEMPLATE_NAME, INTERNAL_ANNOTATION_TYPE_TEMPLATE_NAME, INTERNAL_ENUM_TEMPLATE_NAME};

    private JavaTemplateUtil() {
    }

    public static void setClassAndMethodNameProperties (Properties properties, PsiClass aClass, PsiMethod method) {
        String className = aClass.getQualifiedName();
        if (className == null) className = "";
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, className);

        String classSimpleName = aClass.getName();
        if (classSimpleName == null) classSimpleName = "";
        properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, classSimpleName);

        String methodName = method.getName();
        properties.setProperty(FileTemplate.ATTRIBUTE_METHOD_NAME, methodName);
    }

    public static String getPackageName( PsiDirectory directory) {
        PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
        return aPackage != null ? aPackage.getQualifiedName() : "";
    }

    public static void setPackageNameAttribute( Properties properties,  PsiDirectory directory) {
        properties.setProperty(FileTemplate.ATTRIBUTE_PACKAGE_NAME, getPackageName(directory));
    }
}
