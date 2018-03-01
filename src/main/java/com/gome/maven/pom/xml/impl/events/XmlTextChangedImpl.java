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
package com.gome.maven.pom.xml.impl.events;

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.pom.PomModel;
import com.gome.maven.pom.event.PomModelEvent;
import com.gome.maven.pom.xml.XmlAspect;
import com.gome.maven.pom.xml.events.XmlTextChanged;
import com.gome.maven.pom.xml.impl.XmlAspectChangeSetImpl;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlText;

public class XmlTextChangedImpl implements XmlTextChanged {
    private final String myOldText;
    private final XmlText myText;
    public XmlTextChangedImpl(XmlText xmlText, String oldText) {
        myOldText = oldText;
        myText = xmlText;
    }

    @Override
    public String getOldText() {
        return myOldText;
    }

    @Override
    public XmlText getText() {
        return myText;
    }

    public static PomModelEvent createXmlTextChanged(PomModel source, XmlText xmlText, String oldText) {
        final PomModelEvent event = new PomModelEvent(source);
        final XmlAspectChangeSetImpl xmlAspectChangeSet = new XmlAspectChangeSetImpl(source, PsiTreeUtil.getParentOfType(xmlText, XmlFile.class));
        xmlAspectChangeSet.add(new XmlTextChangedImpl(xmlText, oldText));
        event.registerChangeSet(source.getModelAspect(XmlAspect.class), xmlAspectChangeSet);
        return event;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
        return "text changed to '" + StringUtil.escapeStringCharacters(myText.getValue()) + "' was: '"
                + StringUtil.escapeStringCharacters(myOldText) + "'";
    }
}
