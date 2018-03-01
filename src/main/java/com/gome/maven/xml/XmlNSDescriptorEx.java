package com.gome.maven.xml;

/**
 * @author Eugene.Kudelevsky
 */
public interface XmlNSDescriptorEx extends  XmlNSDescriptor {
    XmlElementDescriptor getElementDescriptor(String localName, String namespace);
}
