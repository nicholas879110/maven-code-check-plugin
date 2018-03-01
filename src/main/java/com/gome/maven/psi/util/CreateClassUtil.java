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
package com.gome.maven.psi.util;

import com.gome.maven.ide.fileTemplates.FileTemplate;
import com.gome.maven.ide.fileTemplates.FileTemplateManager;
import com.gome.maven.ide.fileTemplates.JavaCreateFromTemplateHandler;
import com.gome.maven.ide.util.PackageUtil;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.text.StringTokenizer;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * author: lesya
 */
public class CreateClassUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.j2ee.CreateClassUtil");

     public static final String DEFAULT_CLASS_TEMPLATE = "#DEFAULT_CLASS_TEMPLATE";
     private static final String DO_NOT_CREATE_CLASS_TEMPLATE = "#DO_NOT_CREATE_CLASS_TEMPLATE";
     private static final String CLASS_NAME_PROPERTY = "Class_Name";
     private static final String INTERFACE_NAME_PROPERTY = "Interface_Name";

    private CreateClassUtil() {}

    
    private static PsiClass createClassFromTemplate( final Properties attributes,  final String templateName,
                                                     final PsiDirectory directoryRoot,
                                                     final String className) throws IncorrectOperationException {
        if (templateName == null) return null;
        if (templateName.equals(DO_NOT_CREATE_CLASS_TEMPLATE)) return null;

        final Project project = directoryRoot.getProject();
        try {
            final PsiDirectory directory = createParentDirectories(directoryRoot, className);
            final PsiFile psiFile = directory.findFile(className + "." + StdFileTypes.JAVA.getDefaultExtension());
            if (psiFile != null) {
                psiFile.delete();
            }

            final String rawClassName = extractClassName(className);
            final PsiFile existing = directory.findFile(rawClassName + ".java");
            if (existing instanceof PsiJavaFile) {
                final PsiClass[] classes = ((PsiJavaFile)existing).getClasses();
                if (classes.length > 0) {
                    return classes[0];
                }
                return null;
            }

            final PsiClass aClass;
            if (templateName.equals(DEFAULT_CLASS_TEMPLATE)) {
                aClass = JavaDirectoryService.getInstance().createClass(directory, rawClassName);
            }
            else {
                final FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
                FileTemplate fileTemplate = fileTemplateManager.getJ2eeTemplate(templateName);
                LOG.assertTrue(fileTemplate != null, templateName + " not found");
                final String text = fileTemplate.getText(attributes);
                aClass = JavaCreateFromTemplateHandler.createClassOrInterface(project, directory, text, true, fileTemplate.getExtension());
            }
            return (PsiClass)JavaCodeStyleManager.getInstance(project).shortenClassReferences(aClass);
        }
        catch (IOException e) {
            throw new IncorrectOperationException(e);
        }
    }

    
    private static PsiDirectory createParentDirectories( PsiDirectory directoryRoot,  String className) throws IncorrectOperationException {
        final PsiPackage currentPackage = JavaDirectoryService.getInstance().getPackage(directoryRoot);
        final String packagePrefix = currentPackage == null? null : currentPackage.getQualifiedName() + ".";
        final String packageName = extractPackage(packagePrefix != null && className.startsWith(packagePrefix)?
                className.substring(packagePrefix.length()) : className);
        final StringTokenizer tokenizer = new StringTokenizer(packageName, ".");
        while (tokenizer.hasMoreTokens()) {
            String packagePart = tokenizer.nextToken();
            PsiDirectory subdirectory = directoryRoot.findSubdirectory(packagePart);
            if (subdirectory == null) {
                directoryRoot.checkCreateSubdirectory(packagePart);
                subdirectory = directoryRoot.createSubdirectory(packagePart);
            }
            directoryRoot = subdirectory;
        }
        return directoryRoot;
    }

    public static String extractClassName(String fqName) {
        return StringUtil.getShortName(fqName);
    }

    public static String extractPackage(String fqName) {
        int i = fqName.lastIndexOf('.');
        return i == -1 ? "" : fqName.substring(0, i);
    }

    public static String makeFQName(String aPackage, String className) {
        String fq = aPackage;
        if (!"".equals(aPackage)) {
            fq += ".";
        }
        fq += className;
        return fq;
    }

    
    public static PsiClass createClassNamed(String newClassName, String templateName,  PsiDirectory directory) throws IncorrectOperationException {
        return createClassNamed(newClassName, FileTemplateManager.getInstance(directory.getProject()).getDefaultProperties(), templateName, directory);
    }

    
    public static PsiClass createClassNamed(String newClassName, Map classProperties, String templateName,  PsiDirectory directory)
            throws IncorrectOperationException {
        Properties defaultProperties = FileTemplateManager.getInstance(directory.getProject()).getDefaultProperties();
        Properties properties = new Properties(defaultProperties);
        properties.putAll(classProperties);

        return createClassNamed(newClassName, properties, templateName, directory);
    }

    
    private static PsiClass createClassNamed( String newClassName,
                                              Properties properties,
                                             String templateName,
                                              PsiDirectory directory) throws IncorrectOperationException {
        if (newClassName == null) {
            return null;
        }
        final String className = extractClassName(newClassName);
        properties.setProperty(CLASS_NAME_PROPERTY, className);
        properties.setProperty(INTERFACE_NAME_PROPERTY, className);

        return createClassFromTemplate(properties, templateName, directory, newClassName);
    }

    
    public static PsiClass createClassFromCustomTemplate( PsiDirectory classDirectory,
                                                          final Module module,
                                                         final String className,
                                                         final String templateName) {
        if (classDirectory == null && module != null) {
            try {
                classDirectory = PackageUtil.findOrCreateDirectoryForPackage(module, "", null, false);
            }
            catch (IncorrectOperationException e) {
                return null;
            }
        }
        if (classDirectory == null) {
            return null;
        }
        try {
            final Properties properties = ApplicationManager.getApplication().isUnitTestMode() ?
                    new Properties() :
                    FileTemplateManager.getInstance(classDirectory.getProject()).getDefaultProperties();
            return createClassNamed(className, new Properties(properties), templateName, classDirectory);
        }
        catch (IncorrectOperationException e) {
            LOG.error(e);
            return null;
        }
    }
}
