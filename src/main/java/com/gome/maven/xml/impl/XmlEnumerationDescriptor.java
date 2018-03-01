package com.gome.maven.xml.impl;

import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.xml.util.XmlEnumeratedValueReference;

/**
 * @author Dmitry Avdeev
 *         Date: 22.08.13
 */
public abstract class XmlEnumerationDescriptor<T extends XmlElement> {

    public abstract boolean isFixed();

    public abstract String getDefaultValue();

    public abstract boolean isEnumerated( XmlElement context);

    public abstract String[] getEnumeratedValues();

    public String[] getValuesForCompletion() {
        return StringUtil.filterEmptyStrings(getEnumeratedValues());
    }

    public PsiElement getValueDeclaration(XmlElement attributeValue, String value) {
        String defaultValue = getDefaultValue();
        if (Comparing.equal(defaultValue, value)) {
            return getDefaultValueDeclaration();
        }
        return isFixed() ? null : getEnumeratedValueDeclaration(attributeValue, value);
    }

    protected abstract PsiElement getEnumeratedValueDeclaration(XmlElement value, String s);

    protected abstract PsiElement getDefaultValueDeclaration();

    public PsiReference[] getValueReferences(T element,  String text) {
        return new PsiReference[] { new XmlEnumeratedValueReference(element, this)};
    }
}
