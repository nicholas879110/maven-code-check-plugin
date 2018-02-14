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
package com.gome.maven.codeInsight.template;

import com.gome.maven.codeInsight.template.impl.TemplateImpl;
import com.gome.maven.codeInsight.template.impl.TemplateManagerImpl;
import com.gome.maven.codeInsight.template.impl.TemplateSettings;
import com.gome.maven.diagnostic.AttachmentFactory;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.ScrollType;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
public class CustomTemplateCallback {
    private static final Logger LOGGER = Logger.getInstance(CustomTemplateCallback.class);
    private final TemplateManager myTemplateManager;
     private final Editor myEditor;
     private final PsiFile myFile;
    private final int myOffset;
     private final Project myProject;
    private final boolean myInInjectedFragment;
    private Set<TemplateContextType> myApplicableContextTypes;

    public CustomTemplateCallback( Editor editor,  PsiFile file) {
        myProject = file.getProject();
        myTemplateManager = TemplateManager.getInstance(myProject);

        myOffset = getOffset(editor);
        PsiElement element = InjectedLanguageUtil.findInjectedElementNoCommit(file, myOffset);
        myFile = element != null ? element.getContainingFile() : file;

        myInInjectedFragment = InjectedLanguageManager.getInstance(myProject).isInjectedFragment(myFile);
        myEditor = myInInjectedFragment ? InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file, myOffset) : editor;
    }

    public TemplateManager getTemplateManager() {
        return myTemplateManager;
    }

    
    public PsiFile getFile() {
        return myFile;
    }

    
    public PsiElement getContext() {
        return getContext(myFile, getOffset(), myInInjectedFragment);
    }

    public int getOffset() {
        return myOffset;
    }

    public static int getOffset( Editor editor) {
        return Math.max(editor.getSelectionModel().getSelectionStart() - 1, 0);
    }


    public TemplateImpl findApplicableTemplate( String key) {
        return ContainerUtil.getFirstItem(findApplicableTemplates(key));
    }

    
    public List<TemplateImpl> findApplicableTemplates( String key) {
        List<TemplateImpl> result = new ArrayList<TemplateImpl>();
        for (TemplateImpl candidate : getMatchingTemplates(key)) {
            if (isAvailableTemplate(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private boolean isAvailableTemplate( TemplateImpl template) {
        if (myApplicableContextTypes == null) {
            myApplicableContextTypes = TemplateManagerImpl.getApplicableContextTypes(myFile, myOffset);
        }
        return !template.isDeactivated() && TemplateManagerImpl.isApplicable(template, myApplicableContextTypes);
    }

    public void startTemplate( Template template, Map<String, String> predefinedValues, TemplateEditingListener listener) {
        if (myInInjectedFragment) {
            template.setToReformat(false);
        }
        myTemplateManager.startTemplate(myEditor, template, false, predefinedValues, listener);
    }

    
    private static List<TemplateImpl> getMatchingTemplates( String templateKey) {
        TemplateSettings settings = TemplateSettings.getInstance();
        List<TemplateImpl> candidates = new ArrayList<TemplateImpl>();
        for (TemplateImpl template : settings.getTemplates(templateKey)) {
            if (!template.isDeactivated()) {
                candidates.add(template);
            }
        }
        return candidates;
    }

    
    public Editor getEditor() {
        return myEditor;
    }

    
    public FileType getFileType() {
        return myFile.getFileType();
    }

    
    public Project getProject() {
        return myProject;
    }

    public void deleteTemplateKey( String key) {
        int caretAt = myEditor.getCaretModel().getOffset();
        int templateStart = caretAt - key.length();
        myEditor.getDocument().deleteString(templateStart, caretAt);
        myEditor.getCaretModel().moveToOffset(templateStart);
        myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        myEditor.getSelectionModel().removeSelection();
    }

    
    public static PsiElement getContext( PsiFile file, int offset) {
        return getContext(file, offset, true);
    }

    
    public static PsiElement getContext( PsiFile file, int offset, boolean searchInInjectedFragment) {
        PsiElement element = null;
        if (searchInInjectedFragment && !InjectedLanguageManager.getInstance(file.getProject()).isInjectedFragment(file)) {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
            Document document = documentManager.getDocument(file);
            if (document != null && !documentManager.isCommitted(document)) {
                LOGGER.error("Trying to access to injected template context on uncommited document, offset = " + offset,
                        AttachmentFactory.createAttachment(file.getVirtualFile()));
            }
            else {
                element = InjectedLanguageUtil.findInjectedElementNoCommit(file, offset);
            }
        }
        if (element == null) {
            element = PsiUtilCore.getElementAtOffset(file, offset);
        }
        return element;
    }

    public boolean isInInjectedFragment() {
        return myInInjectedFragment;
    }
}
