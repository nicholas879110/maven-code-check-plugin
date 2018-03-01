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
package com.gome.maven.patterns;

import com.gome.maven.psi.xml.XmlDocument;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.ProcessingContext;

/**
 * @author spleaner
 */
public class XmlFilePattern<Self extends XmlFilePattern<Self>> extends PsiFilePattern<XmlFile, Self>{

    public XmlFilePattern() {
        super(XmlFile.class);
    }

    protected XmlFilePattern( final InitialPatternCondition<XmlFile> condition) {
        super(condition);
    }

    public Self withRootTag(final ElementPattern<XmlTag> rootTag) {
        return with(new PatternCondition<XmlFile>("withRootTag") {
            @Override
            public boolean accepts( final XmlFile xmlFile, final ProcessingContext context) {
                XmlDocument document = xmlFile.getDocument();
                return document != null && rootTag.accepts(document.getRootTag(), context);
            }
        });
    }

    public static class Capture extends XmlFilePattern<Capture> {
    }
}