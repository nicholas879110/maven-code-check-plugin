//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EmptyXMLResolver implements EntityResolver {
    public EmptyXMLResolver() {
    }

    public InputSource resolveEntity(String arg0, String arg1) throws SAXException, IOException {
        return new InputSource(new ByteArrayInputStream("".getBytes()));
    }
}
