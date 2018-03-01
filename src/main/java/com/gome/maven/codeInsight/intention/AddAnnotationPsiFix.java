/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.intention;

import com.gome.maven.codeInsight.AnnotationUtil;
import com.gome.maven.codeInsight.CodeInsightBundle;
import com.gome.maven.codeInsight.ExternalAnnotationsManager;
import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.codeInspection.LocalQuickFixOnPsiElement;
import com.gome.maven.lang.findUsages.FindUsagesProvider;
import com.gome.maven.lang.findUsages.LanguageFindUsages;
import com.gome.maven.openapi.command.undo.UndoUtil;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;

public class AddAnnotationPsiFix extends LocalQuickFixOnPsiElement {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.intention.AddAnnotationPsiFix");
    protected final String myAnnotation;
    private final String[] myAnnotationsToRemove;
    private final PsiNameValuePair[] myPairs; // not used when registering local quick fix
    protected final String myText;

    public AddAnnotationPsiFix( String fqn,
                                PsiModifierListOwner modifierListOwner,
                                PsiNameValuePair[] values,
                                String... annotationsToRemove) {
        super(modifierListOwner);
        myAnnotation = fqn;
        myPairs = values;
        myAnnotationsToRemove = annotationsToRemove;
        myText = calcText(modifierListOwner, myAnnotation);
    }

    public static String calcText(PsiModifierListOwner modifierListOwner,  String annotation) {
        final String shortName = annotation.substring(annotation.lastIndexOf('.') + 1);
        if (modifierListOwner instanceof PsiNamedElement) {
            final String name = ((PsiNamedElement)modifierListOwner).getName();
            if (name != null) {
                FindUsagesProvider provider = LanguageFindUsages.INSTANCE.forLanguage(modifierListOwner.getLanguage());
                return CodeInsightBundle
                        .message("inspection.i18n.quickfix.annotate.element.as", provider.getType(modifierListOwner), name, shortName);
            }
        }
        return CodeInsightBundle.message("inspection.i18n.quickfix.annotate.as", shortName);
    }

    
    public static PsiModifierListOwner getContainer(final PsiFile file, int offset) {
        PsiReference reference = file.findReferenceAt(offset);
        if (reference != null) {
            PsiElement target = reference.resolve();
            if (target instanceof PsiMember) {
                return (PsiMember)target;
            }
        }

        PsiElement element = file.findElementAt(offset);

        PsiModifierListOwner listOwner = PsiTreeUtil.getParentOfType(element, PsiModifierListOwner.class, false);
        if (listOwner instanceof PsiParameter) return listOwner;

        if (listOwner instanceof PsiNameIdentifierOwner) {
            PsiElement id = ((PsiNameIdentifierOwner)listOwner).getNameIdentifier();
            if (id != null && id.getTextRange().containsOffset(offset)) { // Groovy methods will pass this check as well
                return listOwner;
            }
        }

        return null;
    }

    @Override
    
    public String getText() {
        return myText;
    }

    @Override
    
    public String getFamilyName() {
        return CodeInsightBundle.message("intention.add.annotation.family");
    }

    @Override
    public boolean isAvailable( Project project,
                                PsiFile file,
                                PsiElement startElement,
                                PsiElement endElement) {
        if (!startElement.isValid()) return false;
        if (!PsiUtil.isLanguageLevel5OrHigher(startElement)) return false;
        final PsiModifierListOwner myModifierListOwner = (PsiModifierListOwner)startElement;

        return !AnnotationUtil.isAnnotated(myModifierListOwner, myAnnotation, false, false);
    }

    @Override
    public void invoke( Project project,
                        PsiFile file,
                        PsiElement startElement,
                        PsiElement endElement) {
        final PsiModifierListOwner myModifierListOwner = (PsiModifierListOwner)startElement;

        final ExternalAnnotationsManager annotationsManager = ExternalAnnotationsManager.getInstance(project);
        final PsiModifierList modifierList = myModifierListOwner.getModifierList();
        LOG.assertTrue(modifierList != null, myModifierListOwner + " ("+myModifierListOwner.getClass()+")");
        if (modifierList.findAnnotation(myAnnotation) != null) return;
        final ExternalAnnotationsManager.AnnotationPlace annotationAnnotationPlace = annotationsManager.chooseAnnotationsPlace(myModifierListOwner);
        if (annotationAnnotationPlace == ExternalAnnotationsManager.AnnotationPlace.NOWHERE) return;
        if (annotationAnnotationPlace == ExternalAnnotationsManager.AnnotationPlace.EXTERNAL) {
            for (String fqn : myAnnotationsToRemove) {
                annotationsManager.deannotate(myModifierListOwner, fqn);
            }
            annotationsManager.annotateExternally(myModifierListOwner, myAnnotation, file, myPairs);
        }
        else {
            final PsiFile containingFile = myModifierListOwner.getContainingFile();
            if (!FileModificationService.getInstance().preparePsiElementForWrite(containingFile)) return;
            removePhysicalAnnotations(myModifierListOwner, myAnnotationsToRemove);

            PsiAnnotation inserted = addPhysicalAnnotation(myAnnotation, myPairs, modifierList);
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(inserted);
            if (containingFile != file) {
                UndoUtil.markPsiFileForUndo(file);
            }
        }
    }

    public static PsiAnnotation addPhysicalAnnotation(String fqn, PsiNameValuePair[] pairs, PsiModifierList modifierList) {
        PsiAnnotation inserted = modifierList.addAnnotation(fqn);
        for (PsiNameValuePair pair : pairs) {
            inserted.setDeclaredAttributeValue(pair.getName(), pair.getValue());
        }
        return inserted;
    }

    public static void removePhysicalAnnotations(PsiModifierListOwner owner, String... fqns) {
        for (String fqn : fqns) {
            PsiAnnotation annotation = AnnotationUtil.findAnnotation(owner, fqn);
            if (annotation != null) {
                annotation.delete();
            }
        }
    }

    
    public String[] getAnnotationsToRemove() {
        return myAnnotationsToRemove;
    }
}
