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
package com.gome.maven.util.xml;

import com.gome.maven.openapi.util.Comparing;

/**
 * @author peter
 */
public class XmlName implements Comparable<XmlName> {
    private final String myLocalName;
    private final String myNamespaceKey;

    private final int myHashCode;

    public XmlName(  final String localName) {
        this(localName, null);
    }

    public XmlName(  final String localName,  final String namespaceKey) {
        myLocalName = localName;
        myNamespaceKey = namespaceKey;

        myHashCode = 31 * myLocalName.hashCode() + (myNamespaceKey != null ? myNamespaceKey.hashCode() : 0);
    }

    
    public final String getLocalName() {
        return myLocalName;
    }

    
    public final String getNamespaceKey() {
        return myNamespaceKey;
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final XmlName xmlName = (XmlName)o;

        if (!myLocalName.equals(xmlName.myLocalName)) return false;
        if (Comparing.equal(myNamespaceKey, xmlName.myNamespaceKey)) return true;

        if (myNamespaceKey != null ? !myNamespaceKey.equals(xmlName.myNamespaceKey) : xmlName.myNamespaceKey != null) return false;

        return true;
    }

    public int hashCode() {
        return myHashCode;
    }


    @Override
    public int compareTo(XmlName o) {
        final int i = myLocalName.compareTo(o.myLocalName);
        if (i != 0) {
            return i;
        }
        return Comparing.compare(myNamespaceKey, o.myNamespaceKey);
    }

    public String toString() {
        return myNamespaceKey + " : " + myLocalName;
    }
}
