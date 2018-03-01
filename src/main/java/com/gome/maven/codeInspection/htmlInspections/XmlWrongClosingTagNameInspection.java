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

package com.gome.maven.codeInspection.htmlInspections;

import com.gome.maven.codeInsight.daemon.XmlErrorMessages;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.LanguageParserDefinitions;
import com.gome.maven.lang.ParserDefinition;
import com.gome.maven.lang.annotation.Annotation;
import com.gome.maven.lang.annotation.AnnotationHolder;
import com.gome.maven.lang.annotation.Annotator;
import com.gome.maven.lang.html.HTMLLanguage;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiErrorElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.html.HtmlTag;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.psi.xml.XmlToken;
import com.gome.maven.psi.xml.XmlTokenType;
import com.gome.maven.xml.util.HtmlUtil;
import com.gome.maven.xml.util.XmlTagUtil;

/**
 * @author spleaner
 */
public class XmlWrongClosingTagNameInspection implements Annotator {

    @Override
    public void annotate( final PsiElement psiElement,  final AnnotationHolder holder) {
        if (psiElement instanceof XmlToken) {
            final PsiElement parent = psiElement.getParent();
            if (parent instanceof XmlTag) {
                final XmlTag tag = (XmlTag)parent;
                final XmlToken start = XmlTagUtil.getStartTagNameElement(tag);
                XmlToken endTagName = XmlTagUtil.getEndTagNameElement(tag);
                if (start == psiElement) {
                    if (endTagName != null && !(tag instanceof HtmlTag) && !tag.getName().equals(endTagName.getText())) {
                        registerProblemStart(holder, tag, start, endTagName);
                    }
                    else if (endTagName == null && !(tag instanceof HtmlTag && HtmlUtil.isSingleHtmlTag(tag.getName()))) {
                        final PsiErrorElement errorElement = PsiTreeUtil.getChildOfType(tag, PsiErrorElement.class);
                        endTagName = findEndTagName(errorElement);
                        if (endTagName != null) {
                            registerProblemStart(holder, tag, start, endTagName);
                        }
                    }
                }
                else if (endTagName == psiElement) {
                    if (!(tag instanceof HtmlTag) && !tag.getName().equals(endTagName.getText())) {
                        registerProblemEnd(holder, tag, endTagName);
                    }
                }
            }
            else if (parent instanceof PsiErrorElement) {
                if (XmlTokenType.XML_NAME == ((XmlToken)psiElement).getTokenType()) {
                    final PsiFile psiFile = psiElement.getContainingFile();

                    if (psiFile != null && (HTMLLanguage.INSTANCE == psiFile.getViewProvider().getBaseLanguage() || HTMLLanguage.INSTANCE == parent.getLanguage())) {
                        final String message = XmlErrorMessages.message("xml.parsing.closing.tag.matches.nothing");

                        if (message.equals(((PsiErrorElement)parent).getErrorDescription()) &&
                                psiFile.getContext() == null
                                ) {
                            final Annotation annotation = holder.createWarningAnnotation(parent, message);
                            annotation.registerFix(new RemoveExtraClosingTagIntentionAction());
                        }
                    }
                }
            }
        }
    }

    private static void registerProblemStart( final AnnotationHolder holder,
                                              final XmlTag tag,
                                              final XmlToken start,
                                              final XmlToken end) {
        PsiElement context = tag.getContainingFile().getContext();
        if (context != null) {
            ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(context.getLanguage());
            if (parserDefinition != null) {
                ASTNode contextNode = context.getNode();
                if (contextNode != null && contextNode.getChildren(parserDefinition.getStringLiteralElements()) != null) {
                    // TODO: we should check for concatenations here
                    return;
                }
            }
        }
        final String tagName = tag.getName();
        final String endTokenText = end.getText();

        final RenameTagBeginOrEndIntentionAction renameEndAction = new RenameTagBeginOrEndIntentionAction(tagName, endTokenText, false);
        final RenameTagBeginOrEndIntentionAction renameStartAction = new RenameTagBeginOrEndIntentionAction(endTokenText, tagName, true);

        final Annotation annotation = holder.createErrorAnnotation(start, XmlErrorMessages.message("tag.has.wrong.closing.tag.name"));
        annotation.registerFix(renameEndAction);
        annotation.registerFix(renameStartAction);
    }

    private static void registerProblemEnd( final AnnotationHolder holder,
                                            final XmlTag tag,
                                            final XmlToken end) {
        PsiElement context = tag.getContainingFile().getContext();
        if (context != null) {
            ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(context.getLanguage());
            if (parserDefinition != null) {
                ASTNode contextNode = context.getNode();
                if (contextNode != null && contextNode.getChildren(parserDefinition.getStringLiteralElements()) != null) {
                    // TODO: we should check for concatenations here
                    return;
                }
            }
        }
        final String tagName = tag.getName();
        final String endTokenText = end.getText();

        final RenameTagBeginOrEndIntentionAction renameEndAction = new RenameTagBeginOrEndIntentionAction(tagName, endTokenText, false);
        final RenameTagBeginOrEndIntentionAction renameStartAction = new RenameTagBeginOrEndIntentionAction(endTokenText, tagName, true);

        final Annotation annotation = holder.createErrorAnnotation(end, XmlErrorMessages.message("wrong.closing.tag.name"));
        annotation.registerFix(new RemoveExtraClosingTagIntentionAction());
        annotation.registerFix(renameEndAction);
        annotation.registerFix(renameStartAction);
    }

    
    static XmlToken findEndTagName( final PsiErrorElement element) {
        if (element == null) return null;

        final ASTNode astNode = element.getNode();
        if (astNode == null) return null;

        ASTNode current = astNode.getLastChildNode();
        ASTNode prev = current;

        while (current != null) {
            final IElementType elementType = prev.getElementType();

            if ((elementType == XmlTokenType.XML_NAME || elementType == XmlTokenType.XML_TAG_NAME) &&
                    current.getElementType() == XmlTokenType.XML_END_TAG_START) {
                return (XmlToken)prev.getPsi();
            }

            prev = current;
            current = current.getTreePrev();
        }

        return null;
    }
}
