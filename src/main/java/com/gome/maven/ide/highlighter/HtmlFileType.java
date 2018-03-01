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
package com.gome.maven.ide.highlighter;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.html.HTMLLanguage;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.text.XmlCharsetDetector;
import com.gome.maven.xml.util.HtmlUtil;

import javax.swing.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class HtmlFileType extends XmlLikeFileType {
     public static final String DOT_DEFAULT_EXTENSION = ".html";

    public static final HtmlFileType INSTANCE = new HtmlFileType();

    private HtmlFileType() {
        super(HTMLLanguage.INSTANCE);
    }

    HtmlFileType(Language language) {
        super(language);
    }

    @Override
    
    public String getName() {
        return "HTML";
    }

    @Override
    
    public String getDescription() {
        return IdeBundle.message("filetype.description.html");
    }

    @Override
    
    public String getDefaultExtension() {
        return "html";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Html;
    }

    @Override
    public String getCharset( final VirtualFile file,  final byte[] content) {
        String charset = XmlCharsetDetector.extractXmlEncodingFromProlog(content);
        if (charset != null) return charset;
         String strContent;
        try {
            strContent = new String(content, "ISO-8859-1");
        }
        catch (UnsupportedEncodingException e) {
            return null;
        }
        Charset c = HtmlUtil.detectCharsetFromMetaTag(strContent);
        return c == null ? null : c.name();
    }

    @Override
    public Charset extractCharsetFromFileContent( final Project project,  final VirtualFile file,  final CharSequence content) {
        String name = XmlCharsetDetector.extractXmlEncodingFromProlog(content);
        Charset charset = CharsetToolkit.forName(name);

        if (charset != null) {
            return charset;
        }
        return HtmlUtil.detectCharsetFromMetaTag(content);
    }
}
