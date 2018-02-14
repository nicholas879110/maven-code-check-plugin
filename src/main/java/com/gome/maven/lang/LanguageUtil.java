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

package com.gome.maven.lang;

import com.gome.maven.lexer.Lexer;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.templateLanguages.TemplateLanguage;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class LanguageUtil {
    private LanguageUtil() {
    }

    public static final Comparator<Language> LANGUAGE_COMPARATOR = new Comparator<Language>() {
        @Override
        public int compare(Language o1, Language o2) {
            return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
        }
    };

    public static ParserDefinition.SpaceRequirements canStickTokensTogetherByLexer(ASTNode left, ASTNode right, Lexer lexer) {
        String textStr = left.getText() + right.getText();

        lexer.start(textStr, 0, textStr.length());
        if(lexer.getTokenType() != left.getElementType()) return ParserDefinition.SpaceRequirements.MUST;
        if(lexer.getTokenEnd() != left.getTextLength()) return ParserDefinition.SpaceRequirements.MUST;
        lexer.advance();
        if(lexer.getTokenEnd() != textStr.length()) return ParserDefinition.SpaceRequirements.MUST;
        if(lexer.getTokenType() != right.getElementType()) return ParserDefinition.SpaceRequirements.MUST;
        return ParserDefinition.SpaceRequirements.MAY;
    }

    
    public static Language[] getLanguageDialects( final Language base) {
        final List<Language> list = ContainerUtil.findAll(Language.getRegisteredLanguages(), new Condition<Language>() {
            @Override
            public boolean value(final Language language) {
                return language.getBaseLanguage() == base;
            }
        });
        return list.toArray(new Language[list.size()]);
    }

    public static boolean isInTemplateLanguageFile( final PsiElement element) {
        if (element == null) return false;

        final PsiFile psiFile = element.getContainingFile();
        if(psiFile == null) return false;

        final Language language = psiFile.getViewProvider().getBaseLanguage();
        return language instanceof TemplateLanguage;
    }

    public static boolean isInjectableLanguage( Language language) {
        if (language == Language.ANY) {
            return false;
        }
        if (language.getID().startsWith("$")) {
            return false;
        }
        if (language instanceof InjectableLanguage) {
            return true;
        }
        if (language instanceof TemplateLanguage || language instanceof DependentLanguage) {
            return false;
        }
        if (LanguageParserDefinitions.INSTANCE.forLanguage(language) == null) {
            return false;
        }
        return true;
    }

    public static boolean isFileLanguage( Language language) {
        if (language instanceof DependentLanguage || language instanceof InjectableLanguage) return false;
        if (LanguageParserDefinitions.INSTANCE.forLanguage(language) == null) return false;
        LanguageFileType type = language.getAssociatedFileType();
        if (type == null || StringUtil.isEmpty(type.getDefaultExtension())) return false;
        String name = language.getDisplayName();
        if (StringUtil.isEmpty(name) || name.startsWith("<") || name.startsWith("[")) return false;
        return StringUtil.isNotEmpty(type.getDefaultExtension());
    }

    
    public static List<Language> getFileLanguages() {
        List<Language> result = ContainerUtil.newArrayList();
        for (Language language : Language.getRegisteredLanguages()) {
            if (!isFileLanguage(language)) continue;
            result.add(language);
        }
        Collections.sort(result, LANGUAGE_COMPARATOR);
        return result;
    }

    
    public static Language getRootLanguage( PsiElement element) {
        final FileViewProvider provider = element.getContainingFile().getViewProvider();
        final Set<Language> languages = provider.getLanguages();
        if (languages.size() > 1) {
            PsiElement current = element;
            while (current != null) {
                final Language language = current.getLanguage();
                if (languages.contains(language)) return language;
                current = current.getParent();
            }
        }
        return provider.getBaseLanguage();
    }
}
