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
package com.gome.maven.openapi.fileTypes.impl;

import com.gome.maven.ide.highlighter.FileTypeRegistrator;
import com.gome.maven.ide.highlighter.custom.SyntaxTable;
import com.gome.maven.ide.highlighter.custom.impl.CustomFileTypeEditor;
import com.gome.maven.lang.Commenter;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileTypes.*;
import com.gome.maven.openapi.fileTypes.ex.ExternalizableFileType;
import com.gome.maven.openapi.options.ExternalInfo;
import com.gome.maven.openapi.options.ExternalizableScheme;
import com.gome.maven.openapi.options.SettingsEditor;
import com.gome.maven.openapi.util.*;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.text.StringTokenizer;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AbstractFileType extends UserFileType<AbstractFileType> implements ExternalizableFileType, ExternalizableScheme,
        CustomSyntaxTableFileType {
    private static final String SEMICOLON = ";";
    protected SyntaxTable mySyntaxTable;
    private SyntaxTable myDefaultSyntaxTable;
    protected Commenter myCommenter = null;
     public static final String ELEMENT_HIGHLIGHTING = "highlighting";
     private static final String ELEMENT_OPTIONS = "options";
     private static final String ELEMENT_OPTION = "option";
     private static final String ATTRIBUTE_VALUE = "value";
     private static final String VALUE_LINE_COMMENT = "LINE_COMMENT";
     private static final String VALUE_COMMENT_START = "COMMENT_START";
     private static final String VALUE_COMMENT_END = "COMMENT_END";
     private static final String VALUE_HEX_PREFIX = "HEX_PREFIX";
     private static final String VALUE_NUM_POSTFIXES = "NUM_POSTFIXES";
     private static final String VALUE_HAS_BRACES = "HAS_BRACES";
     private static final String VALUE_HAS_BRACKETS = "HAS_BRACKETS";
     private static final String VALUE_HAS_PARENS = "HAS_PARENS";
     private static final String VALUE_HAS_STRING_ESCAPES = "HAS_STRING_ESCAPES";
     private static final String VALUE_LINE_COMMENT_AT_START = "LINE_COMMENT_AT_START";
     private static final String ELEMENT_KEYWORDS = "keywords";
     private static final String ATTRIBUTE_IGNORE_CASE = "ignore_case";
     private static final String ELEMENT_KEYWORD = "keyword";
     private static final String ELEMENT_KEYWORDS2 = "keywords2";
     private static final String ELEMENT_KEYWORDS3 = "keywords3";
     private static final String ELEMENT_KEYWORDS4 = "keywords4";
     private static final String ATTRIBUTE_NAME = "name";
     public static final String ELEMENT_EXTENSION_MAP = "extensionMap";
    private final ExternalInfo myExternalInfo = new ExternalInfo();

    public AbstractFileType(SyntaxTable syntaxTable) {
        mySyntaxTable = syntaxTable;
    }

    public void initSupport() {
        for (FileTypeRegistrator registrator : Extensions.getRootArea().getExtensionPoint(FileTypeRegistrator.EP_NAME).getExtensions()) {
            registrator.initFileType(this);
        }
    }

    @Override
    public SyntaxTable getSyntaxTable() {
        return mySyntaxTable;
    }

    public Commenter getCommenter() {
        return myCommenter;
    }

    public void setSyntaxTable(SyntaxTable syntaxTable) {
        mySyntaxTable = syntaxTable;
    }

    @Override
    public AbstractFileType clone() {
        return (AbstractFileType)super.clone();
    }

    @Override
    public void copyFrom(UserFileType newType) {
        super.copyFrom(newType);
        if (newType instanceof AbstractFileType) {
            mySyntaxTable = ((CustomSyntaxTableFileType)newType).getSyntaxTable();
            myExternalInfo.copy(((AbstractFileType)newType).myExternalInfo);
        }
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public void readExternal( Element typeElement) throws InvalidDataException {
        Element element = typeElement.getChild(ELEMENT_HIGHLIGHTING);
        if (element != null) {
            setSyntaxTable(readSyntaxTable(element));
        }
    }

    
    public static SyntaxTable readSyntaxTable( Element root) {
        SyntaxTable table = new SyntaxTable();

        for (Element element : (List<Element>)root.getChildren()) {
            if (ELEMENT_OPTIONS.equals(element.getName())) {
                for (final Object o1 : element.getChildren(ELEMENT_OPTION)) {
                    Element e = (Element)o1;
                    String name = e.getAttributeValue(ATTRIBUTE_NAME);
                    String value = e.getAttributeValue(ATTRIBUTE_VALUE);
                    if (VALUE_LINE_COMMENT.equals(name)) {
                        table.setLineComment(value);
                    }
                    else if (VALUE_COMMENT_START.equals(name)) {
                        table.setStartComment(value);
                    }
                    else if (VALUE_COMMENT_END.equals(name)) {
                        table.setEndComment(value);
                    }
                    else if (VALUE_HEX_PREFIX.equals(name)) {
                        table.setHexPrefix(value);
                    }
                    else if (VALUE_NUM_POSTFIXES.equals(name)) {
                        table.setNumPostfixChars(value);
                    }
                    else if (VALUE_LINE_COMMENT_AT_START.equals(name)) {
                        table.lineCommentOnlyAtStart = Boolean.valueOf(value).booleanValue();
                    }
                    else if (VALUE_HAS_BRACES.equals(name)) {
                        table.setHasBraces(Boolean.valueOf(value).booleanValue());
                    }
                    else if (VALUE_HAS_BRACKETS.equals(name)) {
                        table.setHasBrackets(Boolean.valueOf(value).booleanValue());
                    }
                    else if (VALUE_HAS_PARENS.equals(name)) {
                        table.setHasParens(Boolean.valueOf(value).booleanValue());
                    }
                    else if (VALUE_HAS_STRING_ESCAPES.equals(name)) {
                        table.setHasStringEscapes(Boolean.valueOf(value).booleanValue());
                    }
                }
            }
            else if (ELEMENT_KEYWORDS.equals(element.getName())) {
                boolean ignoreCase = Boolean.valueOf(element.getAttributeValue(ATTRIBUTE_IGNORE_CASE)).booleanValue();
                table.setIgnoreCase(ignoreCase);
                loadKeywords(element, table.getKeywords1());
            }
            else if (ELEMENT_KEYWORDS2.equals(element.getName())) {
                loadKeywords(element, table.getKeywords2());
            }
            else if (ELEMENT_KEYWORDS3.equals(element.getName())) {
                loadKeywords(element, table.getKeywords3());
            }
            else if (ELEMENT_KEYWORDS4.equals(element.getName())) {
                loadKeywords(element, table.getKeywords4());
            }
        }

        boolean DUMP_TABLE = false;
        if (DUMP_TABLE) {
            Element element = new Element("temp");
            writeTable(element, table);
            XMLOutputter outputter = JDOMUtil.createOutputter("\n");
            try {
                outputter.output((Element)element.getContent().get(0), System.out);
            }
            catch (IOException ignored) {
            }
        }
        return table;
    }

    private static void loadKeywords(Element element, Set<String> keywords) {
        String value = element.getAttributeValue(ELEMENT_KEYWORDS);
        if (value != null) {
            StringTokenizer tokenizer = new StringTokenizer(value, SEMICOLON);
            while(tokenizer.hasMoreElements()) {
                String keyword = tokenizer.nextToken().trim();
                if (keyword.length() != 0) keywords.add(keyword);
            }
        }
        for (final Object o1 : element.getChildren(ELEMENT_KEYWORD)) {
            keywords.add(((Element)o1).getAttributeValue(ATTRIBUTE_NAME));
        }
    }

    @Override
    public void writeExternal( Element element) {
        writeTable(element, getSyntaxTable());
    }

    private static void writeTable( Element element,  SyntaxTable table) {
        Element highlightingElement = new Element(ELEMENT_HIGHLIGHTING);

        Element optionsElement = new Element(ELEMENT_OPTIONS);

        Element lineComment = new Element(ELEMENT_OPTION);
        lineComment.setAttribute(ATTRIBUTE_NAME, VALUE_LINE_COMMENT);
        lineComment.setAttribute(ATTRIBUTE_VALUE, table.getLineComment());
        optionsElement.addContent(lineComment);

        String commentStart = table.getStartComment();
        if (commentStart != null) {
            Element commentStartElement = new Element(ELEMENT_OPTION);
            commentStartElement.setAttribute(ATTRIBUTE_NAME, VALUE_COMMENT_START);
            commentStartElement.setAttribute(ATTRIBUTE_VALUE, commentStart);
            optionsElement.addContent(commentStartElement);
        }

        String endComment = table.getEndComment();

        if (endComment != null) {
            Element commentEndElement = new Element(ELEMENT_OPTION);
            commentEndElement.setAttribute(ATTRIBUTE_NAME, VALUE_COMMENT_END);
            commentEndElement.setAttribute(ATTRIBUTE_VALUE, endComment);
            optionsElement.addContent(commentEndElement);
        }

        String prefix = table.getHexPrefix();

        if (prefix != null) {
            Element hexPrefix = new Element(ELEMENT_OPTION);
            hexPrefix.setAttribute(ATTRIBUTE_NAME, VALUE_HEX_PREFIX);
            hexPrefix.setAttribute(ATTRIBUTE_VALUE, prefix);
            optionsElement.addContent(hexPrefix);
        }

        String chars = table.getNumPostfixChars();

        if (chars != null) {
            Element numPostfixes = new Element(ELEMENT_OPTION);
            numPostfixes.setAttribute(ATTRIBUTE_NAME, VALUE_NUM_POSTFIXES);
            numPostfixes.setAttribute(ATTRIBUTE_VALUE, chars);
            optionsElement.addContent(numPostfixes);
        }

        addElementOption(optionsElement, VALUE_HAS_BRACES, table.isHasBraces());
        addElementOption(optionsElement, VALUE_HAS_BRACKETS, table.isHasBrackets());
        addElementOption(optionsElement, VALUE_HAS_PARENS, table.isHasParens());
        addElementOption(optionsElement, VALUE_HAS_STRING_ESCAPES, table.isHasStringEscapes());
        addElementOption(optionsElement, VALUE_LINE_COMMENT_AT_START, table.lineCommentOnlyAtStart);

        highlightingElement.addContent(optionsElement);

        writeKeywords(table.getKeywords1(), ELEMENT_KEYWORDS, highlightingElement).setAttribute(ATTRIBUTE_IGNORE_CASE, String.valueOf(table.isIgnoreCase()));
        writeKeywords(table.getKeywords2(), ELEMENT_KEYWORDS2, highlightingElement);
        writeKeywords(table.getKeywords3(), ELEMENT_KEYWORDS3, highlightingElement);
        writeKeywords(table.getKeywords4(), ELEMENT_KEYWORDS4, highlightingElement);

        element.addContent(highlightingElement);
    }

    private static void addElementOption(final Element optionsElement, final String valueHasParens, final boolean hasParens) {
        if (!hasParens) {
            return;
        }

        Element supportParens = new Element(ELEMENT_OPTION);
        supportParens.setAttribute(ATTRIBUTE_NAME, valueHasParens);
        supportParens.setAttribute(ATTRIBUTE_VALUE, String.valueOf(true));
        optionsElement.addContent(supportParens);
    }

    private static Element writeKeywords(Set<String> keywords, String tagName, Element highlightingElement) {
        if (keywords.size() == 0 && !ELEMENT_KEYWORDS.equals(tagName)) return null;
        Element keywordsElement = new Element(tagName);
        String[] strings = ArrayUtil.toStringArray(keywords);
        Arrays.sort(strings);
        StringBuilder keywordsAttribute = new StringBuilder();

        for (final String keyword : strings) {
            if (!keyword.contains(SEMICOLON)) {
                if (keywordsAttribute.length() != 0) keywordsAttribute.append(SEMICOLON);
                keywordsAttribute.append(keyword);
            } else {
                Element e = new Element(ELEMENT_KEYWORD);
                e.setAttribute(ATTRIBUTE_NAME, keyword);
                keywordsElement.addContent(e);
            }
        }
        if (keywordsAttribute.length() != 0) {
            keywordsElement.setAttribute(ELEMENT_KEYWORDS, keywordsAttribute.toString());
        }
        highlightingElement.addContent(keywordsElement);
        return keywordsElement;
    }

    @Override
    public void markDefaultSettings() {
        myDefaultSyntaxTable = mySyntaxTable;
    }

    @Override
    public boolean isModified() {
        return !Comparing.equal(myDefaultSyntaxTable, getSyntaxTable());
    }

     private static final String ELEMENT_MAPPING = "mapping";
     private static final String ATTRIBUTE_EXT = "ext";
     private static final String ATTRIBUTE_PATTERN = "pattern";
    /** Applied for removed mappings approved by user */
     private static final String ATTRIBUTE_APPROVED = "approved";

     private static final String ELEMENT_REMOVED_MAPPING = "removed_mapping";
     private static final String ATTRIBUTE_TYPE = "type";

    
    public static List<Pair<FileNameMatcher, String>> readAssociations( Element element) {
        List<Element> children = element.getChildren(ELEMENT_MAPPING);
        if (children.isEmpty()) {
            return Collections.emptyList();
        }

        List<Pair<FileNameMatcher, String>> result = new SmartList<Pair<FileNameMatcher, String>>();
        for (Element mapping : children) {
            String ext = mapping.getAttributeValue(ATTRIBUTE_EXT);
            String pattern = mapping.getAttributeValue(ATTRIBUTE_PATTERN);

            FileNameMatcher matcher = ext != null ? new ExtensionFileNameMatcher(ext) : FileTypeManager.parseFromString(pattern);
            result.add(Pair.create(matcher, mapping.getAttributeValue(ATTRIBUTE_TYPE)));
        }
        return result;
    }

    
    public static List<Trinity<FileNameMatcher, String, Boolean>> readRemovedAssociations( Element element) {
        List<Trinity<FileNameMatcher, String, Boolean>> result = new SmartList<Trinity<FileNameMatcher, String, Boolean>>();
        List<Element> children = element.getChildren(ELEMENT_REMOVED_MAPPING);
        if (children.isEmpty()) {
            return Collections.emptyList();
        }

        for (Element mapping : children) {
            String ext = mapping.getAttributeValue(ATTRIBUTE_EXT);
            FileNameMatcher matcher = ext == null ? FileTypeManager.parseFromString(mapping.getAttributeValue(ATTRIBUTE_PATTERN)) : new ExtensionFileNameMatcher(ext);
            result.add(Trinity.create(matcher, mapping.getAttributeValue(ATTRIBUTE_TYPE), Boolean.parseBoolean(mapping.getAttributeValue(ATTRIBUTE_APPROVED))));
        }
        return result;
    }

    public static Element writeMapping(String typeName,  FileNameMatcher matcher, boolean specifyTypeName) {
        Element mapping = new Element(ELEMENT_MAPPING);
        if (matcher instanceof ExtensionFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_EXT, ((ExtensionFileNameMatcher)matcher).getExtension());
        }
        else if (matcher instanceof WildcardFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_PATTERN, ((WildcardFileNameMatcher)matcher).getPattern());
        }
        else if (matcher instanceof ExactFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_PATTERN, ((ExactFileNameMatcher)matcher).getFileName());
        }
        else {
            return null;
        }

        if (specifyTypeName) {
            mapping.setAttribute(ATTRIBUTE_TYPE, typeName);
        }

        return mapping;
    }

    static Element writeRemovedMapping(final FileType type, final FileNameMatcher matcher, final boolean specifyTypeName, boolean approved) {
        Element mapping = new Element(ELEMENT_REMOVED_MAPPING);
        if (matcher instanceof ExtensionFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_EXT, ((ExtensionFileNameMatcher)matcher).getExtension());
            if (approved) {
                mapping.setAttribute(ATTRIBUTE_APPROVED, "true");
            }
        }
        else if (matcher instanceof WildcardFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_PATTERN, ((WildcardFileNameMatcher)matcher).getPattern());
        }
        else if (matcher instanceof ExactFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_PATTERN, ((ExactFileNameMatcher)matcher).getFileName());
        }
        else {
            return null;
        }
        if (specifyTypeName) {
            mapping.setAttribute(ATTRIBUTE_TYPE, type.getName());
        }

        return mapping;
    }

    @Override
    public SettingsEditor<AbstractFileType> getEditor() {
        return new CustomFileTypeEditor();
    }

    public void setCommenter(final Commenter commenter) {
        myCommenter = commenter;
    }

    @Override
    
    public ExternalInfo getExternalInfo() {
        return myExternalInfo;
    }
}
