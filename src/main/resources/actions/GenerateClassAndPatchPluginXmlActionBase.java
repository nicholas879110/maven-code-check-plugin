///*
// * Copyright 2000-2012 JetBrains s.r.o.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package actions;
//
//import com.gome.maven.openapi.project.Project;
//import com.gome.maven.openapi.ui.Messages;
//import com.gome.maven.psi.PsiClass;
//import com.gome.maven.psi.PsiDirectory;
//import com.gome.maven.psi.PsiElement;
//import com.gome.maven.psi.xml.XmlFile;
//import com.gome.maven.util.IncorrectOperationException;
//import org.jetbrains.idea.devkit.util.ComponentType;
//
//import javax.swing.*;
//
///**
// * @author max
// */
//public abstract class GenerateClassAndPatchPluginXmlActionBase extends GeneratePluginClassAction {
//  public GenerateClassAndPatchPluginXmlActionBase(String text, String description, @Nullable Icon icon) {
//    super(text, description, icon);
//  }
//
//  protected abstract String getClassNamePrompt();
//  protected abstract String getClassNamePromptTitle();
//
//  protected PsiElement[] invokeDialogImpl(Project project, PsiDirectory directory) {
//    MyInputValidator validator = new MyInputValidator(project, directory);
//    Messages.showInputDialog(project, getClassNamePrompt(), getClassNamePromptTitle(), Messages.getQuestionIcon(), "", validator);
//    return validator.getCreatedElements();
//  }
//
//  protected abstract ComponentType getComponentType();
//
//  public void patchPluginXml(XmlFile pluginXml, PsiClass klass) throws IncorrectOperationException {
//    getComponentType().patchPluginXml(pluginXml, klass);
//  }
//}
