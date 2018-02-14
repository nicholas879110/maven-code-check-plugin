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

import com.gome.maven.codeInspection.lang.InspectionExtensionsFactory;
import com.gome.maven.lang.Commenter;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageCommenters;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.util.NullableComputable;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiComment;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiParserFacade;
import com.gome.maven.psi.PsiWhiteSpace;
import com.gome.maven.psi.util.PsiTreeUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public class SuppressionUtil extends SuppressionUtilCore {
    /**
     * Common part of regexp for suppressing in line comments for different languages.
     * Comment start prefix isn't included, e.g. add '//' for Java/C/JS or '#' for Ruby
     */
    
    public static final String COMMON_SUPPRESS_REGEXP = "\\s*" + SUPPRESS_INSPECTIONS_TAG_NAME +
            "\\s+(" + LocalInspectionTool.VALID_ID_PATTERN +
            "(\\s*,\\s*" + LocalInspectionTool.VALID_ID_PATTERN + ")*)\\s*\\w*";

    
    public static final Pattern SUPPRESS_IN_LINE_COMMENT_PATTERN = Pattern.compile("//" + COMMON_SUPPRESS_REGEXP + ".*");  // for Java, C, JS line comments

    
    public static final String ALL = "ALL";

    private SuppressionUtil() {
    }

    public static boolean isInspectionToolIdMentioned( String inspectionsList,  String inspectionToolID) {
        Iterable<String> ids = StringUtil.tokenize(inspectionsList, "[, ]");

        for ( String id : ids) {
             String trim = id.trim();
            if (trim.equals(inspectionToolID) || trim.equalsIgnoreCase(ALL)) return true;
        }
        return false;
    }

    
    public static PsiElement getStatementToolSuppressedIn( PsiElement place,
                                                           String toolId,
                                                           Class<? extends PsiElement> statementClass) {
        return getStatementToolSuppressedIn(place, toolId, statementClass, SUPPRESS_IN_LINE_COMMENT_PATTERN);
    }

    
    public static PsiElement getStatementToolSuppressedIn( PsiElement place,
                                                           String toolId,
                                                           Class<? extends PsiElement> statementClass,
                                                           Pattern suppressInLineCommentPattern) {
        PsiElement statement = PsiTreeUtil.getNonStrictParentOfType(place, statementClass);
        if (statement != null) {
            PsiElement prev = PsiTreeUtil.skipSiblingsBackward(statement, PsiWhiteSpace.class);
            if (prev instanceof PsiComment) {
                String text = prev.getText();
                Matcher matcher = suppressInLineCommentPattern.matcher(text);
                if (matcher.matches() && isInspectionToolIdMentioned(matcher.group(1), toolId)) {
                    return prev;
                }
            }
        }
        return null;
    }

    public static boolean isSuppressedInStatement( final PsiElement place,
                                                   final String toolId,
                                                   final Class<? extends PsiElement> statementClass) {
        return ApplicationManager.getApplication().runReadAction(new NullableComputable<PsiElement>() {
            @Override
            public PsiElement compute() {
                return getStatementToolSuppressedIn(place, toolId, statementClass);
            }
        }) != null;
    }

    
    public static PsiComment createComment( Project project,
                                            String commentText,
                                            Language language) {
        final PsiParserFacade parserFacade = PsiParserFacade.SERVICE.getInstance(project);
        return parserFacade.createLineOrBlockCommentFromText(language, commentText);
    }

    
    public static Couple<String> getBlockPrefixSuffixPair( PsiElement comment) {
        final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(comment.getLanguage());
        if (commenter != null) {
            final String prefix = commenter.getBlockCommentPrefix();
            final String suffix = commenter.getBlockCommentSuffix();
            if (prefix != null || suffix != null) {
                return Couple.of(StringUtil.notNullize(prefix), StringUtil.notNullize(suffix));
            }
        }
        return null;
    }

    
    public static String getLineCommentPrefix( final PsiElement comment) {
        final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(comment.getLanguage());
        return commenter == null ? null : commenter.getLineCommentPrefix();
    }

    public static boolean isSuppressionComment( PsiElement comment) {
        final String prefix = getLineCommentPrefix(comment);
        final String commentText = comment.getText();
        if (prefix != null) {
            return commentText.startsWith(prefix + SUPPRESS_INSPECTIONS_TAG_NAME);
        }
        final Couple<String> prefixSuffixPair = getBlockPrefixSuffixPair(comment);
        return prefixSuffixPair != null
                && commentText.startsWith(prefixSuffixPair.first + SUPPRESS_INSPECTIONS_TAG_NAME)
                && commentText.endsWith(prefixSuffixPair.second);
    }

    public static void replaceSuppressionComment( PsiElement comment,  String id,
                                                 boolean replaceOtherSuppressionIds,  Language commentLanguage) {
        final String oldSuppressionCommentText = comment.getText();
        final String lineCommentPrefix = getLineCommentPrefix(comment);
        Couple<String> blockPrefixSuffix = null;
        if (lineCommentPrefix == null) {
            blockPrefixSuffix = getBlockPrefixSuffixPair(comment);
        }
        assert blockPrefixSuffix != null
                && oldSuppressionCommentText.startsWith(blockPrefixSuffix.first)
                && oldSuppressionCommentText.endsWith(blockPrefixSuffix.second)
                || lineCommentPrefix != null && oldSuppressionCommentText.startsWith(lineCommentPrefix)
                : "Unexpected suppression comment " + oldSuppressionCommentText;

        // append new suppression tool id or replace
        final String newText;
        if(replaceOtherSuppressionIds) {
            newText = SUPPRESS_INSPECTIONS_TAG_NAME + " " + id;
        }
        else if (lineCommentPrefix == null) {
            newText = oldSuppressionCommentText.substring(blockPrefixSuffix.first.length(),
                    oldSuppressionCommentText.length() - blockPrefixSuffix.second.length()) + "," + id;
        }
        else {
            newText = oldSuppressionCommentText.substring(lineCommentPrefix.length()) + "," + id;
        }
        comment.replace(createComment(comment.getProject(), newText, commentLanguage));
    }

    public static void createSuppression( Project project,
                                          PsiElement container,
                                          String id,
                                          Language commentLanguage) {
        final String text = SUPPRESS_INSPECTIONS_TAG_NAME + " " + id;
        PsiComment comment = createComment(project, text, commentLanguage);
        container.getParent().addBefore(comment, container);
    }

    public static boolean isSuppressed( PsiElement psiElement,  String id) {
        for (InspectionExtensionsFactory factory : Extensions.getExtensions(InspectionExtensionsFactory.EP_NAME)) {
            if (!factory.isToCheckMember(psiElement, id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean inspectionResultSuppressed( PsiElement place,  LocalInspectionTool tool) {
        return tool.isSuppressedFor(place);
    }
}
