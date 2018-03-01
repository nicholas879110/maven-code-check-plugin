package com.gome.maven.codeInsight;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiFile;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class ImportFilter {
    public static final ExtensionPointName<ImportFilter> EP_NAME = new ExtensionPointName<ImportFilter>("com.gome.maven.importFilter");

    public abstract boolean shouldUseFullyQualifiedName( PsiFile targetFile,  String classQualifiedName);

    public static boolean shouldImport( PsiFile targetFile,  String classQualifiedName) {
        for (ImportFilter filter : EP_NAME.getExtensions()) {
            if (filter.shouldUseFullyQualifiedName(targetFile, classQualifiedName)) {
                return false;
            }
        }
        return true;
    }
}
