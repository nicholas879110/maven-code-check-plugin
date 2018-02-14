package com.gome.maven.uiDesigner.lw;

import org.jdom.Element;
import com.gome.maven.uiDesigner.UIFormXmlConstants;

public final class LwIntroPrimitiveTypeProperty extends LwIntrospectedProperty {
    private final Class myValueClass;

    public LwIntroPrimitiveTypeProperty(final String name, final Class valueClass){
        super(name, valueClass.getName());
        myValueClass = valueClass;
    }

    public Object read(final Element element) throws Exception{
        return LwXmlReader.getRequiredPrimitiveTypeValue(element, UIFormXmlConstants.ATTRIBUTE_VALUE, myValueClass);
    }
}