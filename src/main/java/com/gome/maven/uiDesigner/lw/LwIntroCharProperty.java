package com.gome.maven.uiDesigner.lw;

import org.jdom.Element;
import com.gome.maven.uiDesigner.UIFormXmlConstants;

public final class LwIntroCharProperty extends LwIntrospectedProperty {
    public LwIntroCharProperty(final String name){
        super(name, Character.class.getName());
    }

    public Object read(final Element element) throws Exception{
        return Character.valueOf(LwXmlReader.getRequiredString(element, UIFormXmlConstants.ATTRIBUTE_VALUE).charAt(0));
    }
}