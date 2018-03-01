package com.gome.maven.psi.xml;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.tree.IElementType;

public interface StartTagEndTokenProvider {
    ExtensionPointName<StartTagEndTokenProvider> EP_NAME = new ExtensionPointName<StartTagEndTokenProvider>("com.gome.maven.xml.startTagEndToken");

    IElementType[] getTypes();
}
