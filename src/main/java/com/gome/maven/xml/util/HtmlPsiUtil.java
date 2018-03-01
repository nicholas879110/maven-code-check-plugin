package com.gome.maven.xml.util;

import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.templateLanguages.TemplateLanguageUtil;
import com.gome.maven.psi.xml.XmlDocument;
import com.gome.maven.psi.xml.XmlFile;

public class HtmlPsiUtil {
    
    public static XmlDocument getRealXmlDocument( XmlDocument doc) {
        if (doc == null) return null;
        final PsiFile containingFile = doc.getContainingFile();

        final PsiFile templateFile = TemplateLanguageUtil.getTemplateFile(containingFile);
        if (templateFile instanceof XmlFile) {
            return ((XmlFile)templateFile).getDocument();
        }
        return doc;
    }
}
