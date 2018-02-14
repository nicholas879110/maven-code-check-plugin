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
package com.gome.maven.ide.browsers;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.html.HTMLLanguage;
import com.gome.maven.lang.xhtml.XHTMLLanguage;
import com.gome.maven.lang.xml.XMLLanguage;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.Url;

import java.util.Collection;
import java.util.Collections;

public abstract class WebBrowserService {
    public static WebBrowserService getInstance() {
        return ServiceManager.getService(WebBrowserService.class);
    }

    
    public abstract Collection<Url> getUrlsToOpen( OpenInBrowserRequest request, boolean preferLocalUrl) throws WebBrowserUrlProvider.BrowserException;

    @SuppressWarnings("unused")
    
    @Deprecated
    public Collection<Url> getUrlsToOpen( final PsiElement element, boolean preferLocalUrl) throws WebBrowserUrlProvider.BrowserException {
        OpenInBrowserRequest request = OpenInBrowserRequest.create(element);
        return request == null ? Collections.<Url>emptyList() : getUrlsToOpen(request, preferLocalUrl);
    }

    public static boolean isHtmlOrXmlFile( Language language) {
        return language == HTMLLanguage.INSTANCE || language == XHTMLLanguage.INSTANCE || language == XMLLanguage.INSTANCE;
    }
}