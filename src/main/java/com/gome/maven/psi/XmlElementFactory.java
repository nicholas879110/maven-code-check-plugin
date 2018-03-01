/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.psi;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.xml.XmlAttribute;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.psi.xml.XmlText;
import com.gome.maven.util.IncorrectOperationException;

/**
 * @author Dmitry Avdeev
 */
public abstract class XmlElementFactory {

    public static XmlElementFactory getInstance(Project project) {
        return ServiceManager.getService(project, XmlElementFactory.class);
    }
    /**
     * Creates an XML text element from the specified string, escaping the special
     * characters in the string as necessary.
     *
     * @param s the text of the element to create.
     * @return the created element.
     * @throws com.gome.maven.util.IncorrectOperationException if the creation failed for some reason.
     */
    
    public abstract XmlText createDisplayText(  String s) throws IncorrectOperationException;

    /**
     * Creates an XHTML tag with the specified text.
     *
     * @param s the text of an XHTML tag (which can contain attributes and subtags).
     * @return the created tag instance.
     * @throws IncorrectOperationException if the text does not specify a valid XML fragment.
     */
    
    public abstract XmlTag createXHTMLTagFromText(  String s) throws IncorrectOperationException;

    /**
     * Creates an HTML tag with the specified text.
     *
     * @param s the text of an HTML tag (which can contain attributes and subtags).
     * @return the created tag instance.
     * @throws IncorrectOperationException if the text does not specify a valid XML fragment.
     */
    
    public abstract XmlTag createHTMLTagFromText(  String s) throws IncorrectOperationException;

    /**
     * Creates an XML tag with the specified text.
     *
     * @param text the text of an XML tag (which can contain attributes and subtags).
     * @return the created tag instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid XML fragment.
     * @see #createTagFromText(CharSequence text, Language language)
     */
    
    public abstract XmlTag createTagFromText(  CharSequence text) throws IncorrectOperationException;

    /**
     * Creates XML like tag with the specified text and language.
     *
     * @param text the text of an XML tag (which can contain attributes and subtags).
     * @param language the language for tag to be created.
     * @return the created tag instance.
     * @throws com.gome.maven.util.IncorrectOperationException if the text does not specify a valid XML fragment.
     * @see #createTagFromText(CharSequence)
     */
    
    public abstract XmlTag createTagFromText(  CharSequence text,  Language language) throws IncorrectOperationException;

    /**
     * Creates an XML attribute with the specified name and value.
     *
     * @param name  the name of the attribute to create.
     * @param value the value of the attribute to create.
     * @return the created attribute instance.
     * @throws IncorrectOperationException if either <code>name</code> or <code>value</code> are not valid.
     */
    
    public abstract XmlAttribute createXmlAttribute(  String name,  String value) throws IncorrectOperationException;
}
