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
package com.gome.maven.spellchecker.tokenizer;

import com.gome.maven.codeInspection.SuppressionUtil;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.psi.xml.XmlAttributeValue;
import com.gome.maven.psi.xml.XmlText;
import com.gome.maven.spellchecker.inspections.PlainTextSplitter;
import com.gome.maven.spellchecker.inspections.TextSplitter;
import com.gome.maven.spellchecker.quickfixes.AcceptWordAsCorrect;
import com.gome.maven.spellchecker.quickfixes.ChangeTo;
import com.gome.maven.spellchecker.quickfixes.RenameTo;
import com.gome.maven.spellchecker.quickfixes.SpellCheckerQuickFix;

public class SpellcheckingStrategy {
    protected final Tokenizer<PsiComment> myCommentTokenizer = new CommentTokenizer();
    protected final Tokenizer<XmlAttributeValue> myXmlAttributeTokenizer = new XmlAttributeValueTokenizer();
    protected final Tokenizer<XmlText> myXmlTextTokenizer = new XmlTextTokenizer();

    public static final ExtensionPointName<SpellcheckingStrategy> EP_NAME = ExtensionPointName.create("com.gome.maven.spellchecker.support");
    public static final Tokenizer EMPTY_TOKENIZER = new Tokenizer() {
        @Override
        public void tokenize( PsiElement element, TokenConsumer consumer) {
        }
    };

    public static final Tokenizer<PsiElement> TEXT_TOKENIZER = new TokenizerBase<PsiElement>(PlainTextSplitter.getInstance());

    private static final SpellCheckerQuickFix[] BATCH_FIXES = new SpellCheckerQuickFix[]{new AcceptWordAsCorrect()};

    
    public Tokenizer getTokenizer(PsiElement element) {
        if (element instanceof PsiLanguageInjectionHost && InjectedLanguageUtil.hasInjections((PsiLanguageInjectionHost)element)) {
            return EMPTY_TOKENIZER;
        }
        if (element instanceof PsiNameIdentifierOwner) return new PsiIdentifierOwnerTokenizer();
        if (element instanceof PsiComment) {
            if (SuppressionUtil.isSuppressionComment(element)) {
                return EMPTY_TOKENIZER;
            }
            return myCommentTokenizer;
        }
        if (element instanceof XmlAttributeValue) return myXmlAttributeTokenizer;
        if (element instanceof XmlText) return myXmlTextTokenizer;
        if (element instanceof PsiPlainText) {
            PsiFile file = element.getContainingFile();
            FileType fileType = file == null ? null : file.getFileType();
            if (fileType instanceof CustomSyntaxTableFileType) {
                return new CustomFileTypeTokenizer(((CustomSyntaxTableFileType)fileType).getSyntaxTable());
            }
            return TEXT_TOKENIZER;
        }
        return EMPTY_TOKENIZER;
    }

    public SpellCheckerQuickFix[] getRegularFixes(PsiElement element,
                                                  int offset,
                                                   TextRange textRange,
                                                  boolean useRename,
                                                  String wordWithTypo) {
        return getDefaultRegularFixes(useRename, wordWithTypo);
    }

    public static SpellCheckerQuickFix[] getDefaultRegularFixes(boolean useRename, String wordWithTypo) {
        return new SpellCheckerQuickFix[]{
                useRename ? new RenameTo(wordWithTypo) : new ChangeTo(wordWithTypo),
                new AcceptWordAsCorrect(wordWithTypo)
        };
    }

    public static SpellCheckerQuickFix[] getDefaultBatchFixes() {
        return BATCH_FIXES;
    }

    protected static class XmlAttributeValueTokenizer extends Tokenizer<XmlAttributeValue> {
        public void tokenize( final XmlAttributeValue element, final TokenConsumer consumer) {
            if (element instanceof PsiLanguageInjectionHost && InjectedLanguageUtil.hasInjections((PsiLanguageInjectionHost)element)) return;

            final String valueTextTrimmed = element.getValue().trim();
            // do not inspect colors like #00aaFF
            if (valueTextTrimmed.startsWith("#") && valueTextTrimmed.length() <= 7 && isHexString(valueTextTrimmed.substring(1))) {
                return;
            }

            consumer.consumeToken(element, TextSplitter.getInstance());
        }

        private static boolean isHexString(final String s) {
            for (int i = 0; i < s.length(); i++) {
                if (!StringUtil.isHexDigit(s.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean isMyContext( PsiElement element) {
        return true;
    }
}
