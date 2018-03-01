/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import com.gome.maven.lang.xhtml.XHTMLLanguage;

import javax.swing.*;

public class XHtmlFileType extends HtmlFileType {
    public static final XHtmlFileType INSTANCE = new XHtmlFileType();

    private XHtmlFileType() {
        super(XHTMLLanguage.INSTANCE);
    }

    @Override
    
    public String getName() {
        return "XHTML";
    }

    @Override
    
    public String getDescription() {
        return IdeBundle.message("filetype.description.xhtml");
    }

    @Override
    
    public String getDefaultExtension() {
        return "xhtml";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Xhtml;
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }
}