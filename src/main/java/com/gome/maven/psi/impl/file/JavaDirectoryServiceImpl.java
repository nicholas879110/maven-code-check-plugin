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
 * @author max
 */
package com.gome.maven.psi.impl.file;

import com.gome.maven.core.CoreJavaDirectoryService;
import com.gome.maven.ide.fileTemplates.FileTemplate;
import com.gome.maven.ide.fileTemplates.FileTemplateManager;
import com.gome.maven.ide.fileTemplates.FileTemplateUtil;
import com.gome.maven.ide.fileTemplates.JavaTemplateUtil;
import com.gome.maven.ide.fileTemplates.ui.CreateFromTemplateDialog;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.JavaPsiImplementationHelper;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class JavaDirectoryServiceImpl extends CoreJavaDirectoryService {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.file.JavaDirectoryServiceImpl");

    @Override
    public PsiPackage getPackage( PsiDirectory dir) {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(dir.getProject()).getFileIndex();
        String packageName = projectFileIndex.getPackageNameByDirectory(dir.getVirtualFile());
        if (packageName == null) return null;
        return JavaPsiFacade.getInstance(dir.getProject()).findPackage(packageName);
    }

    @Override
    
    public PsiClass createClass( PsiDirectory dir,  String name) throws IncorrectOperationException {
        return createClassFromTemplate(dir, name, JavaTemplateUtil.INTERNAL_CLASS_TEMPLATE_NAME);
    }

    @Override
    
    public PsiClass createClass( PsiDirectory dir,  String name,  String templateName) throws IncorrectOperationException {
        return createClassFromTemplate(dir, name, templateName);
    }

    @Override
    public PsiClass createClass( PsiDirectory dir,
                                 String name,
                                 String templateName,
                                boolean askForUndefinedVariables) throws IncorrectOperationException {
        return createClass(dir, name, templateName, askForUndefinedVariables, Collections.<String, String>emptyMap());
    }

    @Override
    public PsiClass createClass( PsiDirectory dir,
                                 String name,
                                 String templateName,
                                boolean askForUndefinedVariables,  final Map<String, String> additionalProperties) throws IncorrectOperationException {
        return createClassFromTemplate(dir, name, templateName, askForUndefinedVariables, additionalProperties);
    }

    @Override
    
    public PsiClass createInterface( PsiDirectory dir,  String name) throws IncorrectOperationException {
        String templateName = JavaTemplateUtil.INTERNAL_INTERFACE_TEMPLATE_NAME;
        PsiClass someClass = createClassFromTemplate(dir, name, templateName);
        if (!someClass.isInterface()) {
            throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName, dir.getProject()));
        }
        return someClass;
    }

    @Override
    
    public PsiClass createEnum( PsiDirectory dir,  String name) throws IncorrectOperationException {
        String templateName = JavaTemplateUtil.INTERNAL_ENUM_TEMPLATE_NAME;
        PsiClass someClass = createClassFromTemplate(dir, name, templateName);
        if (!someClass.isEnum()) {
            throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName, dir.getProject()));
        }
        return someClass;
    }

    @Override
    
    public PsiClass createAnnotationType( PsiDirectory dir,  String name) throws IncorrectOperationException {
        String templateName = JavaTemplateUtil.INTERNAL_ANNOTATION_TYPE_TEMPLATE_NAME;
        PsiClass someClass = createClassFromTemplate(dir, name, templateName);
        if (!someClass.isAnnotationType()) {
            throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName, dir.getProject()));
        }
        return someClass;
    }

    private static PsiClass createClassFromTemplate( PsiDirectory dir, String name, String templateName) throws IncorrectOperationException {
        return createClassFromTemplate(dir, name, templateName, false, Collections.<String, String>emptyMap());
    }

    private static PsiClass createClassFromTemplate( PsiDirectory dir,
                                                    String name,
                                                    String templateName,
                                                    boolean askToDefineVariables,  Map<String, String> additionalProperties) throws IncorrectOperationException {
        //checkCreateClassOrInterface(dir, name);

        Project project = dir.getProject();
        FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate(templateName);

        Properties defaultProperties = FileTemplateManager.getInstance(project).getDefaultProperties();
        Properties properties = new Properties(defaultProperties);
        properties.setProperty(FileTemplate.ATTRIBUTE_NAME, name);
        for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }

        String ext = StdFileTypes.JAVA.getDefaultExtension();
        String fileName = name + "." + ext;

        PsiElement element;
        try {
            element = askToDefineVariables ? new CreateFromTemplateDialog(project, dir, template, null, properties).create()
                    : FileTemplateUtil.createFromTemplate(template, fileName, properties, dir);
        }
        catch (IncorrectOperationException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.error(e);
            return null;
        }
        if (element == null) return null;
        final PsiJavaFile file = (PsiJavaFile)element.getContainingFile();
        PsiClass[] classes = file.getClasses();
        if (classes.length < 1) {
            throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName, project));
        }
        return classes[0];
    }

    private static String getIncorrectTemplateMessage(String templateName, Project project) {
        return PsiBundle.message("psi.error.incorrect.class.template.message",
                FileTemplateManager.getInstance(project).internalTemplateToSubject(templateName), templateName);
    }

    @Override
    public void checkCreateClass( PsiDirectory dir,  String name) throws IncorrectOperationException {
        checkCreateClassOrInterface(dir, name);
    }

    public static void checkCreateClassOrInterface( PsiDirectory directory, String name) throws IncorrectOperationException {
        PsiUtil.checkIsIdentifier(directory.getManager(), name);

        String fileName = name + "." + StdFileTypes.JAVA.getDefaultExtension();
        directory.checkCreateFile(fileName);

        PsiNameHelper helper = PsiNameHelper.getInstance(directory.getProject());
        PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
        String qualifiedName = aPackage == null ? null : aPackage.getQualifiedName();
        if (!StringUtil.isEmpty(qualifiedName) && !helper.isQualifiedName(qualifiedName)) {
            throw new IncorrectOperationException("Cannot create class in invalid package: '"+qualifiedName+"'");
        }
    }

    @Override
    public boolean isSourceRoot( PsiDirectory dir) {
        final VirtualFile file = dir.getVirtualFile();
        final VirtualFile sourceRoot = ProjectRootManager.getInstance(dir.getProject()).getFileIndex().getSourceRootForFile(file);
        return file.equals(sourceRoot);
    }

    @Override
    public LanguageLevel getLanguageLevel( PsiDirectory dir) {
        return JavaPsiImplementationHelper.getInstance(dir.getProject()).getEffectiveLanguageLevel(dir.getVirtualFile());
    }

}
