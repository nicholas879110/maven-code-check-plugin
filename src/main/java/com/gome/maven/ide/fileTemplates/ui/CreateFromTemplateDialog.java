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

package com.gome.maven.ide.fileTemplates.ui;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.actions.CreateFileAction;
import com.gome.maven.ide.fileTemplates.FileTemplate;
import com.gome.maven.ide.fileTemplates.FileTemplateManager;
import com.gome.maven.ide.fileTemplates.FileTemplateUtil;
import com.gome.maven.ide.fileTemplates.actions.AttributesDefaults;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import org.apache.velocity.runtime.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class CreateFromTemplateDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.fileTemplates.ui.CreateFromTemplateDialog");
     private final PsiDirectory myDirectory;
     private final Project myProject;
    private PsiElement myCreatedElement;
    private final CreateFromTemplatePanel myAttrPanel;
    private final JComponent myAttrComponent;
     private final FileTemplate myTemplate;
    private final Properties myDefaultProperties;

    public CreateFromTemplateDialog( Project project,
                                     PsiDirectory directory,
                                     FileTemplate template,
                                     final AttributesDefaults attributesDefaults,
                                     final Properties defaultProperties) {
        super(project, true);
        myDirectory = directory;
        myProject = project;
        myTemplate = template;
        setTitle(IdeBundle.message("title.new.from.template", template.getName()));

        myDefaultProperties = defaultProperties == null ? FileTemplateManager.getInstance(project).getDefaultProperties() : defaultProperties;
        FileTemplateUtil.fillDefaultProperties(myDefaultProperties, directory);
        boolean mustEnterName = FileTemplateUtil.findHandler(template).isNameRequired();
        if (attributesDefaults != null && attributesDefaults.isFixedName()) {
            myDefaultProperties.setProperty(FileTemplate.ATTRIBUTE_NAME, attributesDefaults.getDefaultFileName());
            mustEnterName = false;
        }

        String[] unsetAttributes = null;
        try {
            unsetAttributes = myTemplate.getUnsetAttributes(myDefaultProperties, project);
        }
        catch (ParseException e) {
            showErrorDialog(e);
        }

        if (unsetAttributes != null) {
            myAttrPanel = new CreateFromTemplatePanel(unsetAttributes, mustEnterName, attributesDefaults);
            myAttrComponent = myAttrPanel.getComponent();
            init();
        }
        else {
            myAttrPanel = null;
            myAttrComponent = null;
        }
    }

    public PsiElement create(){
        if (myAttrPanel != null) {
            if (myAttrPanel.hasSomethingToAsk()) {
                show();
                return myCreatedElement;
            }
            doCreate(null);
        }
        close(DialogWrapper.OK_EXIT_CODE);
        return myCreatedElement;
    }

    @Override
    protected void doOKAction(){
        String fileName = myAttrPanel.getFileName();
        if (fileName != null && fileName.length() == 0) {
            Messages.showMessageDialog(myAttrComponent, IdeBundle.message("error.please.enter.a.file.name"), CommonBundle.getErrorTitle(),
                    Messages.getErrorIcon());
            return;
        }
        doCreate(fileName);
        if ( myCreatedElement != null ) {
            super.doOKAction();
        }
    }

    private void doCreate( String fileName)  {
        try {
            String newName = fileName;
            PsiDirectory directory = myDirectory;
            if (fileName != null) {
                final String finalFileName = fileName;
                CreateFileAction.MkDirs mkDirs = ApplicationManager.getApplication().runWriteAction(new Computable<CreateFileAction.MkDirs>() {
                    @Override
                    public CreateFileAction.MkDirs compute() {
                        return new CreateFileAction.MkDirs(finalFileName, myDirectory);
                    }
                });
                newName = mkDirs.newName;
                directory = mkDirs.directory;
            }
            myCreatedElement = FileTemplateUtil.createFromTemplate(myTemplate, newName, myAttrPanel.getProperties(myDefaultProperties), directory);
        }
        catch (Exception e) {
            showErrorDialog(e);
        }
    }

    public Properties getEnteredProperties() {
        return myAttrPanel.getProperties(new Properties());
    }

    private void showErrorDialog(final Exception e) {
        LOG.info(e);
        Messages.showMessageDialog(myProject, filterMessage(e.getMessage()), getErrorMessage(), Messages.getErrorIcon());
    }

    private String getErrorMessage() {
        return FileTemplateUtil.findHandler(myTemplate).getErrorMessage();
    }

    
    private String filterMessage(String message){
        if (message == null) {
            message = "unknown error";
        }

         String ioExceptionPrefix = "java.io.IOException:";
        if (message.startsWith(ioExceptionPrefix)){
            return message.substring(ioExceptionPrefix.length());
        }
        if (message.contains("File already exists")){
            return message;
        }

        return IdeBundle.message("error.unable.to.parse.template.message", myTemplate.getName(), message);
    }

    @Override
    protected JComponent createCenterPanel(){
        myAttrPanel.ensureFitToScreen(200, 200);
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.add(myAttrComponent, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        return centerPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent(){
        return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myAttrComponent);
    }
}
