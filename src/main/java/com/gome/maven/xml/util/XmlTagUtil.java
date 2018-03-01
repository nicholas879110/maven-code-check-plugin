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
package com.gome.maven.xml.util;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.psi.xml.XmlTagValue;
import com.gome.maven.psi.xml.XmlToken;
import com.gome.maven.psi.xml.XmlTokenType;
import com.gome.maven.util.ArrayUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author peter
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class XmlTagUtil extends XmlTagUtilBase {
    private static final Map<String, Character> ourCharacterEntities;

    static {
        ourCharacterEntities = new HashMap<String, Character>();
        ourCharacterEntities.put("lt", '<');
        ourCharacterEntities.put("gt", '>');
        ourCharacterEntities.put("apos", '\'');
        ourCharacterEntities.put("quot", '\"');
        ourCharacterEntities.put("nbsp", '\u00a0');
        ourCharacterEntities.put("amp", '&');
    }

    /**
     * if text contains XML-sensitive characters (<,>), quote text with ![CDATA[ ... ]]
     *
     * @param text
     * @return quoted text
     */
    public static String getCDATAQuote(String text) {
        if (text == null) return null;
        String offensiveChars = "<>&\n";
        final int textLength = text.length();
        if (textLength > 0 && (Character.isWhitespace(text.charAt(0)) || Character.isWhitespace(text.charAt(textLength - 1)))) {
            return "<![CDATA[" + text + "]]>";
        }
        for (int i = 0; i < offensiveChars.length(); i++) {
            char c = offensiveChars.charAt(i);
            if (text.indexOf(c) != -1) {
                return "<![CDATA[" + text + "]]>";
            }
        }
        return text;
    }

    public static String getInlineQuote(String text) {
        if (text == null) return null;
        String offensiveChars = "<>&";
        for (int i = 0; i < offensiveChars.length(); i++) {
            char c = offensiveChars.charAt(i);
            if (text.indexOf(c) != -1) {
                return "<![CDATA[" + text + "]]>";
            }
        }
        return text;
    }


    public static CharSequence composeTagText( String tagName,  String tagValue) {
        StringBuilder builder = new StringBuilder();
        builder.append('<').append(tagName);
        if (StringUtil.isEmpty(tagValue)) {
            builder.append("/>");
        }
        else {
            builder.append('>').append(getCDATAQuote(tagValue)).append("</").append(tagName).append('>');
        }
        return builder;
    }

    public static String[] getCharacterEntityNames() {
        Set<String> strings = ourCharacterEntities.keySet();
        return ArrayUtil.toStringArray(strings);
    }

    public static Character getCharacterByEntityName(String entityName) {
        return ourCharacterEntities.get(entityName);
    }

    
    public static XmlToken getStartTagNameElement( XmlTag tag) {
        final ASTNode node = tag.getNode();
        if (node == null) return null;

        ASTNode current = node.getFirstChildNode();
        IElementType elementType;
        while (current != null
                && (elementType = current.getElementType()) != XmlTokenType.XML_NAME
                && elementType != XmlTokenType.XML_TAG_NAME) {
            current = current.getTreeNext();
        }
        return current == null ? null : (XmlToken)current.getPsi();
    }

    
    public static XmlToken getEndTagNameElement( XmlTag tag) {
        final ASTNode node = tag.getNode();
        if (node == null) return null;

        ASTNode current = node.getLastChildNode();
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

    
    public static TextRange getTrimmedValueRange(final  XmlTag tag) {
        XmlTagValue tagValue = tag.getValue();
        final String text = tagValue.getText();
        final String trimmed = text.trim();
        final int index = text.indexOf(trimmed);
        final int startOffset = tagValue.getTextRange().getStartOffset() - tag.getTextRange().getStartOffset() + index;
        return new TextRange(startOffset, startOffset + trimmed.length());
    }

    
    public static TextRange getStartTagRange( XmlTag tag) {
        XmlToken tagName = getStartTagNameElement(tag);
        return getTag(tagName, XmlTokenType.XML_START_TAG_START);
    }


    
    public static TextRange getEndTagRange( XmlTag tag) {
        XmlToken tagName = getEndTagNameElement(tag);

        return getTag(tagName, XmlTokenType.XML_END_TAG_START);
    }

    private static TextRange getTag(XmlToken tagName, IElementType tagStart) {
        if (tagName != null) {
            PsiElement s = tagName.getPrevSibling();

            while (s != null && s.getNode().getElementType() != tagStart) {
                s = s.getPrevSibling();
            }

            PsiElement f = tagName.getNextSibling();

            while (f != null &&
                    !(f.getNode().getElementType() == XmlTokenType.XML_TAG_END ||
                            f.getNode().getElementType() == XmlTokenType.XML_EMPTY_ELEMENT_END)) {
                f = f.getNextSibling();
            }
            if (s != null && f != null) {
                return new TextRange(s.getTextRange().getStartOffset(), f.getTextRange().getEndOffset());
            }
        }
        return null;
    }
}
