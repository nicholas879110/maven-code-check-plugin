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

package com.gome.maven.codeInspection;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.openapi.command.undo.UndoUtil;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiComment;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.codeStyle.CodeStyleManager;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.xml.*;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.HashSet;

/**
 * @author Dmitry Avdeev
 */
public class DefaultXmlSuppressionProvider extends XmlSuppressionProvider implements InspectionSuppressor {

    public static final String SUPPRESS_MARK = "suppress";

    @Override
    public boolean isProviderAvailable( PsiFile file) {
        return true;
    }

    @Override
    public boolean isSuppressedFor( PsiElement element,  String inspectionId) {
        final XmlTag tag = element instanceof XmlFile ? ((XmlFile)element).getRootTag() : PsiTreeUtil.getContextOfType(element, XmlTag.class, false);
        return tag != null && findSuppression(tag, inspectionId, element) != null;
    }

    @Override
    public void suppressForFile( PsiElement element,  String inspectionId) {
        final PsiFile file = element.getContainingFile();
        final XmlDocument document = ((XmlFile)file).getDocument();
        final PsiElement anchor = document != null ? document.getRootTag() : file.findElementAt(0);
        assert anchor != null;
        suppress(file, findFileSuppression(anchor, null, element), inspectionId, anchor.getTextRange().getStartOffset());
    }

    @Override
    public void suppressForTag( PsiElement element,  String inspectionId) {
        final XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        assert tag != null;
        suppress(element.getContainingFile(), findSuppressionLeaf(tag, null, 0), inspectionId, tag.getTextRange().getStartOffset());
    }

    
    protected PsiElement findSuppression(final PsiElement anchor, final String id, PsiElement originalElement) {
        final PsiElement element = findSuppressionLeaf(anchor, id, 0);
        if (element != null) return element;

        return findFileSuppression(anchor, id, originalElement);
    }

    
    protected PsiElement findFileSuppression(PsiElement anchor, String id, PsiElement originalElement) {
        final PsiFile file = anchor.getContainingFile();
        if (file instanceof XmlFile) {
            final XmlDocument document = ((XmlFile)file).getDocument();
            final XmlTag rootTag = document != null ? document.getRootTag() : null;
            PsiElement leaf = rootTag != null ? rootTag.getPrevSibling() : file.findElementAt(0);
            return findSuppressionLeaf(leaf, id, 0);
        }
        return null;
    }

    
    protected PsiElement findSuppressionLeaf(PsiElement leaf,  final String id, int offset) {
        while (leaf != null && leaf.getTextOffset() >= offset) {
            if (leaf instanceof PsiComment || leaf instanceof XmlProlog || leaf instanceof XmlText) {
                 String text = leaf.getText();
                if (isSuppressedFor(text, id)) return leaf;
            }
            leaf = leaf.getPrevSibling();
            if (leaf instanceof XmlTag) {
                return null;
            }
        }
        return null;
    }

    private boolean isSuppressedFor( final String text,  final String id) {
        if (!text.contains(getPrefix())) {
            return false;
        }
        if (id == null) {
            return true;
        }
         final HashSet<String> parts = ContainerUtil.newHashSet(StringUtil.getWordsIn(text));
        return parts.contains(id) || parts.contains(XmlSuppressableInspectionTool.ALL);
    }

    protected void suppress(PsiFile file, final PsiElement suppressionElement, String inspectionId, final int offset) {
        final Project project = file.getProject();
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        final Document doc = PsiDocumentManager.getInstance(project).getDocument(file);
        assert doc != null;

        if (suppressionElement != null) {
            final TextRange textRange = suppressionElement.getTextRange();
            String text = suppressionElement.getText();
            final String suppressionText = getSuppressionText(inspectionId, text);
            doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), suppressionText);
        } else {
            final String suppressionText = getSuppressionText(inspectionId, null);
            doc.insertString(offset, suppressionText);
            CodeStyleManager.getInstance(project).adjustLineIndent(doc, offset + suppressionText.length());
            UndoUtil.markPsiFileForUndo(file);
        }
    }

    protected String getSuppressionText(String inspectionId,  String originalText) {
        if (originalText == null) {
            return getPrefix() + inspectionId + getSuffix() + "\n";
        } else if (inspectionId.equals(XmlSuppressableInspectionTool.ALL)) {
            final int pos = originalText.indexOf(getPrefix());
            return originalText.substring(0, pos) + getPrefix() + inspectionId + getSuffix() + "\n";
        }
        return StringUtil.replace(originalText, getSuffix(), ", " + inspectionId + getSuffix());
    }

    
    protected String getPrefix() {
        return "<!--" +
                SUPPRESS_MARK +
                " ";
    }

    
    protected String getSuffix() {
        return " -->";
    }
}
