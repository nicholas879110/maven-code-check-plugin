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
package com.gome.maven.xml;

import com.gome.maven.codeInsight.editorActions.TypedHandler;
import com.gome.maven.ide.highlighter.DTDFileType;
import com.gome.maven.ide.highlighter.HtmlFileType;
import com.gome.maven.ide.highlighter.XHtmlFileType;
import com.gome.maven.ide.highlighter.XmlFileType;
import com.gome.maven.lang.xml.XMLLanguage;
import com.gome.maven.openapi.fileTypes.FileTypeConsumer;
import com.gome.maven.openapi.fileTypes.FileTypeFactory;

/**
 * @author yole
 */
public class XmlFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes( final FileTypeConsumer consumer) {
        consumer.consume(HtmlFileType.INSTANCE, "html;htm;sht;shtm;shtml");
        consumer.consume(XHtmlFileType.INSTANCE, "xhtml");
        consumer.consume(DTDFileType.INSTANCE, "dtd;ent;mod;elt");

        consumer.consume(XmlFileType.INSTANCE, "xml;xsd;tld;xsl;jnlp;wsdl;jhm;ant;xul;xslt;rng;fxml");
        TypedHandler.registerBaseLanguageQuoteHandler(XMLLanguage.class, TypedHandler.getQuoteHandlerForType(XmlFileType.INSTANCE));
    }
}
