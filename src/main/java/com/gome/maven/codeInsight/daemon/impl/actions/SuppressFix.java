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
package com.gome.maven.codeInsight.daemon.impl.actions;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.JavaSuppressionUtil;
import com.gome.maven.codeInspection.SuppressionUtil;
import com.gome.maven.codeInspection.SuppressionUtilCore;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.openapi.command.undo.UndoUtil;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.impl.storage.ClassPathStorageUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.javadoc.PsiDocTag;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author ven
 */
public class SuppressFix extends AbstractBatchSuppressByNoInspectionCommentFix {
    private String myAlternativeID;

    public SuppressFix( HighlightDisplayKey key) {
        this(key.getID());
        myAlternativeID = HighlightDisplayKey.getAlternativeID(key);
    }

    public SuppressFix( String ID) {
        super(ID, false);
    }

    @Override
    
    public String getText() {
        String myText = super.getText();
        return StringUtil.isEmpty(myText) ? "Suppress for member" : myText;
    }

    @Override
    
    public PsiDocCommentOwner getContainer(final PsiElement context) {
        if (context == null || !context.getManager().isInProject(context)) {
            return null;
        }
        final PsiFile containingFile = context.getContainingFile();
        if (containingFile == null) {
            // for PsiDirectory
            return null;
        }
        if (!containingFile.getLanguage().isKindOf(JavaLanguage.INSTANCE) || context instanceof PsiFile) {
            return null;
        }
        PsiElement container = context;
        while (container instanceof PsiAnonymousClass || !(container instanceof PsiDocCommentOwner) || container instanceof PsiTypeParameter) {
            container = PsiTreeUtil.getParentOfType(container, PsiDocCommentOwner.class);
            if (container == null) return null;
        }
        return (PsiDocCommentOwner)container;
    }

    @Override
    public boolean isAvailable( final Project project,  final PsiElement context) {
        PsiDocCommentOwner container = getContainer(context);
        boolean isValid = container != null && !(container instanceof PsiMethod && container instanceof SyntheticElement);
        if (!isValid) {
            return false;
        }
        setText(container instanceof PsiClass
                ? InspectionsBundle.message("suppress.inspection.class")
                : container instanceof PsiMethod ? InspectionsBundle.message("suppress.inspection.method") : InspectionsBundle.message("suppress.inspection.field"));
        return true;
    }

    @Override
    public void invoke( final Project project,  final PsiElement element) throws IncorrectOperationException {
        if (doSuppress(project, getContainer(element))) return;
        // todo suppress
        //DaemonCodeAnalyzer.getInstance(project).restart();
        UndoUtil.markPsiFileForUndo(element.getContainingFile());
    }

    private boolean doSuppress( Project project, PsiDocCommentOwner container) {
        assert container != null;
        if (!FileModificationService.getInstance().preparePsiElementForWrite(container)) return true;
        if (use15Suppressions(container)) {
            final PsiModifierList modifierList = container.getModifierList();
            if (modifierList != null) {
                JavaSuppressionUtil.addSuppressAnnotation(project, container, container, getID(container));
            }
        }
        else {
            PsiDocComment docComment = container.getDocComment();
            PsiManager manager = PsiManager.getInstance(project);
            if (docComment == null) {
                String commentText = "/** @" + SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME + " " + getID(container) + "*/";
                docComment = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory().createDocCommentFromText(commentText);
                PsiElement firstChild = container.getFirstChild();
                container.addBefore(docComment, firstChild);
            }
            else {
                PsiDocTag noInspectionTag = docComment.findTagByName(SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME);
                if (noInspectionTag != null) {
                    String tagText = noInspectionTag.getText() + ", " + getID(container);
                    noInspectionTag.replace(JavaPsiFacade.getInstance(manager.getProject()).getElementFactory().createDocTagFromText(tagText));
                }
                else {
                    String tagText = "@" + SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME + " " + getID(container);
                    docComment.add(JavaPsiFacade.getInstance(manager.getProject()).getElementFactory().createDocTagFromText(tagText));
                }
            }
        }
        return false;
    }

    protected boolean use15Suppressions( PsiDocCommentOwner container) {
        return JavaSuppressionUtil.canHave15Suppressions(container) &&
                !JavaSuppressionUtil.alreadyHas14Suppressions(container);
    }

    private String getID( PsiElement place) {
        String id = getID(place, myAlternativeID);
        return id != null ? id : myID;
    }

    
    static String getID( PsiElement place, String alternativeID) {
        if (alternativeID != null) {
            final Module module = ModuleUtilCore.findModuleForPsiElement(place);
            if (module != null) {
                if (!ClassPathStorageUtil.isDefaultStorage(module)) {
                    return alternativeID;
                }
            }
        }

        return null;
    }
}
