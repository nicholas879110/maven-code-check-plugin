package com.gome.maven.codeInspection.htmlInspections;


/**
 * User: anna
 * Date: 16-Dec-2005
 */
public interface XmlEntitiesInspection {
     String BOOLEAN_ATTRIBUTE_SHORT_NAME = "HtmlUnknownBooleanAttribute";
     String ATTRIBUTE_SHORT_NAME = "HtmlUnknownAttribute";
     String TAG_SHORT_NAME = "HtmlUnknownTag";
     String REQUIRED_ATTRIBUTES_SHORT_NAME = "RequiredAttributes";

    String getAdditionalEntries();
    void addEntry(String text);
}
